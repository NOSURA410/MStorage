package ms.core;

import ms.addonimpl.autocollect.AutoCollectLore;
import ms.model.StorageData;
import ms.model.StorageMode;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class StorageLore {

    private AutoCollectLore autoCollectLore;

    public void setAutoCollectLore(AutoCollectLore autoCollectLore) {
        this.autoCollectLore = autoCollectLore;
    }

    public boolean update(ItemStack item, StorageData data) {
        if (item == null || data == null) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        long amount = data.getAmount();

        long lcSize = 64L * 54L;
        long lc = amount / lcSize;
        long remainAfterLC = amount % lcSize;

        long s = remainAfterLC / 64L;
        long remain = remainAfterLC % 64L;

        String baseName = getBaseName(data.getStoredItem());

        String display = ChatColor.AQUA + "MS: "
                + ChatColor.WHITE + baseName
                + ChatColor.GRAY + " ("
                + ChatColor.GOLD + lc + "LC "
                + ChatColor.YELLOW + s + "S "
                + ChatColor.WHITE + remain
                + ChatColor.GRAY + ")";

        meta.setDisplayName(display);

        List<String> lore = new ArrayList<>();

        if (data.getMode() == StorageMode.CONTAINER) {
            lore.add(ChatColor.GRAY + "Mode: " + ChatColor.GREEN + "CONTAINER");
        } else {
            lore.add(ChatColor.GRAY + "Mode: " + ChatColor.AQUA + "HAND");
        }

        lore.add(ChatColor.GRAY + "容量: "
                + ChatColor.GOLD + lc + "LC "
                + ChatColor.YELLOW + s + "S "
                + ChatColor.WHITE + remain);

        meta.setLore(lore);

        applyGlow(meta);

        item.setItemMeta(meta);

        if (autoCollectLore != null) {
            autoCollectLore.apply(item);
        }

        return true;
    }

    private void applyGlow(ItemMeta meta) {
        // Paper / Bukkit 1.20.5+ の安定グロウ指定
        meta.setEnchantmentGlintOverride(true);

        // 旧方式の保険
        if (!meta.hasEnchant(Enchantment.LUCK_OF_THE_SEA)) {
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        }

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    private String getBaseName(ItemStack item) {
        if (item == null) {
            return "Unknown";
        }

        ItemMeta meta = item.getItemMeta();

        if (meta != null && meta.hasDisplayName()) {
            return ChatColor.stripColor(meta.getDisplayName());
        }

        String raw = item.getType().name().toLowerCase().replace("_", " ");

        String[] parts = raw.split(" ");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) continue;

            builder.append(Character.toUpperCase(part.charAt(0)))
                    .append(part.substring(1))
                    .append(" ");
        }

        return builder.toString().trim();
    }
}