package dev.snoozeloot.config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigMerger {
  private static final int CURRENT_CONFIG_VERSION = 3;

  private ConfigMerger() {}

  public static void mergeMissingDefaults(JavaPlugin plugin) {
    try (InputStream in = plugin.getResource("config.yml")) {
      if (in == null) {
        return;
      }

      YamlConfiguration defaults =
          YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
      FileConfiguration current = plugin.getConfig();
      boolean changed = false;

      int version = current.getInt("config-version", 1);
      if (version < CURRENT_CONFIG_VERSION) {
        applyVersionMigration(current, defaults, version);
        current.set("config-version", CURRENT_CONFIG_VERSION);
        changed = true;
      }

      for (String key : defaults.getKeys(true)) {
        if (defaults.isConfigurationSection(key)) {
          continue;
        }
        if (!current.contains(key)) {
          current.set(key, defaults.get(key));
          changed = true;
        }
      }

      if (changed) {
        plugin.saveConfig();
      }
    } catch (Exception e) {
      plugin.getLogger().warning("Failed to merge default config values: " + e.getMessage());
    }
  }

  static void applyVersionMigration(
      FileConfiguration current, YamlConfiguration defaults, int fromVersion) {
    if (fromVersion < 2) {
      if (defaults.isConfigurationSection("messages")) {
        current.set("messages", defaults.getConfigurationSection("messages"));
      }
      if (defaults.isConfigurationSection("shop.items")) {
        current.set("shop.items", defaults.getConfigurationSection("shop.items"));
      }
    }
    if (fromVersion < 3) {
      if (defaults.isConfigurationSection("messages")) {
        current.set("messages", defaults.getConfigurationSection("messages"));
      }
      migrateSection(current, defaults, "storage");
      migrateSection(current, defaults, "afk");
      migrateSection(current, defaults, "bonuses");
      migrateSection(current, defaults, "pay");
      migrateSection(current, defaults, "bstats");
      migrateScalar(current, defaults, "language");
      if (defaults.isConfigurationSection("shop")) {
        current.set("shop.confirm-above-price", defaults.getInt("shop.confirm-above-price", 50));
        current.set(
            "shop.transaction-log-max-entries",
            defaults.getInt("shop.transaction-log-max-entries", 500));
      }
    }
  }

  private static void migrateScalar(
      FileConfiguration current, YamlConfiguration defaults, String path) {
    if (!current.contains(path) && defaults.contains(path)) {
      current.set(path, defaults.get(path));
    }
  }

  private static void migrateSection(
      FileConfiguration current, YamlConfiguration defaults, String path) {
    if (defaults.isConfigurationSection(path) && !current.isConfigurationSection(path)) {
      current.set(path, defaults.getConfigurationSection(path));
    }
  }
}
