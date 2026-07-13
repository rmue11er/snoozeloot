package dev.snoozeloot.shop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class YamlTransactionLog implements TransactionLog {
  private final JavaPlugin plugin;
  private final File file;
  private final int maxEntries;

  private final List<TransactionRecord> entries = new ArrayList<>();

  public YamlTransactionLog(JavaPlugin plugin) {
    this(plugin, plugin.getConfig().getInt("shop.transaction-log-max-entries", 500));
  }

  public YamlTransactionLog(JavaPlugin plugin, int maxEntries) {
    this.plugin = plugin;
    this.file = new File(plugin.getDataFolder(), "transactions.yml");
    this.maxEntries = Math.max(1, maxEntries);
    loadFromDisk();
  }

  @Override
  public synchronized void append(TransactionRecord record) {
    if (record == null) {
      return;
    }
    entries.add(record);
    trimToMax();
    saveToDisk();
  }

  @Override
  public synchronized List<TransactionRecord> recent(int limit) {
    if (limit <= 0 || entries.isEmpty()) {
      return List.of();
    }
    int from = Math.max(0, entries.size() - limit);
    return List.copyOf(entries.subList(from, entries.size()));
  }

  @Override
  public synchronized List<TransactionRecord> byPlayer(UUID playerId, int limit) {
    if (playerId == null || limit <= 0 || entries.isEmpty()) {
      return List.of();
    }
    List<TransactionRecord> result = new ArrayList<>();
    for (int i = entries.size() - 1; i >= 0 && result.size() < limit; i--) {
      TransactionRecord record = entries.get(i);
      if (playerId.equals(record.uuid())) {
        result.add(record);
      }
    }
    return List.copyOf(result);
  }

  public void reloadMaxEntries() {
    trimToMax();
    saveToDisk();
  }

  private void trimToMax() {
    if (entries.size() <= maxEntries) {
      return;
    }
    int remove = entries.size() - maxEntries;
    entries.subList(0, remove).clear();
  }

  private void loadFromDisk() {
    ensureDataFolder();
    entries.clear();

    if (!file.exists()) {
      return;
    }

    YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
    List<?> raw = yml.getList("transactions");
    if (raw == null) {
      return;
    }

    for (Object item : raw) {
      TransactionRecord record = null;
      if (item instanceof ConfigurationSection section) {
        record = readSection(section);
      } else if (item instanceof Map<?, ?> map) {
        record = readMap(map);
      }
      if (record != null) {
        entries.add(record);
      }
    }
    trimToMax();
  }

  private void saveToDisk() {
    ensureDataFolder();

    YamlConfiguration yml = new YamlConfiguration();
    List<Map<String, Object>> serialized = new ArrayList<>();
    for (TransactionRecord record : entries) {
      serialized.add(writeRecord(record));
    }
    yml.set("transactions", serialized);

    try {
      yml.save(file);
    } catch (IOException e) {
      plugin.getLogger().warning("Failed to save transactions.yml: " + e.getMessage());
    }
  }

  private static TransactionRecord readSection(ConfigurationSection section) {
    return readValues(
        section.getString("uuid"),
        section.getString("player-name"),
        section.getString("item-id"),
        section.getInt("price", 0),
        section.getLong("timestamp-millis", 0L));
  }

  private static TransactionRecord readMap(Map<?, ?> map) {
    Object price = map.get("price");
    Object timestamp = map.get("timestamp-millis");
    return readValues(
        stringValue(map.get("uuid")),
        stringValue(map.get("player-name")),
        stringValue(map.get("item-id")),
        price instanceof Number number ? number.intValue() : 0,
        timestamp instanceof Number number ? number.longValue() : 0L);
  }

  private static TransactionRecord readValues(
      String uuidValue, String playerName, String itemId, int price, long timestampMillis) {
    try {
      if (itemId == null || itemId.isBlank()) {
        return null;
      }
      return new TransactionRecord(
          UUID.fromString(uuidValue == null ? "" : uuidValue),
          playerName,
          itemId,
          price,
          timestampMillis);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static String stringValue(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private static java.util.Map<String, Object> writeRecord(TransactionRecord record) {
    java.util.Map<String, Object> map = new LinkedHashMap<>();
    map.put("uuid", record.uuid().toString());
    if (record.playerName() != null) {
      map.put("player-name", record.playerName());
    }
    map.put("item-id", record.itemId());
    map.put("price", record.price());
    map.put("timestamp-millis", record.timestampMillis());
    return map;
  }

  private void ensureDataFolder() {
    if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
      plugin.getLogger().warning("Could not create plugin data folder.");
    }
  }
}
