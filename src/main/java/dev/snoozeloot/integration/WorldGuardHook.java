package dev.snoozeloot.integration;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class WorldGuardHook {
  private final Logger logger;
  private final boolean worldGuardPresent;
  private final Object regionContainer;
  private final Method createQueryMethod;
  private final Method getApplicableRegionsMethod;
  private final Method getRegionsMethod;
  private final Method getIdMethod;
  private final Method adaptLocationMethod;

  public WorldGuardHook(Logger logger) {
    this.logger = logger;
    WorldGuardReflection reflection = WorldGuardReflection.tryCreate(logger);
    this.worldGuardPresent = reflection.present;
    this.regionContainer = reflection.regionContainer;
    this.createQueryMethod = reflection.createQueryMethod;
    this.getApplicableRegionsMethod = reflection.getApplicableRegionsMethod;
    this.getRegionsMethod = reflection.getRegionsMethod;
    this.getIdMethod = reflection.getIdMethod;
    this.adaptLocationMethod = reflection.adaptLocationMethod;
  }

  public boolean isInAnyRegion(Player player, Collection<String> regionIds) {
    if (regionIds == null || regionIds.isEmpty()) {
      return true;
    }
    if (player == null) {
      return false;
    }
    if (!worldGuardPresent) {
      return true;
    }

    try {
      Object adaptedLocation = adaptLocationMethod.invoke(null, player.getLocation());
      Object query = createQueryMethod.invoke(regionContainer);
      Object applicable = getApplicableRegionsMethod.invoke(query, adaptedLocation);
      Object regions = getRegionsMethod.invoke(applicable);
      if (!(regions instanceof Iterable<?> iterable)) {
        return false;
      }

      for (Object region : iterable) {
        String id = (String) getIdMethod.invoke(region);
        if (id != null && regionIds.contains(id)) {
          return true;
        }
      }
      return false;
    } catch (ReflectiveOperationException e) {
      logger.warning("WorldGuard region check failed: " + e.getMessage());
      return true;
    }
  }

  public boolean worldGuardPresent() {
    return worldGuardPresent;
  }

  private static final class WorldGuardReflection {
    private final boolean present;
    private final Object regionContainer;
    private final Method createQueryMethod;
    private final Method getApplicableRegionsMethod;
    private final Method getRegionsMethod;
    private final Method getIdMethod;
    private final Method adaptLocationMethod;

    private WorldGuardReflection(
        boolean present,
        Object regionContainer,
        Method createQueryMethod,
        Method getApplicableRegionsMethod,
        Method getRegionsMethod,
        Method getIdMethod,
        Method adaptLocationMethod) {
      this.present = present;
      this.regionContainer = regionContainer;
      this.createQueryMethod = createQueryMethod;
      this.getApplicableRegionsMethod = getApplicableRegionsMethod;
      this.getRegionsMethod = getRegionsMethod;
      this.getIdMethod = getIdMethod;
      this.adaptLocationMethod = adaptLocationMethod;
    }

    private static WorldGuardReflection tryCreate(Logger logger) {
      Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
      if (plugin == null) {
        return empty();
      }

      try {
        Class<?> worldGuardClass = Class.forName("com.sk89q.worldguard.WorldGuard");
        Method getInstance = worldGuardClass.getMethod("getInstance");
        Object worldGuard = getInstance.invoke(null);

        Method getPlatform = worldGuardClass.getMethod("getPlatform");
        Object platform = getPlatform.invoke(worldGuard);

        Method getRegionContainer = platform.getClass().getMethod("getRegionContainer");
        Object regionContainer = getRegionContainer.invoke(platform);

        Method createQuery = regionContainer.getClass().getMethod("createQuery");
        Class<?> queryClass = createQuery.getReturnType();
        Class<?> bukkitAdapterClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
        Method adaptLocation = bukkitAdapterClass.getMethod("adapt", Location.class);
        Class<?> adaptedLocationClass = adaptLocation.getReturnType();

        Method getApplicableRegions = null;
        for (Method method : queryClass.getMethods()) {
          if (!method.getName().equals("getApplicableRegions") || method.getParameterCount() != 1) {
            continue;
          }
          if (method.getParameterTypes()[0].isAssignableFrom(adaptedLocationClass)) {
            getApplicableRegions = method;
            break;
          }
        }
        if (getApplicableRegions == null) {
          throw new NoSuchMethodException("getApplicableRegions(adaptedLocation)");
        }

        Class<?> applicableClass = getApplicableRegions.getReturnType();
        Method getRegions = applicableClass.getMethod("getRegions");
        Class<?> protectedRegionClass =
            Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion");
        Method getId = protectedRegionClass.getMethod("getId");

        return new WorldGuardReflection(
            true,
            regionContainer,
            createQuery,
            getApplicableRegions,
            getRegions,
            getId,
            adaptLocation);
      } catch (ReflectiveOperationException e) {
        logger.warning("WorldGuard is installed but could not be hooked: " + e.getMessage());
        return empty();
      }
    }

    private static WorldGuardReflection empty() {
      return new WorldGuardReflection(false, null, null, null, null, null, null);
    }
  }
}
