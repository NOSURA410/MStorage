package ms.core;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class StorageValidator {

    private final StorageNBT storageNBT;

    public StorageValidator(StorageNBT storageNBT) {
        this.storageNBT = storageNBT;
    }

    public boolean canCreateStorageFrom(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        if (storageNBT.isStorage(item)) {
            return false;
        }

        if (hasDurability(item)) {
            return false;
        }

        return true;
    }

    public boolean isSameStoredItem(ItemStack storedItem, ItemStack target) {
        if (storedItem == null || target == null) {
            return false;
        }

        if (target.getType() == Material.AIR) {
            return false;
        }

        if (storageNBT.isStorage(target)) {
            return false;
        }

        ItemStack a = storedItem.clone();
        ItemStack b = target.clone();

        a.setAmount(1);
        b.setAmount(1);

        if (a.getType() != b.getType()) {
            return false;
        }

        ItemMeta metaA = a.getItemMeta();
        ItemMeta metaB = b.getItemMeta();

        if (metaA == null && metaB == null) {
            return true;
        }

        if (metaA == null || metaB == null) {
            return false;
        }

        return metaA.equals(metaB);
    }

    private boolean hasDurability(ItemStack item) {
        if (item == null) {
            return false;
        }

        Material material = item.getType();

        return material.getMaxDurability() > 0;
    }
}