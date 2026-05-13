package ms.model;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class StorageData {

    private final ItemStack storedItem;
    private final long amount;
    private final StorageMode mode;
    private final boolean autoCollect;
    private final UUID owner;
    private final int version;

    // 旧コード互換用
    public StorageData(
            ItemStack storedItem,
            long amount,
            StorageMode mode,
            UUID owner,
            int version
    ) {
        this(storedItem, amount, mode, false, owner, version);
    }

    // 新仕様用
    public StorageData(
            ItemStack storedItem,
            long amount,
            StorageMode mode,
            boolean autoCollect,
            UUID owner,
            int version
    ) {
        this.storedItem = storedItem;
        this.amount = amount;
        this.mode = mode;
        this.autoCollect = autoCollect;
        this.owner = owner;
        this.version = version;
    }

    public ItemStack getStoredItem() {
        return storedItem;
    }

    public long getAmount() {
        return amount;
    }

    public StorageMode getMode() {
        return mode;
    }

    public boolean isAutoCollect() {
        return autoCollect;
    }

    public UUID getOwner() {
        return owner;
    }

    public int getVersion() {
        return version;
    }

    public StorageData withAmount(long amount) {
        return new StorageData(storedItem, amount, mode, autoCollect, owner, version);
    }

    public StorageData withMode(StorageMode mode) {
        return new StorageData(storedItem, amount, mode, autoCollect, owner, version);
    }

    public StorageData withAutoCollect(boolean autoCollect) {
        return new StorageData(storedItem, amount, mode, autoCollect, owner, version);
    }

    public StorageData withStoredItem(ItemStack storedItem) {
        return new StorageData(storedItem, amount, mode, autoCollect, owner, version);
    }

    public StorageData withOwner(UUID owner) {
        return new StorageData(storedItem, amount, mode, autoCollect, owner, version);
    }

    public StorageData withVersion(int version) {
        return new StorageData(storedItem, amount, mode, autoCollect, owner, version);
    }
}