package ms.listener;

import ms.core.StorageNBT;
import ms.manager.FeedbackManager;
import ms.model.StorageData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class StorageReleaseListener implements Listener {

    private static final int GRINDSTONE_UPPER_SLOT = 0;
    private static final int GRINDSTONE_LOWER_SLOT = 1;
    private static final int GRINDSTONE_RESULT_SLOT = 2;

    private final StorageNBT nbt;
    private final FeedbackManager feedback;

    public StorageReleaseListener(StorageNBT nbt, FeedbackManager feedback) {
        this.nbt = nbt;
        this.feedback = feedback;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPrepareGrindstone(PrepareGrindstoneEvent e) {
        GrindstoneInventory inv = e.getInventory();

        ItemStack storage = findStorage(inv);
        if (storage == null) {
            return;
        }

        StorageData data = nbt.read(storage);

        if (data == null || data.getAmount() != 0L) {
            e.setResult(null);
            return;
        }

        ItemStack result = data.getStoredItem();
        if (result == null || result.getType() == Material.AIR) {
            e.setResult(null);
            return;
        }

        result.setAmount(1);
        e.setResult(result);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClickGrindstoneResult(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();

        if (top.getType() != InventoryType.GRINDSTONE) {
            return;
        }

        if (e.getRawSlot() != GRINDSTONE_RESULT_SLOT) {
            return;
        }

        if (!(top instanceof GrindstoneInventory inv)) {
            return;
        }

        ItemStack storage = findStorage(inv);
        if (storage == null) {
            return;
        }

        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) {
            return;
        }

        StorageData data = nbt.read(storage);

        if (data == null) {
            feedback.fail(player);
            feedback.actionBar(player, ChatColor.RED + "ストレージデータが不正です");
            return;
        }

        if (data.getAmount() != 0L) {
            feedback.fail(player);
            feedback.actionBar(player, ChatColor.YELLOW + "中身が残っています");
            return;
        }

        ItemStack result = data.getStoredItem();

        if (result == null || result.getType() == Material.AIR) {
            feedback.fail(player);
            feedback.actionBar(player, ChatColor.RED + "保存アイテムが不正です");
            return;
        }

        result.setAmount(1);

        ItemStack cursor = e.getCursor();

        if (cursor == null || cursor.getType() == Material.AIR) {
            player.setItemOnCursor(result);
        } else {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(result);

            if (!leftover.isEmpty()) {
                feedback.fail(player);
                feedback.actionBar(player, ChatColor.RED + "インベントリに空きがありません");
                return;
            }
        }

        clearStorageInput(top, storage);

        feedback.success(player);
        feedback.actionBar(player, ChatColor.GREEN + "ストレージを解除しました");
    }

    private ItemStack findStorage(GrindstoneInventory inv) {
        ItemStack upper = inv.getUpperItem();
        if (nbt.isStorage(upper)) {
            return upper;
        }

        ItemStack lower = inv.getLowerItem();
        if (nbt.isStorage(lower)) {
            return lower;
        }

        return null;
    }

    private void clearStorageInput(Inventory top, ItemStack storage) {
        ItemStack upper = top.getItem(GRINDSTONE_UPPER_SLOT);

        if (nbt.isStorage(upper) && upper.isSimilar(storage)) {
            top.setItem(GRINDSTONE_UPPER_SLOT, null);
            top.setItem(GRINDSTONE_RESULT_SLOT, null);
            return;
        }

        ItemStack lower = top.getItem(GRINDSTONE_LOWER_SLOT);

        if (nbt.isStorage(lower) && lower.isSimilar(storage)) {
            top.setItem(GRINDSTONE_LOWER_SLOT, null);
            top.setItem(GRINDSTONE_RESULT_SLOT, null);
        }
    }
}