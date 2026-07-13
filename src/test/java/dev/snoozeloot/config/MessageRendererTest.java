package dev.snoozeloot.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MessageRendererTest {

  private static final String PREFIX = "<gray>[<gold>SnoozeLoot</gold>]</gray> ";

  @Test
  void rendersOwnPointsMessage() {
    var component =
        MessageRenderer.render(
            DefaultMessages.get("points"),
            PREFIX,
            Map.of("points", "42"));

    String text = MessageRenderer.toPlainText(component);
    assertTrue(text.contains("SnoozeLoot"));
    assertTrue(text.contains("42"));
    assertTrue(text.contains("SnoozePoints"));
  }

  @Test
  void rendersShowPointsMessageWithTarget() {
    var component =
        MessageRenderer.render(
            DefaultMessages.get("points-other"),
            PREFIX,
            Map.of("target", "Steve", "points", "15"));

    String text = MessageRenderer.toPlainText(component);
    assertTrue(text.contains("Steve"));
    assertTrue(text.contains("15"));
    assertTrue(text.contains("SnoozePoints"));
  }

  @Test
  void convertsLegacyPercentPlaceholders() {
    String normalized =
        MessageRenderer.normalizePlaceholders(
            "%prefix%<gray>You have <gold>%points%</gold> SnoozePoints.</gray>");

    var component =
        MessageRenderer.render(normalized, PREFIX, Map.of("points", "7"));

    String text = MessageRenderer.toPlainText(component);
    assertTrue(text.contains("7"));
    assertTrue(text.contains("SnoozePoints"));
  }

  @Test
  void rendersAfkStartMessage() {
    var component =
        MessageRenderer.render(
            DefaultMessages.get("afk-start"),
            PREFIX,
            Map.of("rate", "1.00"));

    String text = MessageRenderer.toPlainText(component);
    assertTrue(text.contains("snoozing"));
    assertTrue(text.contains("1.00"));
  }

  @Test
  void rendersAdminInfoMessage() {
    var component =
        MessageRenderer.render(
            DefaultMessages.get("admin-info"),
            PREFIX,
            Map.of(
                "afk_seconds", "300",
                "payout_seconds", "60",
                "points_per_interval", "1",
                "rate", "1.00"));

    String text = MessageRenderer.toPlainText(component);
    assertTrue(text.contains("300"));
    assertTrue(text.contains("60"));
    assertTrue(text.contains("1.00"));
  }

  @Test
  void emptyTemplateDoesNotCrash() {
    var component = MessageRenderer.render("", PREFIX, Map.of("points", "1"));
    assertEquals("", MessageRenderer.toPlainText(component));
  }
}
