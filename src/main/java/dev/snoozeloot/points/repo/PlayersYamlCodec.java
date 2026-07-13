package dev.snoozeloot.points.repo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

final class PlayersYamlCodec {
  private PlayersYamlCodec() {}

  static Map<UUID, PlayerBalanceRecord> load(YamlConfiguration yml) {
    Map<UUID, PlayerBalanceRecord> map = new HashMap<>();
    ConfigurationSection section = yml.getConfigurationSection("players");
    if (section == null) {
      return map;
    }

    for (String key : section.getKeys(false)) {
      try {
        UUID uuid = UUID.fromString(key);
        Object raw = section.get(key);
        if (raw instanceof ConfigurationSection playerSection) {
          int points = Math.max(0, playerSection.getInt("points", 0));
          String name = playerSection.getString("name");
          map.put(uuid, new PlayerBalanceRecord(points, name));
        } else {
          int points = Math.max(0, section.getInt(key, 0));
          map.put(uuid, new PlayerBalanceRecord(points, null));
        }
      } catch (IllegalArgumentException ignored) {
        // skip invalid uuid keys
      }
    }
    return map;
  }

  static void save(YamlConfiguration yml, Map<UUID, PlayerBalanceRecord> players) {
    yml.set("players", null);
    for (var entry : players.entrySet()) {
      String base = "players." + entry.getKey();
      yml.set(base + ".points", Math.max(0, entry.getValue().points()));
      if (entry.getValue().name() != null && !entry.getValue().name().isBlank()) {
        yml.set(base + ".name", entry.getValue().name());
      }
    }
  }

  record PlayerBalanceRecord(int points, String name) {}
}