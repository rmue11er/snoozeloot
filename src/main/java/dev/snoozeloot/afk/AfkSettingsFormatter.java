package dev.snoozeloot.afk;

import java.util.Locale;

public final class AfkSettingsFormatter {
  private AfkSettingsFormatter() {}

  public static String ratePerMinute(double pointsPerInterval, long payoutIntervalSeconds) {
    if (payoutIntervalSeconds <= 0) {
      return "0.00";
    }
    double rate = (pointsPerInterval / payoutIntervalSeconds) * 60.0;
    return String.format(Locale.ROOT, "%.2f", rate);
  }
}
