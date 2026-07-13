package dev.snoozeloot.afk;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snoozeloot.meta.PlayerMeta;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AfkEligibilityTest {
  @Test
  void allowedWorldEmptyMeansAllWorlds() {
    Player player = mockPlayer("world", GameMode.SURVIVAL);
    assertTrue(AfkEligibility.allowedWorld(player, List.of()));
    assertTrue(AfkEligibility.allowedWorld(player, null));
  }

  @Test
  void allowedWorldChecksMembership() {
    Player player = mockPlayer("lobby", GameMode.SURVIVAL);
    assertTrue(AfkEligibility.allowedWorld(player, List.of("lobby", "spawn")));
    assertFalse(AfkEligibility.allowedWorld(player, List.of("afk")));
  }

  @Test
  void blockedGameModeBlocksCreativeAndSpectator() {
    Player player = mockPlayer("world", GameMode.CREATIVE);
    assertTrue(AfkEligibility.blockedGameMode(player, List.of(GameMode.CREATIVE)));
    assertFalse(AfkEligibility.blockedGameMode(player, List.of(GameMode.SPECTATOR)));
  }

  @Test
  void minActiveSecondsRequiresThreshold() {
    PlayerMeta meta = PlayerMeta.empty().withActivePlaySeconds(30);
    assertFalse(AfkEligibility.minActiveSeconds(meta, 60));
    assertTrue(AfkEligibility.minActiveSeconds(meta.withActivePlaySeconds(60), 60));
    assertTrue(AfkEligibility.minActiveSeconds(meta, 0));
  }

  @Test
  void maxDailyAfkResetsOnNewDay() {
    PlayerMeta meta = PlayerMeta.empty().withAfkSecondsToday(3600, "2020-01-01");
    assertTrue(AfkEligibility.maxDailyAfkSeconds(meta, "2020-01-02", 1800));
    assertFalse(AfkEligibility.maxDailyAfkSeconds(meta, "2020-01-01", 1800));
  }

  @Test
  void maxSessionAfkSecondsEnforcesLimit() {
    PlayerMeta meta = PlayerMeta.empty().withSessionAfkSeconds(100);
    assertFalse(AfkEligibility.maxSessionAfkSeconds(meta, 60));
    assertTrue(AfkEligibility.maxSessionAfkSeconds(meta, 120));
  }

  private static Player mockPlayer(String worldName, GameMode gameMode) {
    Player player = Mockito.mock(Player.class);
    World world = Mockito.mock(World.class);
    Mockito.when(player.getWorld()).thenReturn(world);
    Mockito.when(world.getName()).thenReturn(worldName);
    Mockito.when(player.getGameMode()).thenReturn(gameMode);
    return player;
  }
}
