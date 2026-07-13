package dev.snoozeloot.config;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class MessageRenderer {
  private static final MiniMessage MINI = MiniMessage.miniMessage();
  private static final PlainTextComponentSerializer PLAIN =
      PlainTextComponentSerializer.plainText();

  private MessageRenderer() {}

  public static String normalizePlaceholders(String raw) {
    if (raw == null || raw.isEmpty()) {
      return "";
    }
    return raw.replaceAll("%([a-zA-Z0-9_]+)%", "<$1>");
  }

  public static Component render(String raw, String prefix, Map<String, String> placeholders) {
    String normalized = normalizePlaceholders(raw);
    TagResolver.Builder resolver = TagResolver.builder();

    if (prefix != null && !prefix.isEmpty()) {
      resolver.resolver(Placeholder.parsed("prefix", prefix));
    }
    if (placeholders != null) {
      for (var entry : placeholders.entrySet()) {
        resolver.resolver(Placeholder.parsed(entry.getKey(), entry.getValue()));
      }
    }

    return MINI.deserialize(normalized, resolver.build());
  }

  public static String toPlainText(Component component) {
    return PLAIN.serialize(component);
  }
}
