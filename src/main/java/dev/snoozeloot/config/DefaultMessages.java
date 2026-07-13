package dev.snoozeloot.config;

import java.util.Map;

final class DefaultMessages {
  private static final Map<String, String> DEFAULTS =
      Map.ofEntries(
          Map.entry("prefix", "<gray>[<gold>SnoozeLoot</gold>]</gray> "),
          Map.entry(
              "points", "<prefix><gray>You have <gold><points></gold> SnoozePoints.</gray>"),
          Map.entry(
              "points-other",
              "<prefix><gray><white><target></white> has <gold><points></gold> SnoozePoints.</gray>"),
          Map.entry(
              "actionbar-afk",
              "<gray>You are snoozing... Points: <gold><points></gold> (+<rate>/min)</gray>"),
          Map.entry(
              "afk-start",
              "<prefix><gray>You started snoozing. Earning about <gold><rate></gold> SnoozePoints per minute.</gray>"),
          Map.entry(
              "afk-welcome-back",
              "<prefix><gray>Welcome back! You earned <gold><session_points></gold> SnoozePoints this session.</gray>"),
          Map.entry(
              "afk-blocked-world", "<prefix><red>Snoozing is disabled in this world.</red>"),
          Map.entry(
              "afk-blocked-region", "<prefix><red>Snoozing is disabled in this region.</red>"),
          Map.entry(
              "afk-blocked-gamemode",
              "<prefix><red>Snoozing is disabled in your current game mode.</red>"),
          Map.entry(
              "afk-limit-daily", "<prefix><red>You reached the daily snoozing limit.</red>"),
          Map.entry(
              "afk-limit-session", "<prefix><red>You reached the session snoozing limit.</red>"),
          Map.entry(
              "daily-bonus",
              "<prefix><green>Daily bonus! You received <gold><points></gold> SnoozePoints.</green>"),
          Map.entry(
              "weekly-bonus",
              "<prefix><green>Weekly bonus! You received <gold><points></gold> SnoozePoints.</green>"),
          Map.entry(
              "pay-success",
              "<prefix><green>Sent <gold><amount></gold> SnoozePoints to <white><target></white>.</green>"),
          Map.entry(
              "pay-received",
              "<prefix><green>You received <gold><amount></gold> SnoozePoints from <white><sender></white>.</green>"),
          Map.entry("pay-fail-disabled", "<prefix><red>Paying SnoozePoints is disabled.</red>"),
          Map.entry("pay-fail-self", "<prefix><red>You cannot pay yourself.</red>"),
          Map.entry(
              "pay-fail-min-amount",
              "<prefix><red>Minimum payment is <gold><min></gold> SnoozePoints.</red>"),
          Map.entry(
              "pay-fail-insufficient", "<prefix><red>You do not have enough SnoozePoints.</red>"),
          Map.entry(
              "pay-fail-cooldown",
              "<prefix><red>Please wait <gold><seconds></gold>s before paying again.</red>"),
          Map.entry("shop-title", "<gold>Snooze Shop</gold>"),
          Map.entry(
              "shop-not-enough-points",
              "<prefix><red>You do not have enough SnoozePoints.</red>"),
          Map.entry(
              "shop-success",
              "<prefix><green>Purchased <white><item></white> for <gold><price></gold> points.</green>"),
          Map.entry(
              "shop-confirm-title",
              "<prefix><yellow>Click again to confirm purchase of <white><item></white> for <gold><price></gold> points.</yellow>"),
          Map.entry(
              "shop-confirm-yes",
              "<prefix><green>Confirmed purchase of <white><item></white>.</green>"),
          Map.entry("shop-confirm-no", "<prefix><gray>Purchase cancelled.</gray>"),
          Map.entry(
              "shop-limit-reached",
              "<prefix><red>You have reached the purchase limit for <white><item></white>.</red>"),
          Map.entry("stats-header", "<prefix><gold>Top SnoozePoints</gold>"),
          Map.entry(
              "stats-entry", "<gray><rank>. <white><target></white> — <gold><points></gold>"),
          Map.entry(
              "stats-empty", "<prefix><gray>No SnoozePoints have been earned yet.</gray>"),
          Map.entry(
              "stats-self",
              "<prefix><gray>Your rank: <gold><rank></gold> — <gold><points></gold> SnoozePoints</gray>"),
          Map.entry(
              "transaction-log-header", "<prefix><gold>Recent shop purchases</gold>"),
          Map.entry(
              "transaction-log-entry",
              "<gray><time> <white><player></white> bought <white><item></white> for <gold><price></gold></gray>"),
          Map.entry(
              "history-empty", "<prefix><gray>No shop purchases recorded yet.</gray>"),
          Map.entry("debug-header", "<prefix><yellow>SnoozeLoot debug</yellow>"),
          Map.entry("debug-line", "<gray><line></gray>"),
          Map.entry(
              "debug-player",
              "<gray><player>: afk=<afk>, idle=<idle>s, session=<session>pts, streak=<streak></gray>"),
          Map.entry(
              "admin-give",
              "<prefix><green>Added <amount> points to <white><target></white>.</green>"),
          Map.entry(
              "admin-remove",
              "<prefix><yellow>Removed <amount> points from <white><target></white>.</yellow>"),
          Map.entry(
              "admin-set",
              "<prefix><green>Set <white><target></white> to <gold><amount></gold> SnoozePoints.</green>"),
          Map.entry(
              "admin-reset",
              "<prefix><yellow>Reset <white><target></white> to 0 SnoozePoints.</yellow>"),
          Map.entry("admin-reload", "<prefix><green>Config reloaded.</green>"),
          Map.entry(
              "admin-info",
              "<prefix><gray>AFK after <gold><afk_seconds></gold>s without input. Points from then: <gold><points_per_interval></gold> every <gold><payout_seconds></gold>s (<gold><rate></gold>/min).</gray>"),
          Map.entry("help-header", "<prefix><gold>SnoozeLoot commands</gold>"),
          Map.entry(
              "help-entry", "<gray><command></gray> — <white><description></white>"),
          Map.entry("help-desc-balance", "Show your SnoozePoints balance"),
          Map.entry("help-desc-shop", "Open the points shop"),
          Map.entry("help-desc-show", "Show another player's balance"),
          Map.entry("help-desc-stats", "Show the leaderboard and your rank"),
          Map.entry("help-desc-history", "Show your recent shop purchases"),
          Map.entry(
              "help-desc-history-admin",
              "Show recent shop purchases (all players when limit is set)"),
          Map.entry("help-desc-help", "Show this command list"),
          Map.entry("help-desc-pay", "Send SnoozePoints to another player"),
          Map.entry("help-desc-admin", "Show AFK and payout settings"),
          Map.entry("help-desc-give", "Give SnoozePoints to a player"),
          Map.entry("help-desc-remove", "Remove SnoozePoints from a player"),
          Map.entry("help-desc-set", "Set a player's SnoozePoints balance"),
          Map.entry("help-desc-reset", "Reset a player's balance to zero"),
          Map.entry("help-desc-reload", "Reload config and messages"),
          Map.entry("help-desc-debug", "Show AFK debug information"),
          Map.entry("no-permission", "<prefix><red>You do not have permission to do that.</red>"),
          Map.entry("player-not-found", "<prefix><red>Player not found.</red>"),
          Map.entry("invalid-amount", "<prefix><red>Invalid amount.</red>"));

  private DefaultMessages() {}

  static String get(String path) {
    return DEFAULTS.getOrDefault(path, "");
  }

  static String prefix() {
    return DEFAULTS.get("prefix");
  }
}
