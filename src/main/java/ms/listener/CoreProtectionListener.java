package ms.listener;

import ms.core.StorageNBT;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.TradeSelectEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public class CoreProtectionListener implements Listener {

    private static final int CRAFT_RESULT_RAW_SLOT = 0;

    private final JavaPlugin plugin;
    private final StorageNBT nbt;

    public CoreProtectionListener(JavaPlugin plugin, StorageNBT nbt) {
        this.plugin = plugin;
        this.nbt = nbt;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (nbt.isStorage(e.getItemInHand())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent e) {
        CraftingInventory inv = e.getInventory();

        for (ItemStack item : inv.getMatrix()) {
            if (nbt.isStorage(item)) {
                inv.setResult(null);
                return;
            }
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        CraftingInventory inv = e.getInventory();

        for (ItemStack item : inv.getMatrix()) {
            if (nbt.isStorage(item)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        InventoryType topType = top.getType();

        if (topType == InventoryType.MERCHANT) {
            cleanMerchantRepeatedly(e.getWhoClicked(), top);

            ItemStack cursor = e.getCursor();
            ItemStack current = e.getCurrentItem();

            if (nbt.isStorage(cursor) || nbt.isStorage(current) || containsStorage(top)) {
                e.setCancelled(true);
                cleanMerchantRepeatedly(e.getWhoClicked(), top);
            }

            return;
        }

        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();

        boolean cursorStorage = nbt.isStorage(cursor);
        boolean currentStorage = nbt.isStorage(current);

        if (!cursorStorage && !currentStorage) {
            return;
        }

        Inventory clicked = e.getClickedInventory();

        if (clicked == null) {
            return;
        }

        if (isCraftingInventory(topType)) {
            if (e.getRawSlot() == CRAFT_RESULT_RAW_SLOT && currentStorage) {
                return;
            }

            if (clicked.equals(top)) {
                e.setCancelled(true);
            }

            return;
        }

        if (clicked.equals(top) && isBlockedInventory(topType)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        InventoryType topType = top.getType();

        if (topType == InventoryType.MERCHANT) {
            if (nbt.isStorage(e.getOldCursor())) {
                e.setCancelled(true);
            }

            cleanMerchantRepeatedly(e.getWhoClicked(), top);
            return;
        }

        ItemStack cursor = e.getOldCursor();

        if (!nbt.isStorage(cursor)) {
            return;
        }

        if (!isBlockedInventory(topType) && !isCraftingInventory(topType)) {
            return;
        }

        for (int rawSlot : e.getRawSlots()) {
            if (rawSlot < top.getSize()) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onTradeSelect(TradeSelectEvent e) {
        Inventory top = e.getView().getTopInventory();

        if (top.getType() != InventoryType.MERCHANT) {
            return;
        }

        cleanMerchantRepeatedly(e.getWhoClicked(), top);

        if (containsStorage(top) || nbt.isStorage(e.getWhoClicked().getItemOnCursor())) {
            e.setCancelled(true);
            cleanMerchantRepeatedly(e.getWhoClicked(), top);
        }
    }

    private boolean containsStorage(Inventory inventory) {
        if (inventory == null) {
            return false;
        }

        for (ItemStack item : inventory.getContents()) {
            if (nbt.isStorage(item)) {
                return true;
            }
        }

        return false;
    }

    private void cleanMerchantRepeatedly(HumanEntity human, Inventory top) {
        cleanMerchantLater(human, top, 0L);
        cleanMerchantLater(human, top, 1L);
        cleanMerchantLater(human, top, 2L);
        cleanMerchantLater(human, top, 3L);
        cleanMerchantLater(human, top, 5L);
    }

    private void cleanMerchantLater(HumanEntity human, Inventory top, long delay) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!(human instanceof Player player)) {
                return;
            }

            if (top == null || top.getType() != InventoryType.MERCHANT) {
                return;
            }

            for (int slot = 0; slot < top.getSize(); slot++) {
                ItemStack item = top.getItem(slot);

                if (!nbt.isStorage(item)) {
                    continue;
                }

                top.setItem(slot, null);

                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);

                for (ItemStack remain : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), remain);
                }

                player.updateInventory();
            }
        }, delay);
    }

    private boolean isCraftingInventory(InventoryType type) {
        return type == InventoryType.WORKBENCH
                || type == InventoryType.CRAFTING;
    }

    private boolean isBlockedInventory(InventoryType type) {
        return type == InventoryType.FURNACE
                || type == InventoryType.BLAST_FURNACE
                || type == InventoryType.SMOKER
                || type == InventoryType.ANVIL
                || type == InventoryType.SMITHING
                || type == InventoryType.CARTOGRAPHY
                || type == InventoryType.LOOM
                || type == InventoryType.STONECUTTER;
    }
}