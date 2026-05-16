package ms.api;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.model.StorageData;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class StorageBridge {

    private static final long STACK_SIZE = 64L;
    private static final long LC_STACKS = 54L;
    private static final long LC_SIZE = STACK_SIZE * LC_STACKS;
    private static final long MAX_LC = 100_000_000L;
    private static final long MAX_AMOUNT = LC_SIZE * MAX_LC;

    private final StorageNBT nbt;
    private final StorageLore lore;
    private final StorageValidator validator;

    public StorageBridge(
            StorageNBT nbt,
            StorageLore lore,
            StorageValidator validator
    ) {
        this.nbt = nbt;
        this.lore = lore;
        this.validator = validator;
    }

    public boolean isStorage(ItemStack item) {
        return nbt.isStorage(item);
    }

    public StorageData read(ItemStack storageItem) {
        return nbt.read(storageItem);
    }

    public boolean isSameStoredItem(ItemStack a, ItemStack b) {
        ItemStack cleanA = normalize(a);
        ItemStack cleanB = normalize(b);

        if (cleanA == null || cleanB == null) {
            return false;
        }

        return validator.isSameStoredItem(cleanA, cleanB);
    }

    public boolean canAccept(ItemStack storageItem, ItemStack item) {
        if (!nbt.isStorage(storageItem)) {
            return false;
        }

        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        if (nbt.isStorage(item)) {
            return false;
        }

        StorageData data = nbt.read(storageItem);

        if (data == null) {
            return false;
        }

        return isSameStoredItem(data.getStoredItem(), item);
    }

    public long getAmount(ItemStack storageItem) {
        StorageData data = nbt.read(storageItem);

        if (data == null) {
            return 0L;
        }

        return data.getAmount();
    }

    public ItemStack getStoredItem(ItemStack storageItem) {
        StorageData data = nbt.read(storageItem);

        if (data == null || data.getStoredItem() == null) {
            return null;
        }

        ItemStack result = data.getStoredItem().clone();
        result.setAmount(1);

        return result;
    }

    public long getRemainingCapacity(ItemStack storageItem) {
        StorageData data = nbt.read(storageItem);

        if (data == null) {
            return 0L;
        }

        return Math.max(0L, MAX_AMOUNT - data.getAmount());
    }

    public long addAmount(ItemStack storageItem, long amount) {
        if (storageItem == null || amount <= 0L) {
            return 0L;
        }

        StorageData data = nbt.read(storageItem);

        if (data == null) {
            return 0L;
        }

        long current = data.getAmount();

        if (current >= MAX_AMOUNT) {
            return 0L;
        }

        long add = Math.min(amount, MAX_AMOUNT - current);

        if (add <= 0L) {
            return 0L;
        }

        StorageData updated = data.withAmount(current + add);

        if (!nbt.write(storageItem, updated)) {
            return 0L;
        }

        if (!lore.update(storageItem, updated)) {
            nbt.write(storageItem, data);
            lore.update(storageItem, data);
            return 0L;
        }

        return add;
    }

    public long removeAmount(ItemStack storageItem, long amount) {
        if (storageItem == null || amount <= 0L) {
            return 0L;
        }

        StorageData data = nbt.read(storageItem);

        if (data == null) {
            return 0L;
        }

        long current = data.getAmount();

        if (current <= 0L) {
            return 0L;
        }

        long remove = Math.min(amount, current);

        if (remove <= 0L) {
            return 0L;
        }

        StorageData updated = data.withAmount(current - remove);

        if (!nbt.write(storageItem, updated)) {
            return 0L;
        }

        if (!lore.update(storageItem, updated)) {
            nbt.write(storageItem, data);
            lore.update(storageItem, data);
            return 0L;
        }

        return remove;
    }

    public long moveStorageToStorage(
            ItemStack sourceStorage,
            ItemStack destinationStorage,
            long requestedAmount
    ) {
        if (sourceStorage == null || destinationStorage == null || requestedAmount <= 0L) {
            return 0L;
        }

        StorageData sourceData = nbt.read(sourceStorage);
        StorageData destinationData = nbt.read(destinationStorage);

        if (sourceData == null || destinationData == null) {
            return 0L;
        }

        if (!isSameStoredItem(sourceData.getStoredItem(), destinationData.getStoredItem())) {
            return 0L;
        }

        long sourceAmount = sourceData.getAmount();
        long destinationAmount = destinationData.getAmount();

        if (sourceAmount <= 0L || destinationAmount >= MAX_AMOUNT) {
            return 0L;
        }

        long moveAmount = Math.min(requestedAmount, sourceAmount);
        moveAmount = Math.min(moveAmount, MAX_AMOUNT - destinationAmount);

        if (moveAmount <= 0L) {
            return 0L;
        }

        StorageData newSourceData = sourceData.withAmount(sourceAmount - moveAmount);
        StorageData newDestinationData = destinationData.withAmount(destinationAmount + moveAmount);

        if (!nbt.write(sourceStorage, newSourceData)) {
            return 0L;
        }

        if (!nbt.write(destinationStorage, newDestinationData)) {
            nbt.write(sourceStorage, sourceData);
            lore.update(sourceStorage, sourceData);
            return 0L;
        }

        if (!lore.update(sourceStorage, newSourceData)) {
            rollback(sourceStorage, sourceData);
            rollback(destinationStorage, destinationData);
            return 0L;
        }

        if (!lore.update(destinationStorage, newDestinationData)) {
            rollback(sourceStorage, sourceData);
            rollback(destinationStorage, destinationData);
            return 0L;
        }

        return moveAmount;
    }

    public long storeItemStackIntoStorage(
            ItemStack storageItem,
            ItemStack sourceItem
    ) {
        if (!canAccept(storageItem, sourceItem)) {
            return 0L;
        }

        int sourceAmount = sourceItem.getAmount();

        if (sourceAmount <= 0) {
            return 0L;
        }

        long added = addAmount(storageItem, sourceAmount);

        if (added <= 0L) {
            return 0L;
        }

        sourceItem.setAmount(sourceAmount - (int) added);

        return added;
    }

    private void rollback(ItemStack storageItem, StorageData originalData) {
        if (storageItem == null || originalData == null) {
            return;
        }

        nbt.write(storageItem, originalData);
        lore.update(storageItem, originalData);
    }

    private ItemStack normalize(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }

        ItemStack clean = item.clone();
        clean.setAmount(1);

        return clean;
    }
}