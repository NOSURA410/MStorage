package ms.addonimpl.redstonecontainer;

import ms.api.StorageBridge;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class RedstoneContainerAddon {

    private final JavaPlugin plugin;
    private final StorageBridge bridge;
    private final StorageNBT nbt;
    private final StorageValidator validator;

    private RedstoneContainerListener listener;
    private RedstoneContainerQueue queue;

    public RedstoneContainerAddon(
            JavaPlugin plugin,
            StorageBridge bridge,
            StorageNBT nbt,
            StorageValidator validator
    ) {
        this.plugin = plugin;
        this.bridge = bridge;
        this.nbt = nbt;
        this.validator = validator;
    }

    public void enable() {

        RedstoneContainerCooldown cooldown =
                new RedstoneContainerCooldown(plugin);

        RedstoneContainerLockManager lockManager =
                new RedstoneContainerLockManager(plugin);

        ContainerChainScanner scanner =
                new ContainerChainScanner();

        ContainerStorageTransferService transferService =
                new ContainerStorageTransferService(
                        plugin,
                        bridge,
                        nbt,
                        validator
                );

        RedstoneContainerProcessor processor =
                new RedstoneContainerProcessor(
                        scanner,
                        transferService
                );

        queue =
                new RedstoneContainerQueue(
                        plugin,
                        scanner,
                        cooldown,
                        lockManager,
                        processor
                );

        listener =
                new RedstoneContainerListener(
                        plugin,
                        scanner,
                        queue,
                        lockManager
                );

        plugin.getServer()
                .getPluginManager()
                .registerEvents(
                        listener,
                        plugin
                );

        queue.start();

        plugin.getLogger().info(
                "[MStorage] Redstone container addon enabled."
        );
    }

    public void disable() {

        if (queue != null) {

            queue.stop();
            queue = null;
        }

        if (listener != null) {

            HandlerList.unregisterAll(listener);
            listener = null;
        }

        plugin.getLogger().info(
                "[MStorage] Redstone container addon disabled."
        );
    }
}