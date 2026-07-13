package dev.snoozeloot.points;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.plugin.java.JavaPlugin;

public final class PayService {
  private final PointsService pointsService;
  private final PayConfig config;
  private final Map<UUID, Long> lastPayAtMillis = new ConcurrentHashMap<>();

  public PayService(PointsService pointsService, PayConfig config) {
    this.pointsService = pointsService;
    this.config = config;
  }

  public PayService(PointsService pointsService, JavaPlugin plugin) {
    this(pointsService, PayConfig.from(plugin));
  }

  public PayResult pay(UUID senderId, UUID receiverId, int amount, String senderName, String receiverName) {
    if (senderId == null || receiverId == null) {
      return PayResult.failure("invalid-player");
    }
    if (senderId.equals(receiverId)) {
      return PayResult.failure("self-pay");
    }
    if (amount < config.minAmount()) {
      return PayResult.failure("below-minimum");
    }

    long now = System.currentTimeMillis();
    Long lastPay = lastPayAtMillis.get(senderId);
    if (lastPay != null && now - lastPay < config.cooldownMillis()) {
      long secondsLeft = Math.max(1, (config.cooldownMillis() - (now - lastPay) + 999) / 1000);
      return PayResult.failure("cooldown", secondsLeft);
    }

    if (pointsService.get(senderId) < amount) {
      return PayResult.failure("insufficient-funds");
    }

    pointsService.remove(senderId, amount);
    pointsService.add(receiverId, amount, receiverName);
    if (senderName != null && !senderName.isBlank()) {
      pointsService.rememberName(senderId, senderName);
    }
    lastPayAtMillis.put(senderId, now);

    return PayResult.success(amount, pointsService.get(senderId), pointsService.get(receiverId));
  }

  public record PayConfig(int minAmount, long cooldownSeconds) {
    public long cooldownMillis() {
      return Math.max(0L, cooldownSeconds) * 1000L;
    }

    public static PayConfig from(JavaPlugin plugin) {
      var c = plugin.getConfig();
      return new PayConfig(
          c.getInt("pay.min-amount", 1), c.getLong("pay.cooldown-seconds", 30L));
    }
  }

  public record PayResult(
      boolean success,
      String reason,
      int amount,
      int senderBalance,
      int receiverBalance,
      long cooldownRemainingSeconds) {

    public static PayResult success(int amount, int senderBalance, int receiverBalance) {
      return new PayResult(true, null, amount, senderBalance, receiverBalance, 0L);
    }

    public static PayResult failure(String reason) {
      return new PayResult(false, reason, 0, 0, 0, 0L);
    }

    public static PayResult failure(String reason, long cooldownRemainingSeconds) {
      return new PayResult(false, reason, 0, 0, 0, cooldownRemainingSeconds);
    }
  }
}
