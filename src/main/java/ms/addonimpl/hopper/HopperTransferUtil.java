package ms.addonimpl.hopper;

import ms.core.StorageNBT;
import ms.core.StorageValidator;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class HopperTransferUtil {

    private final StorageNBT nbt;
    private final StorageValidator validator;

    public HopperTransferUtil(
            StorageNBT nbt,
            StorageValidator validator
    ) {
        this.nbt = nbt;
        this.validator = validator;
    }

    public int countMovableItems(
            Inventory inventory,
            ItemStack template
    ) {
        if (inventory == null || template == null) {
            return 0;
        }

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

    public int removeItems(
            Inventory inventory,
            ItemStack template,
            int amount
    ) {
        if (inventory == null || template == null || amount <= 0) {
            return 0;
        }

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

    public void restoreItems(
            Inventory inventory,
            ItemStack template,
            int amount
    ) {
        if (inventory == null || template == null || amount <= 0) {
            return;
        }

        int remaining = amount;
        int maxStackSize = template.getMaxStackSize();

        while (remaining > 0) {
            int putAmount = Math.min(remaining, maxStackSize);

            ItemStack restore = template.clone();
            restore.setAmount(putAmount);

            inventory.addItem(restore);

            remaining -= putAmount;
        }
    }

    public int calculateTransferableAmount(
            Inventory inventory,
            ItemStack item
    ) {
        if (inventory == null || item == null || item.getType() == Material.AIR) {
            return 0;
        }

        int total = 0;
        int maxStack = item.getMaxStackSize();

        for (ItemStack slot : inventory.getStorageContents()) {
            if (slot == null || slot.getType() == Material.AIR) {
                total += maxStack;
                continue;
            }

            if (slot.isSimilar(item)) {
                total += Math.max(0, maxStack - slot.getAmount());
            }
        }

        return total;
    }

    public int calculateAcceptedAmount(
            Inventory inventory,
            ItemStack item
    ) {
        if (item == null) {
            return 0;
        }

        return Math.min(
                item.getAmount(),
                calculateTransferableAmount(inventory, item)
        );
    }

    public int addItemSafely(
            Inventory inventory,
            ItemStack item
    ) {
        if (inventory == null || item == null || item.getType() == Material.AIR) {
            return 0;
        }

        int requested = item.getAmount();

        Map<Integer, ItemStack> leftover = inventory.addItem(item);

        int leftAmount = 0;

        for (ItemStack left : leftover.values()) {
            if (left != null) {
                leftAmount += left.getAmount();
            }
        }

        return requested - leftAmount;
    }
}