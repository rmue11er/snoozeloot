package dev.snoozeloot.points;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snoozeloot.meta.MetaRepository;
import dev.snoozeloot.meta.PlayerMeta;
import dev.snoozeloot.shop.TransactionRecord;
import dev.snoozeloot.storage.PluginEnvironment;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteStorageTest {
  @TempDir Path tempDir;

  private SqliteStorage storage;
  private UUID playerId;

  @BeforeEach
  void setUp() {
    YamlConfiguration config = new YamlConfiguration();
    config.set("shop.transaction-log-max-entries", 5);

    PluginEnvironment environment =
        new PluginEnvironment(
            tempDir.toFile(), config, Logger.getLogger("SnoozeLootTest"), null);

    storage =
        new SqliteStorage(environment, new File(tempDir.toFile(), "snoozeloot-test.db"), 5);
    playerId = UUID.randomUUID();
  }

  @Test
  void initializesWritableDatabase() {
    assertTrue(storage.isWritable());
    assertTrue(storage.loadAll().isEmpty());
  }

  @Test
  void roundTripsPoints() {
    storage.saveNow(Map.of(playerId, new PlayerBalance(42, "Steve")));

    Map<UUID, PlayerBalance> loaded = storage.loadAll();

    assertEquals(1, loaded.size());
    assertEquals(42, loaded.get(playerId).points());
    assertEquals("Steve", loaded.get(playerId).name());
  }

  @Test
  void roundTripsMetaAndPurchaseCounts() {
    MetaRepository metaRepository = storage.asMetaRepository();
    PlayerMeta meta =
        PlayerMeta.empty()
            .withStreakDays(3)
            .withPurchaseCount("vip-rank", 2);

    metaRepository.saveNow(Map.of(playerId, meta));

    PlayerMeta loaded = metaRepository.loadAll().get(playerId);

    assertEquals(3, loaded.streakDays());
    assertEquals(2, loaded.purchaseCount("vip-rank"));
  }

  @Test
  void storesTransactionsAndTrimsOldEntries() {
    for (int i = 0; i < 7; i++) {
      storage.append(
          new TransactionRecord(
              playerId, "Steve", "item-" + i, 10 + i, System.currentTimeMillis() + i));
    }

    assertEquals(5, storage.recent(10).size());
    assertEquals(5, storage.byPlayer(playerId, 10).size());
    assertEquals("item-6", storage.recent(1).getFirst().itemId());
  }
}
