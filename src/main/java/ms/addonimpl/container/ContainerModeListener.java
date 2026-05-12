package ms.addonimpl.container;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.manager.FeedbackManager;
import ms.model.StorageData;
import ms.model.StorageMode;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ContainerModeListener implements Listener {

    private static final long SOUND_COOLDOWN_MS = 300L;

    private final StorageNBT nbt;
    private final StorageLore lore;
    private final FeedbackManager feedback;

    private final Map<UUID, Long> lastSound = new HashMap<>();

    public ContainerModeListener(JavaPlugin plugin, StorageNBT nbt, StorageLore lore, FeedbackManager feedback) {
        this.nbt = nbt;
        this.lore = lore;
        this.feedback = feedback;
    }

    @EventHandler
    public void onSneakRightClick(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = e.getAction();

        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = e.getPlayer();

        if (!player.isSneaking()) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (!nbt.isStorage(item)) {
            return;
        }

        e.setCancelled(true);

        StorageData data = nbt.read(item);

        if (data == null) {
            feedback.fail(player);
            feedback.actionBar(player, ChatColor.RED + "ストレージデータが不正です");
            return;
        }

        StorageMode nextMode = data.getMode() == StorageMode.CONTAINER
                ? StorageMode.HAND
                : StorageMode.CONTAINER;

        StorageData newData = data.withMode(nextMode);

        if (!nbt.write(item, newData)) {
            feedback.fail(player);
            feedback.actionBar(player, ChatColor.RED + "モード更新に失敗しました");
            return;
        }

        if (!lore.update(item, newData)) {
            feedback.fail(player);
            feedback.actionBar(player, ChatColor.RED + "表示更新に失敗しました");
            return;
        }

        player.getInventory().setItemInMainHand(item);

        if (nextMode == StorageMode.CONTAINER) {
            feedback.actionBar(player, ChatColor.GREEN + "Mode: CONTAINER");
        } else {
            feedback.actionBar(player, ChatColor.AQUA + "Mode: HAND");
        }

        playModeSound(player, nextMode);
    }

    private void playModeSound(Player player, StorageMode mode) {
        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        Long last = lastSound.get(uuid);

        if (last != null && now - last < SOUND_COOLDOWN_MS) {
            return;
        }

        lastSound.put(uuid, now);

        float pitch = mode == StorageMode.CONTAINER ? 1.6f : 1.0f;

        player.playSound(
                player.getLocation(),
                Sound.UI_BUTTON_CLICK,
                0.1f,
                pitch
        );
    }
}