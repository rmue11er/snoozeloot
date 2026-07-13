package dev.snoozeloot.afk;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AfkSettingsFormatterTest {

  @Test
  void calculatesRatePerMinute() {
    assertEquals("1.00", AfkSettingsFormatter.ratePerMinute(1, 60));
    assertEquals("2.00", AfkSettingsFormatter.ratePerMinute(1, 30));
  }

  @Test
  void handlesZeroIntervalSafely() {
    assertEquals("0.00", AfkSettingsFormatter.ratePerMinute(1, 0));
  }
}
