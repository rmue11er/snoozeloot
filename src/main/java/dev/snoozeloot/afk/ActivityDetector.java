package dev.snoozeloot.afk;

import dev.snoozeloot.config.ConfigService;

public final class ActivityDetector {
  private ActivityDetector() {}

  /**
   * Returns whether player movement should count as real activity.
   *
   * <p>Normal walking counts. Movement is only ignored in exploit-prone situations (vehicle,
   * water, rails) when there has not been a recent explicit input.
   */
  public static boolean movementCountsAsActivity(
      boolean hasRecentInput,
      boolean inVehicle,
      boolean inWater,
      boolean onRails,
      ConfigService.AntiExploitConfig config) {
    if (hasRecentInput) {
      return true;
    }
    if (config.ignoreMovementWhenInVehicle() && inVehicle) {
      return false;
    }
    if (config.ignoreMovementWhenInWater() && inWater) {
      return false;
    }
    if (config.ignoreMovementWhenOnRails() && onRails) {
      return false;
    }
    return true;
  }
}
