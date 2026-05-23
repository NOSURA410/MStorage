package ms.addonimpl.redstonecontainer;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public class RedstoneContainerListener implements Listener {

    private static final int REDSTONE_SCAN_RADIUS = 2;

    private static final BlockFace[] FACES = {
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    };

    private final JavaPlugin plugin;
    private final ContainerChainScanner scanner;
    private final RedstoneContainerQueue queue;
    private final RedstoneContainerLockManager lockManager;

    public RedstoneContainerListener(
            JavaPlugin plugin,
            ContainerChainScanner scanner,
            RedstoneContainerQueue queue,
            RedstoneContainerLockManager lockManager
    ) {
        this.plugin = plugin;
        this.scanner = scanner;
        this.queue = queue;
        this.lockManager = lockManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRedstone(BlockRedstoneEvent e) {

        if (e.getOldCurrent() > 0 || e.getNewCurrent() <= 0) {
            return;
        }

        Block changedBlock = e.getBlock();

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> scanAroundRedstoneChange(changedBlock),
                        1L
                );
    }

    private void scanAroundRedstoneChange(Block center) {

        if (center == null || !center.getChunk().isLoaded()) {
            return;
        }

        for (int x = -REDSTONE_SCAN_RADIUS; x <= REDSTONE_SCAN_RADIUS; x++) {
            for (int y = -REDSTONE_SCAN_RADIUS; y <= REDSTONE_SCAN_RADIUS; y++) {
                for (int z = -REDSTONE_SCAN_RADIUS; z <= REDSTONE_SCAN_RADIUS; z++) {

                    Block candidate = center.getRelative(x, y, z);

                    if (!scanner.isContainerBlock(candidate)) {
                        continue;
                    }

                    if (isLampLikePowered(candidate)) {
                        queue.queue(candidate);
                    }
                }
            }
        }
    }

    private boolean isLampLikePowered(Block block) {

        if (block == null || !block.getChunk().isLoaded()) {
            return false;
        }

        if (block.isBlockPowered()) {
            return true;
        }

        if (block.isBlockIndirectlyPowered()) {
            return true;
        }

        if (block.getBlockPower() > 0) {
            return true;
        }

        for (BlockFace face : FACES) {
            Block relative = block.getRelative(face);

            if (relative == null || !relative.getChunk().isLoaded()) {
                continue;
            }

            if (relative.isBlockPowered()) {
                return true;
            }

            if (relative.isBlockIndirectlyPowered()) {
                return true;
            }

            if (relative.getBlockPower() > 0) {
                return true;
            }
        }

        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onOpen(InventoryOpenEvent e) {

        if (isLocked(e.getInventory())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§c[MS] 搬送中です。しばらくお待ちください。");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent e) {

        if (isLocked(e.getInventory())) {
            e.setCancelled(true);
            e.getWhoClicked().sendMessage("§c[MS] 搬送中です。しばらくお待ちください。");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent e) {

        if (isLocked(e.getInventory())) {
            e.setCancelled(true);
            e.getWhoClicked().sendMessage("§c[MS] 搬送中です。しばらくお待ちください。");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onMove(InventoryMoveItemEvent e) {

        if (isLocked(e.getSource()) || isLocked(e.getDestination())) {
            e.setCancelled(true);
        }
    }

    private boolean isLocked(Inventory inventory) {

        if (inventory == null) {
            return false;
        }

        String inventoryKey = scanner.getInventoryKey(inventory);

        return lockManager.isLocked(inventoryKey);
    }
}