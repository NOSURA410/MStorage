package ms.addonimpl.autocollect;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.model.StorageData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class AutoCollectPickupListener implements Listener {

    private static final long STACK_SIZE = 64L;
    private static final long LC_STACKS = 54L;
    private static final long LC_SIZE = STACK_SIZE * LC_STACKS;
    private static final long MAX_LC = 100_000_000L;
    private static final long MAX_AMOUNT = LC_SIZE * MAX_LC;

    private final StorageNBT nbt;
    private final StorageLore lore;
    private final StorageValidator validator;
    private final AutoCollectSelector selector;
    private final AutoCollectLore autoCollectLore;

    public AutoCollectPickupListener(
            StorageNBT nbt,
            StorageLore lore,
            StorageValidator validator,
            AutoCollectSelector selector,
            AutoCollectLore autoCollectLore
    ) {
        this.nbt = nbt;
        this.lore = lore;
        this.validator = validator;
        this.selector = selector;
        this.autoCollectLore = autoCollectLore;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        Entity entity = e.getEntity();

        if (!(entity instanceof Player player)) {
            return;
        }

        Item itemEntity = e.getItem();

        if (itemEntity == null || itemEntity.isDead()) {
            return;
        }

        if (itemEntity.getPickupDelay() > 0) {
            return;
        }

        ItemStack dropped = itemEntity.getItemStack();

        if (dropped == null || dropped.getType() == Material.AIR || dropped.getAmount() <= 0) {
            return;
        }

        ItemStack storage = selector.findStorage(player.getInventory(), dropped);

        if (storage == null) {
            return;
        }

        long stored = store(storage, dropped);

        if (stored <= 0L) {
            return;
        }

        e.setCancelled(true);

        if (dropped.getAmount() <= 0) {
            itemEntity.remove();
        } else {
            itemEntity.setItemStack(dropped);
        }

        player.getInventory().setItemInMainHand(player.getInventory().getItemInMainHand());
        player.updateInventory();

        player.playSound(
                player.getLocation(),
                Sound.ENTITY_ITEM_PICKUP,
                0.1f,
                1.6f
        );
    }

    private long store(ItemStack storageItem, ItemStack droppedItem) {
        StorageData data = nbt.read(storageItem);

        if (data == null) {
            return 0L;
        }

        if (!validator.isSameStoredItem(data.getStoredItem(), droppedItem)) {
            return 0L;
        }

        long currentAmount = data.getAmount();

        if (currentAmount >= MAX_AMOUNT) {
            return 0L;
        }

        long remainingCapacity = MAX_AMOUNT - currentAmount;
        int droppedAmount = droppedItem.getAmount();

        long addAmount = Math.min(droppedAmount, remainingCapacity);

        if (addAmount <= 0L) {
            return 0L;
        }

        StorageData newData = data.withAmount(currentAmount + addAmount);

        if (!nbt.write(storageItem, newData)) {
            return 0L;
        }

        if (!lore.update(storageItem, newData)) {
            return 0L;
        }

        autoCollectLore.apply(storageItem);

        if (addAmount >= droppedAmount) {
            droppedItem.setAmount(0);
        } else {
            droppedItem.setAmount((int) (droppedAmount - addAmount));
        }

        return addAmount;
    }
}