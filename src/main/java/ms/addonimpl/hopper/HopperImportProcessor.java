package ms.addonimpl.hopper;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.model.StorageData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class HopperImportProcessor {

    private static final long STACK_SIZE = 64L;
    private static final long LC_STACKS = 54L;
    private static final long LC_SIZE = STACK_SIZE * LC_STACKS;
    private static final long MAX_LC = 100_000_000L;
    private static final long MAX_AMOUNT = LC_SIZE * MAX_LC;

    private final StorageNBT nbt;
    private final StorageLore lore;
    private final StorageValidator validator;
    private final HopperTransferUtil transferUtil;

    public HopperImportProcessor(
            StorageNBT nbt,
            StorageLore lore,
            StorageValidator validator,
            HopperTransferUtil transferUtil
    ) {
        this.nbt = nbt;
        this.lore = lore;
        this.validator = validator;
        this.transferUtil = transferUtil;
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

        int movable = transferUtil.countMovableItems(
                sourceInventory,
                template
        );

        if (movable <= 0) {
            return 0;
        }

        int moveAmount = Math.min(
                movable,
                HopperSettings.MAX_TRANSFER_PER_OPERATION
        );

        moveAmount = (int) Math.min(
                moveAmount,
                MAX_AMOUNT - currentAmount
        );

        if (moveAmount <= 0) {
            return 0;
        }

        int removed = transferUtil.removeItems(
                sourceInventory,
                template,
                moveAmount
        );

        if (removed <= 0) {
            return 0;
        }

        StorageData updated = data.withAmount(currentAmount + removed);

        if (!nbt.write(storageItem, updated)) {
            transferUtil.restoreItems(sourceInventory, template, removed);
            return 0;
        }

        if (!lore.update(storageItem, updated)) {
            nbt.write(storageItem, data);
            lore.update(storageItem, data);
            transferUtil.restoreItems(sourceInventory, template, removed);
            return 0;
        }

        return removed;
    }
}