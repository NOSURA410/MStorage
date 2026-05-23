package ms.addonimpl.redstonecontainer;

import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class ContainerChainScanner {

    private static final BlockFace[] FACES = {
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.UP,
            BlockFace.DOWN
    };

    public List<Inventory> scanConnectedInventories(Block startBlock) {

        List<Inventory> result = new ArrayList<>();

        if (!isContainerBlock(startBlock)) {
            return result;
        }

        Queue<Block> queue = new ArrayDeque<>();
        Set<String> visitedBlocks = new HashSet<>();
        Set<String> visitedInventoryKeys = new HashSet<>();

        queue.add(startBlock);

        int totalSlots = 0;

        while (!queue.isEmpty()) {

            Block current = queue.poll();

            if (current == null) {
                continue;
            }

            if (!current.getChunk().isLoaded()) {
                continue;
            }

            if (!isContainerBlock(current)) {
                continue;
            }

            if (!visitedBlocks.add(toBlockKey(current))) {
                continue;
            }

            Inventory inventory = getInventory(current);

            if (inventory == null) {
                continue;
            }

            String inventoryKey = getInventoryKey(inventory);

            if (visitedInventoryKeys.add(inventoryKey)) {

                int nextTotal = totalSlots + inventory.getSize();

                if (nextTotal > RedstoneContainerSettings.MAX_NETWORK_SLOTS) {
                    continue;
                }

                totalSlots = nextTotal;
                result.add(inventory);
            }

            for (Block base : getInventoryBlocks(inventory)) {

                for (BlockFace face : FACES) {

                    Block next = base.getRelative(face);

                    if (next == null) {
                        continue;
                    }

                    if (!next.getChunk().isLoaded()) {
                        continue;
                    }

                    if (!isContainerBlock(next)) {
                        continue;
                    }

                    if (visitedBlocks.contains(toBlockKey(next))) {
                        continue;
                    }

                    queue.add(next);
                }
            }
        }

        return result;
    }

    public Inventory getInventory(Block block) {

        if (block == null) {
            return null;
        }

        BlockState state = block.getState();

        if (!(state instanceof InventoryHolder holder)) {
            return null;
        }

        return holder.getInventory();
    }

    public boolean isContainerBlock(Block block) {

        if (block == null) {
            return false;
        }

        Material type = block.getType();

        if (type == Material.BARREL) {
            return true;
        }

        if (type == Material.CHEST
                || type == Material.TRAPPED_CHEST) {
            return true;
        }

        BlockState state = block.getState();

        return state instanceof Barrel
                || state instanceof Chest;
    }

    public Block getInventoryBlock(Inventory inventory) {

        List<Block> blocks = getInventoryBlocks(inventory);

        if (blocks.isEmpty()) {
            return null;
        }

        return blocks.get(0);
    }

    public List<Block> getInventoryBlocks(Inventory inventory) {

        List<Block> blocks = new ArrayList<>();

        if (inventory == null || inventory.getHolder() == null) {
            return blocks;
        }

        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof BlockState state) {
            blocks.add(state.getBlock());
            return blocks;
        }

        if (holder instanceof DoubleChest doubleChest) {

            InventoryHolder left = doubleChest.getLeftSide();
            InventoryHolder right = doubleChest.getRightSide();

            if (left instanceof BlockState leftState) {
                blocks.add(leftState.getBlock());
            }

            if (right instanceof BlockState rightState) {
                blocks.add(rightState.getBlock());
            }
        }

        return blocks;
    }

    public String getInventoryKey(Inventory inventory) {

        if (inventory == null || inventory.getHolder() == null) {
            return "null";
        }

        InventoryHolder holder = inventory.getHolder();

        if (holder instanceof BlockState state) {
            return toBlockKey(state.getBlock());
        }

        if (holder instanceof DoubleChest doubleChest) {

            List<String> keys = new ArrayList<>();

            InventoryHolder left = doubleChest.getLeftSide();
            InventoryHolder right = doubleChest.getRightSide();

            if (left instanceof BlockState leftState) {
                keys.add(toBlockKey(leftState.getBlock()));
            }

            if (right instanceof BlockState rightState) {
                keys.add(toBlockKey(rightState.getBlock()));
            }

            keys.sort(String::compareTo);

            return String.join("|", keys);
        }

        return String.valueOf(System.identityHashCode(inventory));
    }

    public String toBlockKey(Block block) {

        if (block == null) {
            return "null";
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