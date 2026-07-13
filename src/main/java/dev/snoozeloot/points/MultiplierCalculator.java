package dev.snoozeloot.points;

import java.util.Map;
import java.util.function.Predicate;

public final class MultiplierCalculator {
  private MultiplierCalculator() {}

  public static double highest(Map<String, Double> permissionToMultiplier, Predicate<String> hasPermission) {
    double multiplier = 1.0;
    for (var entry : permissionToMultiplier.entrySet()) {
      if (hasPermission.test(entry.getKey())) {
        multiplier = Math.max(multiplier, entry.getValue());
      }
    }
    return multiplier;
  }
}
