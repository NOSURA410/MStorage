package ms.addonimpl.hopper;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import org.bukkit.plugin.java.JavaPlugin;

public class HopperAddon {

    private final JavaPlugin plugin;
    private final StorageNBT nbt;
    private final StorageLore lore;
    private final StorageValidator validator;

    public HopperAddon(
            JavaPlugin plugin,
            StorageNBT nbt,
            StorageLore lore,
            StorageValidator validator
    ) {
        this.plugin = plugin;
        this.nbt = nbt;
        this.lore = lore;
        this.validator = validator;
    }

    public void enable() {
        HopperSelector selector = new HopperSelector(nbt, validator);
        HopperTransferUtil transferUtil = new HopperTransferUtil(nbt, validator);

        HopperImportProcessor importProcessor = new HopperImportProcessor(
                nbt,
                lore,
                validator,
                transferUtil
        );

        HopperExportProcessor exportProcessor = new HopperExportProcessor(
                nbt,
                lore,
                validator,
                selector,
                transferUtil,
                importProcessor
        );

        plugin.getServer().getPluginManager().registerEvents(
                new HopperStorageItemGuardListener(nbt),
                plugin
        );

        plugin.getServer().getPluginManager().registerEvents(
                new HopperMoveListener(
                        plugin,
                        nbt,
                        selector,
                        importProcessor,
                        exportProcessor
                ),
                plugin
        );

        plugin.getLogger().info("[MS-Addon] HopperAddon enabled.");
    }

    public void disable() {
        plugin.getLogger().info("[MS-Addon] HopperAddon disabled.");
    }
}