package ms.addonimpl.hopper;

import ms.core.StorageNBT;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class HopperAddon {

    private final JavaPlugin plugin;
    private final StorageNBT nbt;

    private HopperMoveListener listener;

    public HopperAddon(
            JavaPlugin plugin,
            StorageNBT nbt
    ) {
        this.plugin = plugin;
        this.nbt = nbt;
    }

    public void enable() {
        listener = new HopperMoveListener(nbt);

        plugin.getServer()
                .getPluginManager()
                .registerEvents(
                        listener,
                        plugin
                );

        plugin.getLogger().info(
                "[MStorage] Hopper storage-item protection enabled."
        );
    }

    public void disable() {
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }

        plugin.getLogger().info(
                "[MStorage] Hopper storage-item protection disabled."
        );
    }
}