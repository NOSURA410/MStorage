package ms.addonimpl.hopper;

import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class HopperLockManager {

    private static final long LOCK_KEEP_TICKS = 8L;

    private final JavaPlugin plugin;
    private final HopperSelector selector;
    private final Map<String, Long> lockedUntilTick = new HashMap<>();

    public HopperLockManager(JavaPlugin plugin, HopperSelector selector) {
        this.plugin = plugin;
        this.selector = selector;
    }

    public void refreshLock(Inventory inventory) {
        String key = createKey(inventory);

        if (key == null) {
            return;
        }

        long untilTick = (long) plugin.getServer().getCurrentTick() + LOCK_KEEP_TICKS;
        lockedUntilTick.put(key, untilTick);
    }

    public boolean isLocked(Inventory inventory) {
        String key = createKey(inventory);

        if (key == null) {
            return false;
        }

        Long untilTick = lockedUntilTick.get(key);

        if (untilTick == null) {
            return false;
        }

        long currentTick = (long) plugin.getServer().getCurrentTick();

        if (currentTick > untilTick) {
            lockedUntilTick.remove(key);
            return false;
        }

        return true;
    }

    private String createKey(Inventory inventory) {
        Block block = selector.getInventoryBlock(inventory);

        if (block == null) {
            return null;
        }

        return block.getWorld().getName()
                + ":"
                + block.getX()
                + ":"
                + block.getY()
                + ":"
                + block.getZ();
    }
}