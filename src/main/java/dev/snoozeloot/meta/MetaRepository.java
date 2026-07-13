package dev.snoozeloot.meta;

import java.util.Map;
import java.util.UUID;

public interface MetaRepository {
  Map<UUID, PlayerMeta> loadAll();

  default boolean isWritable() {
    return true;
  }

  void saveAll(Map<UUID, PlayerMeta> meta);

  default void queueSave(Map<UUID, PlayerMeta> meta) {
    saveAll(meta);
  }

  default void saveNow(Map<UUID, PlayerMeta> meta) {
    saveAll(meta);
  }
}
