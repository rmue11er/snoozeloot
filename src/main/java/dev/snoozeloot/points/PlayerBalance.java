package dev.snoozeloot.points;

import java.util.UUID;

public record PlayerBalance(int points, String name) {
  public PlayerBalance {
    points = Math.max(0, points);
  }

  public PlayerBalance withPoints(int newPoints) {
    return new PlayerBalance(newPoints, name);
  }

  public PlayerBalance withName(String newName) {
    if (newName == null || newName.isBlank()) {
      return this;
    }
    return new PlayerBalance(points, newName);
  }
}
