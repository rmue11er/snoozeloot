package dev.snoozeloot.points;

import dev.snoozeloot.points.repo.PointsRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PointsService {
  private final PointsRepository repository;
  private final Map<UUID, PlayerBalance> balances = new ConcurrentHashMap<>();
  private final Object transferLock = new Object();

  public PointsService(PointsRepository repository) {
    this.repository = repository;
    if (!repository.isWritable()) {
      throw new IllegalStateException("Points storage is not writable after load failure.");
    }
    balances.putAll(repository.loadAll());
  }

  public boolean isWritable() {
    return repository.isWritable();
  }

  public int get(UUID playerId) {
    PlayerBalance balance = balances.get(playerId);
    return balance == null ? 0 : balance.points();
  }

  public Optional<String> getName(UUID playerId) {
    PlayerBalance balance = balances.get(playerId);
    if (balance == null || balance.name() == null || balance.name().isBlank()) {
      return Optional.empty();
    }
    return Optional.of(balance.name());
  }

  public void rememberName(UUID playerId, String name) {
    if (name == null || name.isBlank()) {
      return;
    }
    balances.compute(
        playerId,
        (id, existing) -> {
          if (existing == null) {
            return new PlayerBalance(0, name);
          }
          return existing.withName(name);
        });
    persist();
  }

  public void set(UUID playerId, int value, String name) {
    int points = Math.max(0, value);
    balances.compute(
        playerId,
        (id, existing) -> {
          PlayerBalance base = existing == null ? new PlayerBalance(0, name) : existing;
          return new PlayerBalance(points, name != null ? name : base.name());
        });
    persist();
  }

  public int add(UUID playerId, int delta) {
    return add(playerId, delta, null);
  }

  public int add(UUID playerId, int delta, String name) {
    if (delta <= 0) {
      return get(playerId);
    }
    PlayerBalance next =
        balances.compute(
            playerId,
            (id, existing) -> {
              PlayerBalance base = existing == null ? new PlayerBalance(0, name) : existing;
              if (name != null) {
                base = base.withName(name);
              }
              return base.withPoints(safeAdd(base.points(), delta));
            });
    persist();
    return next.points();
  }

  public int remove(UUID playerId, int delta) {
    if (delta <= 0) {
      return get(playerId);
    }
    PlayerBalance next =
        balances.compute(
            playerId,
            (id, existing) -> {
              PlayerBalance base = existing == null ? new PlayerBalance(0, null) : existing;
              return base.withPoints(Math.max(0, base.points() - delta));
            });
    persist();
    return next.points();
  }

  public Optional<TransferResult> transfer(
      UUID senderId, UUID receiverId, int amount, String senderName, String receiverName) {
    if (senderId == null || receiverId == null || amount <= 0 || senderId.equals(receiverId)) {
      return Optional.empty();
    }

    synchronized (transferLock) {
      PlayerBalance sender = balances.get(senderId);
      int senderPoints = sender == null ? 0 : sender.points();
      if (senderPoints < amount) {
        return Optional.empty();
      }

      balances.compute(
          senderId,
          (id, existing) -> {
            PlayerBalance base = existing == null ? new PlayerBalance(0, senderName) : existing;
            if (senderName != null && !senderName.isBlank()) {
              base = base.withName(senderName);
            }
            return base.withPoints(base.points() - amount);
          });

      balances.compute(
          receiverId,
          (id, existing) -> {
            PlayerBalance base = existing == null ? new PlayerBalance(0, receiverName) : existing;
            if (receiverName != null && !receiverName.isBlank()) {
              base = base.withName(receiverName);
            }
            return base.withPoints(safeAdd(base.points(), amount));
          });

      persist();
      return Optional.of(
          new TransferResult(
              amount, get(senderId), get(receiverId)));
    }
  }

  public Optional<UUID> findUuidByName(String name) {
    if (name == null || name.isBlank()) {
      return Optional.empty();
    }
    String needle = name.toLowerCase(java.util.Locale.ROOT);
    for (var entry : balances.entrySet()) {
      String stored = entry.getValue().name();
      if (stored != null && stored.toLowerCase(java.util.Locale.ROOT).equals(needle)) {
        return Optional.of(entry.getKey());
      }
    }
    return Optional.empty();
  }

  public Map<UUID, PlayerBalance> snapshot() {
    return new HashMap<>(balances);
  }

  public void flushNow() {
    repository.saveNow(snapshot());
  }

  private void persist() {
    repository.queueSave(snapshot());
  }

  private static int safeAdd(int current, int delta) {
    long sum = (long) current + delta;
    if (sum > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) sum;
  }

  public record TransferResult(int amount, int senderBalance, int receiverBalance) {}
}
