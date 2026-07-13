package dev.snoozeloot.integration;

import dev.snoozeloot.afk.AfkEngine;
import dev.snoozeloot.meta.PlayerMetaStore;
import dev.snoozeloot.points.LeaderboardBuilder;
import dev.snoozeloot.points.PointsService;
import java.util.OptionalInt;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SnoozeLootPlaceholderExpansion extends PlaceholderExpansion {
  private final JavaPlugin plugin;
  private final PointsService points;
  private final PlayerMetaStore metaStore;
  private final AfkEngine afkEngine;

  public SnoozeLootPlaceholderExpansion(
      JavaPlugin plugin, PointsService points, PlayerMetaStore metaStore, AfkEngine afkEngine) {
    this.plugin = plugin;
    this.points = points;
    this.metaStore = metaStore;
    this.afkEngine = afkEngine;
  }

  @Override
  public @NotNull String getIdentifier() {
    return "snoozeloot";
  }

  @Override
  public @NotNull String getAuthor() {
    return "SnoozeLoot";
  }

  @Override
  public @NotNull String getVersion() {
    return plugin.getDescription().getVersion();
  }

  @Override
  public boolean persist() {
    return true;
  }

  @Override
  public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
    if (player == null) {
      return "";
    }

    return switch (params.toLowerCase(java.util.Locale.ROOT)) {
      case "points" -> Integer.toString(points.get(player.getUniqueId()));
      case "rank" -> formatRank(player.getUniqueId());
      case "afk" -> Boolean.toString(afkEngine.isAfk(player));
      case "streak" -> Integer.toString(metaStore.get(player.getUniqueId()).streakDays());
      default -> null;
    };
  }

  private String formatRank(java.util.UUID playerId) {
    OptionalInt rank = LeaderboardBuilder.rank(points.snapshot(), playerId);
    return rank.isPresent() ? Integer.toString(rank.getAsInt()) : "-";
  }
}
