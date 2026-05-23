package ms.plugin;

import ms.addonimpl.autocollect.AutoCollectAddon;
import ms.addonimpl.container.ContainerAddon;
import ms.addonimpl.hopper.HopperAddon;
import ms.addonimpl.redstonecontainer.RedstoneContainerAddon;
import ms.api.StorageBridge;
import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.listener.AmmoProtectionListener;
import ms.listener.BarrelGuideLoreListener;
import ms.listener.CoreActionListener;
import ms.listener.CoreProtectionListener;
import ms.listener.CraftListener;
import ms.listener.StorageReleaseListener;
import ms.manager.FeedbackManager;
import ms.manager.StorageLoreUpdateQueue;
import ms.service.StorageService;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class MStoragePlugin extends JavaPlugin {

    private AutoCollectAddon autoCollectAddon;
    private ContainerAddon containerAddon;
    private HopperAddon hopperAddon;
    private RedstoneContainerAddon redstoneContainerAddon;

    private StorageBridge storageBridge;
    private StorageLoreUpdateQueue loreUpdateQueue;

    @Override
    public void onEnable() {

        /*
         * ------------------------------------------------------------
         * Core
         * ------------------------------------------------------------
         */

        StorageNBT nbt =
                new StorageNBT(this);

        StorageLore lore =
                new StorageLore();

        StorageValidator validator =
                new StorageValidator(nbt);

        /*
         * ------------------------------------------------------------
         * Lore queue
         * ------------------------------------------------------------
         */

        loreUpdateQueue =
                new StorageLoreUpdateQueue(
                        this,
                        nbt,
                        lore
                );

        loreUpdateQueue.start();

        /*
         * ------------------------------------------------------------
         * Service
         * ------------------------------------------------------------
         */

        StorageService service =
                new StorageService(
                        nbt,
                        lore,
                        validator
                );

        FeedbackManager feedback =
                new FeedbackManager();

        /*
         * ------------------------------------------------------------
         * Storage API
         * ------------------------------------------------------------
         */

        storageBridge =
                new StorageBridge(
                        this,
                        nbt,
                        lore,
                        validator,
                        loreUpdateQueue
                );

        getServer()
                .getServicesManager()
                .register(
                        StorageBridge.class,
                        storageBridge,
                        this,
                        ServicePriority.Normal
                );

        /*
         * ------------------------------------------------------------
         * Core listeners
         * ------------------------------------------------------------
         */

        getServer()
                .getPluginManager()
                .registerEvents(
                        new CoreActionListener(
                                nbt,
                                service,
                                feedback
                        ),
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        new CoreProtectionListener(
                                this,
                                nbt
                        ),
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        new CraftListener(
                                nbt,
                                lore,
                                validator
                        ),
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        new BarrelGuideLoreListener(
                                nbt
                        ),
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        new StorageReleaseListener(
                                nbt,
                                feedback
                        ),
                        this
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        new AmmoProtectionListener(
                                nbt
                        ),
                        this
                );

        /*
         * ------------------------------------------------------------
         * Addons
         * ------------------------------------------------------------
         */

        autoCollectAddon =
                new AutoCollectAddon(
                        this,
                        nbt,
                        lore,
                        validator,
                        feedback
                );

        autoCollectAddon.enable();

        containerAddon =
                new ContainerAddon(
                        this,
                        nbt,
                        lore,
                        validator,
                        feedback
                );

        containerAddon.enable();

        /*
         * ------------------------------------------------------------
         * Hopper protection only
         * ------------------------------------------------------------
         *
         * 自動搬出入は廃止。
         * MS本体移動禁止のみ維持。
         */

        hopperAddon =
                new HopperAddon(
                        this,
                        nbt
                );

        hopperAddon.enable();

        /*
         * ------------------------------------------------------------
         * Redstone container logistics
         * ------------------------------------------------------------
         */

        redstoneContainerAddon =
                new RedstoneContainerAddon(
                        this,
                        storageBridge,
                        nbt,
                        validator
                );

        redstoneContainerAddon.enable();

        getLogger().info("MStorage enabled.");
    }

    @Override
    public void onDisable() {

        /*
         * ------------------------------------------------------------
         * Redstone container addon
         * ------------------------------------------------------------
         */

        if (redstoneContainerAddon != null) {

            redstoneContainerAddon.disable();
            redstoneContainerAddon = null;
        }

        /*
         * ------------------------------------------------------------
         * Lore queue stop
         * ------------------------------------------------------------
         */

        if (loreUpdateQueue != null) {

            loreUpdateQueue.stop();
            loreUpdateQueue = null;
        }

        /*
         * ------------------------------------------------------------
         * API unregister
         * ------------------------------------------------------------
         */

        if (storageBridge != null) {

            getServer()
                    .getServicesManager()
                    .unregister(
                            StorageBridge.class,
                            storageBridge
                    );

            storageBridge = null;
        }

        /*
         * ------------------------------------------------------------
         * Addons disable
         * ------------------------------------------------------------
         */

        if (autoCollectAddon != null) {

            autoCollectAddon.disable();
            autoCollectAddon = null;
        }

        if (containerAddon != null) {

            containerAddon.disable();
            containerAddon = null;
        }

        if (hopperAddon != null) {

            hopperAddon.disable();
            hopperAddon = null;
        }

        getLogger().info("MStorage disabled.");
    }

    public StorageBridge getStorageBridge() {
        return storageBridge;
    }

    public StorageLoreUpdateQueue getLoreUpdateQueue() {
        return loreUpdateQueue;
    }
}