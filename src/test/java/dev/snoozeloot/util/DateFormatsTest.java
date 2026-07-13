package dev.snoozeloot.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DateFormatsTest {

  @Test
  void consecutiveDayDetectionWorks() {
    assertTrue(DateFormats.isConsecutiveDay("2026-07-12", "2026-07-13"));
    assertFalse(DateFormats.isConsecutiveDay("2026-07-12", "2026-07-14"));
  }

  @Test
  void sameDayAndWeekChecks() {
    assertTrue(DateFormats.isSameDay("2026-07-13", "2026-07-13"));
    assertFalse(DateFormats.isSameDay("2026-07-12", "2026-07-13"));
    assertTrue(DateFormats.isSameWeek("2026-W28", "2026-W28"));
  }
}
