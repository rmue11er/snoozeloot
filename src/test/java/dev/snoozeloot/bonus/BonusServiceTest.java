package dev.snoozeloot.bonus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snoozeloot.config.ConfigService;
import dev.snoozeloot.meta.MetaRepository;
import dev.snoozeloot.meta.PlayerMeta;
import dev.snoozeloot.meta.PlayerMetaStore;
import dev.snoozeloot.util.DateFormats;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BonusServiceTest {
  private PlayerMetaStore metaStore;
  private BonusService bonusService;
  private UUID playerId;

  @BeforeEach
  void setUp() {
    metaStore = new PlayerMetaStore(new InMemoryMetaRepository());
    bonusService =
        new BonusService(
            metaStore,
            new ConfigService.BonusesConfig(
                new ConfigService.DailyBonusConfig(true, 25),
                new ConfigService.WeeklyBonusConfig(true, 100),
                new ConfigService.StreakBonusConfig(true, 30, 0.05, 1.5)));
    playerId = UUID.randomUUID();
  }

  @Test
  void claimsDailyBonusOncePerDay() {
    BonusService.BonusResult first = bonusService.claimDailyBonus(playerId);
    BonusService.BonusResult second = bonusService.claimDailyBonus(playerId);

    assertTrue(first.claimed());
    assertEquals(26, first.amount());
    assertFalse(second.claimed());
  }

  @Test
  void streakMultiplierCapsAtMax() {
    assertEquals(1.0, bonusService.streakMultiplier(0));
    assertEquals(1.10, bonusService.streakMultiplier(2), 0.001);
    assertEquals(1.50, bonusService.streakMultiplier(20), 0.001);
  }

  @Test
  void streakEligibleRequiresActivePlayMinutes() {
    metaStore.update(playerId, meta -> meta.withStreakDays(3).withActivePlaySeconds(60));
    assertFalse(bonusService.streakEligible(metaStore.get(playerId)));

    metaStore.update(playerId, meta -> meta.withActivePlaySeconds(30 * 60L));
    assertTrue(bonusService.streakEligible(metaStore.get(playerId)));
  }

  @Test
  void dailyBonusIncreasesStreakOnConsecutiveDays() {
    String yesterday =
        DateFormats.parseDay(DateFormats.todayUtc()).minusDays(1).format(DateFormats.DAY);
    metaStore.update(
        playerId,
        meta -> meta.withStreakDays(2).withLastStreakDate(yesterday).withLastDailyBonusDate(yesterday));

    BonusService.BonusResult result = bonusService.claimDailyBonus(playerId);

    assertTrue(result.claimed());
    assertEquals(3, result.streakDays());
    assertEquals(3, metaStore.get(playerId).streakDays());
  }

  private static final class InMemoryMetaRepository implements MetaRepository {
    private final Map<UUID, PlayerMeta> data = new HashMap<>();

    @Override
    public Map<UUID, PlayerMeta> loadAll() {
      return new HashMap<>(data);
    }

    @Override
    public void saveAll(Map<UUID, PlayerMeta> meta) {
      data.clear();
      data.putAll(meta);
    }
  }
}
