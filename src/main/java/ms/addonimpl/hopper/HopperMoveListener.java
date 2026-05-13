package ms.addonimpl.hopper;

import ms.core.StorageNBT;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class HopperMoveListener implements Listener {

    private final JavaPlugin plugin;
    private final StorageNBT nbt;
    private final HopperSelector selector;
    private final HopperProcessor processor;

    private final Map<String, Long> lastProcessedTick = new HashMap<>();

    public HopperMoveListener(
            JavaPlugin plugin,
            StorageNBT nbt,
            HopperSelector selector,
            HopperProcessor processor
    ) {
        this.plugin = plugin;
        this.nbt = nbt;
        this.selector = selector;
        this.processor = processor;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMove(InventoryMoveItemEvent e) {

        Inventory source = e.getSource();
        Inventory destination = e.getDestination();
        ItemStack movingItem = e.getItem();

        /*
         * 最優先：
         * 通常アイテム → 搬送先ホッパー内MSストレージへ収納
         */
        if (!nbt.isStorage(movingItem)
                && HopperSettings.ENABLE_IMPORT_TO_STORAGE
                && selector.isHopperInventory(destination)) {

            ItemStack storage = selector.findMatchingStorageInHopper(
                    destination,
                    movingItem
            );

            if (storage != null) {

                e.setCancelled(true);

                /*
                 * ホッパー → ホッパー は即時処理
                 */
                if (selector.isHopperInventory(source)) {

                    int imported = processor.importToStorage(
                            storage,
                            source,
                            movingItem
                    );

                    if (imported > 0) {
                        markCooldown(destination);
                    }

                    return;
                }

                /*
                 * チェスト/樽 → ホッパー は次tick処理
                 */
                ItemStack template = movingItem.clone();

                plugin.getServer().getScheduler().runTask(plugin, () -> {

                    int imported = processor.importToStorage(
                            storage,
                            source,
                            template
                    );

                    if (imported > 0) {
                        markCooldown(destination);
                    }
                });

                return;
            }
        }

        /*
         * source内MSストレージ → destinationへ搬出
         */
        if (HopperSettings.ENABLE_EXPORT_FROM_STORAGE) {

            Inventory cooldownTarget =
                    selector.isHopperInventory(source)
                            ? source
                            : destination;

            if (!isCooldown(cooldownTarget)) {

                ItemStack storage =
                        selector.findExportableStorage(source);

                if (storage != null) {

                    int exported = processor.exportFromStorage(
                            storage,
                            destination
                    );

                    if (exported > 0) {

                        markCooldown(cooldownTarget);

                        e.setCancelled(true);

                        return;
                    }
                }
            }
        }

        /*
         * MSストレージ本体はホッパー移動禁止
         */
        if (HopperSettings.BLOCK_STORAGE_ITEM_MOVE
                && nbt.isStorage(movingItem)) {

            e.setCancelled(true);
        }
    }

    private boolean isCooldown(Inventory inventory) {

        Block block = selector.getHopperBlock(inventory);

        if (block == null) {
            return false;
        }

        String key = block.getWorld().getName()
                + ":" + block.getX()
                + ":" + block.getY()
                + ":" + block.getZ();

        long currentTick =
                (long) plugin.getServer().getCurrentTick();

        Long lastTick = lastProcessedTick.get(key);

        return lastTick != null
                && currentTick - lastTick
                < HopperSettings.HOPPER_COOLDOWN_TICKS;
    }

    private void markCooldown(Inventory inventory) {

        Block block = selector.getHopperBlock(inventory);

        if (block == null) {
            return;
        }

        String key = block.getWorld().getName()
                + ":" + block.getX()
                + ":" + block.getY()
                + ":" + block.getZ();

        lastProcessedTick.put(
                key,
                (long) plugin.getServer().getCurrentTick()
        );
    }
}