package ms.manager;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.model.StorageData;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

public class StorageLoreUpdateQueue {

    private static final long UPDATE_DELAY_TICKS = 10L;
    private static final int MAX_UPDATES_PER_TICK = 24;

    private final JavaPlugin plugin;
    private final StorageNBT nbt;
    private final StorageLore lore;

    private final Map<ItemStack, Long> queuedItems =
            new IdentityHashMap<>();

    private BukkitTask task;

    public StorageLoreUpdateQueue(
            JavaPlugin plugin,
            StorageNBT nbt,
            StorageLore lore
    ) {
        this.plugin = plugin;
        this.nbt = nbt;
        this.lore = lore;
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
                        UPDATE_DELAY_TICKS,
                        UPDATE_DELAY_TICKS
                );
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        flushAll();
        queuedItems.clear();
    }

    public void queue(ItemStack storageItem) {
        if (!nbt.isStorage(storageItem)) {
            return;
        }

        /*
         * 同じItemStackは上書き予約。
         * 連続変更されてもLore更新は最後の1回に集約する。
         */
        queuedItems.put(
                storageItem,
                (long) plugin.getServer().getCurrentTick()
        );
    }

    public void flush(ItemStack storageItem) {
        if (!nbt.isStorage(storageItem)) {
            return;
        }

        StorageData data = nbt.read(storageItem);

        if (data == null) {
            queuedItems.remove(storageItem);
            return;
        }

        lore.update(storageItem, data);
        queuedItems.remove(storageItem);
    }

    public void flushAll() {
        for (ItemStack item : queuedItems.keySet().toArray(new ItemStack[0])) {
            flush(item);
        }
    }

    private void tick() {
        if (queuedItems.isEmpty()) {
            return;
        }

        long currentTick =
                plugin.getServer().getCurrentTick();

        int updated = 0;

        Iterator<Map.Entry<ItemStack, Long>> iterator =
                queuedItems.entrySet().iterator();

        while (iterator.hasNext()
                && updated < MAX_UPDATES_PER_TICK) {

            Map.Entry<ItemStack, Long> entry =
                    iterator.next();

            ItemStack item =
                    entry.getKey();

            long queuedTick =
                    entry.getValue();

            if (currentTick - queuedTick < UPDATE_DELAY_TICKS) {
                continue;
            }

            StorageData data =
                    nbt.read(item);

            if (data == null) {
                iterator.remove();
                continue;
            }

            if (lore.update(item, data)) {
                iterator.remove();
            }

            updated++;
        }
    }
}