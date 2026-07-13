package dev.snoozeloot.util;

import java.util.Locale;
import org.bukkit.Sound;

public final class BukkitSounds {
  private BukkitSounds() {}

  public static Sound parse(String name, Sound fallback) {
    if (name == null || name.isBlank()) {
      return fallback;
    }
    try {
      return Sound.valueOf(name.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ignored) {
      return fallback;
    }
  }
}
