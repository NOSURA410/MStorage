package ms.listener;

import ms.core.StorageNBT;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ChestGuideLoreListener implements Listener {

    private static final String MARKER = ChatColor.DARK_GRAY + "[MS_STORAGE_GUIDE]";

    private final StorageNBT nbt;

    public ChestGuideLoreListener(StorageNBT nbt) {
        this.nbt = nbt;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        apply(e.getCurrentItem());
        apply(e.getCursor());
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        ItemStack item = e.getPlayer().getInventory().getItem(e.getNewSlot());
        apply(item);
    }

    private void apply(ItemStack item) {

        if (item == null || item.getType() != Material.CHEST) {
            return;
        }

        // ストレージ化済みチェストは除外
        if (nbt.isStorage(item)) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        List<String> lore = meta.getLore();

        if (lore == null) {
            lore = new ArrayList<>();
        } else {
            lore = new ArrayList<>(lore);
        }

        for (String line : lore) {
            if (MARKER.equals(line)) {
                return;
            }
        }

        lore.add("");
        lore.add(MARKER);
        lore.add(ChatColor.AQUA + "MSストレージ化方法");
        lore.add(ChatColor.GRAY + "チェスト8個で中央アイテムを囲む");
        lore.add(ChatColor.GRAY + "ことでストレージ化できます");
        lore.add("");
        lore.add(ChatColor.YELLOW + "配置:");
        lore.add(ChatColor.WHITE + "チェスト チェスト チェスト");
        lore.add(ChatColor.WHITE + "チェスト アイテム チェスト");
        lore.add(ChatColor.WHITE + "チェスト チェスト チェスト");

        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}