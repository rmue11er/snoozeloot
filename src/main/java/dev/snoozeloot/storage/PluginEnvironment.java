package dev.snoozeloot.storage;

import java.io.File;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public record PluginEnvironment(
    File dataFolder, FileConfiguration config, Logger logger, JavaPlugin plugin) {

  public static PluginEnvironment from(JavaPlugin plugin) {
    return new PluginEnvironment(
        plugin.getDataFolder(), plugin.getConfig(), plugin.getLogger(), plugin);
  }

  public int transactionLogMaxEntries() {
    return config.getInt("shop.transaction-log-max-entries", 500);
  }
}
