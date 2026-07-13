package dev.snoozeloot.meta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerMetaStore {
  private final MetaRepository repository;
  private final Map<UUID, PlayerMeta> meta = new ConcurrentHashMap<>();

  public PlayerMetaStore(MetaRepository repository) {
    this.repository = repository;
    Map<UUID, PlayerMeta> loaded = repository.loadAll();
    if (!repository.isWritable()) {
      throw new IllegalStateException("Player meta storage is not writable after load failure.");
    }
    meta.putAll(loaded);
  }

  public boolean isWritable() {
    return repository.isWritable();
  }

  public PlayerMeta get(UUID playerId) {
    return meta.computeIfAbsent(playerId, id -> PlayerMeta.empty());
  }

  public PlayerMeta update(UUID playerId, Function<PlayerMeta, PlayerMeta> updater) {
    PlayerMeta updated =
        meta.compute(
            playerId,
            (id, existing) -> updater.apply(existing == null ? PlayerMeta.empty() : existing));
    queueFlush();
    return updated;
  }

  public int getPurchaseCount(UUID playerId, String itemId) {
    if (itemId == null || itemId.isBlank()) {
      return 0;
    }
    return get(playerId).purchaseCount(itemId);
  }

  public PlayerMeta incrementPurchase(UUID playerId, String itemId) {
    if (itemId == null || itemId.isBlank()) {
      return get(playerId);
    }
    return update(
        playerId,
        current -> current.withPurchaseCount(itemId, current.purchaseCount(itemId) + 1));
  }

  public Map<UUID, PlayerMeta> snapshot() {
    return new HashMap<>(meta);
  }

  public void queueFlush() {
    repository.queueSave(snapshot());
  }

  public void flushNow() {
    repository.saveNow(snapshot());
  }
}
