package ms.addonimpl.hopper;

import ms.core.StorageNBT;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;

public class HopperMoveListener implements Listener {

    private final StorageNBT nbt;

    public HopperMoveListener(StorageNBT nbt) {
        this.nbt = nbt;
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onMove(InventoryMoveItemEvent e) {

        ItemStack movingItem = e.getItem();

        if (movingItem == null
                || movingItem.getType() == Material.AIR
                || movingItem.getAmount() <= 0) {
            return;
        }

        /*
         * MS本体だけホッパー移動禁止。
         *
         * 通常アイテムのMS自動収納、
         * MS内アイテムの自動搬出、
         * 横流れ制御、
         * ロック処理、
         * 継続吸い出し処理は廃止。
         */
        if (nbt.isStorage(movingItem)) {
            e.setCancelled(true);
        }
    }
}