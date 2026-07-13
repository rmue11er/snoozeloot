package dev.snoozeloot.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CommandPlaceholdersTest {

  @Test
  void replacesKnownPlaceholders() {
    Player player = Mockito.mock(Player.class);
    World world = Mockito.mock(World.class);
    UUID uuid = UUID.randomUUID();

    Mockito.when(player.getName()).thenReturn("Steve");
    Mockito.when(player.getUniqueId()).thenReturn(uuid);
    Mockito.when(player.getWorld()).thenReturn(world);
    Mockito.when(world.getName()).thenReturn("world");

    String result =
        CommandPlaceholders.replace(
            "give %player% %uuid% %world% %points%", player, 42);

    assertEquals("give Steve " + uuid + " world 42", result);
  }

  @Test
  void handlesNullPlayerSafely() {
    assertEquals("balance 0", CommandPlaceholders.replace("balance %points%", null, -5));
  }
}
