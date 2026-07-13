package dev.snoozeloot.afk;

import dev.snoozeloot.integration.WorldGuardHook;
import dev.snoozeloot.meta.PlayerMeta;
import dev.snoozeloot.util.DateFormats;
import java.util.Collection;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public final class AfkEligibility {
  private AfkEligibility() {}

  public static boolean allowedWorld(Player player, Collection<String> allowedWorlds) {
    if (player == null) {
      return false;
    }
    if (allowedWorlds == null || allowedWorlds.isEmpty()) {
      return true;
    }
    return allowedWorlds.contains(player.getWorld().getName());
  }

  public static boolean blockedGameMode(Player player, Collection<GameMode> blockedModes) {
    if (player == null) {
      return false;
    }
    if (blockedModes == null || blockedModes.isEmpty()) {
      return false;
    }
    return blockedModes.contains(player.getGameMode());
  }

  public static boolean minActiveSeconds(PlayerMeta meta, long minActiveSeconds) {
    if (minActiveSeconds <= 0) {
      return true;
    }
    if (meta == null) {
      return false;
    }
    return meta.activePlaySeconds() >= minActiveSeconds;
  }

  public static boolean maxDailyAfkSeconds(PlayerMeta meta, String todayUtc, long maxDailyAfkSeconds) {
    if (maxDailyAfkSeconds <= 0) {
      return true;
    }
    if (meta == null) {
      return true;
    }
    if (!DateFormats.isSameDay(meta.afkDay(), todayUtc)) {
      return true;
    }
    return meta.afkSecondsToday() < maxDailyAfkSeconds;
  }

  public static boolean maxSessionAfkSeconds(PlayerMeta meta, long maxSessionAfkSeconds) {
    if (maxSessionAfkSeconds <= 0) {
      return true;
    }
    if (meta == null) {
      return true;
    }
    return meta.sessionAfkSeconds() < maxSessionAfkSeconds;
  }

  public static boolean allowedRegion(
      Player player, Collection<String> regionIds, WorldGuardHook worldGuardHook) {
    if (player == null) {
      return false;
    }
    if (regionIds == null || regionIds.isEmpty()) {
      return true;
    }
    if (worldGuardHook == null) {
      return true;
    }
    return worldGuardHook.isInAnyRegion(player, regionIds);
  }
}
