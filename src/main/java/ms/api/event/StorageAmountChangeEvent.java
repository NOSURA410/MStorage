package ms.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class StorageAmountChangeEvent extends Event {

    private static final HandlerList HANDLERS =
            new HandlerList();

    private final Inventory inventory;
    private final ItemStack storageItem;
    private final long oldAmount;
    private final long newAmount;

    public StorageAmountChangeEvent(
            Inventory inventory,
            ItemStack storageItem,
            long oldAmount,
            long newAmount
    ) {
        this.inventory = inventory;
        this.storageItem = storageItem;
        this.oldAmount = oldAmount;
        this.newAmount = newAmount;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public ItemStack getStorageItem() {
        return storageItem;
    }

    public long getOldAmount() {
        return oldAmount;
    }

    public long getNewAmount() {
        return newAmount;
    }

    public long getDifference() {
        return newAmount - oldAmount;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}