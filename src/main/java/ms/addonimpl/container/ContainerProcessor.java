package ms.addonimpl.container;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.model.StorageData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ContainerProcessor {

    private static final long STACK_SIZE = 64L;
    private static final long LC_STACKS = 54L;
    private static final long LC_SIZE = STACK_SIZE * LC_STACKS;
    private static final long MAX_LC = 100_000_000L;
    private static final long MAX_AMOUNT = LC_SIZE * MAX_LC;

    private final StorageNBT nbt;
    private final StorageLore lore;
    private final StorageValidator validator;

    public ContainerProcessor(StorageNBT nbt, StorageLore lore, StorageValidator validator) {
        this.nbt = nbt;
        this.lore = lore;
        this.validator = validator;
    }

    public long collectFromContainer(ItemStack storageItem, Inventory containerInventory) {
        if (storageItem == null || containerInventory == null) {
            return 0L;
        }

        StorageData data = nbt.read(storageItem);
        if (data == null) {
            return 0L;
        }

        long currentAmount = data.getAmount();

        if (currentAmount >= MAX_AMOUNT) {
            return 0L;
        }

        long remainingCapacity = MAX_AMOUNT - currentAmount;
        long totalAdded = 0L;

        ItemStack target = data.getStoredItem();

        int limit = Math.min(
                containerInventory.getSize(),
                ContainerSettings.MAX_CONTAINER_SLOTS_PER_OPERATION
        );

        for (int slot = 0; slot < limit; slot++) {
            if (remainingCapacity <= 0L) {
                break;
            }

            ItemStack item = containerInventory.getItem(slot);

            if (!validator.isSameStoredItem(target, item)) {
                continue;
            }

            int itemAmount = item.getAmount();

            if (itemAmount <= 0) {
                continue;
            }

            long addAmount = Math.min(itemAmount, remainingCapacity);

            totalAdded += addAmount;
            remainingCapacity -= addAmount;

            if (addAmount >= itemAmount) {
                containerInventory.setItem(slot, null);
            } else {
                item.setAmount((int) (itemAmount - addAmount));
                containerInventory.setItem(slot, item);
            }
        }

        if (totalAdded <= 0L) {
            return 0L;
        }

        StorageData newData = data.withAmount(currentAmount + totalAdded);

        if (!nbt.write(storageItem, newData)) {
            return 0L;
        }

        if (!lore.update(storageItem, newData)) {
            return 0L;
        }

        return totalAdded;
    }

    public long fillContainer(ItemStack storageItem, Inventory containerInventory) {
        if (storageItem == null || containerInventory == null) {
            return 0L;
        }

        StorageData data = nbt.read(storageItem);
        if (data == null) {
            return 0L;
        }

        long currentAmount = data.getAmount();

        if (currentAmount <= 0L) {
            return 0L;
        }

        ItemStack base = data.getStoredItem();
        if (base == null) {
            return 0L;
        }

        long totalRemoved = 0L;
        long remainingStorage = currentAmount;

        int maxStackSize = base.getMaxStackSize();

        int limit = Math.min(
                containerInventory.getSize(),
                ContainerSettings.MAX_CONTAINER_SLOTS_PER_OPERATION
        );

        int filledStacks = 0;

        for (int slot = 0; slot < limit; slot++) {
            if (remainingStorage <= 0L) {
                break;
            }

            if (filledStacks >= ContainerSettings.MAX_FILL_STACKS_PER_OPERATION) {
                break;
            }

            ItemStack item = containerInventory.getItem(slot);

            if (item == null || item.getType().isAir()) {
                int putAmount = (int) Math.min(maxStackSize, remainingStorage);

                ItemStack put = base.clone();
                put.setAmount(putAmount);

                containerInventory.setItem(slot, put);

                totalRemoved += putAmount;
                remainingStorage -= putAmount;
                filledStacks++;
                continue;
            }

            if (!validator.isSameStoredItem(base, item)) {
                continue;
            }

            int space = maxStackSize - item.getAmount();

            if (space <= 0) {
                continue;
            }

            int putAmount = (int) Math.min(space, remainingStorage);

            item.setAmount(item.getAmount() + putAmount);
            containerInventory.setItem(slot, item);

            totalRemoved += putAmount;
            remainingStorage -= putAmount;

            if (item.getAmount() >= maxStackSize) {
                filledStacks++;
            }
        }

        if (totalRemoved <= 0L) {
            return 0L;
        }

        StorageData newData = data.withAmount(currentAmount - totalRemoved);

        if (!nbt.write(storageItem, newData)) {
            return 0L;
        }

        if (!lore.update(storageItem, newData)) {
            return 0L;
        }

        return totalRemoved;
    }
}