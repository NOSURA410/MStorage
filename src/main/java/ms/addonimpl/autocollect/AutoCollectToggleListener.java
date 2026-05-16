package ms.addonimpl.autocollect;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.manager.FeedbackManager;
import ms.model.StorageData;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoCollectToggleListener implements Listener {

    private static final long TOGGLE_COOLDOWN_MS = 0L;
    private static final long RIGHT_CLICK_GUARD_MS = 250L;
    private static final int BLOCK_REACH_DISTANCE = 5;

    private final StorageNBT nbt;
    private final StorageLore lore;
    private final FeedbackManager feedback;
    private final AutoCollectLore autoCollectLore;
    private final NamespacedKey autoKey;

    private final Map<UUID, Long> lastToggle = new HashMap<>();
    private final Map<UUID, Long> lastRightClickStorage = new HashMap<>();

    public AutoCollectToggleListener(
            JavaPlugin plugin,
            StorageNBT nbt,
            StorageLore lore,
            FeedbackManager feedback,
            AutoCollectLore autoCollectLore
    ) {
        this.nbt = nbt;
        this.lore = lore;
        this.feedback = feedback;
        this.autoCollectLore = autoCollectLore;
        this.autoKey = new NamespacedKey(plugin, AutoCollectSettings.KEY_AUTO);
    }

    /*
     * ------------------------------------------------------------
     * 右クリック記録
     * ------------------------------------------------------------
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onStorageRightClick(PlayerInteractEvent e) {

        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (e.getAction() != Action.RIGHT_CLICK_AIR
                && e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = e.getItem();

        if (!nbt.isStorage(item)) {
            return;
        }

        lastRightClickStorage.put(
                e.getPlayer().getUniqueId(),
                System.currentTimeMillis()
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSneakLeftClick(PlayerAnimationEvent e) {

        Player player = e.getPlayer();

        /*
         * 右クリック直後ガード
         */
        if (isRecentlyRightClickedStorage(player)) {
            return;
        }

        if (!player.isSneaking()) {
            return;
        }

        if (isLookingAtBlock(player)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        Long last = lastToggle.get(uuid);

        if (last != null
                && now - last < TOGGLE_COOLDOWN_MS) {
            return;
        }

        ItemStack item =
                player.getInventory().getItemInMainHand();

        if (!nbt.isStorage(item)) {
            return;
        }

        StorageData data = nbt.read(item);

        if (data == null) {
            feedback.fail(player);
            feedback.actionBar(
                    player,
                    ChatColor.RED + "ストレージデータが不正です"
            );
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            feedback.fail(player);
            feedback.actionBar(
                    player,
                    ChatColor.RED + "アイテムデータが不正です"
            );
            return;
        }

        PersistentDataContainer pdc =
                meta.getPersistentDataContainer();

        byte current = readAutoSafely(pdc);

        byte next =
                current == AutoCollectSettings.AUTO_ON
                        ? AutoCollectSettings.AUTO_OFF
                        : AutoCollectSettings.AUTO_ON;

        pdc.remove(autoKey);

        pdc.set(
                autoKey,
                PersistentDataType.BYTE,
                next
        );

        item.setItemMeta(meta);

        lastToggle.put(uuid, now);

        if (!lore.update(item, data)) {
            feedback.fail(player);

            feedback.actionBar(
                    player,
                    ChatColor.RED + "表示更新に失敗しました"
            );

            return;
        }

        if (autoCollectLore != null) {
            autoCollectLore.apply(item);
        }

        player.getInventory().setItemInMainHand(item);
        player.updateInventory();

        if (next == AutoCollectSettings.AUTO_ON) {

            feedback.actionBar(
                    player,
                    ChatColor.GREEN + "AutoCollect: ON"
            );

            player.playSound(
                    player.getLocation(),
                    Sound.UI_BUTTON_CLICK,
                    0.1f,
                    1.4f
            );

        } else {

            feedback.actionBar(
                    player,
                    ChatColor.RED + "AutoCollect: OFF"
            );

            player.playSound(
                    player.getLocation(),
                    Sound.UI_BUTTON_CLICK,
                    0.1f,
                    0.8f
            );
        }
    }

    private boolean isRecentlyRightClickedStorage(
            Player player
    ) {

        Long last =
                lastRightClickStorage.get(
                        player.getUniqueId()
                );

        if (last == null) {
            return false;
        }

        return System.currentTimeMillis() - last
                < RIGHT_CLICK_GUARD_MS;
    }

    private byte readAutoSafely(
            PersistentDataContainer pdc
    ) {

        if (pdc == null) {
            return AutoCollectSettings.AUTO_OFF;
        }

        try {

            if (pdc.has(
                    autoKey,
                    PersistentDataType.BYTE
            )) {

                Byte value =
                        pdc.get(
                                autoKey,
                                PersistentDataType.BYTE
                        );

                if (value != null
                        && value == AutoCollectSettings.AUTO_ON) {

                    return AutoCollectSettings.AUTO_ON;
                }

                return AutoCollectSettings.AUTO_OFF;
            }

        } catch (Exception ignored) {

            pdc.remove(autoKey);
        }

        return AutoCollectSettings.AUTO_OFF;
    }

    private boolean isLookingAtBlock(Player player) {

        Block block =
                player.getTargetBlockExact(
                        BLOCK_REACH_DISTANCE,
                        FluidCollisionMode.NEVER
                );

        return block != null;
    }
}