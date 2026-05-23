package ms.addonimpl.redstonecontainer;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RedstoneContainerLockManager {

    private final JavaPlugin plugin;

    private final Map<String, Long> lockedUntil =
            new HashMap<>();

    public RedstoneContainerLockManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void lock(String inventoryKey) {

        if (inventoryKey == null || inventoryKey.isBlank()) {
            return;
        }

        long currentTick =
                plugin.getServer().getCurrentTick();

        lockedUntil.put(
                inventoryKey,
                currentTick
                        + RedstoneContainerSettings.LOCK_TIMEOUT_TICKS
        );
    }

    public void unlock(String inventoryKey) {

        if (inventoryKey == null || inventoryKey.isBlank()) {
            return;
        }

        lockedUntil.remove(inventoryKey);
    }

    public boolean isLocked(String inventoryKey) {

        if (inventoryKey == null || inventoryKey.isBlank()) {
            return false;
        }

        long currentTick =
                plugin.getServer().getCurrentTick();

        Long until =
                lockedUntil.get(inventoryKey);

        return until != null
                && currentTick < until;
    }

    public void cleanup() {

        long currentTick =
                plugin.getServer().getCurrentTick();

        Iterator<Map.Entry<String, Long>> iterator =
                lockedUntil.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<String, Long> entry =
                    iterator.next();

            if (currentTick >= entry.getValue()) {
                iterator.remove();
            }
        }
    }
}