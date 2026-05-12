package ms.listener;

import ms.core.StorageNBT;
import ms.manager.FeedbackManager;
import ms.model.StorageData;
import ms.model.StorageMode;
import ms.service.StorageService;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CoreActionListener implements Listener {

    private static final int BLOCK_REACH_DISTANCE = 5;

    private final StorageNBT nbt;
    private final StorageService service;
    private final FeedbackManager feedback;

    public CoreActionListener(StorageNBT nbt, StorageService service, FeedbackManager feedback) {
        this.nbt = nbt;
        this.service = service;
        this.feedback = feedback;
    }

    @EventHandler
    public void onRightClickAir(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (e.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        Player player = e.getPlayer();

        if (player.isSneaking()) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (!nbt.isStorage(item)) {
            return;
        }

        StorageData data = nbt.read(item);

        if (data == null || data.getMode() != StorageMode.HAND) {
            return;
        }

        if (isLookingAtBlock(player)) {
            return;
        }

        e.setCancelled(true);

        int removed = service.withdraw(player, item);

        if (removed > 0) {
            player.getInventory().setItemInMainHand(item);
            player.updateInventory();
            feedback.success(player);
        } else {
            feedback.fail(player);
        }
    }

    @EventHandler
    public void onLeftClickAir(PlayerAnimationEvent e) {
        Player player = e.getPlayer();

        if (player.isSneaking()) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (!nbt.isStorage(item)) {
            return;
        }

        StorageData data = nbt.read(item);

        if (data == null || data.getMode() != StorageMode.HAND) {
            return;
        }

        if (isLookingAtBlock(player)) {
            return;
        }

        long added = service.storeAll(player, item);

        if (added > 0L) {
            player.getInventory().setItemInMainHand(item);
            player.updateInventory();
            feedback.success(player);
        } else {
            feedback.fail(player);
        }
    }

    private boolean isLookingAtBlock(Player player) {
        Block block = player.getTargetBlockExact(
                BLOCK_REACH_DISTANCE,
                FluidCollisionMode.NEVER
        );

        return block != null;
    }
}