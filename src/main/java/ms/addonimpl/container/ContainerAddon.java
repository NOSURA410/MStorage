package ms.addonimpl.container;

import ms.core.StorageLore;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.manager.FeedbackManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ContainerAddon {

    private final JavaPlugin plugin;
    private final StorageNBT nbt;
    private final StorageLore lore;
    private final StorageValidator validator;
    private final FeedbackManager feedback;

    public ContainerAddon(
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
        ContainerSelector selector = new ContainerSelector();
        ContainerProcessor processor = new ContainerProcessor(nbt, lore, validator);

        plugin.getServer().getPluginManager().registerEvents(
                new ContainerModeListener(plugin, nbt, lore, feedback),
                plugin
        );

        plugin.getServer().getPluginManager().registerEvents(
                new ContainerListener(nbt, feedback, selector, processor),
                plugin
        );

        plugin.getLogger().info("[MS-Addon] ContainerAddon enabled.");
    }

    public void disable() {
        plugin.getLogger().info("[MS-Addon] ContainerAddon disabled.");
    }
}