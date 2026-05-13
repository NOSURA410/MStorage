package ms.listener;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.model.StorageData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;

public class CraftListener implements Listener {

    private static final int CRAFT_RESULT_RAW_SLOT = 0;

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

        ItemStack center = getCleanCenterItem(inv);

        if (!validator.canCreateStorageFrom(center)) {
            inv.setResult(null);
            return;
        }

        ItemStack preview = center.clone();
        preview.setAmount(1);

        ItemMeta meta = preview.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "MSストレージ作成");
            preview.setItemMeta(meta);
        }

        inv.setResult(preview);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onResultClick(InventoryClickEvent e) {
        if (!(e.getInventory() instanceof CraftingInventory inv)) {
            return;
        }

        if (e.getRawSlot() != CRAFT_RESULT_RAW_SLOT) {
            return;
        }

        if (!isStorageRecipeShape(inv)) {
            return;
        }

        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        ClickType click = e.getClick();

        if (e.isShiftClick()
                || click.isShiftClick()
                || click.isKeyboardClick()
                || click == ClickType.NUMBER_KEY
                || click == ClickType.DOUBLE_CLICK
                || click == ClickType.DROP
                || click == ClickType.CONTROL_DROP
                || click == ClickType.MIDDLE) {
            return;
        }

        ItemStack cursor = player.getItemOnCursor();

        if (cursor != null && cursor.getType() != Material.AIR) {
            return;
        }

        ItemStack center = getCleanCenterItem(inv);

        if (!validator.canCreateStorageFrom(center)) {
            return;
        }

        UUID uuid = player.getUniqueId();

        ItemStack result = nbt.createStorage(center, uuid);

        if (result == null) {
            return;
        }

        result.setAmount(1);

        StorageData data = nbt.read(result);

        if (data == null) {
            return;
        }

        if (!lore.update(result, data)) {
            return;
        }

        result.setAmount(1);

        if (!consumeOneRecipe(inv)) {
            return;
        }

        player.setItemOnCursor(result);
        player.updateInventory();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCraft(CraftItemEvent e) {
        if (e.getInventory() instanceof CraftingInventory inv && isStorageRecipeShape(inv)) {
            e.setCancelled(true);
        }
    }

    private boolean consumeOneRecipe(CraftingInventory inv) {
        if (inv == null) {
            return false;
        }

        ItemStack[] matrix = inv.getMatrix();

        if (matrix.length < 9) {
            return false;
        }

        ItemStack[] updated = new ItemStack[matrix.length];

        for (int i = 0; i < matrix.length; i++) {
            ItemStack item = matrix[i];

            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) {
                return false;
            }

            if (i != 4 && item.getType() != Material.BARREL) {
                return false;
            }

            ItemStack next = item.clone();
            next.setAmount(item.getAmount() - 1);

            if (next.getAmount() <= 0) {
                updated[i] = null;
            } else {
                updated[i] = next;
            }
        }

        inv.setMatrix(updated);
        return true;
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
            ItemStack item = matrix[i];

            if (item == null || item.getType() == Material.AIR) {
                return false;
            }

            if (i == 4) {
                continue;
            }

            if (item.getType() != Material.BARREL) {
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