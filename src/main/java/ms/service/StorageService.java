package ms.service;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.model.StorageData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class StorageService {

    private static final long STACK_SIZE = 64L;
    private static final long LC_STACKS = 54L;
    private static final long LC_SIZE = STACK_SIZE * LC_STACKS;
    private static final long MAX_LC = 100_000_000L;
    private static final long MAX_AMOUNT = LC_SIZE * MAX_LC;

    private final StorageNBT nbt;
    private final StorageLore lore;
    private final StorageValidator validator;

    public StorageService(StorageNBT nbt, StorageLore lore, StorageValidator validator) {
        this.nbt = nbt;
        this.lore = lore;
        this.validator = validator;
    }

    public long storeAll(Player player, ItemStack storageItem) {
        if (player == null || storageItem == null) {
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

        Inventory inv = player.getInventory();
        ItemStack target = data.getStoredItem();

        long totalAdded = 0L;

        for (int i = 0; i < inv.getSize(); i++) {
            if (remainingCapacity <= 0L) {
                break;
            }

            ItemStack item = inv.getItem(i);

            if (nbt.isStorage(item)) {
                continue;
            }

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
                inv.setItem(i, null);
            } else {
                item.setAmount((int) (itemAmount - addAmount));
                inv.setItem(i, item);
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

    public int withdraw(Player player, ItemStack storageItem) {
        if (player == null || storageItem == null) {
            return 0;
        }

        StorageData data = nbt.read(storageItem);

        if (data == null) {
            return 0;
        }

        long currentAmount = data.getAmount();

        if (currentAmount <= 0L) {
            return 0;
        }

        ItemStack base = data.getStoredItem();

        if (base == null || base.getType() == Material.AIR) {
            return 0;
        }

        int maxStackSize = Math.max(1, base.getMaxStackSize());

        int giveAmount = (int) Math.min(
                currentAmount,
                maxStackSize
        );

        if (giveAmount <= 0) {
            return 0;
        }

        ItemStack give = base.clone();
        give.setAmount(giveAmount);

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(give);

        int leftoverAmount = 0;

        for (ItemStack item : leftover.values()) {
            if (item != null && item.getType() != Material.AIR) {
                leftoverAmount += item.getAmount();
            }
        }

        int actuallyGiven = giveAmount - leftoverAmount;

        if (actuallyGiven <= 0) {
            return 0;
        }

        StorageData newData = data.withAmount(currentAmount - actuallyGiven);

        if (!nbt.write(storageItem, newData)) {
            rollbackWithdraw(player, base, actuallyGiven);
            return 0;
        }

        if (!lore.update(storageItem, newData)) {
            nbt.write(storageItem, data);
            lore.update(storageItem, data);
            rollbackWithdraw(player, base, actuallyGiven);
            return 0;
        }

        return actuallyGiven;
    }

    private void rollbackWithdraw(Player player, ItemStack baseItem, int amount) {
        if (player == null || baseItem == null || amount <= 0) {
            return;
        }

        Inventory inventory = player.getInventory();

        int remaining = amount;

        for (int i = 0; i < inventory.getSize(); i++) {
            if (remaining <= 0) {
                break;
            }

            ItemStack item = inventory.getItem(i);

            if (item == null || item.getType() == Material.AIR) {
                continue;
            }

            if (!validator.isSameStoredItem(baseItem, item)) {
                continue;
            }

            int take = Math.min(item.getAmount(), remaining);

            item.setAmount(item.getAmount() - take);

            if (item.getAmount() <= 0) {
                inventory.setItem(i, null);
            } else {
                inventory.setItem(i, item);
            }

            remaining -= take;
        }

        player.updateInventory();
    }

    private boolean canFit(Inventory inventory, ItemStack baseItem, int amount) {
        if (inventory == null || baseItem == null || amount <= 0) {
            return false;
        }

        int remaining = amount;
        int maxStackSize = Math.max(1, baseItem.getMaxStackSize());

        for (ItemStack item : inventory.getStorageContents()) {
            if (item == null || item.getType().isAir()) {
                remaining -= maxStackSize;
            } else if (validator.isSameStoredItem(baseItem, item)) {
                remaining -= Math.max(0, maxStackSize - item.getAmount());
            }

            if (remaining <= 0) {
                return true;
            }
        }

        return false;
    }
}