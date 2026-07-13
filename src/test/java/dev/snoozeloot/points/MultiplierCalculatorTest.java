package dev.snoozeloot.points;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MultiplierCalculatorTest {

  @Test
  void returnsOneWithoutPermissions() {
    double result =
        MultiplierCalculator.highest(
            Map.of("snoozeloot.multiplier.vip", 1.5, "snoozeloot.multiplier.mvp", 2.0),
            perm -> false);
    assertEquals(1.0, result);
  }

  @Test
  void returnsHighestMatchingPermission() {
    double result =
        MultiplierCalculator.highest(
            Map.of("snoozeloot.multiplier.vip", 1.5, "snoozeloot.multiplier.mvp", 2.0),
            perm -> perm.equals("snoozeloot.multiplier.vip"));
    assertEquals(1.5, result);
  }

  @Test
  void picksMaxWhenMultipleMatch() {
    double result =
        MultiplierCalculator.highest(
            Map.of("snoozeloot.multiplier.vip", 1.5, "snoozeloot.multiplier.mvp", 2.0),
            perm -> true);
    assertEquals(2.0, result);
  }
}
