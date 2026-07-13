package dev.snoozeloot.shop;

import java.util.UUID;

public record TransactionRecord(
    UUID uuid, String playerName, String itemId, int price, long timestampMillis) {

  public TransactionRecord {
    price = Math.max(0, price);
    if (playerName != null && playerName.isBlank()) {
      playerName = null;
    }
  }
}
