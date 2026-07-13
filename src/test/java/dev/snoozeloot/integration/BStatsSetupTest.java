package dev.snoozeloot.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BStatsSetupTest {

  @Test
  void pluginIdIsRegisteredValue() {
    assertEquals(32608, BStatsSetup.PLUGIN_ID);
  }
}
