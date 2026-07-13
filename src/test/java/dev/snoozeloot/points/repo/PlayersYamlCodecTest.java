package dev.snoozeloot.points.repo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class PlayersYamlCodecTest {
  private static final UUID PLAYER = UUID.fromString("887a2922-e0df-4fe3-b133-1cdbbc1bfb19");

  @Test
  void loadsLegacyFlatFormat() {
    YamlConfiguration yml = new YamlConfiguration();
    yml.set("players." + PLAYER, 26);

    var loaded = PlayersYamlCodec.load(yml);
    assertEquals(26, loaded.get(PLAYER).points());
  }

  @Test
  void roundTripsNamedFormat() {
    YamlConfiguration yml = new YamlConfiguration();
    PlayersYamlCodec.save(
        yml, Map.of(PLAYER, new PlayersYamlCodec.PlayerBalanceRecord(26, "cringekachu")));

    var loaded = PlayersYamlCodec.load(yml);
    assertEquals(26, loaded.get(PLAYER).points());
    assertEquals("cringekachu", loaded.get(PLAYER).name());
    assertTrue(yml.getString("players." + PLAYER + ".name").equals("cringekachu"));
  }
}
