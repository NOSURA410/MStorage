package ms.api;

import ms.api.event.StorageAmountChangeEvent;
import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.manager.StorageLoreUpdateQueue;
import ms.model.StorageData;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class StorageBridge {

    private static final long STACK_SIZE = 64L;
    private static final long LC_STACKS = 54L;
    private static final long LC_SIZE = STACK_SIZE * LC_STACKS;
    private static final long MAX_LC = 100_000_000L;
    private static final long MAX_AMOUNT = LC_SIZE * MAX_LC;

    private final JavaPlugin plugin;
    private final StorageNBT nbt;
    private final StorageLore lore;
    private final StorageValidator validator;
    private final StorageLoreUpdateQueue loreQueue;

    public StorageBridge(
            JavaPlugin plugin,
            StorageNBT nbt,
            StorageLore lore,
            StorageValidator validator,
            StorageLoreUpdateQueue loreQueue
    ) {
        this.plugin = plugin;
        this.nbt = nbt;
        this.lore = lore;
        this.validator = validator;
        this.loreQueue = loreQueue;
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
        return addAmount(null, storageItem, amount);
    }

    public long addAmount(
            Inventory inventory,
            ItemStack storageItem,
            long amount
    ) {
        if (storageItem == null || amount <= 0L) {
            return 0L;
        }

        StorageData data = nbt.read(storageItem);

        if (data == null) {
            return 0L;
        }

        long oldAmount = data.getAmount();

        if (oldAmount >= MAX_AMOUNT) {
            return 0L;
        }

        long add = Math.min(amount, MAX_AMOUNT - oldAmount);

        if (add <= 0L) {
            return 0L;
        }

        long newAmount = oldAmount + add;

        StorageData updated = data.withAmount(newAmount);

        if (!nbt.write(storageItem, updated)) {
            return 0L;
        }

        loreQueue.queue(storageItem);

        callAmountChangeEvent(
                inventory,
                storageItem,
                oldAmount,
                newAmount
        );

        return add;
    }

    public long removeAmount(ItemStack storageItem, long amount) {
        return removeAmount(null, storageItem, amount);
    }

    public long removeAmount(
            Inventory inventory,
            ItemStack storageItem,
            long amount
    ) {
        if (storageItem == null || amount <= 0L) {
            return 0L;
        }

        StorageData data = nbt.read(storageItem);

        if (data == null) {
            return 0L;
        }

        long oldAmount = data.getAmount();

        if (oldAmount <= 0L) {
            return 0L;
        }

        long remove = Math.min(amount, oldAmount);

        if (remove <= 0L) {
            return 0L;
        }

        long newAmount = oldAmount - remove;

        StorageData updated = data.withAmount(newAmount);

        if (!nbt.write(storageItem, updated)) {
            return 0L;
        }

        loreQueue.queue(storageItem);

        callAmountChangeEvent(
                inventory,
                storageItem,
                oldAmount,
                newAmount
        );

        return remove;
    }

    public long moveStorageToStorage(
            ItemStack sourceStorage,
            ItemStack destinationStorage,
            long requestedAmount
    ) {
        return moveStorageToStorage(
                null,
                sourceStorage,
                null,
                destinationStorage,
                requestedAmount
        );
    }

    public long moveStorageToStorage(
            Inventory sourceInventory,
            ItemStack sourceStorage,
            Inventory destinationInventory,
            ItemStack destinationStorage,
            long requestedAmount
    ) {

        /*
         * 重要:
         * 同一MSへの転送禁止。
         */
        if (sourceStorage == destinationStorage) {
            return 0L;
        }

        if (sourceStorage == null
                || destinationStorage == null
                || requestedAmount <= 0L) {
            return 0L;
        }

        StorageData sourceData = nbt.read(sourceStorage);
        StorageData destinationData = nbt.read(destinationStorage);

        if (sourceData == null || destinationData == null) {
            return 0L;
        }

        if (!isSameStoredItem(
                sourceData.getStoredItem(),
                destinationData.getStoredItem()
        )) {
            return 0L;
        }

        long sourceOldAmount = sourceData.getAmount();
        long destinationOldAmount = destinationData.getAmount();

        if (sourceOldAmount <= 0L
                || destinationOldAmount >= MAX_AMOUNT) {
            return 0L;
        }

        long moveAmount =
                Math.min(requestedAmount, sourceOldAmount);

        moveAmount =
                Math.min(
                        moveAmount,
                        MAX_AMOUNT - destinationOldAmount
                );

        if (moveAmount <= 0L) {
            return 0L;
        }

        long sourceNewAmount =
                sourceOldAmount - moveAmount;

        long destinationNewAmount =
                destinationOldAmount + moveAmount;

        StorageData newSourceData =
                sourceData.withAmount(sourceNewAmount);

        StorageData newDestinationData =
                destinationData.withAmount(destinationNewAmount);

        if (!nbt.write(sourceStorage, newSourceData)) {
            return 0L;
        }

        if (!nbt.write(destinationStorage, newDestinationData)) {

            rollback(sourceStorage, sourceData);

            return 0L;
        }

        loreQueue.queue(sourceStorage);
        loreQueue.queue(destinationStorage);

        callAmountChangeEvent(
                sourceInventory,
                sourceStorage,
                sourceOldAmount,
                sourceNewAmount
        );

        callAmountChangeEvent(
                destinationInventory,
                destinationStorage,
                destinationOldAmount,
                destinationNewAmount
        );

        return moveAmount;
    }

    public long storeItemStackIntoStorage(
            ItemStack storageItem,
            ItemStack sourceItem
    ) {
        return storeItemStackIntoStorage(
                null,
                storageItem,
                sourceItem
        );
    }

    public long storeItemStackIntoStorage(
            Inventory inventory,
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

        long added = addAmount(
                inventory,
                storageItem,
                sourceAmount
        );

        if (added <= 0L) {
            return 0L;
        }

        sourceItem.setAmount(
                sourceAmount - (int) added
        );

        return added;
    }

    private void callAmountChangeEvent(
            Inventory inventory,
            ItemStack storageItem,
            long oldAmount,
            long newAmount
    ) {
        if (oldAmount == newAmount) {
            return;
        }

        plugin.getServer()
                .getPluginManager()
                .callEvent(
                        new StorageAmountChangeEvent(
                                inventory,
                                storageItem,
                                oldAmount,
                                newAmount
                        )
                );
    }

    private void rollback(
            ItemStack storageItem,
            StorageData originalData
    ) {
        if (storageItem == null
                || originalData == null) {
            return;
        }

        nbt.write(storageItem, originalData);
        lore.update(storageItem, originalData);
    }

    private ItemStack normalize(ItemStack item) {
        if (item == null
                || item.getType() == Material.AIR) {
            return null;
        }

        ItemStack clean = item.clone();
        clean.setAmount(1);

        return clean;
    }
}