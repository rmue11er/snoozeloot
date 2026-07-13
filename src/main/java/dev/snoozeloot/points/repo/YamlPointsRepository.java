package dev.snoozeloot.points.repo;

import dev.snoozeloot.points.PlayerBalance;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class YamlPointsRepository implements PointsRepository {
  private final JavaPlugin plugin;
  private final File file;

  private volatile Map<UUID, PlayerBalance> pendingSnapshot = Map.of();
  private BukkitTask pendingSaveTask;

  public YamlPointsRepository(JavaPlugin plugin) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "players.yml");
  }

  public void load() {
    pendingSnapshot = loadAll();
  }

  public void queueSave(Map<UUID, PlayerBalance> balances) {
    pendingSnapshot = new HashMap<>(balances);
    scheduleSave();
  }

  public void saveNow(Map<UUID, PlayerBalance> balances) {
    pendingSnapshot = new HashMap<>(balances);
    cancelPendingSave();
    saveAll(pendingSnapshot);
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
  public void saveAll(Map<UUID, PlayerBalance> balances) {
    if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
      plugin.getLogger().warning("Could not create plugin data folder.");
      return;
    }

    YamlConfiguration yml = new YamlConfiguration();
    Map<UUID, PlayersYamlCodec.PlayerBalanceRecord> raw = new HashMap<>();
    for (var entry : balances.entrySet()) {
      PlayerBalance balance = entry.getValue();
      raw.put(entry.getKey(), new PlayersYamlCodec.PlayerBalanceRecord(balance.points(), balance.name()));
    }
    PlayersYamlCodec.save(yml, raw);

    try {
      yml.save(file);
    } catch (IOException e) {
      plugin.getLogger().warning("Failed to save players.yml: " + e.getMessage());
    }
  }

  private void scheduleSave() {
    cancelPendingSave();
    pendingSaveTask =
        Bukkit.getScheduler()
            .runTaskLater(
                plugin,
                () ->
                    Bukkit.getScheduler()
                        .runTaskAsynchronously(
                            plugin,
                            () -> {
                              saveAll(pendingSnapshot);
                              pendingSaveTask = null;
                            }),
                40L);
  }

  private void cancelPendingSave() {
    if (pendingSaveTask != null) {
      pendingSaveTask.cancel();
      pendingSaveTask = null;
    }
  }

  private static PlayerBalance toBalance(PlayersYamlCodec.PlayerBalanceRecord record) {
    return new PlayerBalance(record.points(), record.name());
  }
}
