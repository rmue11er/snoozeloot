package dev.snoozeloot.integration;

import dev.snoozeloot.config.ConfigService;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class BStatsSetup {
  static final int PLUGIN_ID = 32608;

  private BStatsSetup() {}

  public static void register(
      JavaPlugin plugin, ConfigService config, WorldGuardHook worldGuardHook) {
    if (!config.bstats().enabled()) {
      plugin
          .getLogger()
          .info("SnoozeLoot metrics disabled via config.yml (bstats.enabled: false).");
      return;
    }

    try {
      Metrics metrics = new Metrics(plugin, PLUGIN_ID);

      metrics.addCustomChart(new SimplePie("language", () -> normalize(config.language())));
      metrics.addCustomChart(
          new SimplePie("storage_type", () -> normalize(config.storage().type())));
      metrics.addCustomChart(
          new SimplePie(
              "sqlite", () -> yesNo(config.storage().isSqlite())));
      metrics.addCustomChart(
          new SimplePie(
              "daily_bonus", () -> yesNo(config.bonuses().daily().enabled())));
      metrics.addCustomChart(
          new SimplePie(
              "weekly_bonus", () -> yesNo(config.bonuses().weekly().enabled())));
      metrics.addCustomChart(
          new SimplePie(
              "streak_bonus", () -> yesNo(config.bonuses().streak().enabled())));
      metrics.addCustomChart(
          new SimplePie("pay_enabled", () -> yesNo(config.pay().enabled())));
      metrics.addCustomChart(
          new SimplePie(
              "worldguard",
              () -> yesNo(worldGuardHook != null && worldGuardHook.worldGuardPresent())));
      metrics.addCustomChart(
          new SimplePie(
              "placeholderapi",
              () -> yesNo(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null)));

      plugin.getLogger().info("SnoozeLoot metrics enabled (bStats id " + PLUGIN_ID + ").");
    } catch (Exception e) {
      plugin.getLogger().warning("bStats setup failed: " + e.getMessage());
    }
  }

  private static String yesNo(boolean value) {
    return value ? "yes" : "no";
  }

  private static String normalize(String value) {
    if (value == null || value.isBlank()) {
      return "unknown";
    }
    return value.trim().toLowerCase(java.util.Locale.ROOT);
  }
}
