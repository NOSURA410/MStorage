package ms.addonimpl.autocollect;

import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.model.StorageData;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoCollectSelector {

    private final StorageNBT nbt;
    private final StorageValidator validator;
    private final NamespacedKey autoKey;

    public AutoCollectSelector(
            JavaPlugin plugin,
            StorageNBT nbt,
            StorageValidator validator
    ) {
        this.nbt = nbt;
        this.validator = validator;
        this.autoKey = new NamespacedKey(
                plugin,
                AutoCollectSettings.KEY_AUTO
        );
    }

    public ItemStack findStorage(
            Inventory inventory,
            ItemStack droppedItem
    ) {

        if (inventory == null || droppedItem == null) {
            return null;
        }

        if (droppedItem.getType() == Material.AIR) {
            return null;
        }

        // MSストレージ本体は回収対象外
        if (nbt.isStorage(droppedItem)) {
            return null;
        }

        for (ItemStack candidate : inventory.getContents()) {

            if (!nbt.isStorage(candidate)) {
                continue;
            }

            // OFFなら候補にしない
            if (!isAutoEnabled(candidate)) {
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

            if (validator.isSameStoredItem(
                    stored,
                    droppedItem
            )) {
                return candidate;
            }
        }

        return null;
    }

    public boolean isAutoEnabled(ItemStack item) {

        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return false;
        }

        Byte value = meta.getPersistentDataContainer().get(
                autoKey,
                PersistentDataType.BYTE
        );

        return value != null
                && value == AutoCollectSettings.AUTO_ON;
    }
}