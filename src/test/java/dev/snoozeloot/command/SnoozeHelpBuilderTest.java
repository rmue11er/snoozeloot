package dev.snoozeloot.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.snoozeloot.config.ConfigService;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

class SnoozeHelpBuilderTest {

  @Test
  void playerWithUsePermissionGetsBaseCommands() {
    CommandSender sender = new FakeSender(false, true, false);

    List<SnoozeHelpBuilder.Entry> entries =
        SnoozeHelpBuilder.entries(sender, new ConfigService.PayConfig(true, 1, 30));

    assertTrue(entries.stream().anyMatch(e -> e.command().equals("/snooze")));
    assertTrue(entries.stream().anyMatch(e -> e.command().equals("/snooze shop")));
    assertTrue(entries.stream().anyMatch(e -> e.command().equals("/snooze help")));
    assertFalse(entries.stream().anyMatch(e -> e.command().contains("give")));
    assertFalse(entries.stream().anyMatch(e -> e.command().contains("pay")));
  }

  @Test
  void adminSeesAdminCommands() {
    CommandSender sender = new FakeSender(true, true, false);

    List<SnoozeHelpBuilder.Entry> entries =
        SnoozeHelpBuilder.entries(sender, new ConfigService.PayConfig(true, 1, 30));

    assertTrue(entries.stream().anyMatch(e -> e.command().equals("/snooze reload")));
    assertTrue(entries.stream().anyMatch(e -> e.command().equals("/snooze debug [player]")));
  }

  @Test
  void payOnlyWhenEnabledAndPermitted() {
    CommandSender sender = new FakeSender(false, true, true);

    assertTrue(
        SnoozeHelpBuilder.entries(sender, new ConfigService.PayConfig(true, 1, 30)).stream()
            .anyMatch(e -> e.command().startsWith("/snooze pay")));

    assertFalse(
        SnoozeHelpBuilder.entries(sender, new ConfigService.PayConfig(false, 1, 30)).stream()
            .anyMatch(e -> e.command().startsWith("/snooze pay")));
  }

  @Test
  void historyAdminGetsAdminDescription() {
    CommandSender sender = new FakeSender(true, true, false);

    SnoozeHelpBuilder.Entry history =
        SnoozeHelpBuilder.entries(sender, new ConfigService.PayConfig(true, 1, 30)).stream()
            .filter(e -> e.command().startsWith("/snooze history"))
            .findFirst()
            .orElseThrow();

    assertEquals("help-desc-history-admin", history.descriptionKey());
  }

  private static final class FakeSender implements CommandSender {
    private final boolean admin;
    private final boolean use;
    private final boolean pay;

    private FakeSender(boolean admin, boolean use, boolean pay) {
      this.admin = admin;
      this.use = use;
      this.pay = pay;
    }

    @Override
    public void sendMessage(String message) {}

    @Override
    public void sendMessage(String... messages) {}

    @Override
    public void sendMessage(java.util.UUID sender, String message) {}

    @Override
    public void sendMessage(java.util.UUID sender, String... messages) {}

    @Override
    public org.bukkit.Server getServer() {
      return null;
    }

    @Override
    public String getName() {
      return "Tester";
    }

    @Override
    public net.kyori.adventure.text.Component name() {
      return net.kyori.adventure.text.Component.text(getName());
    }

    @Override
    public Spigot spigot() {
      return null;
    }

    @Override
    public boolean isPermissionSet(String name) {
      return hasPermission(name);
    }

    @Override
    public boolean isPermissionSet(org.bukkit.permissions.Permission perm) {
      return hasPermission(perm.getName());
    }

    @Override
    public boolean hasPermission(String name) {
      return switch (name) {
        case "snoozeloot.admin", "snoozeloot.history.admin" -> admin;
        case "snoozeloot.use" -> use;
        case "snoozeloot.pay" -> pay;
        default -> false;
      };
    }

    @Override
    public boolean hasPermission(org.bukkit.permissions.Permission perm) {
      return hasPermission(perm.getName());
    }

    @Override
    public org.bukkit.permissions.PermissionAttachment addAttachment(
        org.bukkit.plugin.Plugin plugin, String name, boolean value) {
      return null;
    }

    @Override
    public org.bukkit.permissions.PermissionAttachment addAttachment(
        org.bukkit.plugin.Plugin plugin) {
      return null;
    }

    @Override
    public org.bukkit.permissions.PermissionAttachment addAttachment(
        org.bukkit.plugin.Plugin plugin, String name, boolean value, int ticks) {
      return null;
    }

    @Override
    public org.bukkit.permissions.PermissionAttachment addAttachment(
        org.bukkit.plugin.Plugin plugin, int ticks) {
      return null;
    }

    @Override
    public void removeAttachment(org.bukkit.permissions.PermissionAttachment attachment) {}

    @Override
    public void recalculatePermissions() {}

    @Override
    public java.util.Set<org.bukkit.permissions.PermissionAttachmentInfo> getEffectivePermissions() {
      return java.util.Set.of();
    }

    @Override
    public boolean isOp() {
      return admin;
    }

    @Override
    public void setOp(boolean value) {}
  }
}
