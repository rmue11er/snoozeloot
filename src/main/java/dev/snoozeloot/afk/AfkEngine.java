package dev.snoozeloot.afk;

import dev.snoozeloot.bonus.BonusService;
import dev.snoozeloot.config.ConfigService;
import dev.snoozeloot.integration.WorldGuardHook;
import dev.snoozeloot.meta.PlayerMeta;
import dev.snoozeloot.meta.PlayerMetaStore;
import dev.snoozeloot.points.MultiplierCalculator;
import dev.snoozeloot.points.PointsService;
import dev.snoozeloot.util.BukkitSounds;
import dev.snoozeloot.util.DateFormats;
import java.time.Duration;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class AfkEngine implements Listener {
  private final JavaPlugin plugin;
  private final ConfigService config;
  private final PointsService points;
  private final PlayerMetaStore metaStore;
  private BonusService bonusService;
  private final WorldGuardHook worldGuard;

  private final Map<UUID, State> state = new HashMap<>();

  private BukkitTask idleTask;
  private BukkitTask payoutTask;
  private BukkitTask actionbarTask;
  private BukkitTask particleTask;

  public AfkEngine(
      JavaPlugin plugin,
      ConfigService config,
      PointsService points,
      PlayerMetaStore metaStore,
      BonusService bonusService,
      WorldGuardHook worldGuard) {
    this.plugin = plugin;
    this.config = config;
    this.points = points;
    this.metaStore = metaStore;
    this.bonusService = bonusService;
    this.worldGuard = worldGuard;
  }

  public void start() {
    stop();

    var afk = config.afk();
    idleTask =
        Bukkit.getScheduler()
            .runTaskTimer(plugin, this::idleCheckTick, 20L, afk.idleCheckIntervalSeconds() * 20L);
    payoutTask =
        Bukkit.getScheduler()
            .runTaskTimer(plugin, this::payoutTick, 20L, afk.payoutIntervalSeconds() * 20L);
    actionbarTask =
        Bukkit.getScheduler()
            .runTaskTimer(plugin, this::actionbarTick, 20L, afk.actionbarIntervalTicks());

    if (afk.particlesEnabled()) {
      particleTask =
          Bukkit.getScheduler()
              .runTaskTimer(plugin, this::particleTick, 20L, afk.particlesIntervalTicks());
    }
  }

  public void stop() {
    if (idleTask != null) idleTask.cancel();
    if (payoutTask != null) payoutTask.cancel();
    if (actionbarTask != null) actionbarTask.cancel();
    if (particleTask != null) particleTask.cancel();
    idleTask = payoutTask = actionbarTask = particleTask = null;
  }

  public void reload(BonusService bonusService) {
    this.bonusService = bonusService;
    start();
  }

  public void reload() {
    start();
  }

  public boolean isAfk(Player player) {
    return player != null && getState(player.getUniqueId()).afk;
  }

  public boolean isAfk(OfflinePlayer player) {
    if (player instanceof Player online) {
      return isAfk(online);
    }
    return false;
  }

  private State getState(UUID id) {
    return state.computeIfAbsent(id, k -> new State());
  }

  private void idleCheckTick() {
    long now = System.currentTimeMillis();
    var afk = config.afk();
    long thresholdMs = Duration.ofSeconds(afk.afkTimeThresholdSeconds()).toMillis();
    long intervalSeconds = Math.max(1L, afk.idleCheckIntervalSeconds());
    String today = DateFormats.todayUtc();

    for (Player player : Bukkit.getOnlinePlayers()) {
      State s = getState(player.getUniqueId());

      if (!s.afk) {
        trackActivePlay(player.getUniqueId(), intervalSeconds, today);
      }

      if (s.afk && !canRemainAfk(player, today)) {
        s.afk = false;
        onAfkExit(player, s);
        continue;
      }

      long idleFor = now - s.lastTrustedInputAtMs;
      boolean shouldAfk = idleFor >= thresholdMs;

      if (shouldAfk && !s.afk) {
        if (!canEnterAfk(player, today)) {
          continue;
        }
        s.afk = true;
        s.sessionPointsEarned = 0;
        s.lastPayoutAtMs = 0;
        resetSessionAfk(player.getUniqueId());
        onAfkEnter(player);
      } else if (!shouldAfk && s.afk) {
        s.afk = false;
        onAfkExit(player, s);
      }
    }
  }

  private void payoutTick() {
    long now = System.currentTimeMillis();
    var afk = config.afk();
    long payoutSeconds = Math.max(1L, afk.payoutIntervalSeconds());
    String today = DateFormats.todayUtc();

    for (Player player : Bukkit.getOnlinePlayers()) {
      State s = getState(player.getUniqueId());
      if (!s.afk) continue;

      if (!canRemainAfk(player, today)) {
        s.afk = false;
        onAfkExit(player, s);
        continue;
      }

      double multiplier = getMultiplier(player);
      int delta = (int) Math.max(0, Math.floor(afk.pointsPerInterval() * multiplier));
      if (delta <= 0) continue;

      points.add(player.getUniqueId(), delta, player.getName());
      s.sessionPointsEarned += delta;
      s.lastPayoutAtMs = now;
      trackAfkSeconds(player.getUniqueId(), payoutSeconds, today);
    }
  }

  private void actionbarTick() {
    var afk = config.afk();
    for (Player player : Bukkit.getOnlinePlayers()) {
      State s = getState(player.getUniqueId());
      if (!s.afk) continue;

      int currentPoints = points.get(player.getUniqueId());
      double multiplier = getMultiplier(player);
      double perMin = (afk.pointsPerInterval() / afk.payoutIntervalSeconds()) * 60.0 * multiplier;
      String rate = String.format(java.util.Locale.ROOT, "%.2f", perMin);

      Component c =
          config
              .messages()
              .component(
                  "actionbar-afk",
                  Map.of("points", Integer.toString(currentPoints), "rate", rate));
      player.sendActionBar(c);
    }
  }

  private void particleTick() {
    for (Player player : Bukkit.getOnlinePlayers()) {
      State s = getState(player.getUniqueId());
      if (!s.afk) continue;

      Location eye = player.getEyeLocation();
      Location loc = eye.clone().add(0, 0.6, 0);
      player.getWorld().spawnParticle(Particle.CLOUD, loc, 2, 0.15, 0.15, 0.15, 0.0);
      player.getWorld().spawnParticle(Particle.WHITE_ASH, loc, 1, 0.10, 0.20, 0.10, 0.0);
    }
  }

  private void onAfkEnter(Player player) {
    var afk = config.afk();
    claimBonuses(player);

    double multiplier = getMultiplier(player);
    double perMin = (afk.pointsPerInterval() / afk.payoutIntervalSeconds()) * 60.0 * multiplier;
    String rate = String.format(java.util.Locale.ROOT, "%.2f", perMin);

    config.messages().send(player, "afk-start", Map.of("rate", rate));

    if (afk.startTitleEnabled()) {
      Component title = config.messages().miniMessage(afk.startTitle(), Map.of("rate", rate));
      Component subtitle = config.messages().miniMessage(afk.startSubtitle(), Map.of("rate", rate));
      player.showTitle(
          Title.title(title, subtitle, Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(2), Duration.ofMillis(500))));
    }

    if (afk.startSoundEnabled()) {
      Sound sound = BukkitSounds.parse(afk.startSoundName(), Sound.BLOCK_NOTE_BLOCK_CHIME);
      player.playSound(
          player.getLocation(), sound, afk.startSoundVolume(), afk.startSoundPitch());
    }
  }

  private void claimBonuses(Player player) {
    UUID id = player.getUniqueId();
    var bonuses = config.bonuses();

    if (bonuses.daily().enabled()) {
      BonusService.BonusResult daily = bonusService.claimDailyBonus(id);
      if (daily.claimed() && daily.amount() > 0) {
        points.add(id, daily.amount(), player.getName());
        config
            .messages()
            .send(player, "daily-bonus", Map.of("points", Integer.toString(daily.amount())));
      }
    }

    if (bonuses.weekly().enabled()) {
      BonusService.BonusResult weekly = bonusService.claimWeeklyBonus(id);
      if (weekly.claimed() && weekly.amount() > 0) {
        points.add(id, weekly.amount(), player.getName());
        config
            .messages()
            .send(player, "weekly-bonus", Map.of("points", Integer.toString(weekly.amount())));
      }
    }
  }

  private void onAfkExit(Player player, State s) {
    if (s.sessionPointsEarned > 0) {
      config
          .messages()
          .send(
              player,
              "afk-welcome-back",
              Map.of("session_points", Integer.toString(s.sessionPointsEarned)));
    }

    var afk = config.afk();
    if (afk.endSoundEnabled()) {
      Sound sound = BukkitSounds.parse(afk.endSoundName(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP);
      player.playSound(
          player.getLocation(), sound, afk.endSoundVolume(), afk.endSoundPitch());
    }
  }

  private boolean canEnterAfk(Player player, String today) {
    var afk = config.afk();
    PlayerMeta meta = metaStore.get(player.getUniqueId());

    if (!AfkEligibility.allowedWorld(player, afk.allowedWorlds())) {
      config.messages().send(player, "afk-blocked-world", Map.of());
      return false;
    }
    if (AfkEligibility.blockedGameMode(player, afk.blockedGamemodes())) {
      config.messages().send(player, "afk-blocked-gamemode", Map.of());
      return false;
    }
    if (!AfkEligibility.allowedRegion(player, afk.allowedRegions(), worldGuard)) {
      config.messages().send(player, "afk-blocked-region", Map.of());
      return false;
    }
    if (!AfkEligibility.minActiveSeconds(meta, afk.minActiveSecondsBeforeAfk())) {
      return false;
    }
    if (!AfkEligibility.maxDailyAfkSeconds(meta, today, afk.maxAfkSecondsPerDay())) {
      config.messages().send(player, "afk-limit-daily", Map.of());
      return false;
    }
    if (!AfkEligibility.maxSessionAfkSeconds(meta, afk.maxAfkSecondsPerSession())) {
      config.messages().send(player, "afk-limit-session", Map.of());
      return false;
    }
    return true;
  }

  private boolean canRemainAfk(Player player, String today) {
    var afk = config.afk();
    PlayerMeta meta = metaStore.get(player.getUniqueId());

    if (!AfkEligibility.allowedWorld(player, afk.allowedWorlds())) {
      return false;
    }
    if (AfkEligibility.blockedGameMode(player, afk.blockedGamemodes())) {
      return false;
    }
    if (!AfkEligibility.allowedRegion(player, afk.allowedRegions(), worldGuard)) {
      return false;
    }
    if (!AfkEligibility.maxDailyAfkSeconds(meta, today, afk.maxAfkSecondsPerDay())) {
      config.messages().send(player, "afk-limit-daily", Map.of());
      return false;
    }
    if (!AfkEligibility.maxSessionAfkSeconds(meta, afk.maxAfkSecondsPerSession())) {
      config.messages().send(player, "afk-limit-session", Map.of());
      return false;
    }
    return true;
  }

  private void trackActivePlay(UUID playerId, long seconds, String today) {
    metaStore.update(
        playerId,
        meta -> {
          long active = meta.activePlaySeconds();
          if (meta.afkDay() != null && !DateFormats.isSameDay(meta.afkDay(), today)) {
            active = 0L;
          }
          return meta.withActivePlaySeconds(active + seconds);
        });
  }

  private void trackAfkSeconds(UUID playerId, long seconds, String today) {
    metaStore.update(
        playerId,
        meta -> {
          long daily =
              DateFormats.isSameDay(meta.afkDay(), today) ? meta.afkSecondsToday() : 0L;
          return meta
              .withAfkSecondsToday(daily + seconds, today)
              .withSessionAfkSeconds(meta.sessionAfkSeconds() + seconds);
        });
  }

  private void resetSessionAfk(UUID playerId) {
    metaStore.update(playerId, meta -> meta.withSessionAfkSeconds(0L));
  }

  private double getMultiplier(Player player) {
    double permissionMultiplier =
        MultiplierCalculator.highest(
            config.multipliers().permissionToMultiplier(), player::hasPermission);

    PlayerMeta meta = metaStore.get(player.getUniqueId());
    double streakMultiplier =
        bonusService.streakEligible(meta) ? bonusService.streakMultiplier(meta) : 1.0;

    return permissionMultiplier * streakMultiplier;
  }

  private void registerActivity(UUID id) {
    State s = getState(id);
    s.lastTrustedInputAtMs = System.currentTimeMillis();
    s.recentPositions.clear();

    if (s.afk) {
      s.afk = false;
      Player player = Bukkit.getPlayer(id);
      if (player != null) {
        onAfkExit(player, s);
      }
    }
  }

  private boolean movementCountsAsActivity(Player player, State s) {
    long now = System.currentTimeMillis();
    long graceMs =
        Duration.ofSeconds(config.antiExploit().untrustedMovementInputGraceSeconds()).toMillis();
    boolean hasRecentInput = (now - s.lastTrustedInputAtMs) <= graceMs;

    return ActivityDetector.movementCountsAsActivity(
        hasRecentInput,
        player.getVehicle() != null,
        player.isInWater(),
        isOnRails(player),
        config.antiExploit());
  }

  private boolean isOnRails(Player player) {
    Block b = player.getLocation().subtract(0, 0.1, 0).getBlock();
    String type = b.getType().name();
    return type.endsWith("_RAIL") || type.equals("RAIL");
  }

  private boolean isCircularPoolMovement(Player player, State s, Location to) {
    var afk = config.afk();
    if (!afk.poolDetectionEnabled()) {
      return false;
    }

    s.recentPositions.addLast(new AfkPoolDetector.Position(to.getX(), to.getZ()));
    int maxSamples = Math.max(4, afk.poolDetectionSamples());
    while (s.recentPositions.size() > maxSamples) {
      s.recentPositions.removeFirst();
    }

    return AfkPoolDetector.isCircularPool(new ArrayList<>(s.recentPositions), maxSamples);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onJoin(PlayerJoinEvent e) {
    Player player = e.getPlayer();
    points.rememberName(player.getUniqueId(), player.getName());

    State s = getState(player.getUniqueId());
    s.lastTrustedInputAtMs = System.currentTimeMillis();
    s.lastLoc = player.getLocation().clone();
    s.lastYaw = player.getLocation().getYaw();
    s.lastPitch = player.getLocation().getPitch();
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onQuit(PlayerQuitEvent e) {
    state.remove(e.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onChat(AsyncPlayerChatEvent e) {
    UUID id = e.getPlayer().getUniqueId();
    Bukkit.getScheduler().runTask(plugin, () -> registerActivity(id));
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onCommand(PlayerCommandPreprocessEvent e) {
    registerActivity(e.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onAnimation(PlayerAnimationEvent e) {
    registerActivity(e.getPlayer().getUniqueId());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onInteract(PlayerInteractEvent e) {
    if (e.getHand() == EquipmentSlot.OFF_HAND) return;
    Action action = e.getAction();
    if (action == Action.LEFT_CLICK_AIR
        || action == Action.LEFT_CLICK_BLOCK
        || action == Action.RIGHT_CLICK_AIR
        || action == Action.RIGHT_CLICK_BLOCK) {
      registerActivity(e.getPlayer().getUniqueId());
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onMove(PlayerMoveEvent e) {
    Player player = e.getPlayer();
    State s = getState(player.getUniqueId());

    Location from = e.getFrom();
    Location to = e.getTo();
    if (to == null) return;

    boolean moved =
        from.getWorld() != to.getWorld()
            || from.distanceSquared(to) > 0.0001
            || Math.abs(from.getYaw() - to.getYaw()) > 0.01
            || Math.abs(from.getPitch() - to.getPitch()) > 0.01;

    if (!moved) return;

    if (isCircularPoolMovement(player, s, to)) {
      s.lastLoc = to.clone();
      s.lastYaw = to.getYaw();
      s.lastPitch = to.getPitch();
      return;
    }

    if (movementCountsAsActivity(player, s)) {
      registerActivity(player.getUniqueId());
    }

    s.lastLoc = to.clone();
    s.lastYaw = to.getYaw();
    s.lastPitch = to.getPitch();
  }

  public void debugDump(CommandSender sender) {
    config.messages().send(sender, "debug-header", Map.of());
    var afk = config.afk();
    config
        .messages()
        .send(
            sender,
            "debug-line",
            Map.of(
                "line",
                "threshold="
                    + afk.afkTimeThresholdSeconds()
                    + "s payout="
                    + afk.payoutIntervalSeconds()
                    + "s tracked="
                    + state.size()));
    for (Player player : Bukkit.getOnlinePlayers()) {
      State s = getState(player.getUniqueId());
      long idleSeconds =
          Math.max(0, (System.currentTimeMillis() - s.lastTrustedInputAtMs) / 1000L);
      PlayerMeta meta = metaStore.get(player.getUniqueId());
      config
          .messages()
          .send(
              sender,
              "debug-player",
              Map.of(
                  "player",
                  player.getName(),
                  "afk",
                  Boolean.toString(s.afk),
                  "idle",
                  Long.toString(idleSeconds),
                  "session",
                  Integer.toString(s.sessionPointsEarned),
                  "streak",
                  Integer.toString(meta.streakDays())));
    }
  }

  private static final class State {
    boolean afk = false;

    long lastTrustedInputAtMs = System.currentTimeMillis();
    long lastPayoutAtMs = 0;

    int sessionPointsEarned = 0;

    Location lastLoc;
    float lastYaw;
    float lastPitch;

    final Deque<AfkPoolDetector.Position> recentPositions = new ArrayDeque<>();
  }
}
