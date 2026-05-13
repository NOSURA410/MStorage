package ms.addonimpl.hopper;

import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.model.StorageData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class HopperSelector {

    private final StorageNBT nbt;
    private final StorageValidator validator;

    public HopperSelector(StorageNBT nbt, StorageValidator validator) {
        this.nbt = nbt;
        this.validator = validator;
    }

    public boolean isHopperInventory(Inventory inventory) {
        if (inventory == null) {
            return false;
        }

        return inventory.getHolder() instanceof Hopper;
    }

    public Hopper getHopperHolder(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        if (inventory.getHolder() instanceof Hopper hopper) {
            return hopper;
        }

        return null;
    }

    public Block getHopperBlock(Inventory inventory) {
        Hopper hopper = getHopperHolder(inventory);

        if (hopper == null) {
            return null;
        }

        return hopper.getBlock();
    }

    public Block getInventoryBlock(Inventory inventory) {
        if (inventory == null || inventory.getHolder() == null) {
            return null;
        }

        if (inventory.getHolder() instanceof BlockState state) {
            return state.getBlock();
        }

        return null;
    }

    public boolean hasStorage(Inventory inventory) {
        if (inventory == null) {
            return false;
        }

        for (ItemStack item : inventory.getStorageContents()) {
            if (nbt.isStorage(item)) {
                return true;
            }
        }

        return false;
    }

    public ItemStack findMatchingStorageInHopper(
            Inventory hopperInventory,
            ItemStack sourceItem
    ) {
        return findMatchingStorageInInventory(
                hopperInventory,
                sourceItem
        );
    }

    public ItemStack findMatchingStorageInInventory(
            Inventory inventory,
            ItemStack sourceItem
    ) {
        if (inventory == null
                || sourceItem == null
                || sourceItem.getType() == Material.AIR) {
            return null;
        }

        if (nbt.isStorage(sourceItem)) {
            return null;
        }

        for (ItemStack candidate : inventory.getStorageContents()) {

            if (!nbt.isStorage(candidate)) {
                continue;
            }

            StorageData data = nbt.read(candidate);

            if (data == null) {
                continue;
            }

            ItemStack stored = data.getStoredItem();

            if (stored == null || stored.getType() == Material.AIR) {
                continue;
            }

            if (validator.isSameStoredItem(stored, sourceItem)) {
                return candidate;
            }
        }

        return null;
    }

    public ItemStack findExportableStorage(Inventory sourceInventory) {

        if (sourceInventory == null) {
            return null;
        }

        for (ItemStack candidate : sourceInventory.getStorageContents()) {

            if (!nbt.isStorage(candidate)) {
                continue;
            }

            StorageData data = nbt.read(candidate);

            if (data == null) {
                continue;
            }

            if (data.getAmount() <= 0L) {
                continue;
            }

            ItemStack stored = data.getStoredItem();

            if (stored == null || stored.getType() == Material.AIR) {
                continue;
            }

            return candidate;
        }

        return null;
    }
}