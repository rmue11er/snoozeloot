package dev.snoozeloot.points;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LeaderboardBuilderTest {
  private static final UUID A = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID B = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID C = UUID.fromString("00000000-0000-0000-0000-000000000003");

  @Test
  void sortsByPointsDescending() {
    Map<UUID, PlayerBalance> points =
        Map.of(
            A, new PlayerBalance(10, "A"),
            B, new PlayerBalance(30, "B"),
            C, new PlayerBalance(20, "C"));
    var top = LeaderboardBuilder.topEntries(points, 10);

    assertEquals(3, top.size());
    assertEquals(B, top.get(0).uuid());
    assertEquals(30, top.get(0).points());
    assertEquals(C, top.get(1).uuid());
    assertEquals(A, top.get(2).uuid());
  }

  @Test
  void limitsResultSize() {
    Map<UUID, PlayerBalance> points =
        Map.of(
            A, new PlayerBalance(10, "A"),
            B, new PlayerBalance(30, "B"),
            C, new PlayerBalance(20, "C"));
    var top = LeaderboardBuilder.topEntries(points, 2);

    assertEquals(2, top.size());
    assertEquals(B, top.get(0).uuid());
    assertEquals(C, top.get(1).uuid());
  }

  @Test
  void ignoresZeroBalances() {
    Map<UUID, PlayerBalance> points =
        Map.of(
            A, new PlayerBalance(0, "A"),
            C, new PlayerBalance(5, "C"));
    var top = LeaderboardBuilder.topEntries(points, 10);
    assertEquals(1, top.size());
    assertEquals(C, top.get(0).uuid());
  }

  @Test
  void returnsEmptyWhenNobodyHasPoints() {
    assertEquals(0, LeaderboardBuilder.topEntries(Map.of(A, new PlayerBalance(0, "A")), 10).size());
  }
}
