package dev.snoozeloot.meta;

import java.util.Map;
import java.util.UUID;

public interface MetaRepository {
  Map<UUID, PlayerMeta> loadAll();

  void saveAll(Map<UUID, PlayerMeta> meta);
}
