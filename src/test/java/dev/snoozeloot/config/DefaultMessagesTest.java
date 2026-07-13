package dev.snoozeloot.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DefaultMessagesTest {

  @Test
  void pointsOtherDefaultIsPresent() {
    String message = DefaultMessages.get("points-other");
    assertFalse(message.isBlank());
    assertTrue(message.contains("<target>"));
    assertTrue(message.contains("<points>"));
  }

  @Test
  void allCoreMessagesHaveDefaults() {
    for (String key :
        new String[] {
          "points",
          "points-other",
          "actionbar-afk",
          "afk-start",
          "afk-welcome-back",
          "afk-blocked-world",
          "afk-blocked-region",
          "afk-blocked-gamemode",
          "daily-bonus",
          "weekly-bonus",
          "pay-success",
          "pay-received",
          "pay-fail-disabled",
          "pay-fail-self",
          "pay-fail-min-amount",
          "pay-fail-insufficient",
          "pay-fail-cooldown",
          "admin-info",
          "admin-set",
          "admin-reset",
          "shop-not-enough-points",
          "shop-confirm-title",
          "shop-limit-reached",
          "stats-header",
          "stats-entry",
          "stats-empty",
          "stats-self",
          "transaction-log-header",
          "transaction-log-entry",
          "history-empty",
          "debug-header",
          "debug-line",
          "debug-player",
          "shop-success",
          "no-permission"
        }) {
      assertFalse(DefaultMessages.get(key).isBlank(), "Missing default for " + key);
    }
  }
}
