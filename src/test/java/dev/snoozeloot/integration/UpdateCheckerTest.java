package dev.snoozeloot.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpdateCheckerTest {

  @Test
  void detectsNewerVersion() {
    assertTrue(UpdateChecker.isNewer("1.2.0", "1.1.0"));
    assertTrue(UpdateChecker.isNewer("2.0.0", "1.9.9"));
    assertFalse(UpdateChecker.isNewer("1.1.0", "1.1.0"));
    assertFalse(UpdateChecker.isNewer("1.0.0", "1.2.0"));
  }
}
