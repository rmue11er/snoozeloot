package dev.snoozeloot.points.repo;

import dev.snoozeloot.points.PlayerBalance;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class InMemoryPointsRepository implements PointsRepository {
  private Map<UUID, PlayerBalance> points = new HashMap<>();

  public void seed(Map<UUID, PlayerBalance> initial) {
    points = new HashMap<>(initial);
  }

  public Map<UUID, PlayerBalance> snapshot() {
    return new HashMap<>(points);
  }

  @Override
  public Map<UUID, PlayerBalance> loadAll() {
    return snapshot();
  }

  @Override
  public void saveAll(Map<UUID, PlayerBalance> balances) {
    this.points = new HashMap<>(balances);
  }
}
