package dev.snoozeloot.points;

import dev.snoozeloot.meta.MetaRepository;
import dev.snoozeloot.meta.PlayerMeta;
import dev.snoozeloot.points.repo.PointsRepository;
import dev.snoozeloot.shop.TransactionLog;
import dev.snoozeloot.shop.TransactionRecord;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.plugin.java.JavaPlugin;

public final class SqliteStorage implements PointsRepository, TransactionLog {
  private final JavaPlugin plugin;
  private final File dbFile;
  private final int maxTransactionEntries;

  public SqliteStorage(JavaPlugin plugin) {
    this(
        plugin,
        new File(plugin.getDataFolder(), "snoozeloot.db"),
        plugin.getConfig().getInt("shop.transaction-log-max-entries", 500));
  }

  public SqliteStorage(JavaPlugin plugin, File dbFile, int maxTransactionEntries) {
    this.plugin = plugin;
    this.dbFile = dbFile;
    this.maxTransactionEntries = Math.max(1, maxTransactionEntries);
    init();
  }

  public MetaRepository asMetaRepository() {
    return new MetaRepository() {
      @Override
      public Map<UUID, PlayerMeta> loadAll() {
        return SqliteStorage.this.loadAllMeta();
      }

      @Override
      public void saveAll(Map<UUID, PlayerMeta> meta) {
        SqliteStorage.this.saveAllMeta(meta);
      }
    };
  }

  private void init() {
    if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
      plugin.getLogger().warning("Could not create plugin data folder.");
    }

