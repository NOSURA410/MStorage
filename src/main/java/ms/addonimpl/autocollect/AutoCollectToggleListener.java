package ms.addonimpl.autocollect;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.manager.FeedbackManager;
import ms.model.StorageData;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoCollectToggleListener implements Listener {

    private final JavaPlugin plugin;
    private final StorageNBT nbt;
    private final StorageLore lore;
    private final FeedbackManager feedback;
    private final NamespacedKey autoKey;
    private final AutoCollectLore autoCollectLore;

    public AutoCollectToggleListener(
            JavaPlugin plugin,
            StorageNBT nbt,
            StorageLore lore,
            FeedbackManager feedback,
            AutoCollectLore autoCollectLore
    ) {
        this.plugin = plugin;
        this.nbt = nbt;
        this.lore = lore;
        this.feedback = feedback;
        this.autoKey = new NamespacedKey(plugin, AutoCollectSettings.KEY_AUTO);
        this.autoCollectLore = autoCollectLore;
    }

    @EventHandler
    public void onSneakLeftClick(PlayerAnimationEvent e) {
        Player player = e.getPlayer();

        if (!player.isSneaking()) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (!nbt.isStorage(item)) {
            return;
        }

        StorageData data = nbt.read(item);

        if (data == null) {
            feedback.fail(player);
            feedback.actionBar(player, ChatColor.RED + "ストレージデータが不正です");
            return;
        }

        if (data.getAmount() == 0L) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            feedback.fail(player);
            feedback.actionBar(player, ChatColor.RED + "アイテムデータが不正です");
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        byte current = readAutoSafely(pdc);
        byte next = current == AutoCollectSettings.AUTO_ON
                ? AutoCollectSettings.AUTO_OFF
                : AutoCollectSettings.AUTO_ON;

        pdc.remove(autoKey);
        pdc.set(autoKey, PersistentDataType.BYTE, next);

        item.setItemMeta(meta);

        StorageData currentData = nbt.read(item);

        if (currentData == null) {
            feedback.fail(player);
            feedback.actionBar(player, ChatColor.RED + "ストレージデータが不正です");
            return;
        }

        if (!lore.update(item, currentData)) {
            feedback.fail(player);
            feedback.actionBar(player, ChatColor.RED + "表示更新に失敗しました");
            return;
        }

        autoCollectLore.apply(item);
        player.getInventory().setItemInMainHand(item);

        if (next == AutoCollectSettings.AUTO_ON) {
            feedback.actionBar(player, ChatColor.GREEN + "AutoCollect: ON");
        } else {
            feedback.actionBar(player, ChatColor.RED + "AutoCollect: OFF");
        }

        feedback.success(player);
    }

    private byte readAutoSafely(PersistentDataContainer pdc) {
        if (pdc == null) {
            return AutoCollectSettings.AUTO_OFF;
        }

        try {
            if (pdc.has(autoKey, PersistentDataType.BYTE)) {
                Byte value = pdc.get(autoKey, PersistentDataType.BYTE);

                if (value == null) {
                    return AutoCollectSettings.AUTO_OFF;
                }

                if (value == AutoCollectSettings.AUTO_ON) {
                    return AutoCollectSettings.AUTO_ON;
                }

                return AutoCollectSettings.AUTO_OFF;
            }
        } catch (Exception ignored) {
            pdc.remove(autoKey);
        }

        return AutoCollectSettings.AUTO_OFF;
    }
}