package ms.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class StorageData {

    private final ItemStack storedItem;
    private final long amount;
    private final StorageMode mode;
    private final UUID owner;
    private final int version;

    public StorageData(ItemStack storedItem, long amount, StorageMode mode, UUID owner, int version) {
        this.storedItem = storedItem;
        this.amount = amount;
        this.mode = mode;
        this.owner = owner;
        this.version = version;
    }

    public ItemStack getStoredItem() {
        return storedItem == null ? null : storedItem.clone();
    }

    public long getAmount() {
        return amount;
    }

    public StorageMode getMode() {
        return mode;
    }

    public UUID getOwner() {
        return owner;
    }

    public int getVersion() {
        return version;
    }

    public StorageData withAmount(long newAmount) {
        return new StorageData(storedItem, newAmount, mode, owner, version);
    }

    public StorageData withMode(StorageMode newMode) {
        return new StorageData(storedItem, amount, newMode, owner, version);
    }
}