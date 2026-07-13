package dev.snoozeloot.meta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerMetaStoreTest {

  @Test
  void queuesFlushAfterUpdate() {
    InMemoryMetaRepository repository = new InMemoryMetaRepository();
    PlayerMetaStore store = new PlayerMetaStore(repository);
    UUID playerId = UUID.randomUUID();

    store.update(playerId, meta -> meta.withStreakDays(3));

    assertTrue(repository.lastQueuedSnapshot().containsKey(playerId));
    assertEquals(3, repository.lastQueuedSnapshot().get(playerId).streakDays());
  }

  private static final class InMemoryMetaRepository implements MetaRepository {
    private Map<UUID, PlayerMeta> saved = Map.of();
    private Map<UUID, PlayerMeta> queued = Map.of();

    @Override
    public Map<UUID, PlayerMeta> loadAll() {
      return new HashMap<>(saved);
    }

    @Override
    public void saveAll(Map<UUID, PlayerMeta> meta) {
      saved = new HashMap<>(meta);
    }

    @Override
    public void queueSave(Map<UUID, PlayerMeta> meta) {
      queued = new HashMap<>(meta);
    }

    Map<UUID, PlayerMeta> lastQueuedSnapshot() {
      return queued;
    }
  }
}
