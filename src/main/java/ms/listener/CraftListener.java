package ms.listener;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.model.StorageData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class CraftListener implements Listener {

    private final StorageNBT nbt;
    private final StorageLore lore;
    private final StorageValidator validator;

    public CraftListener(StorageNBT nbt, StorageLore lore, StorageValidator validator) {
        this.nbt = nbt;
        this.lore = lore;
        this.validator = validator;
    }

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent e) {
        CraftingInventory inv = e.getInventory();

        if (!isStorageRecipeShape(inv)) {
            return;
        }

        if (!(e.getView().getPlayer() instanceof Player player)) {
            inv.setResult(null);
            return;
        }

        ItemStack center = getCleanCenterItem(inv);

        if (!validator.canCreateStorageFrom(center)) {
            inv.setResult(null);
            return;
        }

        UUID uuid = player.getUniqueId();

        ItemStack result = nbt.createStorage(center, uuid);
        if (result == null) {
            inv.setResult(null);
            return;
        }

        StorageData data = nbt.read(result);
        if (data == null) {
            inv.setResult(null);
            return;
        }

        if (!lore.update(result, data)) {
            inv.setResult(null);
            return;
        }

        inv.setResult(result);
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        ItemStack result = e.getCurrentItem();

        if (!nbt.isStorage(result)) {
            return;
        }

        if (!(e.getWhoClicked() instanceof Player)) {
            e.setCancelled(true);
            return;
        }

        if (e.isShiftClick()) {
            e.setCancelled(true);
            return;
        }

        CraftingInventory inv = e.getInventory();

        if (!isStorageRecipeShape(inv)) {
            e.setCancelled(true);
            return;
        }

        ItemStack center = getCleanCenterItem(inv);

        if (!validator.canCreateStorageFrom(center)) {
            e.setCancelled(true);
        }
    }

    private boolean isStorageRecipeShape(CraftingInventory inv) {
        if (inv == null) {
            return false;
        }

        ItemStack[] matrix = inv.getMatrix();

        if (matrix.length < 9) {
            return false;
        }

        for (int i = 0; i < matrix.length; i++) {
            if (i == 4) {
                continue;
            }

            ItemStack item = matrix[i];

            if (item == null || item.getType() != Material.BARREL) {
                return false;
            }

            if (nbt.isStorage(item)) {
                return false;
            }
        }

        return true;
    }

    private ItemStack getCleanCenterItem(CraftingInventory inv) {
        if (inv == null) {
            return null;
        }

        ItemStack[] matrix = inv.getMatrix();

        if (matrix.length < 9) {
            return null;
        }

        ItemStack center = matrix[4];

        if (center == null || center.getType() == Material.AIR) {
            return null;
        }

        ItemStack clean = center.clone();
        clean.setAmount(1);

        return clean;
    }
}