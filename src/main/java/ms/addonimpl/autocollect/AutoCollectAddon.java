package ms.addonimpl.autocollect;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.manager.FeedbackManager;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoCollectAddon {

    private final JavaPlugin plugin;
    private final StorageNBT nbt;
    private final StorageLore lore;
    private final StorageValidator validator;
    private final FeedbackManager feedback;

    private AutoCollectTask task;

    public AutoCollectAddon(
            JavaPlugin plugin,
            StorageNBT nbt,
            StorageLore lore,
            StorageValidator validator,
            FeedbackManager feedback
    ) {
        this.plugin = plugin;
        this.nbt = nbt;
        this.lore = lore;
        this.validator = validator;
        this.feedback = feedback;
    }

    public void enable() {

        AutoCollectLore autoCollectLore = new AutoCollectLore(plugin, nbt);
        lore.setAutoCollectLore(autoCollectLore);

        AutoCollectSelector selector = new AutoCollectSelector(plugin, nbt, validator);

        AutoCollectProcessor processor = new AutoCollectProcessor(
                nbt,
                lore,
                validator,
                selector,
                autoCollectLore
        );

        this.task = new AutoCollectTask(processor);

        plugin.getServer().getPluginManager().registerEvents(
                new AutoCollectToggleListener(plugin, nbt, lore, feedback, autoCollectLore),
                plugin
        );

        plugin.getServer().getPluginManager().registerEvents(
                new AutoCollectPickupListener(
                        nbt,
                        lore,
                        validator,
                        selector,
                        autoCollectLore
                ),
                plugin
        );

        task.start();

        plugin.getLogger().info("[MS-Addon] AutoCollectAddon enabled.");
    }

    public void disable() {
        if (task != null) {
            task.stop();
            task = null;
        }

        plugin.getLogger().info("[MS-Addon] AutoCollectAddon disabled.");
    }
}