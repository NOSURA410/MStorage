package ms.addonimpl.autocollect;

import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.model.StorageData;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoCollectSelector {

    private final StorageNBT nbt;
    private final StorageValidator validator;
    private final NamespacedKey autoKey;

    public AutoCollectSelector(JavaPlugin plugin, StorageNBT nbt, StorageValidator validator) {
        this.nbt = nbt;
        this.validator = validator;
        this.autoKey = new NamespacedKey(plugin, AutoCollectSettings.KEY_AUTO);
    }

    public ItemStack findStorage(PlayerInventory inventory, ItemStack droppedItem) {
        if (inventory == null || droppedItem == null) {
            return null;
        }

        ItemStack storage;

        // 1. ホットバー優先
        for (int slot = 0; slot <= 8; slot++) {
            storage = inventory.getItem(slot);
            if (matches(storage, droppedItem)) {
                return storage;
            }
        }

        // 2. メインインベントリ
        for (int slot = 9; slot <= 35; slot++) {
            storage = inventory.getItem(slot);
            if (matches(storage, droppedItem)) {
                return storage;
            }
        }

        // 3. オフハンド
        storage = inventory.getItemInOffHand();
        if (matches(storage, droppedItem)) {
            return storage;
        }

        return null;
    }

    private boolean matches(ItemStack storage, ItemStack droppedItem) {
        if (!nbt.isStorage(storage)) {
            return false;
        }

        if (!isAutoEnabled(storage)) {
            return false;
        }

        StorageData data = nbt.read(storage);
        if (data == null) {
            return false;
        }

        return validator.isSameStoredItem(data.getStoredItem(), droppedItem);
    }

    private boolean isAutoEnabled(ItemStack storage) {
        if (storage == null || !storage.hasItemMeta()) {
            return false;
        }

        Byte value = storage.getItemMeta()
                .getPersistentDataContainer()
                .get(autoKey, PersistentDataType.BYTE);

        return value != null && value == AutoCollectSettings.AUTO_ON;
    }
}