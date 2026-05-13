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
    private final HopperImportProcessor importProcessor;
    private final HopperExportProcessor exportProcessor;

    private final Map<String, Long> lastProcessedTick = new HashMap<>();

    public HopperMoveListener(
            JavaPlugin plugin,
            StorageNBT nbt,
            HopperSelector selector,
            HopperImportProcessor importProcessor,
            HopperExportProcessor exportProcessor
    ) {
        this.plugin = plugin;
        this.nbt = nbt;
        this.selector = selector;
        this.importProcessor = importProcessor;
        this.exportProcessor = exportProcessor;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMove(InventoryMoveItemEvent e) {

        Inventory source = e.getSource();
        Inventory destination = e.getDestination();
        ItemStack movingItem = e.getItem();

        // 1. 通常アイテム → 搬送先Inventory内の同種MSストレージへ直接収納
        if (!nbt.isStorage(movingItem)
                && HopperSettings.ENABLE_IMPORT_TO_STORAGE) {

            ItemStack destinationStorage = selector.findMatchingStorageInInventory(
                    destination,
                    movingItem
            );

            if (destinationStorage != null) {
                e.setCancelled(true);

                ItemStack template = movingItem.clone();

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    int imported = importProcessor.importToStorage(
                            destinationStorage,
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

        // 2. source内MSストレージ → destinationへ搬出
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

        // 3. MSストレージ本体はホッパー移動禁止
        //    空ストレージ等で詰まる場合は、同じInventory内の通常アイテムを代替搬送
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

    private boolean isCooldown(Inventory inventory) {

        Block block = selector.getHopperBlock(inventory);

        if (block == null) {
            return false;
        }

        if (!block.getChunk().isLoaded()) {
            return true;
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

        if (!block.getChunk().isLoaded()) {
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