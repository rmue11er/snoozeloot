package dev.snoozeloot.command;

import dev.snoozeloot.config.ConfigService;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;

public final class SnoozeHelpBuilder {
  public record Entry(String command, String descriptionKey) {}

  private SnoozeHelpBuilder() {}

  public static List<Entry> entries(CommandSender sender, ConfigService.PayConfig payConfig) {
    List<Entry> lines = new ArrayList<>();

    if (sender.hasPermission("snoozeloot.use")) {
      lines.add(new Entry("/snooze", "help-desc-balance"));
      lines.add(new Entry("/snooze shop", "help-desc-shop"));
      lines.add(new Entry("/snooze show <player>", "help-desc-show"));
      lines.add(new Entry("/snooze stats", "help-desc-stats"));
      if (sender.hasPermission("snoozeloot.history.admin")) {
        lines.add(new Entry("/snooze history [limit]", "help-desc-history-admin"));
      } else {
        lines.add(new Entry("/snooze history [limit]", "help-desc-history"));
      }
      lines.add(new Entry("/snooze help", "help-desc-help"));
    }

    if (sender.hasPermission("snoozeloot.pay") && payConfig.enabled()) {
      lines.add(new Entry("/snooze pay <player> <amount>", "help-desc-pay"));
    }

    if (sender.hasPermission("snoozeloot.admin")) {
      lines.add(new Entry("/snooze admin", "help-desc-admin"));
      lines.add(new Entry("/snooze give <player> <amount>", "help-desc-give"));
      lines.add(new Entry("/snooze remove <player> <amount>", "help-desc-remove"));
      lines.add(new Entry("/snooze set <player> <amount>", "help-desc-set"));
      lines.add(new Entry("/snooze reset <player>", "help-desc-reset"));
      lines.add(new Entry("/snooze reload", "help-desc-reload"));
      lines.add(new Entry("/snooze debug [player]", "help-desc-debug"));
    }

    return List.copyOf(lines);
  }
}
