package dev.snoozeloot.util;

import org.bukkit.entity.Player;

public final class CommandPlaceholders {
  private CommandPlaceholders() {}

  public static String replace(String command, Player player, int points) {
    if (command == null || command.isEmpty()) {
      return command;
    }
    if (player == null) {
      return command
          .replace("%player%", "")
          .replace("%uuid%", "")
          .replace("%world%", "")
          .replace("%points%", Integer.toString(Math.max(0, points)));
    }

    return command
        .replace("%player%", player.getName())
        .replace("%uuid%", player.getUniqueId().toString())
        .replace("%world%", player.getWorld().getName())
        .replace("%points%", Integer.toString(Math.max(0, points)));
  }
}
