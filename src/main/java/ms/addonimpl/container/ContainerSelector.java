package ms.addonimpl.container;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;

public class ContainerSelector {

    public boolean isValidContainer(Block block) {
        if (block == null) {
            return false;
        }

        Material type = block.getType();

        if (type == Material.CHEST
                || type == Material.TRAPPED_CHEST
                || type == Material.BARREL) {
            return true;
        }

        return isShulkerBox(type);
    }

    public Inventory getInventory(Block block) {
        if (block == null) {
            return null;
        }

        if (!(block.getState() instanceof Container container)) {
            return null;
        }

        return container.getInventory();
    }

    private boolean isShulkerBox(Material material) {
        if (material == null) {
            return false;
        }

        return material.name().endsWith("SHULKER_BOX");
    }
}