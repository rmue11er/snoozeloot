package dev.snoozeloot.points;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snoozeloot.points.repo.InMemoryPointsRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PayServiceTest {
  private PointsService pointsService;
  private PayService payService;
  private UUID sender;
  private UUID receiver;

  @BeforeEach
  void setUp() {
    pointsService = new PointsService(new InMemoryPointsRepository());
    payService = new PayService(pointsService, new PayService.PayConfig(5, 30));
    sender = UUID.randomUUID();
    receiver = UUID.randomUUID();
    pointsService.set(sender, 100, "Sender");
  }

  @Test
  void paysBetweenPlayers() {
    PayService.PayResult result =
        payService.pay(sender, receiver, 20, "Sender", "Receiver");

    assertTrue(result.success());
    assertEquals(80, pointsService.get(sender));
    assertEquals(20, pointsService.get(receiver));
  }

  @Test
  void rejectsSelfPayAndBelowMinimum() {
    assertFalse(payService.pay(sender, sender, 10, "Sender", "Sender").success());
    assertFalse(payService.pay(sender, receiver, 2, "Sender", "Receiver").success());
  }

  @Test
  void enforcesCooldown() {
    assertTrue(payService.pay(sender, receiver, 10, "Sender", "Receiver").success());
    PayService.PayResult second = payService.pay(sender, receiver, 10, "Sender", "Receiver");

    assertFalse(second.success());
    assertEquals("cooldown", second.reason());
    assertTrue(second.cooldownRemainingSeconds() > 0);
  }
}
