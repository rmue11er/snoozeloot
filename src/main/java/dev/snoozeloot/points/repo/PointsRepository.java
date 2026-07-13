package dev.snoozeloot.points.repo;

import dev.snoozeloot.points.PlayerBalance;
import java.util.Map;
import java.util.UUID;

public interface PointsRepository {
  Map<UUID, PlayerBalance> loadAll();

  default boolean isWritable() {
    return true;
  }

  void saveAll(Map<UUID, PlayerBalance> balances);

  default void queueSave(Map<UUID, PlayerBalance> balances) {
    saveAll(balances);
  }

  default void saveNow(Map<UUID, PlayerBalance> balances) {
    saveAll(balances);
  }
}
