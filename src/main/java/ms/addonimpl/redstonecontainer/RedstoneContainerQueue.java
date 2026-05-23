package ms.addonimpl.redstonecontainer;

import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class RedstoneContainerQueue {

    private final JavaPlugin plugin;
    private final ContainerChainScanner scanner;
    private final RedstoneContainerCooldown cooldown;
    private final RedstoneContainerLockManager lockManager;
    private final RedstoneContainerProcessor processor;

    private final Queue<QueuedContainer> queue =
            new LinkedList<>();

    private final Set<String> queuedInventoryKeys =
            new HashSet<>();

    private BukkitTask task;

    public RedstoneContainerQueue(
            JavaPlugin plugin,
            ContainerChainScanner scanner,
            RedstoneContainerCooldown cooldown,
            RedstoneContainerLockManager lockManager,
            RedstoneContainerProcessor processor
    ) {
        this.plugin = plugin;
        this.scanner = scanner;
        this.cooldown = cooldown;
        this.lockManager = lockManager;
        this.processor = processor;
    }

    public void start() {
        if (task != null) {
            return;
        }

        task = plugin.getServer()
                .getScheduler()
                .runTaskTimer(
                        plugin,
                        this::tick,
                        RedstoneContainerSettings.SIGNAL_DELAY_TICKS,
                        1L
                );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        queue.clear();
        queuedInventoryKeys.clear();
    }

    public boolean queue(Block sourceBlock) {
        if (sourceBlock == null) {
            return false;
        }

        if (!sourceBlock.getChunk().isLoaded()) {
            return false;
        }

        if (!scanner.isContainerBlock(sourceBlock)) {
            return false;
        }

        Inventory inventory =
                scanner.getInventory(sourceBlock);

        if (inventory == null) {
            return false;
        }

        String inventoryKey =
                scanner.getInventoryKey(inventory);

        if (cooldown.isCooling(inventoryKey)) {
            return false;
        }

        if (lockManager.isLocked(inventoryKey)) {
            return false;
        }

        if (queue.size()
                >= RedstoneContainerSettings.MAX_QUEUE_SIZE) {
            return false;
        }

        if (!queuedInventoryKeys.add(inventoryKey)) {
            return false;
        }

        lockManager.lock(inventoryKey);

        queue.add(
                new QueuedContainer(
                        sourceBlock,
                        inventoryKey
                )
        );

        return true;
    }

    private void tick() {
        cooldown.cleanup();
        lockManager.cleanup();

        int processed = 0;

        while (!queue.isEmpty()
                && processed
                < RedstoneContainerSettings.MAX_TASKS_PER_TICK) {

            QueuedContainer queued =
                    queue.poll();

            if (queued == null) {
                continue;
            }

            queuedInventoryKeys.remove(
                    queued.inventoryKey()
            );

            try {
                long moved =
                        processor.process(
                                queued.sourceBlock()
                        );

                if (moved > 0L) {
                    cooldown.setSuccessCooldown(
                            queued.inventoryKey()
                    );
                } else {
                    cooldown.setFailedCooldown(
                            queued.inventoryKey()
                    );
                }

            } finally {
                lockManager.unlock(
                        queued.inventoryKey()
                );
            }

            processed++;
        }
    }

    private record QueuedContainer(
            Block sourceBlock,
            String inventoryKey
    ) {
    }
}