package dev.snoozeloot.afk;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AfkPoolDetectorTest {

  @Test
  void detectsCircularPoolMovement() {
    List<AfkPoolDetector.Position> positions = new ArrayList<>();
    for (int i = 0; i < 16; i++) {
      double angle = i * (Math.PI / 4);
      positions.add(new AfkPoolDetector.Position(Math.cos(angle), Math.sin(angle)));
    }

    assertTrue(AfkPoolDetector.isCircularPool(positions, 12));
  }

  @Test
  void rejectsStraightLineMovement() {
    List<AfkPoolDetector.Position> positions = new ArrayList<>();
    for (int i = 0; i < 16; i++) {
      positions.add(new AfkPoolDetector.Position(i * 0.2, 0));
    }

    assertFalse(AfkPoolDetector.isCircularPool(positions, 12));
  }

  @Test
  void requiresMinimumSamples() {
    List<AfkPoolDetector.Position> positions =
        List.of(
            new AfkPoolDetector.Position(0, 0),
            new AfkPoolDetector.Position(1, 0),
            new AfkPoolDetector.Position(1, 1));

    assertFalse(AfkPoolDetector.isCircularPool(positions, 12));
  }
}
