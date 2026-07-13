package dev.snoozeloot.config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Messages {
  private final JavaPlugin plugin;
  private YamlConfiguration externalMessages = new YamlConfiguration();

  public Messages(JavaPlugin plugin) {
    this.plugin = plugin;
    reload();
  }

  public void reload() {
    externalMessages = loadExternalMessages(plugin, plugin.getConfig().getString("language", "en"));
  }

  public void send(CommandSender sender, String path, Map<String, String> placeholders) {
    sender.sendMessage(component(path, placeholders));
  }

  public Component component(String path, Map<String, String> placeholders) {
    String raw = resolve(path);
    return MessageRenderer.render(raw, resolvePrefix(), placeholders);
  }

  public Component miniMessage(String raw, Map<String, String> placeholders) {
    return MessageRenderer.render(raw, null, placeholders);
  }

  public String text(String path) {
    return resolve(path);
  }

  private String resolve(String path) {
    String raw = externalMessages.getString(path);
    if (raw == null || raw.isBlank()) {
      raw = plugin.getConfig().getString("messages." + path);
    }
    if (raw == null || raw.isBlank()) {
      raw = DefaultMessages.get(path);
    }
    return raw == null ? "" : raw;
  }

  private String resolvePrefix() {
    String prefix = externalMessages.getString("prefix");
    if (prefix == null || prefix.isBlank()) {
      prefix = plugin.getConfig().getString("messages.prefix");
    }
    if (prefix == null || prefix.isBlank()) {
      prefix = DefaultMessages.prefix();
    }
    return prefix;
  }

  private static YamlConfiguration loadExternalMessages(JavaPlugin plugin, String language) {
    String lang = language == null || language.isBlank() ? "en" : language.trim();
    String resourcePath = "messages_" + lang + ".yml";
    try (InputStream in = plugin.getResource(resourcePath)) {
      if (in == null) {
        return new YamlConfiguration();
      }
      return YamlConfiguration.loadConfiguration(
          new InputStreamReader(in, StandardCharsets.UTF_8));
    } catch (Exception e) {
      return new YamlConfiguration();
    }
  }
}
