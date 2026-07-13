package dev.snoozeloot.integration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class UpdateChecker {
  private static final String VERSION_URL =
      "https://raw.githubusercontent.com/rmue11er/snoozeloot/main/version.txt";
  private static final String NOTIFY_PERMISSION = "snoozeloot.admin";

  private final JavaPlugin plugin;
  private final Logger logger;
  private volatile String latestVersion;

  public UpdateChecker(JavaPlugin plugin) {
    this.plugin = plugin;
    this.logger = plugin.getLogger();
  }

  public void checkAsync() {
    String current = plugin.getDescription().getVersion();

    Bukkit.getScheduler()
        .runTaskAsynchronously(
            plugin,
            () -> {
              String remote = fetchVersion(VERSION_URL);
              if (remote == null || remote.isBlank()) {
                return;
              }
              latestVersion = remote.trim();
              if (isNewer(latestVersion, current)) {
                logger.info(
                    "A new SnoozeLoot version is available: "
                        + latestVersion
                        + " (running "
                        + current
                        + ")");
                Bukkit.getScheduler()
                    .runTask(
                        plugin,
                        () ->
                            Bukkit.getOnlinePlayers().stream()
                                .filter(player -> player.hasPermission(NOTIFY_PERMISSION))
                                .forEach(
                                    player ->
                                        player.sendMessage(
                                            "§6[SnoozeLoot] §eUpdate available: §f"
                                                + latestVersion
                                                + "§e (you run §f"
                                                + current
                                                + "§e)")));
              }
            });
  }

  public void notifySender(CommandSender sender) {
    if (sender == null) {
      return;
    }
    String current = plugin.getDescription().getVersion();
    if (latestVersion != null && isNewer(latestVersion, current)) {
      sender.sendMessage(
          "§6[SnoozeLoot] §eUpdate available: §f" + latestVersion + "§e (running §f" + current + "§e)");
      return;
    }
    sender.sendMessage("§6[SnoozeLoot] §aYou are running SnoozeLoot " + current + ".");
  }

  private String fetchVersion(String url) {
    try {
      HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
      connection.setConnectTimeout(4000);
      connection.setReadTimeout(4000);
      connection.setRequestProperty("User-Agent", "SnoozeLoot/" + plugin.getDescription().getVersion());

      try (BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
        String line = reader.readLine();
        return line == null ? null : line.trim();
      }
    } catch (Exception e) {
      logger.fine("Update check skipped: " + e.getMessage());
      return null;
    }
  }

  static boolean isNewer(String remote, String current) {
    if (remote == null || current == null) {
      return false;
    }
    String[] remoteParts = remote.replaceAll("[^0-9.]", "").split("\\.");
    String[] currentParts = current.replaceAll("[^0-9.]", "").split("\\.");
    int length = Math.max(remoteParts.length, currentParts.length);
    for (int i = 0; i < length; i++) {
      int remotePart = i < remoteParts.length ? parsePart(remoteParts[i]) : 0;
      int currentPart = i < currentParts.length ? parsePart(currentParts[i]) : 0;
      if (remotePart > currentPart) {
        return true;
      }
      if (remotePart < currentPart) {
        return false;
      }
    }
    return false;
  }

  private static int parsePart(String value) {
    if (value == null || value.isBlank()) {
      return 0;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}
