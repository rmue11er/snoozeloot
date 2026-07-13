package dev.snoozeloot.command;

import dev.snoozeloot.SnoozeLootPlugin;
import dev.snoozeloot.afk.AfkEngine;
import dev.snoozeloot.afk.AfkSettingsFormatter;
import dev.snoozeloot.config.ConfigService;
import dev.snoozeloot.points.LeaderboardBuilder;
import dev.snoozeloot.points.PayService;
import dev.snoozeloot.points.PointsService;
import dev.snoozeloot.shop.TransactionLog;
import dev.snoozeloot.shop.TransactionRecord;
import dev.snoozeloot.ui.ShopGui;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class SnoozeCommand implements CommandExecutor, TabCompleter {
  private static final DateTimeFormatter HISTORY_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

  private final ConfigService config;
  private final PointsService points;
  private final ShopGui shop;
  private final AfkEngine afk;
  private final TransactionLog transactionLog;

  private final SnoozeLootPlugin plugin;

  public SnoozeCommand(SnoozeLootPlugin plugin) {
    this.plugin = plugin;
    this.config = plugin.configService();
    this.points = plugin.pointsService();
    this.shop = plugin.shopGui();
    this.afk = plugin.afkEngine();
    this.transactionLog = plugin.transactionLog();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) {
      return showPoints(sender);
    }

    String sub = args[0].toLowerCase(java.util.Locale.ROOT);
    return switch (sub) {
      case "shop" -> openShop(sender);
      case "show" -> showOtherPoints(sender, args);
      case "stats" -> showStats(sender);
      case "pay" -> payPoints(sender, args);
      case "history" -> showHistory(sender, args);
      case "debug" -> debugInfo(sender, args);
      case "set" -> adminSet(sender, args);
      case "reset" -> adminReset(sender, args);
      case "give" -> adminGive(sender, args);
      case "remove" -> adminRemove(sender, args);
      case "reload" -> adminReload(sender);
      case "admin" -> adminInfo(sender);
      case "help" -> showHelp(sender);
      default -> showPoints(sender);
    };
  }

  private boolean showHelp(CommandSender sender) {
    var lines = SnoozeHelpBuilder.entries(sender, config.pay());
    if (lines.isEmpty()) {
      config.messages().send(sender, "no-permission", Map.of());
      return true;
    }

    config.messages().send(sender, "help-header", Map.of());
    for (SnoozeHelpBuilder.Entry entry : lines) {
      config
          .messages()
          .send(
              sender,
              "help-entry",
              Map.of(
                  "command", entry.command(),
                  "description", config.messages().text(entry.descriptionKey())));
    }
    return true;
  }

  private boolean showPoints(CommandSender sender) {
    if (!(sender instanceof Player p)) {
      sender.sendMessage("This command can only be used in-game.");
      return true;
    }
    if (!p.hasPermission("snoozeloot.use")) {
      config.messages().send(p, "no-permission", Map.of());
      return true;
    }
    int balance = points.get(p.getUniqueId());
    config.messages().send(p, "points", Map.of("points", Integer.toString(balance)));
    return true;
  }

  private boolean showOtherPoints(CommandSender sender, String[] args) {
    if (!sender.hasPermission("snoozeloot.use")) {
      config.messages().send(sender, "no-permission", Map.of());
      return true;
    }
    if (args.length < 2) {
      return false;
    }

    OfflinePlayer target = resolveOffline(args[1]);
    if (target == null) {
      config.messages().send(sender, "player-not-found", Map.of());
      return true;
    }

    int balance = points.get(target.getUniqueId());
    String name = target.getName() != null ? target.getName() : args[1];
    config
        .messages()
        .send(
            sender,
            "points-other",
            Map.of("target", name, "points", Integer.toString(balance)));
    return true;
  }

  private boolean showStats(CommandSender sender) {
    if (!sender.hasPermission("snoozeloot.use")) {
      config.messages().send(sender, "no-permission", Map.of());
      return true;
    }

    int topSize = config.stats().topSize();
    var snapshot = points.snapshot();
    var entries = LeaderboardBuilder.topEntries(snapshot, topSize);
    if (entries.isEmpty()) {
      config.messages().send(sender, "stats-empty", Map.of());
      return true;
    }

    config.messages().send(sender, "stats-header", Map.of());
    for (int i = 0; i < entries.size(); i++) {
      LeaderboardBuilder.Entry entry = entries.get(i);
      config
          .messages()
          .send(
              sender,
              "stats-entry",
              Map.of(
                  "rank",
                  Integer.toString(i + 1),
                  "target",
                  displayName(entry.uuid(), entry.name()),
                  "points",
                  Integer.toString(entry.points())));
    }

    if (sender instanceof Player p) {
      int balance = points.get(p.getUniqueId());
      String rank =
          LeaderboardBuilder.rank(snapshot, p.getUniqueId())
              .stream()
              .mapToObj(Integer::toString)
              .findFirst()
              .orElse("-");
      config
          .messages()
          .send(
              p,
              "stats-self",
              Map.of("rank", rank, "points", Integer.toString(balance)));
    }
    return true;
  }

  private boolean payPoints(CommandSender sender, String[] args) {
    if (!(sender instanceof Player payer)) {
      sender.sendMessage("This command can only be used in-game.");
      return true;
    }
    if (!payer.hasPermission("snoozeloot.pay")) {
      config.messages().send(payer, "no-permission", Map.of());
      return true;
    }
    if (args.length < 3) {
      return false;
    }

    var pay = config.pay();
    if (!pay.enabled()) {
      config.messages().send(payer, "pay-fail-disabled", Map.of());
      return true;
    }

    OfflinePlayer target = resolveOffline(args[1]);
    if (target == null) {
      config.messages().send(payer, "player-not-found", Map.of());
      return true;
    }

    if (target.getUniqueId().equals(payer.getUniqueId())) {
      config.messages().send(payer, "pay-fail-self", Map.of());
      return true;
    }

    Integer amount = parseAmount(args[2]);
    if (amount == null) {
      config.messages().send(payer, "invalid-amount", Map.of());
      return true;
    }
    if (amount < pay.minAmount()) {
      config
          .messages()
          .send(payer, "pay-fail-min-amount", Map.of("min", Integer.toString(pay.minAmount())));
      return true;
    }

    PayService.PayResult result =
        plugin
            .payService()
            .pay(
            payer.getUniqueId(),
            target.getUniqueId(),
            amount,
            payer.getName(),
            target.getName() != null ? target.getName() : args[1]);

    if (!result.success()) {
      switch (result.reason()) {
        case "self-pay" -> config.messages().send(payer, "pay-fail-self", Map.of());
        case "below-minimum" ->
            config
                .messages()
                .send(payer, "pay-fail-min-amount", Map.of("min", Integer.toString(pay.minAmount())));
        case "cooldown" ->
            config
                .messages()
                .send(
                    payer,
                    "pay-fail-cooldown",
                    Map.of("seconds", Long.toString(Math.max(1, result.cooldownRemainingSeconds()))));
        case "insufficient-funds" ->
            config.messages().send(payer, "pay-fail-insufficient", Map.of());
        default -> config.messages().send(payer, "invalid-amount", Map.of());
      }
      return true;
    }

    String targetName = target.getName() != null ? target.getName() : args[1];
    config
        .messages()
        .send(
            payer,
            "pay-success",
            Map.of("target", targetName, "amount", Integer.toString(amount)));

    Player onlineTarget = target.getPlayer();
    if (onlineTarget != null) {
      config
          .messages()
          .send(
              onlineTarget,
              "pay-received",
              Map.of("sender", payer.getName(), "amount", Integer.toString(amount)));
    }
    return true;
  }

  private boolean showHistory(CommandSender sender, String[] args) {
    if (!sender.hasPermission("snoozeloot.use")) {
      config.messages().send(sender, "no-permission", Map.of());
      return true;
    }

    int limit = 10;
    if (args.length >= 2) {
      Integer parsed = parseOptionalLimit(args[1]);
      if (parsed == null) {
        config.messages().send(sender, "invalid-amount", Map.of());
        return true;
      }
      limit = parsed;
    }

    List<TransactionRecord> records;
    if (sender.hasPermission("snoozeloot.history.admin")) {
      records = transactionLog.recent(limit);
    } else if (sender instanceof Player p) {
      records = transactionLog.byPlayer(p.getUniqueId(), limit);
    } else {
      sender.sendMessage("This command can only be used in-game.");
      return true;
    }

    if (records.isEmpty()) {
      config.messages().send(sender, "history-empty", Map.of());
      return true;
    }

    config.messages().send(sender, "transaction-log-header", Map.of());
    for (TransactionRecord record : records) {
      String playerName =
          record.playerName() != null
              ? record.playerName()
              : displayName(record.uuid(), null);
      config
          .messages()
          .send(
              sender,
              "transaction-log-entry",
              Map.of(
                  "time",
                  HISTORY_TIME.format(Instant.ofEpochMilli(record.timestampMillis())),
                  "player",
                  playerName,
                  "item",
                  record.itemId(),
                  "price",
                  Integer.toString(record.price())));
    }
    return true;
  }

  private boolean debugInfo(CommandSender sender, String[] args) {
    if (!sender.hasPermission("snoozeloot.admin")) {
      config.messages().send(sender, "no-permission", Map.of());
      return true;
    }

    if (args.length >= 2) {
      Player target = Bukkit.getPlayerExact(args[1]);
      if (target == null) {
        config.messages().send(sender, "player-not-found", Map.of());
        return true;
      }
      config
          .messages()
          .send(
              sender,
              "debug-line",
              Map.of(
                  "line",
                  target.getName()
                      + ": afk="
                      + afk.isAfk(target)
                      + " points="
                      + points.get(target.getUniqueId())));
      return true;
    }

    afk.debugDump(sender);
    return true;
  }

  private boolean adminSet(CommandSender sender, String[] args) {
    if (!sender.hasPermission("snoozeloot.admin")) {
      config.messages().send(sender, "no-permission", Map.of());
      return true;
    }
    if (args.length < 3) return false;

    OfflinePlayer target = resolveOffline(args[1]);
    if (target == null) {
      config.messages().send(sender, "player-not-found", Map.of());
      return true;
    }

    Integer amount = parseNonNegativeAmount(args[2]);
    if (amount == null) {
      config.messages().send(sender, "invalid-amount", Map.of());
      return true;
    }

    points.set(target.getUniqueId(), amount, target.getName());
    config
        .messages()
        .send(
            sender,
            "admin-set",
            Map.of(
                "target",
                target.getName() == null ? args[1] : target.getName(),
                "amount",
                Integer.toString(amount)));
    return true;
  }

  private boolean adminReset(CommandSender sender, String[] args) {
    if (!sender.hasPermission("snoozeloot.admin")) {
      config.messages().send(sender, "no-permission", Map.of());
      return true;
    }
    if (args.length < 2) return false;

    OfflinePlayer target = resolveOffline(args[1]);
    if (target == null) {
      config.messages().send(sender, "player-not-found", Map.of());
      return true;
    }

    points.set(target.getUniqueId(), 0, target.getName());
    config
        .messages()
        .send(
            sender,
            "admin-reset",
            Map.of("target", target.getName() == null ? args[1] : target.getName()));
    return true;
  }

  private String displayName(UUID uuid, String storedName) {
    if (storedName != null && !storedName.isBlank()) {
      return storedName;
    }
    OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
    if (offline.getName() != null) {
      return offline.getName();
    }
    return uuid.toString().substring(0, 8);
  }

  private boolean openShop(CommandSender sender) {
    if (!(sender instanceof Player p)) {
      sender.sendMessage("This command can only be used in-game.");
      return true;
    }
    if (!p.hasPermission("snoozeloot.use")) {
      config.messages().send(p, "no-permission", Map.of());
      return true;
    }
    shop.open(p);
    return true;
  }

  private boolean adminGive(CommandSender sender, String[] args) {
    if (!sender.hasPermission("snoozeloot.admin")) {
      config.messages().send(sender, "no-permission", Map.of());
      return true;
    }
    if (args.length < 3) return false;

    OfflinePlayer target = resolveOffline(args[1]);
    if (target == null) {
      config.messages().send(sender, "player-not-found", Map.of());
      return true;
    }

    Integer amount = parseAmount(args[2]);
    if (amount == null) {
      config.messages().send(sender, "invalid-amount", Map.of());
      return true;
    }

    points.add(target.getUniqueId(), amount, target.getName());
    config
        .messages()
        .send(
            sender,
            "admin-give",
            Map.of(
                "target",
                target.getName() == null ? args[1] : target.getName(),
                "amount",
                Integer.toString(amount)));
    return true;
  }

  private boolean adminRemove(CommandSender sender, String[] args) {
    if (!sender.hasPermission("snoozeloot.admin")) {
      config.messages().send(sender, "no-permission", Map.of());
      return true;
    }
    if (args.length < 3) return false;

    OfflinePlayer target = resolveOffline(args[1]);
    if (target == null) {
      config.messages().send(sender, "player-not-found", Map.of());
      return true;
    }

    Integer amount = parseAmount(args[2]);
    if (amount == null) {
      config.messages().send(sender, "invalid-amount", Map.of());
      return true;
    }

    points.remove(target.getUniqueId(), amount);
    config
        .messages()
        .send(
            sender,
            "admin-remove",
            Map.of(
                "target",
                target.getName() == null ? args[1] : target.getName(),
                "amount",
                Integer.toString(amount)));
    return true;
  }

  private boolean adminReload(CommandSender sender) {
    if (!sender.hasPermission("snoozeloot.admin")) {
      config.messages().send(sender, "no-permission", Map.of());
      return true;
    }

    plugin.reloadRuntime();
    config.messages().send(sender, "admin-reload", Map.of());
    return true;
  }

  private boolean adminInfo(CommandSender sender) {
    if (!sender.hasPermission("snoozeloot.admin")) {
      config.messages().send(sender, "no-permission", Map.of());
      return true;
    }

    var afkConfig = config.afk();
    String rate =
        AfkSettingsFormatter.ratePerMinute(
            afkConfig.pointsPerInterval(), afkConfig.payoutIntervalSeconds());

    config
        .messages()
        .send(
            sender,
            "admin-info",
            Map.of(
                "afk_seconds",
                Long.toString(afkConfig.afkTimeThresholdSeconds()),
                "payout_seconds",
                Long.toString(afkConfig.payoutIntervalSeconds()),
                "points_per_interval",
                formatPoints(afkConfig.pointsPerInterval()),
                "rate",
                rate));
    return true;
  }

  private static String formatPoints(double value) {
    if (value == Math.rint(value)) {
      return Long.toString((long) value);
    }
    return Double.toString(value);
  }

  private OfflinePlayer resolveOffline(String name) {
    Player online = Bukkit.getPlayerExact(name);
    if (online != null) {
      return online;
    }

    var stored = points.findUuidByName(name);
    if (stored.isPresent()) {
      return Bukkit.getOfflinePlayer(stored.get());
    }

    return Bukkit.getOfflinePlayerIfCached(name);
  }

  private Integer parseAmount(String s) {
    try {
      int v = Integer.parseInt(s);
      if (v <= 0) return null;
      return v;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Integer parseNonNegativeAmount(String s) {
    try {
      int v = Integer.parseInt(s);
      if (v < 0) return null;
      return v;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Integer parseOptionalLimit(String s) {
    try {
      int v = Integer.parseInt(s);
      if (v <= 0 || v > 100) return null;
      return v;
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1) {
      List<String> subs = new ArrayList<>();
      if (sender.hasPermission("snoozeloot.use")) {
        subs.add("shop");
        subs.add("show");
        subs.add("stats");
        subs.add("history");
        subs.add("help");
      }
      if (sender.hasPermission("snoozeloot.pay")) {
        subs.add("pay");
      }
      if (sender.hasPermission("snoozeloot.admin")) {
        subs.add("admin");
        subs.add("give");
        subs.add("remove");
        subs.add("set");
        subs.add("reset");
        subs.add("reload");
        subs.add("debug");
      }
      return filterPrefix(subs, args[0]);
    }

    if (args.length == 2) {
      String sub = args[0].toLowerCase(java.util.Locale.ROOT);
      if (sub.equals("show") && sender.hasPermission("snoozeloot.use")) {
        return filterPrefix(onlineNames(), args[1]);
      }
      if (sub.equals("pay") && sender.hasPermission("snoozeloot.pay")) {
        return filterPrefix(onlineNames(), args[1]);
      }
      if (sub.equals("history") && sender.hasPermission("snoozeloot.use")) {
        return filterPrefix(List.of("5", "10", "20"), args[1]);
      }
      if (sub.equals("debug") && sender.hasPermission("snoozeloot.admin")) {
        return filterPrefix(onlineNames(), args[1]);
      }
      if (sender.hasPermission("snoozeloot.admin")
          && (sub.equals("give")
              || sub.equals("remove")
              || sub.equals("set")
              || sub.equals("reset"))) {
        return filterPrefix(onlineNames(), args[1]);
      }
    }

    if (args.length == 3) {
      String sub = args[0].toLowerCase(java.util.Locale.ROOT);
      if (sender.hasPermission("snoozeloot.admin")
          && (sub.equals("give") || sub.equals("remove") || sub.equals("set"))) {
        return filterPrefix(List.of("1", "10", "100", "1000"), args[2]);
      }
      if (sub.equals("pay") && sender.hasPermission("snoozeloot.pay")) {
        return filterPrefix(List.of("1", "10", "100"), args[2]);
      }
    }

    return List.of();
  }

  private List<String> onlineNames() {
    return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
  }

  private List<String> filterPrefix(List<String> list, String prefix) {
    String p = prefix.toLowerCase(java.util.Locale.ROOT);
    return list.stream()
        .filter(s -> s.toLowerCase(java.util.Locale.ROOT).startsWith(p))
        .toList();
  }
}
