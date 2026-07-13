package dev.snoozeloot.points;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snoozeloot.points.repo.InMemoryPointsRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PointsServiceTest {
  private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

  private InMemoryPointsRepository repository;
  private PointsService service;

  @BeforeEach
  void setUp() {
    repository = new InMemoryPointsRepository();
    service = new PointsService(repository);
  }

  @Test
  void startsAtZero() {
    assertEquals(0, service.get(PLAYER));
  }

  @Test
  void addIncreasesBalance() {
    service.add(PLAYER, 10, "Steve");
    assertEquals(10, service.get(PLAYER));
    service.add(PLAYER, 5, "Steve");
    assertEquals(15, service.get(PLAYER));
  }

  @Test
  void removeDoesNotGoBelowZero() {
    service.add(PLAYER, 5, "Steve");
    service.remove(PLAYER, 3);
    assertEquals(2, service.get(PLAYER));
    service.remove(PLAYER, 99);
    assertEquals(0, service.get(PLAYER));
  }

  @Test
  void setClampsNegativeValues() {
    service.set(PLAYER, -5, "Steve");
    assertEquals(0, service.get(PLAYER));
  }

  @Test
  void persistsToRepository() {
    service.add(PLAYER, 12, "Steve");
    assertEquals(12, repository.loadAll().get(PLAYER).points());
  }

  @Test
  void remembersPlayerName() {
    service.add(PLAYER, 1, "Steve");
    assertEquals("Steve", service.getName(PLAYER).orElseThrow());
    assertEquals(PLAYER, service.findUuidByName("Steve").orElseThrow());
  }

  @Test
  void statsAndBalanceUseSameSnapshot() {
    service.add(PLAYER, 26, "Steve");
    var top = LeaderboardBuilder.topEntries(service.snapshot(), 10);
    assertEquals(1, top.size());
    assertEquals(26, top.getFirst().points());
    assertEquals(26, service.get(PLAYER));
    assertEquals("Steve", top.getFirst().name());
  }
}
