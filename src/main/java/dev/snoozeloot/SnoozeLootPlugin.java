package dev.snoozeloot;

import dev.snoozeloot.afk.AfkEngine;
import dev.snoozeloot.bonus.BonusService;
import dev.snoozeloot.command.SnoozeCommand;
import dev.snoozeloot.config.ConfigMerger;
import dev.snoozeloot.config.ConfigService;
import dev.snoozeloot.integration.BStatsSetup;
import dev.snoozeloot.integration.SnoozeLootPlaceholderExpansion;
import dev.snoozeloot.integration.UpdateChecker;
import dev.snoozeloot.integration.WorldGuardHook;
import dev.snoozeloot.meta.PlayerMetaStore;
import dev.snoozeloot.points.PayService;
import dev.snoozeloot.points.PointsService;
import dev.snoozeloot.shop.TransactionLog;
import dev.snoozeloot.storage.StorageFactory;
import dev.snoozeloot.ui.ShopGui;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SnoozeLootPlugin extends JavaPlugin {
  private ConfigService configService;
  private PointsService pointsService;
  private PlayerMetaStore metaStore;
  private TransactionLog transactionLog;
  private BonusService bonusService;
  private PayService payService;
  private WorldGuardHook worldGuardHook;
  private AfkEngine afkEngine;
  private ShopGui shopGui;
  private UpdateChecker updateChecker;
  private SnoozeLootPlaceholderExpansion placeholderExpansion;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    ConfigMerger.mergeMissingDefaults(this);

    try {
      this.configService = new ConfigService(this);
      StorageFactory.Bundle storage = StorageFactory.create(this, configService);
      this.pointsService = new PointsService(storage.pointsRepository());
      this.metaStore = new PlayerMetaStore(storage.metaRepository());
      this.transactionLog = storage.transactionLog();
    } catch (IllegalStateException e) {
      getLogger().severe("Failed to initialize SnoozeLoot storage: " + e.getMessage());
      Bukkit.getPluginManager().disablePlugin(this);
      return;
    }

    this.bonusService = new BonusService(metaStore, configService.bonuses());
    this.payService = createPayService();
    this.worldGuardHook = new WorldGuardHook(getLogger());
    this.shopGui =
        new ShopGui(this, configService, pointsService, metaStore, transactionLog);
    this.afkEngine =
        new AfkEngine(
            this, configService, pointsService, metaStore, bonusService, worldGuardHook);
    this.updateChecker = new UpdateChecker(this);

    var snoozeCommand = new SnoozeCommand(this);
    Objects.requireNonNull(getCommand("snooze"), "Command 'snooze' not defined in plugin.yml")
        .setExecutor(snoozeCommand);
    Objects.requireNonNull(getCommand("snooze"), "Command 'snooze' not defined in plugin.yml")
        .setTabCompleter(snoozeCommand);

    Bukkit.getPluginManager().registerEvents(shopGui, this);
    Bukkit.getPluginManager().registerEvents(afkEngine, this);

    BStatsSetup.register(this, configService, worldGuardHook);
    setupPlaceholderApi();
    updateChecker.checkAsync();

    afkEngine.start();
    getLogger()
        .info(
            "SnoozeLoot enabled (storage="
                + configService.storage().type()
                + ", worldguard="
                + worldGuardHook.worldGuardPresent()
                + ").");
  }

  @Override
  public void onDisable() {
    if (afkEngine != null) {
      afkEngine.stop();
    }
    if (pointsService != null) {
      pointsService.flushNow();
    }
    if (metaStore != null) {
      metaStore.flushNow();
    }
    if (placeholderExpansion != null) {
      placeholderExpansion.unregister();
    }
    getLogger().info("SnoozeLoot disabled.");
  }

  public void reloadRuntime() {
    pointsService.flushNow();
    metaStore.flushNow();

    ConfigMerger.mergeMissingDefaults(this);
    configService.reload();
    configService.messages().reload();

    bonusService = new BonusService(metaStore, configService.bonuses());
    payService = createPayService();
    afkEngine.reload(bonusService);
    shopGui.reload();
  }

  public ConfigService configService() {
    return configService;
  }

  public PointsService pointsService() {
    return pointsService;
  }

  public PlayerMetaStore metaStore() {
    return metaStore;
  }

  public PayService payService() {
    return payService;
  }

  public BonusService bonusService() {
    return bonusService;
  }

  public AfkEngine afkEngine() {
    return afkEngine;
  }

  public ShopGui shopGui() {
    return shopGui;
  }

  public TransactionLog transactionLog() {
    return transactionLog;
  }

  private PayService createPayService() {
    return new PayService(
        pointsService,
        new PayService.PayConfig(
            configService.pay().minAmount(), configService.pay().cooldownSeconds()));
  }

  private void setupPlaceholderApi() {
    if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
      return;
    }
    placeholderExpansion =
        new SnoozeLootPlaceholderExpansion(this, pointsService, metaStore, afkEngine);
    if (!placeholderExpansion.register()) {
      getLogger().warning("PlaceholderAPI expansion failed to register.");
    }
  }
}
