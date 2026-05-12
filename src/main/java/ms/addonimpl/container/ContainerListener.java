package ms.addonimpl.container;

import ms.core.StorageNBT;
import ms.manager.FeedbackManager;
import ms.model.StorageData;
import ms.model.StorageMode;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ContainerListener implements Listener {

    private static final long SOUND_COOLDOWN_MS = 300L;

    private final StorageNBT nbt;
    private final FeedbackManager feedback;
    private final ContainerSelector selector;
    private final ContainerProcessor processor;

    private final Map<UUID, Long> lastSound = new HashMap<>();

    public ContainerListener(
            StorageNBT nbt,
            FeedbackManager feedback,
            ContainerSelector selector,
            ContainerProcessor processor
    ) {
        this.nbt = nbt;
        this.feedback = feedback;
        this.selector = selector;
        this.processor = processor;
    }

    @EventHandler
    public void onContainerClick(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = e.getAction();

        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (e.getClickedBlock() == null) {
            return;
        }

        Player player = e.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!nbt.isStorage(item)) {
            return;
        }

        StorageData data = nbt.read(item);

        if (data == null || data.getMode() != StorageMode.CONTAINER) {
            return;
        }

        Block block = e.getClickedBlock();

        if (!selector.isValidContainer(block)) {
            return;
        }

        Inventory inventory = selector.getInventory(block);

        if (inventory == null) {
            return;
        }

        e.setCancelled(true);

        if (action == Action.LEFT_CLICK_BLOCK) {
            long added = processor.collectFromContainer(item, inventory);

            if (added > 0L) {
                player.getInventory().setItemInMainHand(item);
                player.updateInventory();

                playContainerSound(player);
                feedback.success(player);
                feedback.actionBar(player, ChatColor.GREEN + "コンテナから収納しました: +" + added);
            } else {
                feedback.fail(player);
                feedback.actionBar(player, ChatColor.RED + "収納できるアイテムがありません");
            }

            return;
        }

        if (action == Action.RIGHT_CLICK_BLOCK) {
            long removed = processor.fillContainer(item, inventory);

            if (removed > 0L) {
                player.getInventory().setItemInMainHand(item);
                player.updateInventory();

                playContainerSound(player);
                feedback.success(player);
                feedback.actionBar(player, ChatColor.AQUA + "コンテナへ補充しました: -" + removed);
            } else {
                feedback.fail(player);
                feedback.actionBar(player, ChatColor.RED + "補充できません");
            }
        }
    }

    private void playContainerSound(Player player) {
        if (player == null) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        Long last = lastSound.get(uuid);

        if (last != null && now - last < SOUND_COOLDOWN_MS) {
            return;
        }

        lastSound.put(uuid, now);

        player.playSound(
                player.getLocation(),
                Sound.ENTITY_ITEM_PICKUP,
                0.1f,
                1.6f
        );
    }
}