package dev.snoozeloot.points.repo;

import dev.snoozeloot.points.PlayerBalance;
import java.util.Map;
import java.util.UUID;

public interface PointsRepository {
  Map<UUID, PlayerBalance> loadAll();

  void saveAll(Map<UUID, PlayerBalance> balances);
}
