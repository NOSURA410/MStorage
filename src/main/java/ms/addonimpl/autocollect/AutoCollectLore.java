package ms.addonimpl.autocollect;

import ms.core.StorageNBT;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class AutoCollectLore {

    private static final String LABEL = "自動回収:";

    private final StorageNBT nbt;
    private final NamespacedKey autoKey;

    public AutoCollectLore(JavaPlugin plugin, StorageNBT nbt) {
        this.nbt = nbt;
        this.autoKey = new NamespacedKey(plugin, AutoCollectSettings.KEY_AUTO);
    }

    public void apply(ItemStack item) {
        if (!nbt.isStorage(item)) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        byte value = meta.getPersistentDataContainer().getOrDefault(
                autoKey,
                PersistentDataType.BYTE,
                AutoCollectSettings.AUTO_OFF
        );

        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        } else {
            lore = new ArrayList<>(lore);
        }

        // 古い表示削除
        lore.removeIf(line -> {
            String stripped = ChatColor.stripColor(line);
            return stripped != null && stripped.startsWith(LABEL);
        });

        // 追加
        if (value == AutoCollectSettings.AUTO_ON) {
            lore.add(ChatColor.GRAY + "自動回収: " + ChatColor.GREEN + "ON");
        } else {
            lore.add(ChatColor.GRAY + "自動回収: " + ChatColor.RED + "OFF");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}