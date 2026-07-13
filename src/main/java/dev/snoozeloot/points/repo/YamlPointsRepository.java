package dev.snoozeloot.points.repo;

import dev.snoozeloot.points.PlayerBalance;
import dev.snoozeloot.storage.DebouncedSnapshotWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class YamlPointsRepository implements PointsRepository {
  private final JavaPlugin plugin;
  private final File file;
  private final DebouncedSnapshotWriter<Map<UUID, PlayerBalance>> writer;

  public YamlPointsRepository(JavaPlugin plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "players.yml");
    this.writer = new DebouncedSnapshotWriter<>(plugin, this::saveAll, 40L);
  }

  @Override
  public Map<UUID, PlayerBalance> loadAll() {
    if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
      plugin.getLogger().warning("Could not create plugin data folder.");
    }

    if (!file.exists()) {
      return new HashMap<>();
    }

    YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
    Map<UUID, PlayersYamlCodec.PlayerBalanceRecord> raw = PlayersYamlCodec.load(yml);
    Map<UUID, PlayerBalance> map = new HashMap<>();
    for (var entry : raw.entrySet()) {
      map.put(entry.getKey(), toBalance(entry.getValue()));
    }
    return map;
  }

  @Override
  public void queueSave(Map<UUID, PlayerBalance> balances) {
    writer.queue(new HashMap<>(balances));
  }

  @Override
  public void saveNow(Map<UUID, PlayerBalance> balances) {
    writer.flushNow(new HashMap<>(balances));
  }

  @Override
  public void saveAll(Map<UUID, PlayerBalance> balances) {
    if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
      plugin.getLogger().warning("Could not create plugin data folder.");
      return;
    }

    YamlConfiguration yml = new YamlConfiguration();
    Map<UUID, PlayersYamlCodec.PlayerBalanceRecord> raw = new HashMap<>();
    for (var entry : balances.entrySet()) {
      PlayerBalance balance = entry.getValue();
      raw.put(
          entry.getKey(),
          new PlayersYamlCodec.PlayerBalanceRecord(balance.points(), balance.name()));
    }
    PlayersYamlCodec.save(yml, raw);

    try {
      yml.save(file);
    } catch (IOException e) {
      plugin.getLogger().warning("Failed to save players.yml: " + e.getMessage());
    }
  }

  private static PlayerBalance toBalance(PlayersYamlCodec.PlayerBalanceRecord record) {
    return new PlayerBalance(record.points(), record.name());
  }
}
