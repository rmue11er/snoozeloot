package dev.snoozeloot.storage;

import dev.snoozeloot.config.ConfigService;
import dev.snoozeloot.meta.PlayerMetaStore;
import dev.snoozeloot.meta.YamlMetaRepository;
import dev.snoozeloot.points.SqliteStorage;
import dev.snoozeloot.points.repo.PointsRepository;
import dev.snoozeloot.points.repo.YamlPointsRepository;
import dev.snoozeloot.shop.TransactionLog;
import dev.snoozeloot.shop.YamlTransactionLog;
import org.bukkit.plugin.java.JavaPlugin;

public final class StorageFactory {
  private StorageFactory() {}

  public record Bundle(
      PointsRepository pointsRepository, PlayerMetaStore metaStore, TransactionLog transactionLog) {}

  public static Bundle create(JavaPlugin plugin, ConfigService config) {
    if (config.storage().isSqlite()) {
      SqliteStorage sqlite = new SqliteStorage(plugin);
      return new Bundle(
          sqlite, new PlayerMetaStore(sqlite.asMetaRepository()), sqlite);
    }

    return new Bundle(
        new YamlPointsRepository(plugin),
        new PlayerMetaStore(new YamlMetaRepository(plugin)),
        new YamlTransactionLog(plugin));
  }
}
