package dev.snoozeloot.afk;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snoozeloot.config.ConfigService;
import org.junit.jupiter.api.Test;

class ActivityDetectorTest {

  private static final ConfigService.AntiExploitConfig DEFAULT =
      new ConfigService.AntiExploitConfig(4, true, true, true);

  @Test
  void normalWalkingCountsAsActivity() {
    assertTrue(
        ActivityDetector.movementCountsAsActivity(false, false, false, false, DEFAULT));
  }

  @Test
  void waterMovementIgnoredWithoutRecentInput() {
    assertFalse(
        ActivityDetector.movementCountsAsActivity(false, false, true, false, DEFAULT));
  }

  @Test
  void waterMovementAllowedWithRecentInput() {
    assertTrue(
        ActivityDetector.movementCountsAsActivity(true, false, true, false, DEFAULT));
  }

  @Test
  void vehicleMovementIgnoredWithoutRecentInput() {
    assertFalse(
        ActivityDetector.movementCountsAsActivity(false, true, false, false, DEFAULT));
  }

  @Test
  void railMovementIgnoredWithoutRecentInput() {
    assertFalse(
        ActivityDetector.movementCountsAsActivity(false, false, false, true, DEFAULT));
  }
}
