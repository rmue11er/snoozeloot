package dev.snoozeloot.meta;

import java.util.HashMap;
import java.util.Map;

public record PlayerMeta(
    int streakDays,
    String lastStreakDate,
    String lastDailyBonusDate,
    String lastWeeklyBonusWeek,
    long afkSecondsToday,
    String afkDay,
    long sessionAfkSeconds,
    long activePlaySeconds,
    Map<String, Integer> purchaseCounts) {

  public PlayerMeta {
    streakDays = Math.max(0, streakDays);
    afkSecondsToday = Math.max(0, afkSecondsToday);
    sessionAfkSeconds = Math.max(0, sessionAfkSeconds);
    activePlaySeconds = Math.max(0, activePlaySeconds);
    purchaseCounts =
        purchaseCounts == null || purchaseCounts.isEmpty()
            ? Map.of()
            : Map.copyOf(purchaseCounts);
  }

  public static PlayerMeta empty() {
    return new PlayerMeta(0, null, null, null, 0L, null, 0L, 0L, Map.of());
  }

  public PlayerMeta withStreakDays(int value) {
    return new PlayerMeta(
        value,
        lastStreakDate,
        lastDailyBonusDate,
        lastWeeklyBonusWeek,
        afkSecondsToday,
        afkDay,
        sessionAfkSeconds,
        activePlaySeconds,
        purchaseCounts);
  }

  public PlayerMeta withLastStreakDate(String value) {
    return new PlayerMeta(
        streakDays,
        value,
        lastDailyBonusDate,
        lastWeeklyBonusWeek,
        afkSecondsToday,
        afkDay,
        sessionAfkSeconds,
        activePlaySeconds,
        purchaseCounts);
  }

  public PlayerMeta withLastDailyBonusDate(String value) {
    return new PlayerMeta(
        streakDays,
        lastStreakDate,
        value,
        lastWeeklyBonusWeek,
        afkSecondsToday,
        afkDay,
        sessionAfkSeconds,
        activePlaySeconds,
        purchaseCounts);
  }

  public PlayerMeta withLastWeeklyBonusWeek(String value) {
    return new PlayerMeta(
        streakDays,
        lastStreakDate,
        lastDailyBonusDate,
        value,
        afkSecondsToday,
        afkDay,
        sessionAfkSeconds,
        activePlaySeconds,
        purchaseCounts);
  }

  public PlayerMeta withAfkSecondsToday(long value, String day) {
    return new PlayerMeta(
        streakDays,
        lastStreakDate,
        lastDailyBonusDate,
        lastWeeklyBonusWeek,
        Math.max(0, value),
        day,
        sessionAfkSeconds,
        activePlaySeconds,
        purchaseCounts);
  }

  public PlayerMeta withSessionAfkSeconds(long value) {
    return new PlayerMeta(
        streakDays,
        lastStreakDate,
        lastDailyBonusDate,
        lastWeeklyBonusWeek,
        afkSecondsToday,
        afkDay,
        Math.max(0, value),
        activePlaySeconds,
        purchaseCounts);
  }

  public PlayerMeta withActivePlaySeconds(long value) {
    return new PlayerMeta(
        streakDays,
        lastStreakDate,
        lastDailyBonusDate,
        lastWeeklyBonusWeek,
        afkSecondsToday,
        afkDay,
        sessionAfkSeconds,
        Math.max(0, value),
        purchaseCounts);
  }

  public PlayerMeta withPurchaseCount(String itemId, int count) {
    Map<String, Integer> next = new HashMap<>(purchaseCounts);
    if (count <= 0) {
      next.remove(itemId);
    } else {
      next.put(itemId, count);
    }
    return new PlayerMeta(
        streakDays,
        lastStreakDate,
        lastDailyBonusDate,
        lastWeeklyBonusWeek,
        afkSecondsToday,
        afkDay,
        sessionAfkSeconds,
        activePlaySeconds,
        next);
  }

  public int purchaseCount(String itemId) {
    return purchaseCounts.getOrDefault(itemId, 0);
  }
}
