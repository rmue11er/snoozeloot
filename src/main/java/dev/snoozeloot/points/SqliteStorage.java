package dev.snoozeloot.points;

import dev.snoozeloot.meta.MetaRepository;
import dev.snoozeloot.meta.PlayerMeta;
import dev.snoozeloot.points.repo.PointsRepository;
import dev.snoozeloot.shop.TransactionLog;
import dev.snoozeloot.shop.TransactionRecord;
import dev.snoozeloot.storage.DebouncedSnapshotWriter;
import dev.snoozeloot.storage.PluginEnvironment;
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
  private static final String UPSERT_PLAYER =
      """
      INSERT INTO players (uuid, name, points) VALUES (?, ?, ?)
      ON CONFLICT(uuid) DO UPDATE SET
        name = excluded.name,
        points = excluded.points
      """;

  private static final String UPSERT_META =
      """
      INSERT INTO player_meta (
        uuid, streak_days, last_streak_date, last_daily_bonus_date,
        last_weekly_bonus_week, afk_seconds_today, afk_day,
        session_afk_seconds, active_play_seconds
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(uuid) DO UPDATE SET
        streak_days = excluded.streak_days,
        last_streak_date = excluded.last_streak_date,
        last_daily_bonus_date = excluded.last_daily_bonus_date,
        last_weekly_bonus_week = excluded.last_weekly_bonus_week,
        afk_seconds_today = excluded.afk_seconds_today,
        afk_day = excluded.afk_day,
        session_afk_seconds = excluded.session_afk_seconds,
        active_play_seconds = excluded.active_play_seconds
      """;

  private final PluginEnvironment environment;
  private final File dbFile;
  private final int maxTransactionEntries;
  private volatile boolean writable;
  private final DebouncedSnapshotWriter<Map<UUID, PlayerBalance>> pointsWriter;
  private final DebouncedSnapshotWriter<Map<UUID, PlayerMeta>> metaWriter;

  public SqliteStorage(JavaPlugin plugin) {
    this(PluginEnvironment.from(plugin));
  }

  public SqliteStorage(PluginEnvironment environment) {
    this(
        environment,
        new File(environment.dataFolder(), "snoozeloot.db"),
        environment.transactionLogMaxEntries());
  }

  public SqliteStorage(PluginEnvironment environment, File dbFile, int maxTransactionEntries) {
    this.environment = environment;
    this.dbFile = dbFile;
    this.maxTransactionEntries = Math.max(1, maxTransactionEntries);
    this.writable = initSchema();
    this.pointsWriter =
        new DebouncedSnapshotWriter<>(environment.plugin(), this::saveAllPoints, 40L);
    this.metaWriter =
        new DebouncedSnapshotWriter<>(environment.plugin(), this::saveAllMeta, 40L);
  }

  public MetaRepository asMetaRepository() {
    return new MetaRepository() {
      @Override
      public Map<UUID, PlayerMeta> loadAll() {
        return SqliteStorage.this.loadAllMeta();
      }

      @Override
      public boolean isWritable() {
        return SqliteStorage.this.isWritable();
      }

      @Override
      public void saveAll(Map<UUID, PlayerMeta> meta) {
        SqliteStorage.this.saveAllMeta(meta);
      }

      @Override
      public void queueSave(Map<UUID, PlayerMeta> meta) {
        metaWriter.queue(new HashMap<>(meta));
      }

      @Override
      public void saveNow(Map<UUID, PlayerMeta> meta) {
        metaWriter.flushNow(new HashMap<>(meta));
      }
    };
  }

  @Override
  public boolean isWritable() {
    return writable;
  }

  @Override
  public Map<UUID, PlayerBalance> loadAll() {
    if (!writable) {
      return Map.of();
    }

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
      writable = false;
      environment
          .logger()
          .severe("Failed to load player balances from SQLite. Writes are disabled: " + e.getMessage());
    }
    return balances;
  }

  @Override
  public void queueSave(Map<UUID, PlayerBalance> balances) {
    pointsWriter.queue(new HashMap<>(balances));
  }

  @Override
  public void saveNow(Map<UUID, PlayerBalance> balances) {
    pointsWriter.flushNow(new HashMap<>(balances));
  }

  @Override
  public void saveAll(Map<UUID, PlayerBalance> balances) {
    saveAllPoints(balances);
  }

  private void saveAllPoints(Map<UUID, PlayerBalance> balances) {
    if (!writable || balances == null || balances.isEmpty()) {
      return;
    }

    try (Connection connection = openConnection()) {
      connection.setAutoCommit(false);
      try (PreparedStatement upsert = connection.prepareStatement(UPSERT_PLAYER)) {
        for (var entry : balances.entrySet()) {
          PlayerBalance balance = entry.getValue();
          upsert.setString(1, entry.getKey().toString());
          upsert.setString(2, balance.name());
          upsert.setInt(3, balance.points());
          upsert.addBatch();
        }
        upsert.executeBatch();
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      } finally {
        connection.setAutoCommit(true);
      }
    } catch (SQLException e) {
      environment.logger().warning("Failed to save player balances: " + e.getMessage());
    }
  }

  public Map<UUID, PlayerMeta> loadAllMeta() {
    if (!writable) {
      return Map.of();
    }

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
      writable = false;
      environment
          .logger()
          .severe("Failed to load player meta from SQLite. Writes are disabled: " + e.getMessage());
    }
    return meta;
  }

  public void saveAllMeta(Map<UUID, PlayerMeta> meta) {
    if (!writable || meta == null || meta.isEmpty()) {
      return;
    }

    try (Connection connection = openConnection()) {
      connection.setAutoCommit(false);
      try (PreparedStatement upsertMeta = connection.prepareStatement(UPSERT_META);
          PreparedStatement deletePurchases =
              connection.prepareStatement("DELETE FROM purchase_counts WHERE uuid = ?");
          PreparedStatement insertPurchase =
              connection.prepareStatement(
                  "INSERT INTO purchase_counts (uuid, item_id, count) VALUES (?, ?, ?)")) {
        for (var entry : meta.entrySet()) {
          UUID uuid = entry.getKey();
          PlayerMeta value = entry.getValue();

          upsertMeta.setString(1, uuid.toString());
          upsertMeta.setInt(2, value.streakDays());
          upsertMeta.setString(3, value.lastStreakDate());
          upsertMeta.setString(4, value.lastDailyBonusDate());
          upsertMeta.setString(5, value.lastWeeklyBonusWeek());
          upsertMeta.setLong(6, value.afkSecondsToday());
          upsertMeta.setString(7, value.afkDay());
          upsertMeta.setLong(8, value.sessionAfkSeconds());
          upsertMeta.setLong(9, value.activePlaySeconds());
          upsertMeta.addBatch();

          deletePurchases.setString(1, uuid.toString());
          deletePurchases.addBatch();

          for (var purchase : value.purchaseCounts().entrySet()) {
            insertPurchase.setString(1, uuid.toString());
            insertPurchase.setString(2, purchase.getKey());
            insertPurchase.setInt(3, purchase.getValue());
            insertPurchase.addBatch();
          }
        }

        upsertMeta.executeBatch();
        deletePurchases.executeBatch();
        insertPurchase.executeBatch();
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      } finally {
        connection.setAutoCommit(true);
      }
    } catch (SQLException e) {
      environment.logger().warning("Failed to save player meta: " + e.getMessage());
    }
  }

  @Override
  public void append(TransactionRecord record) {
    if (record == null || !writable) {
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
      environment.logger().warning("Failed to append transaction: " + e.getMessage());
    }
  }

  @Override
  public List<TransactionRecord> recent(int limit) {
    if (limit <= 0 || !writable) {
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
      environment.logger().warning("Failed to load recent transactions: " + e.getMessage());
    }
    return List.copyOf(records);
  }

  @Override
  public List<TransactionRecord> byPlayer(UUID playerId, int limit) {
    if (playerId == null || limit <= 0 || !writable) {
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
      environment.logger().warning("Failed to load player transactions: " + e.getMessage());
    }
    return List.copyOf(records);
  }

  private boolean initSchema() {
    if (!environment.dataFolder().exists() && !environment.dataFolder().mkdirs()) {
      environment.logger().severe("Could not create plugin data folder.");
      return false;
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
      return true;
    } catch (SQLException e) {
      environment.logger().severe("Failed to initialize snoozeloot.db: " + e.getMessage());
      return false;
    }
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
      environment.logger().warning("Failed to load purchase counts: " + e.getMessage());
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
    Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
    try (Statement statement = connection.createStatement()) {
      statement.execute("PRAGMA journal_mode=WAL");
      statement.execute("PRAGMA busy_timeout=5000");
    }
    return connection;
  }
}
