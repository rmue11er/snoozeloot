package dev.snoozeloot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bukkit.Sound;
import org.junit.jupiter.api.Test;

class BukkitSoundsTest {

  @Test
  void parsesValidSoundName() {
    assertEquals(Sound.BLOCK_NOTE_BLOCK_CHIME, BukkitSounds.parse("BLOCK_NOTE_BLOCK_CHIME", Sound.UI_BUTTON_CLICK));
  }

  @Test
  void fallsBackOnInvalidName() {
    assertEquals(Sound.UI_BUTTON_CLICK, BukkitSounds.parse("NOT_A_REAL_SOUND", Sound.UI_BUTTON_CLICK));
  }

  @Test
  void fallsBackOnBlankName() {
    assertEquals(Sound.UI_BUTTON_CLICK, BukkitSounds.parse("  ", Sound.UI_BUTTON_CLICK));
  }
}
