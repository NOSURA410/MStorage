package ms.listener;

import ms.core.StorageNBT;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

public class AmmoProtectionListener implements Listener {

    private final StorageNBT nbt;

    public AmmoProtectionListener(StorageNBT nbt) {
        this.nbt = nbt;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onUseBowOrCrossbow(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = e.getAction();

        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = e.getPlayer();

        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();

        if (!isBowOrCrossbow(main) && !isBowOrCrossbow(off)) {
            return;
        }

        if (!hasStorageArrow(player)) {
            return;
        }

        e.setCancelled(true);
        e.setUseItemInHand(Result.DENY);
        e.setUseInteractedBlock(Result.DENY);

        player.sendActionBar(ChatColor.RED + "MSストレージ矢は弾薬として使用できません");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onShootBow(EntityShootBowEvent e) {
        ItemStack consumable = e.getConsumable();

        if (!nbt.isStorage(consumable)) {
            return;
        }

        e.setCancelled(true);

        if (e.getEntity() instanceof Player player) {
            player.sendActionBar(ChatColor.RED + "MSストレージ矢は弾薬として使用できません");
        }
    }

    private boolean hasStorageArrow(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isArrow(item) && nbt.isStorage(item)) {
                return true;
            }
        }

        ItemStack offHand = player.getInventory().getItemInOffHand();
        return isArrow(offHand) && nbt.isStorage(offHand);
    }

    private boolean isBowOrCrossbow(ItemStack item) {
        if (item == null) {
            return false;
        }

        return item.getType() == Material.BOW
                || item.getType() == Material.CROSSBOW;
    }

    private boolean isArrow(ItemStack item) {
        if (item == null) {
            return false;
        }

        return item.getType() == Material.ARROW
                || item.getType() == Material.SPECTRAL_ARROW
                || item.getType() == Material.TIPPED_ARROW;
    }
}