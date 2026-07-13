package dev.snoozeloot.points;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LeaderboardRanksTest {

  @Test
  void rankOfReturnsZeroForMissingOrZeroBalance() {
    UUID player = UUID.randomUUID();
    Map<UUID, PlayerBalance> balances =
        Map.of(player, new PlayerBalance(0, "Zero"), UUID.randomUUID(), new PlayerBalance(10, "Top"));

    assertEquals(0, LeaderboardRanks.rankOf(balances, player));
    assertEquals(0, LeaderboardRanks.rankOf(balances, UUID.randomUUID()));
  }

  @Test
  void rankOfMatchesSortedOrder() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    UUID third = UUID.randomUUID();
    Map<UUID, PlayerBalance> balances =
        Map.of(
            first, new PlayerBalance(100, "A"),
            second, new PlayerBalance(50, "B"),
            third, new PlayerBalance(25, "C"));

    assertEquals(1, LeaderboardRanks.rankOf(balances, first));
    assertEquals(2, LeaderboardRanks.rankOf(balances, second));
    assertEquals(3, LeaderboardRanks.rankOf(balances, third));
    assertEquals(3, LeaderboardRanks.totalPlayers(balances));
  }
}
