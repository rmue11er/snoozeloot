package dev.snoozeloot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ConfigMergerTest {

  @Test
  void migratesMessagesToEnglishOnVersionBumpFromV1() {
    YamlConfiguration defaults = new YamlConfiguration();
    defaults.set("config-version", 3);
    defaults.set("messages.points", "<prefix><gray>You have <gold><points></gold> SnoozePoints.</gray>");

    YamlConfiguration current = new YamlConfiguration();
    current.set("config-version", 1);
    current.set("messages.points", "<prefix><gray>Du hast <gold><points></gold> SnoozePoints.</gray>");

    ConfigMerger.applyVersionMigration(current, defaults, 1);

    assertEquals(
        "<prefix><gray>You have <gold><points></gold> SnoozePoints.</gray>",
        current.getString("messages.points"));
  }

  @Test
  void migratesNewSectionsOnVersionBumpToV3() {
    YamlConfiguration defaults = new YamlConfiguration();
    defaults.set("config-version", 3);
    defaults.set("language", "en");
    defaults.set("storage.type", "yaml");
    defaults.set("pay.enabled", true);
    defaults.set("pay.min-amount", 1);
    defaults.set("shop.confirm-above-price", 50);

    YamlConfiguration current = new YamlConfiguration();
    current.set("config-version", 2);

    ConfigMerger.applyVersionMigration(current, defaults, 2);

    assertEquals("en", current.getString("language"));
    assertEquals("yaml", current.getString("storage.type"));
    assertTrue(current.getBoolean("pay.enabled"));
    assertEquals(50, current.getInt("shop.confirm-above-price"));
  }
}
