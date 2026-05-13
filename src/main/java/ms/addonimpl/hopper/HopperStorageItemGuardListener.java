package ms.addonimpl.hopper;

import ms.core.StorageNBT;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;

public class HopperStorageItemGuardListener implements Listener {

    private final StorageNBT nbt;

    public HopperStorageItemGuardListener(StorageNBT nbt) {
        this.nbt = nbt;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onMoveStorageItem(InventoryMoveItemEvent e) {
        ItemStack item = e.getItem();

        if (!HopperSettings.BLOCK_STORAGE_ITEM_MOVE) {
            return;
        }

        if (nbt.isStorage(item)) {
            e.setCancelled(true);
        }
    }
}