    try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
      statement.executeUpdate(
          """
          CREATE TABLE IF NOT EXISTS players (
            uuid TEXT PRIMARY KEY,
            name TEXT,
            points INTEGER NOT NULL DEFAULT 0
          )
          """);
      statement.executeUpdate(
          """
          CREATE TABLE IF NOT EXISTS player_meta (
            uuid TEXT PRIMARY KEY,
            streak_days INTEGER NOT NULL DEFAULT 0,
            last_streak_date TEXT,
            last_daily_bonus_date TEXT,
            last_weekly_bonus_week TEXT,
            afk_seconds_today INTEGER NOT NULL DEFAULT 0,
            afk_day TEXT,
            session_afk_seconds INTEGER NOT NULL DEFAULT 0,
            active_play_seconds INTEGER NOT NULL DEFAULT 0
          )
          """);
      statement.executeUpdate(
          """
          CREATE TABLE IF NOT EXISTS purchase_counts (
            uuid TEXT NOT NULL,
            item_id TEXT NOT NULL,
            count INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY (uuid, item_id)
          )
          """);
      statement.executeUpdate(
          """
          CREATE TABLE IF NOT EXISTS transactions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            uuid TEXT NOT NULL,
            player_name TEXT,
            item_id TEXT NOT NULL,
            price INTEGER NOT NULL,
            timestamp_millis INTEGER NOT NULL
          )
          """);
    } catch (SQLException e) {
      plugin.getLogger().severe("Failed to initialize snoozeloot.db: " + e.getMessage());
    }
  }

  @Override
  public Map<UUID, PlayerBalance> loadAll() {
    Map<UUID, PlayerBalance> balances = new HashMap<>();
    try (Connection connection = openConnection();
        PreparedStatement statement =
            connection.prepareStatement("SELECT uuid, name, points FROM players");
        ResultSet rs = statement.executeQuery()) {
      while (rs.next()) {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        balances.put(uuid, new PlayerBalance(rs.getInt("points"), rs.getString("name")));
      }
    } catch (SQLException e) {
      plugin.getLogger().warning("Failed to load player balances: " + e.getMessage());
    }
    return balances;
  }

  @Override
  public void saveAll(Map<UUID, PlayerBalance> balances) {
    if (balances == null) {
      return;
    }

    try (Connection connection = openConnection()) {
      connection.setAutoCommit(false);
      try (PreparedStatement delete = connection.prepareStatement("DELETE FROM players");
          PreparedStatement insert =
              connection.prepareStatement(
                  "INSERT INTO players (uuid, name, points) VALUES (?, ?, ?)")) {
        delete.executeUpdate();
        for (var entry : balances.entrySet()) {
          PlayerBalance balance = entry.getValue();
          insert.setString(1, entry.getKey().toString());
          insert.setString(2, balance.name());
          insert.setInt(3, balance.points());
          insert.addBatch();
        }
        insert.executeBatch();
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      } finally {
        connection.setAutoCommit(true);
      }
    } catch (SQLException e) {
      plugin.getLogger().warning("Failed to save player balances: " + e.getMessage());
    }
  }

  public Map<UUID, PlayerMeta> loadAllMeta() {
    Map<UUID, PlayerMeta> meta = new HashMap<>();
    Map<UUID, Map<String, Integer>> purchaseCounts = loadPurchaseCounts();

    try (Connection connection = openConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                SELECT uuid, streak_days, last_streak_date, last_daily_bonus_date,
                       last_weekly_bonus_week, afk_seconds_today, afk_day,
                       session_afk_seconds, active_play_seconds
                FROM player_meta
                """);
        ResultSet rs = statement.executeQuery()) {
      while (rs.next()) {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        meta.put(
            uuid,
            new PlayerMeta(
                rs.getInt("streak_days"),
                rs.getString("last_streak_date"),
                rs.getString("last_daily_bonus_date"),
                rs.getString("last_weekly_bonus_week"),
                rs.getLong("afk_seconds_today"),
                rs.getString("afk_day"),
                rs.getLong("session_afk_seconds"),
                rs.getLong("active_play_seconds"),
                purchaseCounts.getOrDefault(uuid, Map.of())));
      }
    } catch (SQLException e) {
      plugin.getLogger().warning("Failed to load player meta: " + e.getMessage());
    }
    return meta;
  }

  public void saveAllMeta(Map<UUID, PlayerMeta> meta) {
    if (meta == null) {
      return;
    }

    try (Connection connection = openConnection()) {
      connection.setAutoCommit(false);
      try (PreparedStatement deleteMeta = connection.prepareStatement("DELETE FROM player_meta");
          PreparedStatement deletePurchases =
              connection.prepareStatement("DELETE FROM purchase_counts");
          PreparedStatement insertMeta =
              connection.prepareStatement(
                  """
                  INSERT INTO player_meta (
                    uuid, streak_days, last_streak_date, last_daily_bonus_date,
                    last_weekly_bonus_week, afk_seconds_today, afk_day,
                    session_afk_seconds, active_play_seconds
                  ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                  """);
          PreparedStatement insertPurchase =
              connection.prepareStatement(
                  "INSERT INTO purchase_counts (uuid, item_id, count) VALUES (?, ?, ?)")) {
        deleteMeta.executeUpdate();
        deletePurchases.executeUpdate();

        for (var entry : meta.entrySet()) {
          UUID uuid = entry.getKey();
          PlayerMeta value = entry.getValue();
          insertMeta.setString(1, uuid.toString());
          insertMeta.setInt(2, value.streakDays());
          insertMeta.setString(3, value.lastStreakDate());
          insertMeta.setString(4, value.lastDailyBonusDate());
          insertMeta.setString(5, value.lastWeeklyBonusWeek());
          insertMeta.setLong(6, value.afkSecondsToday());
          insertMeta.setString(7, value.afkDay());
          insertMeta.setLong(8, value.sessionAfkSeconds());
          insertMeta.setLong(9, value.activePlaySeconds());
          insertMeta.addBatch();

          for (var purchase : value.purchaseCounts().entrySet()) {
            insertPurchase.setString(1, uuid.toString());
            insertPurchase.setString(2, purchase.getKey());
            insertPurchase.setInt(3, purchase.getValue());
            insertPurchase.addBatch();
          }
        }

        insertMeta.executeBatch();
        insertPurchase.executeBatch();
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      } finally {
        connection.setAutoCommit(true);
      }
    } catch (SQLException e) {
      plugin.getLogger().warning("Failed to save player meta: " + e.getMessage());
    }
  }

  @Override
  public void append(TransactionRecord record) {
    if (record == null) {
      return;
    }

    try (Connection connection = openConnection();
        PreparedStatement insert =
            connection.prepareStatement(
                """
                INSERT INTO transactions (uuid, player_name, item_id, price, timestamp_millis)
                VALUES (?, ?, ?, ?, ?)
                """)) {
      insert.setString(1, record.uuid().toString());
      insert.setString(2, record.playerName());
      insert.setString(3, record.itemId());
      insert.setInt(4, record.price());
      insert.setLong(5, record.timestampMillis());
      insert.executeUpdate();
      trimTransactions(connection);
    } catch (SQLException e) {
      plugin.getLogger().warning("Failed to append transaction: " + e.getMessage());
    }
  }

  @Override
  public List<TransactionRecord> recent(int limit) {
    if (limit <= 0) {
      return List.of();
    }

    List<TransactionRecord> records = new ArrayList<>();
    try (Connection connection = openConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                SELECT uuid, player_name, item_id, price, timestamp_millis
                FROM transactions
                ORDER BY id DESC
                LIMIT ?
                """)) {
      statement.setInt(1, limit);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          records.add(readTransaction(rs));
        }
      }
    } catch (SQLException e) {
      plugin.getLogger().warning("Failed to load recent transactions: " + e.getMessage());
    }
    return List.copyOf(records);
  }

  @Override
  public List<TransactionRecord> byPlayer(UUID playerId, int limit) {
    if (playerId == null || limit <= 0) {
      return List.of();
    }

    List<TransactionRecord> records = new ArrayList<>();
    try (Connection connection = openConnection();
        PreparedStatement statement =
            connection.prepareStatement(
                """
                SELECT uuid, player_name, item_id, price, timestamp_millis
                FROM transactions
                WHERE uuid = ?
                ORDER BY id DESC
                LIMIT ?
                """)) {
      statement.setString(1, playerId.toString());
      statement.setInt(2, limit);
      try (ResultSet rs = statement.executeQuery()) {
        while (rs.next()) {
          records.add(readTransaction(rs));
        }
      }
    } catch (SQLException e) {
      plugin.getLogger().warning("Failed to load player transactions: " + e.getMessage());
    }
    return List.copyOf(records);
  }

  private Map<UUID, Map<String, Integer>> loadPurchaseCounts() {
    Map<UUID, Map<String, Integer>> counts = new HashMap<>();
    try (Connection connection = openConnection();
        PreparedStatement statement =
            connection.prepareStatement("SELECT uuid, item_id, count FROM purchase_counts");
        ResultSet rs = statement.executeQuery()) {
      while (rs.next()) {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        counts
            .computeIfAbsent(uuid, ignored -> new HashMap<>())
            .put(rs.getString("item_id"), Math.max(0, rs.getInt("count")));
      }
    } catch (SQLException e) {
      plugin.getLogger().warning("Failed to load purchase counts: " + e.getMessage());
    }
    return counts;
  }

  private void trimTransactions(Connection connection) throws SQLException {
    try (PreparedStatement countStatement =
            connection.prepareStatement("SELECT COUNT(*) FROM transactions");
        ResultSet countResult = countStatement.executeQuery()) {
      if (!countResult.next()) {
        return;
      }
      long count = countResult.getLong(1);
      if (count <= maxTransactionEntries) {
        return;
      }
      long remove = count - maxTransactionEntries;
      try (PreparedStatement delete =
          connection.prepareStatement(
              """
              DELETE FROM transactions
              WHERE id IN (
                SELECT id FROM transactions
                ORDER BY id ASC
                LIMIT ?
              )
              """)) {
        delete.setLong(1, remove);
        delete.executeUpdate();
      }
    }
  }

  private static TransactionRecord readTransaction(ResultSet rs) throws SQLException {
    return new TransactionRecord(
        UUID.fromString(rs.getString("uuid")),
        rs.getString("player_name"),
        rs.getString("item_id"),
        rs.getInt("price"),
        rs.getLong("timestamp_millis"));
  }

  private Connection openConnection() throws SQLException {
    return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
  }
}
