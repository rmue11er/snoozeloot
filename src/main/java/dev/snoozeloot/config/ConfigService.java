package dev.snoozeloot.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigService {
  private final JavaPlugin plugin;

  public ConfigService(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public void reload() {
    ConfigMerger.mergeMissingDefaults(plugin);
    plugin.reloadConfig();
  }

  public StorageConfig storage() {
    String type = plugin.getConfig().getString("storage.type", "yaml");
    return new StorageConfig(type == null ? "yaml" : type.toLowerCase(java.util.Locale.ROOT));
  }

  public String language() {
    return plugin.getConfig().getString("language", "en");
  }

  public AfkConfig afk() {
    var c = plugin.getConfig();
    var pool = c.getConfigurationSection("afk.pool-detection");
    return new AfkConfig(
        c.getLong("afk.idle-check-interval-seconds", 10),
        c.getLong("afk.afk-time-threshold-seconds", 300),
        c.getLong("afk.payout-interval-seconds", 60),
        c.getDouble("afk.points-per-interval", 1.0),
        c.getLong("afk.actionbar-interval-ticks", 40),
        c.getStringList("afk.allowed-worlds"),
        c.getStringList("afk.allowed-regions"),
        parseGameModes(c.getStringList("afk.blocked-gamemodes")),
        c.getLong("afk.min-active-seconds-before-afk", 0),
        c.getLong("afk.max-afk-seconds-per-day", -1),
        c.getLong("afk.max-afk-seconds-per-session", -1),
        pool != null && pool.getBoolean("enabled", false),
        pool == null ? 5 : pool.getInt("samples", 5),
        c.getBoolean("afk.particles.enabled", true),
        c.getLong("afk.particles.interval-ticks", 40),
        c.getBoolean("afk.start-notification.title-enabled", true),
        c.getString("afk.start-notification.title", "<gold>Snoozing...</gold>"),
        c.getString("afk.start-notification.subtitle", "<gray>Earning SnoozePoints</gray>"),
        c.getBoolean("afk.start-notification.sound-enabled", true),
        c.getString("afk.start-notification.sound", "BLOCK_NOTE_BLOCK_CHIME"),
        (float) c.getDouble("afk.start-notification.volume", 0.7),
        (float) c.getDouble("afk.start-notification.pitch", 1.0),
        c.getBoolean("afk.end-notification.sound-enabled", true),
        c.getString("afk.end-notification.sound", "ENTITY_EXPERIENCE_ORB_PICKUP"),
        (float) c.getDouble("afk.end-notification.volume", 0.6),
        (float) c.getDouble("afk.end-notification.pitch", 1.2));
  }

  public AntiExploitConfig antiExploit() {
    var c = plugin.getConfig();
    return new AntiExploitConfig(
        c.getLong("anti-exploit.untrusted-movement-input-grace-seconds", 4),
        c.getBoolean("anti-exploit.ignore-movement-when-in-water", true),
        c.getBoolean("anti-exploit.ignore-movement-when-in-vehicle", true),
        c.getBoolean("anti-exploit.ignore-movement-when-on-rails", true));
  }

  public BonusesConfig bonuses() {
    var c = plugin.getConfig();
    return new BonusesConfig(
        new DailyBonusConfig(
            c.getBoolean("bonuses.daily.enabled", false),
            c.getInt("bonuses.daily.points", 0)),
        new WeeklyBonusConfig(
            c.getBoolean("bonuses.weekly.enabled", false),
            c.getInt("bonuses.weekly.points", 0)),
        new StreakBonusConfig(
            c.getBoolean("bonuses.streak.enabled", false),
            c.getInt("bonuses.streak.min-minutes-per-day", 30),
            c.getDouble("bonuses.streak.multiplier-per-day", 0.05),
            c.getDouble("bonuses.streak.max-multiplier", 1.5)));
  }

  public PayConfig pay() {
    var c = plugin.getConfig();
    return new PayConfig(
        c.getBoolean("pay.enabled", true),
        c.getInt("pay.min-amount", 1),
        c.getLong("pay.cooldown-seconds", 30));
  }

  public Messages messages() {
    return new Messages(plugin);
  }

  public ShopConfig shop() {
    var c = plugin.getConfig();
    int size = c.getInt("shop.size", 27);
    int confirmAbovePrice = c.getInt("shop.confirm-above-price", -1);
    int transactionLogMaxEntries = c.getInt("shop.transaction-log-max-entries", 500);
    var itemsSection = c.getConfigurationSection("shop.items");
    Map<Integer, ShopItem> bySlot = new HashMap<>();

    if (itemsSection != null) {
      for (String key : itemsSection.getKeys(false)) {
        var s = itemsSection.getConfigurationSection(key);
        if (s == null) continue;
        int slot = s.getInt("slot", -1);
        if (slot < 0 || slot >= size) continue;

        String materialName = s.getString("material", "STONE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.STONE;

        String name = s.getString("name", key);
        var lore = s.getStringList("lore");
        int price = s.getInt("price", 0);
        int purchaseLimit = s.getInt("purchase-limit", -1);
        var commands = s.getStringList("commands");

        bySlot.put(
            slot,
            new ShopItem(key, slot, material, name, lore, price, purchaseLimit, commands));
      }
    }

    return new ShopConfig(size, confirmAbovePrice, transactionLogMaxEntries, bySlot);
  }

  public MultiplierConfig multipliers() {
    var section = plugin.getConfig().getConfigurationSection("multipliers");
    Map<String, Double> map = new HashMap<>();
    if (section != null) {
      Set<String> keys = section.getKeys(false);
      for (String perm : keys) {
        map.put(perm, section.getDouble(perm, 1.0));
      }
    }
    return new MultiplierConfig(map);
  }

  public StatsConfig stats() {
    return new StatsConfig(plugin.getConfig().getInt("stats.top-size", 10));
  }

  public UpdateCheckerConfig updateChecker() {
    var c = plugin.getConfig();
    return new UpdateCheckerConfig(
        c.getBoolean("update-checker.enabled", true),
        c.getString("update-checker.version-url", ""),
        c.getString("update-checker.notify-permission", "snoozeloot.admin"));
  }

  public BStatsConfig bstats() {
    return new BStatsConfig(plugin.getConfig().getBoolean("bstats.enabled", true));
  }

  private static List<GameMode> parseGameModes(List<String> names) {
    if (names == null || names.isEmpty()) {
      return List.of();
    }
    var modes = new java.util.ArrayList<GameMode>();
    for (String name : names) {
      if (name == null || name.isBlank()) continue;
      try {
        modes.add(GameMode.valueOf(name.trim().toUpperCase(java.util.Locale.ROOT)));
      } catch (IllegalArgumentException ignored) {
        // skip unknown game modes
      }
    }
    return List.copyOf(modes);
  }

  public record StorageConfig(String type) {
    public boolean isSqlite() {
      return "sqlite".equalsIgnoreCase(type);
    }
  }

  public record AfkConfig(
      long idleCheckIntervalSeconds,
      long afkTimeThresholdSeconds,
      long payoutIntervalSeconds,
      double pointsPerInterval,
      long actionbarIntervalTicks,
      List<String> allowedWorlds,
      List<String> allowedRegions,
      List<GameMode> blockedGamemodes,
      long minActiveSecondsBeforeAfk,
      long maxAfkSecondsPerDay,
      long maxAfkSecondsPerSession,
      boolean poolDetectionEnabled,
      int poolDetectionSamples,
      boolean particlesEnabled,
      long particlesIntervalTicks,
      boolean startTitleEnabled,
      String startTitle,
      String startSubtitle,
      boolean startSoundEnabled,
      String startSoundName,
      float startSoundVolume,
      float startSoundPitch,
      boolean endSoundEnabled,
      String endSoundName,
      float endSoundVolume,
      float endSoundPitch) {}

  public record AntiExploitConfig(
      long untrustedMovementInputGraceSeconds,
      boolean ignoreMovementWhenInWater,
      boolean ignoreMovementWhenInVehicle,
      boolean ignoreMovementWhenOnRails) {}

  public record DailyBonusConfig(boolean enabled, int points) {}

  public record WeeklyBonusConfig(boolean enabled, int points) {}

  public record StreakBonusConfig(
      boolean enabled, int minMinutesPerDay, double multiplierPerDay, double maxMultiplier) {}

  public record BonusesConfig(
      DailyBonusConfig daily, WeeklyBonusConfig weekly, StreakBonusConfig streak) {}

  public record PayConfig(boolean enabled, int minAmount, long cooldownSeconds) {}

  public record ShopConfig(
      int size, int confirmAbovePrice, int transactionLogMaxEntries, Map<Integer, ShopItem> itemsBySlot) {}

  public record ShopItem(
      String id,
      int slot,
      Material material,
      String name,
      java.util.List<String> lore,
      int price,
      int purchaseLimit,
      java.util.List<String> commands) {
    public boolean hasPurchaseLimit() {
      return purchaseLimit >= 0;
    }
  }

  public record MultiplierConfig(Map<String, Double> permissionToMultiplier) {}

  public record StatsConfig(int topSize) {}

  public record UpdateCheckerConfig(
      boolean enabled, String versionUrl, String notifyPermission) {}

  public record BStatsConfig(boolean enabled) {}
}
