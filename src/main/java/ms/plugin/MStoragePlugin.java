package ms.plugin;

import ms.addonimpl.autocollect.AutoCollectAddon;
import ms.addonimpl.container.ContainerAddon;
import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.listener.AmmoProtectionListener;
import ms.listener.ChestGuideLoreListener;
import ms.listener.CoreActionListener;
import ms.listener.CoreProtectionListener;
import ms.listener.CraftListener;
import ms.listener.StorageReleaseListener;
import ms.manager.FeedbackManager;
import ms.service.StorageService;
import org.bukkit.plugin.java.JavaPlugin;

public class MStoragePlugin extends JavaPlugin {

    private AutoCollectAddon autoCollectAddon;
    private ContainerAddon containerAddon;

    @Override
    public void onEnable() {

        StorageNBT nbt = new StorageNBT(this);
        StorageLore lore = new StorageLore();
        StorageValidator validator = new StorageValidator(nbt);
        StorageService service = new StorageService(nbt, lore, validator);
        FeedbackManager feedback = new FeedbackManager();

        getServer().getPluginManager().registerEvents(
                new CoreActionListener(nbt, service, feedback), this
        );

        getServer().getPluginManager().registerEvents(
                new CoreProtectionListener(this, nbt), this
        );

        getServer().getPluginManager().registerEvents(
                new CraftListener(nbt, lore, validator), this
        );

        getServer().getPluginManager().registerEvents(
                new ChestGuideLoreListener(nbt), this
        );

        getServer().getPluginManager().registerEvents(
                new StorageReleaseListener(nbt, feedback), this
        );

        getServer().getPluginManager().registerEvents(
                new AmmoProtectionListener(nbt), this
        );

        autoCollectAddon = new AutoCollectAddon(
                this,
                nbt,
                lore,
                validator,
                feedback
        );
        autoCollectAddon.enable();

        containerAddon = new ContainerAddon(
                this,
                nbt,
                lore,
                validator,
                feedback
        );
        containerAddon.enable();
    }

    @Override
    public void onDisable() {
        if (autoCollectAddon != null) {
            autoCollectAddon.disable();
            autoCollectAddon = null;
        }

        if (containerAddon != null) {
            containerAddon.disable();
            containerAddon = null;
        }
    }
}