package ms.addonimpl.hopper;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.model.StorageData;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class HopperExportProcessor {

    private static final long STACK_SIZE = 64L;
    private static final long LC_STACKS = 54L;
    private static final long LC_SIZE = STACK_SIZE * LC_STACKS;
    private static final long MAX_LC = 100_000_000L;
    private static final long MAX_AMOUNT = LC_SIZE * MAX_LC;

    private final StorageNBT nbt;
    private final StorageLore lore;
    private final StorageValidator validator;
    private final HopperSelector selector;
    private final HopperTransferUtil transferUtil;
    private final HopperImportProcessor importProcessor;

    public HopperExportProcessor(
            StorageNBT nbt,
            StorageLore lore,
            StorageValidator validator,
            HopperSelector selector,
            HopperTransferUtil transferUtil,
            HopperImportProcessor importProcessor
    ) {
        this.nbt = nbt;
        this.lore = lore;
        this.validator = validator;
        this.selector = selector;
        this.transferUtil = transferUtil;
        this.importProcessor = importProcessor;
    }

    public int exportFromStorage(
            ItemStack sourceStorage,
            Inventory destinationInventory
    ) {
        if (sourceStorage == null || destinationInventory == null) {
            return 0;
        }

        StorageData sourceData = nbt.read(sourceStorage);

        if (sourceData == null || sourceData.getAmount() <= 0L) {
            return 0;
        }

        ItemStack stored = sourceData.getStoredItem();

        if (stored == null || stored.getType() == Material.AIR) {
            return 0;
        }

        int requestedAmount = (int) Math.min(
                sourceData.getAmount(),
                HopperSettings.MAX_TRANSFER_PER_OPERATION
        );

        if (requestedAmount <= 0) {
            return 0;
        }

        ItemStack template = stored.clone();
        template.setAmount(1);

        ItemStack destinationStorage = selector.findMatchingStorageInInventory(
                destinationInventory,
                template
        );

        if (destinationStorage != null) {
            return exportStorageToStorage(
                    sourceStorage,
                    sourceData,
                    destinationStorage,
                    requestedAmount
            );
        }

        return exportStorageToInventory(
                sourceStorage,
                sourceData,
                destinationInventory,
                stored,
                requestedAmount
        );
    }

    /**
     * 空ストレージ等でホッパーが詰まる際、
     * 同じInventory内の通常アイテムを代替搬送する。
     */
    public int moveNormalItemInsteadOfBlockedStorage(
            Inventory sourceInventory,
            Inventory destinationInventory
    ) {

        if (sourceInventory == null || destinationInventory == null) {
            return 0;
        }

        ItemStack[] contents = sourceInventory.getStorageContents();

        for (ItemStack item : contents) {

            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            // MS本体はスキップ
            if (nbt.isStorage(item)) {
                continue;
            }

            // 搬送先に対応MSストレージがあるなら直接収納
            ItemStack destinationStorage =
                    selector.findMatchingStorageInInventory(
                            destinationInventory,
                            item
                    );

            if (destinationStorage != null) {

                int imported =
                        importNormalItemToDestinationStorage(
                                sourceInventory,
                                destinationStorage,
                                item
                        );

                if (imported > 0) {
                    return imported;
                }
            }

            // なければ通常搬送
            int moved = moveNormalItemToInventory(
                    sourceInventory,
                    destinationInventory,
                    item
            );

            if (moved > 0) {
                return moved;
            }
        }

        return 0;
    }

    private int importNormalItemToDestinationStorage(
            Inventory sourceInventory,
            ItemStack destinationStorage,
            ItemStack sourceItem
    ) {

        if (sourceInventory == null
                || destinationStorage == null
                || sourceItem == null) {

            return 0;
        }

        ItemStack template = sourceItem.clone();
        template.setAmount(1);

        return importProcessor.importToStorage(
                destinationStorage,
                sourceInventory,
                template
        );
    }

    private int moveNormalItemToInventory(
            Inventory sourceInventory,
            Inventory destinationInventory,
            ItemStack sourceItem
    ) {

        if (sourceInventory == null
                || destinationInventory == null
                || sourceItem == null) {

            return 0;
        }

        int movable = Math.min(
                sourceItem.getAmount(),
                HopperSettings.MAX_TRANSFER_PER_OPERATION
        );

        if (movable <= 0) {
            return 0;
        }

        ItemStack moving = sourceItem.clone();
        moving.setAmount(movable);

        int accepted = transferUtil.calculateAcceptedAmount(
                destinationInventory,
                moving
        );

        if (accepted <= 0) {
            return 0;
        }

        moving.setAmount(accepted);

        int moved = transferUtil.addItemSafely(
                destinationInventory,
                moving
        );

        if (moved <= 0) {
            return 0;
        }

        sourceItem.setAmount(sourceItem.getAmount() - moved);

        return moved;
    }

    private int exportStorageToStorage(
            ItemStack sourceStorage,
            StorageData sourceData,
            ItemStack destinationStorage,
            int requestedAmount
    ) {

        StorageData destinationData = nbt.read(destinationStorage);

        if (destinationData == null) {
            return 0;
        }

        if (!validator.isSameStoredItem(
                sourceData.getStoredItem(),
                destinationData.getStoredItem()
        )) {
            return 0;
        }

        long sourceAmount = sourceData.getAmount();
        long destinationAmount = destinationData.getAmount();

        if (sourceAmount <= 0L
                || destinationAmount >= MAX_AMOUNT) {

            return 0;
        }

        int moveAmount = (int) Math.min(
                requestedAmount,
                sourceAmount
        );

        moveAmount = (int) Math.min(
                moveAmount,
                MAX_AMOUNT - destinationAmount
        );

        if (moveAmount <= 0) {
            return 0;
        }

        StorageData newSourceData =
                sourceData.withAmount(sourceAmount - moveAmount);

        StorageData newDestinationData =
                destinationData.withAmount(destinationAmount + moveAmount);

        if (!nbt.write(sourceStorage, newSourceData)) {
            return 0;
        }

        if (!nbt.write(destinationStorage, newDestinationData)) {
            rollbackStorage(sourceStorage, sourceData);
            return 0;
        }

        if (!lore.update(sourceStorage, newSourceData)) {
            rollbackStorage(sourceStorage, sourceData);
            rollbackStorage(destinationStorage, destinationData);
            return 0;
        }

        if (!lore.update(destinationStorage, newDestinationData)) {
            rollbackStorage(sourceStorage, sourceData);
            rollbackStorage(destinationStorage, destinationData);
            return 0;
        }

        return moveAmount;
    }

    private int exportStorageToInventory(
            ItemStack sourceStorage,
            StorageData sourceData,
            Inventory destinationInventory,
            ItemStack stored,
            int requestedAmount
    ) {

        int transferable =
                transferUtil.calculateTransferableAmount(
                        destinationInventory,
                        stored
                );

        if (transferable <= 0) {
            return 0;
        }

        int moveAmount =
                Math.min(requestedAmount, transferable);

        if (moveAmount <= 0) {
            return 0;
        }

        ItemStack output = stored.clone();
        output.setAmount(moveAmount);

        StorageData updated =
                sourceData.withAmount(
                        sourceData.getAmount() - moveAmount
                );

        if (!nbt.write(sourceStorage, updated)) {
            return 0;
        }

        if (!lore.update(sourceStorage, updated)) {
            rollbackStorage(sourceStorage, sourceData);
            return 0;
        }

        int moved =
                transferUtil.addItemSafely(
                        destinationInventory,
                        output
                );

        if (moved <= 0) {
            rollbackStorage(sourceStorage, sourceData);
            return 0;
        }

        if (moved < moveAmount) {

            StorageData corrected =
                    sourceData.withAmount(
                            sourceData.getAmount() - moved
                    );

            if (!nbt.write(sourceStorage, corrected)) {
                rollbackStorage(sourceStorage, sourceData);
                return 0;
            }

            if (!lore.update(sourceStorage, corrected)) {
                rollbackStorage(sourceStorage, sourceData);
                return 0;
            }

            return moved;
        }

        return moveAmount;
    }

    private void rollbackStorage(
            ItemStack storageItem,
            StorageData originalData
    ) {

        if (storageItem == null || originalData == null) {
            return;
        }

        nbt.write(storageItem, originalData);
        lore.update(storageItem, originalData);
    }
}