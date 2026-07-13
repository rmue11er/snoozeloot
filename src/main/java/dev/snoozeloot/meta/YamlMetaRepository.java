package dev.snoozeloot.meta;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class YamlMetaRepository implements MetaRepository {
  private final JavaPlugin plugin;
  private final File file;

  public YamlMetaRepository(JavaPlugin plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "meta.yml");
  }

  @Override
  public Map<UUID, PlayerMeta> loadAll() {
    ensureDataFolder();

    if (!file.exists()) {
      return new HashMap<>();
    }

    YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
    ConfigurationSection section = yml.getConfigurationSection("players");
    if (section == null) {
      return new HashMap<>();
    }

    Map<UUID, PlayerMeta> map = new HashMap<>();
    for (String key : section.getKeys(false)) {
      try {
        UUID uuid = UUID.fromString(key);
        ConfigurationSection player = section.getConfigurationSection(key);
        if (player == null) {
          continue;
        }
        map.put(uuid, readMeta(player));
      } catch (IllegalArgumentException ignored) {
        // skip invalid uuid keys
      }
    }
    return map;
  }

  @Override
  public void saveAll(Map<UUID, PlayerMeta> meta) {
    ensureDataFolder();

    YamlConfiguration yml = new YamlConfiguration();
    yml.set("players", null);
    for (var entry : meta.entrySet()) {
      writeMeta(yml, "players." + entry.getKey(), entry.getValue());
    }

    try {
      yml.save(file);
    } catch (IOException e) {
      plugin.getLogger().warning("Failed to save meta.yml: " + e.getMessage());
    }
  }

  private void ensureDataFolder() {
    if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
      plugin.getLogger().warning("Could not create plugin data folder.");
    }
  }

  private static PlayerMeta readMeta(ConfigurationSection section) {
    Map<String, Integer> purchaseCounts = new HashMap<>();
    ConfigurationSection purchases = section.getConfigurationSection("purchase-counts");
    if (purchases != null) {
      for (String itemId : purchases.getKeys(false)) {
        int count = Math.max(0, purchases.getInt(itemId, 0));
        if (count > 0) {
          purchaseCounts.put(itemId, count);
        }
      }
    }

    return new PlayerMeta(
        section.getInt("streak-days", 0),
        section.getString("last-streak-date"),
        section.getString("last-daily-bonus-date"),
        section.getString("last-weekly-bonus-week"),
        section.getLong("afk-seconds-today", 0L),
        section.getString("afk-day"),
        section.getLong("session-afk-seconds", 0L),
        section.getLong("active-play-seconds", 0L),
        purchaseCounts);
  }

  private static void writeMeta(YamlConfiguration yml, String base, PlayerMeta meta) {
    yml.set(base + ".streak-days", meta.streakDays());
    yml.set(base + ".last-streak-date", meta.lastStreakDate());
    yml.set(base + ".last-daily-bonus-date", meta.lastDailyBonusDate());
    yml.set(base + ".last-weekly-bonus-week", meta.lastWeeklyBonusWeek());
    yml.set(base + ".afk-seconds-today", meta.afkSecondsToday());
    yml.set(base + ".afk-day", meta.afkDay());
    yml.set(base + ".session-afk-seconds", meta.sessionAfkSeconds());
    yml.set(base + ".active-play-seconds", meta.activePlaySeconds());

    yml.set(base + ".purchase-counts", null);
    for (var entry : meta.purchaseCounts().entrySet()) {
      yml.set(base + ".purchase-counts." + entry.getKey(), entry.getValue());
    }
  }
}
