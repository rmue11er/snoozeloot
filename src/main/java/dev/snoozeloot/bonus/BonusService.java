package dev.snoozeloot.bonus;

import dev.snoozeloot.config.ConfigService;
import dev.snoozeloot.meta.PlayerMeta;
import dev.snoozeloot.meta.PlayerMetaStore;
import dev.snoozeloot.util.DateFormats;
import java.util.UUID;

public final class BonusService {
  private final PlayerMetaStore metaStore;
  private final ConfigService.BonusesConfig config;

  public BonusService(PlayerMetaStore metaStore, ConfigService.BonusesConfig config) {
    this.metaStore = metaStore;
    this.config = config;
  }

  public BonusResult claimDailyBonus(UUID playerId) {
    if (!config.daily().enabled()) {
      return BonusResult.alreadyClaimed("daily", 0, metaStore.get(playerId).streakDays());
    }

    String today = DateFormats.todayUtc();
    PlayerMeta current = metaStore.get(playerId);

    if (DateFormats.isSameDay(current.lastDailyBonusDate(), today)) {
      return BonusResult.alreadyClaimed("daily", 0, current.streakDays());
    }

    int streakDays = resolveNextStreakDays(current, today);
    double multiplier = streakMultiplier(streakDays);
    int amount = (int) Math.max(0, Math.floor(config.daily().points() * multiplier));

    metaStore.update(
        playerId,
        meta ->
            meta.withLastDailyBonusDate(today)
                .withLastStreakDate(today)
                .withStreakDays(streakDays));

    return BonusResult.claimed("daily", amount, streakDays, multiplier);
  }

  public BonusResult claimWeeklyBonus(UUID playerId) {
    if (!config.weekly().enabled()) {
      return BonusResult.alreadyClaimed("weekly", 0, metaStore.get(playerId).streakDays());
    }

    String week = DateFormats.weekKeyUtc();
    PlayerMeta current = metaStore.get(playerId);

    if (DateFormats.isSameWeek(current.lastWeeklyBonusWeek(), week)) {
      return BonusResult.alreadyClaimed("weekly", 0, current.streakDays());
    }

    int streakDays = current.streakDays();
    double multiplier = streakMultiplier(streakDays);
    int amount = (int) Math.max(0, Math.floor(config.weekly().points() * multiplier));

    metaStore.update(playerId, meta -> meta.withLastWeeklyBonusWeek(week));

    return BonusResult.claimed("weekly", amount, streakDays, multiplier);
  }

  public double streakMultiplier(PlayerMeta meta) {
    return streakMultiplier(meta == null ? 0 : meta.streakDays());
  }

  public double streakMultiplier(int streakDays) {
    if (!config.streak().enabled() || streakDays <= 0) {
      return 1.0;
    }

    var streak = config.streak();
    double bonus = streakDays * Math.max(0.0, streak.multiplierPerDay());
    return Math.min(streak.maxMultiplier(), 1.0 + bonus);
  }

  public boolean streakEligible(PlayerMeta meta) {
    if (!config.streak().enabled() || meta == null) {
      return false;
    }
    long requiredSeconds = config.streak().minMinutesPerDay() * 60L;
    if (requiredSeconds <= 0) {
      return meta.streakDays() > 0;
    }
    return meta.activePlaySeconds() >= requiredSeconds;
  }

  private int resolveNextStreakDays(PlayerMeta meta, String today) {
    if (meta.lastStreakDate() == null || meta.lastStreakDate().isBlank()) {
      return 1;
    }
    if (DateFormats.isSameDay(meta.lastStreakDate(), today)) {
      return Math.max(1, meta.streakDays());
    }
    if (DateFormats.isConsecutiveDay(meta.lastStreakDate(), today)) {
      return meta.streakDays() + 1;
    }
    return 1;
  }

  public record BonusResult(
      boolean claimed,
      String type,
      int amount,
      int streakDays,
      double streakMultiplier,
      String reason) {

    public static BonusResult claimed(
        String type, int amount, int streakDays, double streakMultiplier) {
      return new BonusResult(true, type, amount, streakDays, streakMultiplier, null);
    }

    public static BonusResult alreadyClaimed(String type, int amount, int streakDays) {
      return new BonusResult(false, type, amount, streakDays, 1.0, "already-claimed");
    }
  }
}
