package dev.snoozeloot.shop;

import java.util.List;
import java.util.UUID;

public interface TransactionLog {
  void append(TransactionRecord record);

  List<TransactionRecord> recent(int limit);

  List<TransactionRecord> byPlayer(UUID playerId, int limit);
}
