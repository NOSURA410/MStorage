package ms.addonimpl.hopper;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.model.StorageData;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class HopperProcessor {

    private static final long STACK_SIZE = 64L;
    private static final long LC_STACKS = 54L;
    private static final long LC_SIZE = STACK_SIZE * LC_STACKS;
    private static final long MAX_LC = 100_000_000L;
    private static final long MAX_AMOUNT = LC_SIZE * MAX_LC;

    private final StorageNBT nbt;
    private final StorageLore lore;
    private final StorageValidator validator;
    private final HopperSelector selector;

    public HopperProcessor(
            StorageNBT nbt,
            StorageLore lore,
            StorageValidator validator,
            HopperSelector selector
    ) {
        this.nbt = nbt;
        this.lore = lore;
        this.validator = validator;
        this.selector = selector;
    }

    public int importToStorage(
            ItemStack storageItem,
            Inventory sourceInventory,
            ItemStack template
    ) {
        if (storageItem == null || sourceInventory == null || template == null) {
            return 0;
        }

        if (nbt.isStorage(template)) {
            return 0;
        }

        StorageData data = nbt.read(storageItem);

        if (data == null) {
            return 0;
        }

        if (!validator.isSameStoredItem(data.getStoredItem(), template)) {
            return 0;
        }

        long currentAmount = data.getAmount();

        if (currentAmount >= MAX_AMOUNT) {
            return 0;
        }

        int movable = countMovableItems(sourceInventory, template);

        if (movable <= 0) {
            return 0;
        }

        int moveAmount = Math.min(
                movable,
                HopperSettings.MAX_TRANSFER_PER_OPERATION
        );

        long remainingCapacity = MAX_AMOUNT - currentAmount;

        moveAmount = (int) Math.min(moveAmount, remainingCapacity);

        if (moveAmount <= 0) {
            return 0;
        }

        int removed = removeItems(sourceInventory, template, moveAmount);

        if (removed <= 0) {
            return 0;
        }

        StorageData updated = data.withAmount(currentAmount + removed);

        if (!nbt.write(storageItem, updated)) {
            restoreItems(sourceInventory, template, removed);
            return 0;
        }

        if (!lore.update(storageItem, updated)) {
            restoreItems(sourceInventory, template, removed);
            return 0;
        }

        return removed;
    }

    public int exportFromStorage(
            ItemStack storageItem,
            Inventory destinationInventory
    ) {
        if (storageItem == null || destinationInventory == null) {
            return 0;
        }

        StorageData sourceData = nbt.read(storageItem);

        if (sourceData == null || sourceData.getAmount() <= 0L) {
            return 0;
        }

        ItemStack stored = sourceData.getStoredItem();

        if (stored == null || stored.getType() == Material.AIR) {
            return 0;
        }

        int moveAmount = (int) Math.min(
                sourceData.getAmount(),
                HopperSettings.MAX_TRANSFER_PER_OPERATION
        );

        if (moveAmount <= 0) {
            return 0;
        }

        ItemStack template = stored.clone();
        template.setAmount(1);

        ItemStack destinationStorage = selector.findMatchingStorageInInventory(
                destinationInventory,
                template
        );

        if (destinationStorage != null) {
            return exportToDestinationStorage(
                    storageItem,
                    sourceData,
                    destinationStorage,
                    moveAmount
            );
        }

        return exportToNormalInventory(
                storageItem,
                sourceData,
                destinationInventory,
                stored,
                moveAmount
        );
    }

    private int exportToDestinationStorage(
            ItemStack sourceStorage,
            StorageData sourceData,
            ItemStack destinationStorage,
            int requestedAmount
    ) {
        StorageData destinationData = nbt.read(destinationStorage);

        if (destinationData == null) {
            return 0;
        }

        if (!validator.isSameStoredItem(sourceData.getStoredItem(), destinationData.getStoredItem())) {
            return 0;
        }

        long destinationAmount = destinationData.getAmount();

        if (destinationAmount >= MAX_AMOUNT) {
            return 0;
        }

        long destinationCapacity = MAX_AMOUNT - destinationAmount;

        int moveAmount = (int) Math.min(requestedAmount, destinationCapacity);

        if (moveAmount <= 0) {
            return 0;
        }

        StorageData newSourceData = sourceData.withAmount(sourceData.getAmount() - moveAmount);
        StorageData newDestinationData = destinationData.withAmount(destinationAmount + moveAmount);

        if (!nbt.write(sourceStorage, newSourceData)) {
            return 0;
        }

        if (!nbt.write(destinationStorage, newDestinationData)) {
            nbt.write(sourceStorage, sourceData);
            lore.update(sourceStorage, sourceData);
            return 0;
        }

        if (!lore.update(sourceStorage, newSourceData)) {
            nbt.write(sourceStorage, sourceData);
            nbt.write(destinationStorage, destinationData);
            lore.update(sourceStorage, sourceData);
            lore.update(destinationStorage, destinationData);
            return 0;
        }

        if (!lore.update(destinationStorage, newDestinationData)) {
            nbt.write(sourceStorage, sourceData);
            nbt.write(destinationStorage, destinationData);
            lore.update(sourceStorage, sourceData);
            lore.update(destinationStorage, destinationData);
            return 0;
        }

        return moveAmount;
    }

    private int exportToNormalInventory(
            ItemStack sourceStorage,
            StorageData sourceData,
            Inventory destinationInventory,
            ItemStack stored,
            int requestedAmount
    ) {
        int transferable = calculateTransferableAmount(
                destinationInventory,
                stored
        );

        if (transferable <= 0) {
            return 0;
        }

        int moveAmount = Math.min(requestedAmount, transferable);

        if (moveAmount <= 0) {
            return 0;
        }

        ItemStack output = stored.clone();
        output.setAmount(moveAmount);

        StorageData updated = sourceData.withAmount(
                sourceData.getAmount() - moveAmount
        );

        if (!nbt.write(sourceStorage, updated)) {
            return 0;
        }

        if (!lore.update(sourceStorage, updated)) {
            nbt.write(sourceStorage, sourceData);
            lore.update(sourceStorage, sourceData);
            return 0;
        }

        Map<Integer, ItemStack> leftover = destinationInventory.addItem(output);

        if (!leftover.isEmpty()) {
            int returned = 0;

            for (ItemStack left : leftover.values()) {
                if (left != null) {
                    returned += left.getAmount();
                }
            }

            StorageData rollback = updated.withAmount(
                    updated.getAmount() + returned
            );

            nbt.write(sourceStorage, rollback);
            lore.update(sourceStorage, rollback);

            return moveAmount - returned;
        }

        return moveAmount;
    }

    private int countMovableItems(
            Inventory inventory,
            ItemStack template
    ) {
        int total = 0;

        for (ItemStack item : inventory.getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            if (nbt.isStorage(item)) {
                continue;
            }

            if (validator.isSameStoredItem(template, item)) {
                total += item.getAmount();
            }
        }

        return total;
    }

    private int removeItems(
            Inventory inventory,
            ItemStack template,
            int amount
    ) {
        int remaining = amount;
        int removed = 0;

        ItemStack[] contents = inventory.getStorageContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];

            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            if (nbt.isStorage(item)) {
                continue;
            }

            if (!validator.isSameStoredItem(template, item)) {
                continue;
            }

            int take = Math.min(item.getAmount(), remaining);

            item.setAmount(item.getAmount() - take);

            if (item.getAmount() <= 0) {
                contents[i] = null;
            }

            removed += take;
            remaining -= take;

            if (remaining <= 0) {
                break;
            }
        }

        inventory.setStorageContents(contents);

        return removed;
    }

    private void restoreItems(
            Inventory inventory,
            ItemStack template,
            int amount
    ) {
        ItemStack restore = template.clone();
        restore.setAmount(amount);

        inventory.addItem(restore);
    }

    private int calculateTransferableAmount(
            Inventory inventory,
            ItemStack item
    ) {
        int total = 0;
        int maxStack = item.getMaxStackSize();

        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                total += maxStack;
                continue;
            }

            if (slot.isSimilar(item)) {
                total += (maxStack - slot.getAmount());
            }
        }

        return total;
    }
}