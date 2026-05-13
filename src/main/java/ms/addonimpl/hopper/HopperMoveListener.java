package ms.addonimpl.hopper;

import ms.core.StorageNBT;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class HopperMoveListener implements Listener {

    private static final int DOWNSTREAM_CHECK_DEPTH = 4;

    private final JavaPlugin plugin;
    private final StorageNBT nbt;
    private final HopperSelector selector;
    private final HopperImportProcessor importProcessor;
    private final HopperExportProcessor exportProcessor;
    private final HopperLockManager lockManager;

    private final Map<String, Long> lastProcessedTick = new HashMap<>();

    public HopperMoveListener(
            JavaPlugin plugin,
            StorageNBT nbt,
            HopperSelector selector,
            HopperImportProcessor importProcessor,
            HopperExportProcessor exportProcessor,
            HopperLockManager lockManager
    ) {
        this.plugin = plugin;
        this.nbt = nbt;
        this.selector = selector;
        this.importProcessor = importProcessor;
        this.exportProcessor = exportProcessor;
        this.lockManager = lockManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMove(InventoryMoveItemEvent e) {
        Inventory source = e.getSource();
        Inventory destination = e.getDestination();
        ItemStack movingItem = e.getItem();

        if (source == null || destination == null || movingItem == null || movingItem.getAmount() <= 0) {
            return;
        }

        /*
         * 最優先：
         * 横方向に流れようとしているが、下方向に対応MSストレージ経路がある場合、
         * 横流しをキャンセルして source をロック延長する。
         */
        if (!nbt.isStorage(movingItem)
                && selector.isHopperInventory(source)
                && selector.isHopperInventory(destination)
                && isHorizontalTransfer(source, destination)
                && hasMatchingStorageDownstream(source, movingItem)) {

            e.setCancelled(true);
            lockManager.refreshLock(source);
            return;
        }

        /*
         * source がロック中なら、対応MS収納以外の搬出は止める。
         */
        if (selector.hasStorage(source)
                && lockManager.isLocked(source)
                && !hasMatchingStorage(destination, movingItem)) {

            e.setCancelled(true);
            return;
        }

        /*
         * 通常アイテム → 搬送先Inventory内の同種MSストレージへ直接収納
         */
        if (!nbt.isStorage(movingItem)
                && HopperSettings.ENABLE_IMPORT_TO_STORAGE) {

            ItemStack destinationStorage = selector.findMatchingStorageInInventory(
                    destination,
                    movingItem
            );

            if (destinationStorage != null) {
                e.setCancelled(true);

                ItemStack template = movingItem.clone();

                if (selector.isHopperInventory(source)) {
                    int imported = importProcessor.importToStorage(
                            destinationStorage,
                            source,
                            template
                    );

                    if (imported > 0) {
                        lockManager.refreshLock(source);
                        markCooldown(destination);
                    }

                    return;
                }

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    int imported = importProcessor.importToStorage(
                            destinationStorage,
                            source,
                            template
                    );

                    if (imported > 0) {
                        lockManager.refreshLock(source);
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
            Inventory cooldownTarget = selector.isHopperInventory(source)
                    ? source
                    : destination;

            if (!isCooldown(cooldownTarget)) {
                ItemStack storage = selector.findExportableStorage(source);

                if (storage != null) {
                    int exported = exportProcessor.exportFromStorage(
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
        if (HopperSettings.BLOCK_STORAGE_ITEM_MOVE && nbt.isStorage(movingItem)) {
            e.setCancelled(true);

            if (!isCooldown(destination)) {
                int moved = exportProcessor.moveNormalItemInsteadOfBlockedStorage(
                        source,
                        destination
                );

                if (moved > 0) {
                    markCooldown(destination);
                }
            }
        }
    }

    private boolean isHorizontalTransfer(
            Inventory source,
            Inventory destination
    ) {
        Block sourceBlock = selector.getInventoryBlock(source);
        Block destinationBlock = selector.getInventoryBlock(destination);

        if (sourceBlock == null || destinationBlock == null) {
            return false;
        }

        return sourceBlock.getY() == destinationBlock.getY()
                && (
                sourceBlock.getX() != destinationBlock.getX()
                        || sourceBlock.getZ() != destinationBlock.getZ()
        );
    }

    private boolean hasMatchingStorageDownstream(
            Inventory source,
            ItemStack movingItem
    ) {
        Block sourceBlock = selector.getInventoryBlock(source);

        if (sourceBlock == null || movingItem == null) {
            return false;
        }

        Block current = sourceBlock;

        for (int i = 0; i < DOWNSTREAM_CHECK_DEPTH; i++) {
            current = current.getRelative(0, -1, 0);

            BlockState state = current.getState();

            if (!(state instanceof InventoryHolder holder)) {
                return false;
            }

            Inventory inventory = holder.getInventory();

            if (selector.findMatchingStorageInInventory(inventory, movingItem) != null) {
                return true;
            }
        }

        return false;
    }

    private boolean hasMatchingStorage(
            Inventory destination,
            ItemStack movingItem
    ) {
        return selector.findMatchingStorageInInventory(
                destination,
                movingItem
        ) != null;
    }

    private boolean isCooldown(Inventory inventory) {
        Block block = selector.getHopperBlock(inventory);

        if (block == null) {
            return false;
        }

        if (!block.getChunk().isLoaded()) {
            return true;
        }

        String key = block.getWorld().getName()
                + ":"
                + block.getX()
                + ":"
                + block.getY()
                + ":"
                + block.getZ();

        long currentTick = (long) plugin.getServer().getCurrentTick();
        Long lastTick = lastProcessedTick.get(key);

        return lastTick != null
                && currentTick - lastTick < HopperSettings.HOPPER_COOLDOWN_TICKS;
    }

    private void markCooldown(Inventory inventory) {
        Block block = selector.getHopperBlock(inventory);

        if (block == null) {
            return;
        }

        if (!block.getChunk().isLoaded()) {
            return;
        }

        String key = block.getWorld().getName()
                + ":"
                + block.getX()
                + ":"
                + block.getY()
                + ":"
                + block.getZ();

        lastProcessedTick.put(
                key,
                (long) plugin.getServer().getCurrentTick()
        );
    }
}