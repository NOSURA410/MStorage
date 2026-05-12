package ms.addonimpl.autocollect;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.model.StorageData;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AutoCollectProcessor {

    private static final long STACK_SIZE = 64L;
    private static final long LC_STACKS = 54L;
    private static final long LC_SIZE = STACK_SIZE * LC_STACKS;
    private static final long MAX_LC = 100_000_000L;
    private static final long MAX_AMOUNT = LC_SIZE * MAX_LC;

    private static final long PICKUP_SOUND_COOLDOWN_MS = 300L;

    private final StorageNBT nbt;
    private final StorageLore lore;
    private final StorageValidator validator;
    private final AutoCollectSelector selector;
    private final AutoCollectLore autoCollectLore;

    private final Map<UUID, Long> lastPickupSound = new HashMap<>();

    public AutoCollectProcessor(
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

    public void process(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        Collection<Item> nearbyItems = player.getWorld().getNearbyEntitiesByType(
                Item.class,
                player.getLocation(),
                AutoCollectSettings.RADIUS
        );

        int processed = 0;

        for (Item entityItem : nearbyItems) {
            if (processed >= AutoCollectSettings.MAX_ITEMS_PER_PLAYER) {
                break;
            }

            if (entityItem == null || entityItem.isDead()) {
                continue;
            }

            if (entityItem.getPickupDelay() > 0) {
                continue;
            }

            ItemStack dropped = entityItem.getItemStack();

            if (dropped == null || dropped.getType() == Material.AIR || dropped.getAmount() <= 0) {
                continue;
            }

            ItemStack storage = selector.findStorage(player.getInventory(), dropped);

            if (storage == null) {
                continue;
            }

            if (storeDroppedItem(storage, dropped)) {
                playPickupSound(player);

                if (dropped.getAmount() <= 0) {
                    entityItem.remove();
                } else {
                    entityItem.setItemStack(dropped);
                }

                processed++;
            }
        }
    }

    private boolean storeDroppedItem(ItemStack storageItem, ItemStack droppedItem) {
        StorageData data = nbt.read(storageItem);

        if (data == null) {
            return false;
        }

        if (!validator.isSameStoredItem(data.getStoredItem(), droppedItem)) {
            return false;
        }

        long currentAmount = data.getAmount();

        if (currentAmount >= MAX_AMOUNT) {
            return false;
        }

        long remainingCapacity = MAX_AMOUNT - currentAmount;

        int droppedAmount = droppedItem.getAmount();
        long addAmount = Math.min(droppedAmount, remainingCapacity);

        if (addAmount <= 0L) {
            return false;
        }

        StorageData newData = data.withAmount(currentAmount + addAmount);

        if (!nbt.write(storageItem, newData)) {
            return false;
        }

        if (!lore.update(storageItem, newData)) {
            return false;
        }

        autoCollectLore.apply(storageItem);

        if (addAmount >= droppedAmount) {
            droppedItem.setAmount(0);
        } else {
            droppedItem.setAmount((int) (droppedAmount - addAmount));
        }

        return true;
    }

    private void playPickupSound(Player player) {
        if (player == null) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID uuid = player.getUniqueId();

        Long last = lastPickupSound.get(uuid);

        if (last != null && now - last < PICKUP_SOUND_COOLDOWN_MS) {
            return;
        }

        lastPickupSound.put(uuid, now);

        player.playSound(
                player.getLocation(),
                Sound.ENTITY_ITEM_PICKUP,
                0.1f,
                1.6f
        );
    }
}