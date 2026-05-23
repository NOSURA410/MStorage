package ms.addonimpl.redstonecontainer;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RedstoneContainerCooldown {

    private final JavaPlugin plugin;

    private final Map<String, Long> cooldownUntil =
            new HashMap<>();

    public RedstoneContainerCooldown(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isCooling(String inventoryKey) {

        if (inventoryKey == null || inventoryKey.isBlank()) {
            return true;
        }

        long currentTick =
                plugin.getServer().getCurrentTick();

        Long until =
                cooldownUntil.get(inventoryKey);

        return until != null
                && currentTick < until;
    }

    public void setSuccessCooldown(String inventoryKey) {
        setCooldown(
                inventoryKey,
                RedstoneContainerSettings.SUCCESS_COOLDOWN_TICKS
        );
    }

    public void setFailedCooldown(String inventoryKey) {
        setCooldown(
                inventoryKey,
                RedstoneContainerSettings.FAILED_COOLDOWN_TICKS
        );
    }

    public void setCooldown(
            String inventoryKey,
            long ticks
    ) {
        if (inventoryKey == null
                || inventoryKey.isBlank()
                || ticks <= 0L) {
            return;
        }

        long currentTick =
                plugin.getServer().getCurrentTick();

        cooldownUntil.put(
                inventoryKey,
                currentTick + ticks
        );
    }

    public void clear(String inventoryKey) {

        if (inventoryKey == null || inventoryKey.isBlank()) {
            return;
        }

        cooldownUntil.remove(inventoryKey);
    }

    public void cleanup() {

        long currentTick =
                plugin.getServer().getCurrentTick();

        Iterator<Map.Entry<String, Long>> iterator =
                cooldownUntil.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<String, Long> entry =
                    iterator.next();

            if (currentTick >= entry.getValue()) {
                iterator.remove();
            }
        }
    }
}