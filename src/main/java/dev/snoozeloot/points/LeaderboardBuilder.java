package dev.snoozeloot.points;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

public final class LeaderboardBuilder {
  public record Entry(UUID uuid, int points, String name) {}

  private LeaderboardBuilder() {}

  public static List<Entry> topEntries(Map<UUID, PlayerBalance> balances, int limit) {
    if (limit <= 0 || balances.isEmpty()) {
      return List.of();
    }

    return balances.entrySet().stream()
        .filter(e -> e.getValue().points() > 0)
        .map(
            e ->
                new Entry(
                    e.getKey(),
                    e.getValue().points(),
                    e.getValue().name() == null ? null : e.getValue().name()))
        .sorted((a, b) -> Integer.compare(b.points(), a.points()))
        .limit(limit)
        .toList();
  }

  public static OptionalInt rank(Map<UUID, PlayerBalance> balances, UUID playerId) {
    if (playerId == null || balances.isEmpty()) {
      return OptionalInt.empty();
    }

    PlayerBalance self = balances.get(playerId);
    if (self == null || self.points() <= 0) {
      return OptionalInt.empty();
    }

    int higher =
        (int)
            balances.values().stream()
                .filter(b -> b.points() > self.points())
                .count();
    return OptionalInt.of(higher + 1);
  }

  public static List<Entry> rankedEntries(Map<UUID, PlayerBalance> balances) {
    if (balances.isEmpty()) {
      return List.of();
    }

    return balances.entrySet().stream()
        .filter(e -> e.getValue().points() > 0)
        .map(
            e ->
                new Entry(
                    e.getKey(),
                    e.getValue().points(),
                    e.getValue().name() == null ? null : e.getValue().name()))
        .sorted(Comparator.comparingInt(Entry::points).reversed())
        .toList();
  }
}
