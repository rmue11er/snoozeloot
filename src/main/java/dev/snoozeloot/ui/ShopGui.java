package dev.snoozeloot.ui;

import dev.snoozeloot.config.ConfigService;
import dev.snoozeloot.meta.PlayerMetaStore;
import dev.snoozeloot.points.PointsService;
import dev.snoozeloot.shop.TransactionLog;
import dev.snoozeloot.shop.TransactionRecord;
import dev.snoozeloot.util.CommandPlaceholders;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public final class ShopGui implements Listener {
  private static final long CONFIRM_TIMEOUT_MS = 30_000L;

  private final JavaPlugin plugin;
  private final ConfigService config;
  private final PointsService points;
  private final PlayerMetaStore metaStore;
  private final TransactionLog transactionLog;

  private final Map<UUID, Inventory> open = new HashMap<>();
  private final Map<UUID, PendingPurchase> pendingConfirm = new HashMap<>();

  public ShopGui(
      JavaPlugin plugin,
      ConfigService config,
      PointsService points,
      PlayerMetaStore metaStore,
      TransactionLog transactionLog) {
    this.plugin = plugin;
    this.config = config;
    this.points = points;
    this.metaStore = metaStore;
    this.transactionLog = transactionLog;
  }

  public void reload() {
    pendingConfirm.clear();
  }

  public void open(Player player) {
    var shop = config.shop();
    Component title = config.messages().component("shop-title", Map.of());

    Inventory inv = Bukkit.createInventory(new ShopHolder(), shop.size(), title);
    for (var e : shop.itemsBySlot().entrySet()) {
      var item = e.getValue();
      ItemStack stack = new ItemStack(item.material());
      ItemMeta meta = stack.getItemMeta();
      meta.displayName(
          config
              .messages()
              .miniMessage(item.name(), Map.of("price", Integer.toString(item.price()))));
      meta.lore(
          item.lore().stream()
              .map(
                  l ->
                      config
                          .messages()
                          .miniMessage(l, Map.of("price", Integer.toString(item.price()))))
              .toList());
      stack.setItemMeta(meta);
      inv.setItem(item.slot(), stack);
    }

    open.put(player.getUniqueId(), inv);
    player.openInventory(inv);
  }

  private String substituteCommand(String cmd, Player player) {
    return CommandPlaceholders.replace(cmd, player, points.get(player.getUniqueId()));
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player p)) return;
    Inventory top = e.getView().getTopInventory();
    InventoryHolder holder = top.getHolder();
    if (!(holder instanceof ShopHolder)) return;

    e.setCancelled(true);

    if (e.getClickedInventory() == null || e.getClickedInventory() != top) return;
    int slot = e.getSlot();

    var shop = config.shop();
    var item = shop.itemsBySlot().get(slot);
    if (item == null) return;

    if (item.hasPurchaseLimit()) {
      int bought = metaStore.getPurchaseCount(p.getUniqueId(), item.id());
      if (bought >= item.purchaseLimit()) {
        config.messages().send(p, "shop-limit-reached", Map.of("item", item.id()));
        p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7f, 1.0f);
        return;
      }
    }

    int balance = points.get(p.getUniqueId());
    if (balance < item.price()) {
      config.messages().send(p, "shop-not-enough-points", Map.of());
      p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7f, 1.0f);
      return;
    }

    if (needsConfirmation(item)) {
      PendingPurchase pending = pendingConfirm.get(p.getUniqueId());
      long now = System.currentTimeMillis();
      if (pending != null
          && pending.slot() == slot
          && pending.itemId().equals(item.id())
          && now - pending.createdAtMs() <= CONFIRM_TIMEOUT_MS) {
        completePurchase(p, item);
        pendingConfirm.remove(p.getUniqueId());
        return;
      }

      pendingConfirm.put(
          p.getUniqueId(), new PendingPurchase(slot, item.id(), now));
      config
          .messages()
          .send(
              p,
              "shop-confirm-title",
              Map.of("item", item.id(), "price", Integer.toString(item.price())));
      return;
    }

    completePurchase(p, item);
  }

  private boolean canPurchase(Player player, ConfigService.ShopItem item) {
    if (item.price() < 0) {
      return false;
    }
    if (item.hasPurchaseLimit()) {
      int bought = metaStore.getPurchaseCount(player.getUniqueId(), item.id());
      if (bought >= item.purchaseLimit()) {
        return false;
      }
    }
    return points.get(player.getUniqueId()) >= item.price();
  }

  private boolean needsConfirmation(ConfigService.ShopItem item) {
    int threshold = config.shop().confirmAbovePrice();
    return threshold >= 0 && item.price() >= threshold;
  }

  private void completePurchase(Player p, ConfigService.ShopItem item) {
    if (!canPurchase(p, item)) {
      config.messages().send(p, "shop-not-enough-points", Map.of());
      p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.7f, 1.0f);
      return;
    }

    points.remove(p.getUniqueId(), item.price());
    if (item.hasPurchaseLimit()) {
      metaStore.incrementPurchase(p.getUniqueId(), item.id());
    }

    p.closeInventory();
    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);

    config
        .messages()
        .send(
            p,
            "shop-success",
            Map.of("item", item.id(), "price", Integer.toString(item.price())));

    transactionLog.append(
        new TransactionRecord(
            p.getUniqueId(),
            p.getName(),
            item.id(),
            item.price(),
            System.currentTimeMillis()));

    ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
    for (String cmd : item.commands()) {
      if (cmd == null || cmd.isBlank()) continue;
      Bukkit.dispatchCommand(console, substituteCommand(cmd, p));
    }
  }

  @EventHandler
  public void onClose(InventoryCloseEvent e) {
    UUID id = e.getPlayer().getUniqueId();
    open.remove(id);
    pendingConfirm.remove(id);
  }

  private record PendingPurchase(int slot, String itemId, long createdAtMs) {}

  private static final class ShopHolder implements InventoryHolder {
    @Override
    public Inventory getInventory() {
      return null;
    }
  }
}
