package dev.snoozeloot.afk;

import java.util.List;

public final class AfkPoolDetector {
  private static final int DEFAULT_MIN_SAMPLES = 12;
  private static final double MAX_SPREAD_BLOCKS = 3.0;
  private static final double MIN_ANGULAR_DISPLACEMENT_DEGREES = 300.0;
  private static final double MIN_RADIUS = 0.35;
  private static final double MAX_RADIUS = 2.5;

  private AfkPoolDetector() {}

  public record Position(double x, double z) {}

  public static boolean isCircularPool(List<Position> recentPositions) {
    return isCircularPool(recentPositions, DEFAULT_MIN_SAMPLES);
  }

  public static boolean isCircularPool(List<Position> recentPositions, int minSamples) {
    int requiredSamples = Math.max(4, minSamples);
    if (recentPositions == null || recentPositions.size() < requiredSamples) {
      return false;
    }

    double sumX = 0.0;
    double sumZ = 0.0;
    for (Position position : recentPositions) {
      sumX += position.x();
      sumZ += position.z();
    }
    double count = recentPositions.size();
    double centerX = sumX / count;
    double centerZ = sumZ / count;

    double minX = Double.MAX_VALUE;
    double maxX = -Double.MAX_VALUE;
    double minZ = Double.MAX_VALUE;
    double maxZ = -Double.MAX_VALUE;

    double[] radii = new double[recentPositions.size()];
    double[] angles = new double[recentPositions.size()];

    for (int i = 0; i < recentPositions.size(); i++) {
      Position position = recentPositions.get(i);
      minX = Math.min(minX, position.x());
      maxX = Math.max(maxX, position.x());
      minZ = Math.min(minZ, position.z());
      maxZ = Math.max(maxZ, position.z());

      double dx = position.x() - centerX;
      double dz = position.z() - centerZ;
      radii[i] = Math.hypot(dx, dz);
      angles[i] = Math.atan2(dz, dx);
    }

    if (Math.max(maxX - minX, maxZ - minZ) > MAX_SPREAD_BLOCKS) {
      return false;
    }

    double avgRadius = average(radii);
    if (avgRadius < MIN_RADIUS || avgRadius > MAX_RADIUS) {
      return false;
    }

    double radiusVariance = variance(radii, avgRadius);
    if (radiusVariance > 0.12) {
      return false;
    }

    double angularDisplacement = totalAngularDisplacement(angles);
    return angularDisplacement >= Math.toRadians(MIN_ANGULAR_DISPLACEMENT_DEGREES);
  }

  private static double totalAngularDisplacement(double[] angles) {
    double total = 0.0;
    for (int i = 1; i < angles.length; i++) {
      total += Math.abs(normalizeAngle(angles[i] - angles[i - 1]));
    }
    return total;
  }

  private static double normalizeAngle(double angle) {
    while (angle <= -Math.PI) {
      angle += 2 * Math.PI;
    }
    while (angle > Math.PI) {
      angle -= 2 * Math.PI;
    }
    return angle;
  }

  private static double average(double[] values) {
    double sum = 0.0;
    for (double value : values) {
      sum += value;
    }
    return sum / values.length;
  }

  private static double variance(double[] values, double mean) {
    double sum = 0.0;
    for (double value : values) {
      double delta = value - mean;
      sum += delta * delta;
    }
    return sum / values.length;
  }
}
