package dev.snoozeloot.util;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.WeekFields;

public final class DateFormats {
  public static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;
  public static final DateTimeFormatter WEEK =
      DateTimeFormatter.ofPattern("yyyy-'W'ww").withLocale(java.util.Locale.ROOT);

  private DateFormats() {}

  public static String todayUtc() {
    return LocalDate.now(ZoneOffset.UTC).format(DAY);
  }

  public static String weekKeyUtc() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    int week = today.get(WeekFields.ISO.weekOfWeekBasedYear());
    int year = today.get(WeekFields.ISO.weekBasedYear());
    return String.format(java.util.Locale.ROOT, "%d-W%02d", year, week);
  }

  public static LocalDate parseDay(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value, DAY);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  public static boolean isSameDay(String storedDay, String currentDay) {
    return storedDay != null && storedDay.equals(currentDay);
  }

  public static boolean isSameWeek(String storedWeek, String currentWeek) {
    return storedWeek != null && storedWeek.equals(currentWeek);
  }

  public static boolean isConsecutiveDay(String previousDay, String currentDay) {
    LocalDate previous = parseDay(previousDay);
    LocalDate current = parseDay(currentDay);
    if (previous == null || current == null) {
      return false;
    }
    return previous.plusDays(1).equals(current);
  }

  public static boolean isBeforeDay(String earlierDay, String laterDay) {
    LocalDate earlier = parseDay(earlierDay);
    LocalDate later = parseDay(laterDay);
    if (earlier == null || later == null) {
      return false;
    }
    return earlier.isBefore(later);
  }
}
