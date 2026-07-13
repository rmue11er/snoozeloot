package dev.snoozeloot.points;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LeaderboardRanks {
  private LeaderboardRanks() {}

  public static int rankOf(Map<UUID, PlayerBalance> balances, UUID playerId) {
    if (playerId == null || balances == null || balances.isEmpty()) {
      return 0;
    }
    PlayerBalance target = balances.get(playerId);
    if (target == null || target.points() <= 0) {
      return 0;
    }

    List<Map.Entry<UUID, PlayerBalance>> sorted =
        balances.entrySet().stream()
            .filter(entry -> entry.getValue().points() > 0)
            .sorted(
                Comparator.<Map.Entry<UUID, PlayerBalance>>comparingInt(
                        entry -> entry.getValue().points())
                    .reversed())
            .toList();

    for (int i = 0; i < sorted.size(); i++) {
      if (sorted.get(i).getKey().equals(playerId)) {
        return i + 1;
      }
    }
    return 0;
  }

  public static int totalPlayers(Map<UUID, PlayerBalance> balances) {
    if (balances == null || balances.isEmpty()) {
      return 0;
    }
    return (int)
        balances.values().stream().filter(balance -> balance.points() > 0).count();
  }
}
