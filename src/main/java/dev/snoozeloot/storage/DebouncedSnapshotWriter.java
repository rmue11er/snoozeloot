package dev.snoozeloot.storage;

import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class DebouncedSnapshotWriter<T> {
  private final JavaPlugin plugin;
  private final Consumer<T> asyncSaver;
  private final long delayTicks;

  private volatile T pendingSnapshot;
  private volatile long generation;
  private BukkitTask scheduledTask;

  public DebouncedSnapshotWriter(JavaPlugin plugin, Consumer<T> asyncSaver, long delayTicks) {
    this.plugin = plugin;
    this.asyncSaver = asyncSaver;
    this.delayTicks = Math.max(1L, delayTicks);
  }

  public void queue(T snapshot) {
    pendingSnapshot = snapshot;
    long gen = ++generation;
    cancelScheduled();

    scheduledTask =
        Bukkit.getScheduler()
            .runTaskLater(
                plugin,
                () ->
                    Bukkit.getScheduler()
                        .runTaskAsynchronously(
                            plugin,
                            () -> {
                              if (gen != generation) {
                                return;
                              }
                              asyncSaver.accept(pendingSnapshot);
                            }),
                delayTicks);
  }

  public void flushNow(T snapshot) {
    pendingSnapshot = snapshot;
    generation++;
    cancelScheduled();
    asyncSaver.accept(snapshot);
  }

  private void cancelScheduled() {
    if (scheduledTask != null) {
      scheduledTask.cancel();
      scheduledTask = null;
    }
  }
}
