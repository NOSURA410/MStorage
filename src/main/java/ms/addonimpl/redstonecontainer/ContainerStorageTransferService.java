package ms.addonimpl.redstonecontainer;

import ms.api.StorageBridge;
import ms.core.StorageNBT;
import ms.core.StorageValidator;
import ms.model.StorageData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.Lidded;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContainerStorageTransferService {

    private final JavaPlugin plugin;
    private final StorageBridge bridge;
    private final StorageNBT nbt;
    private final StorageValidator validator;

    public ContainerStorageTransferService(
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

    public long transferAll(
            Inventory sourceInventory,
            List<Inventory> networkInventories
    ) {
        if (sourceInventory == null
                || networkInventories == null
                || networkInventories.isEmpty()) {
            return 0L;
        }

        Set<String> sourceBlockKeys =
                getInventoryBlockKeys(sourceInventory);

        Map<String, List<StorageTarget>> targetMap =
                buildStorageTargetMap(
                        sourceInventory,
                        sourceBlockKeys,
                        networkInventories
                );

        if (targetMap.isEmpty()) {
            return 0L;
        }

        long movedTotal = 0L;

        movedTotal += transferStorageItems(
                sourceInventory,
                sourceBlockKeys,
                targetMap
        );

        movedTotal += transferNormalItems(
                sourceInventory,
                sourceBlockKeys,
                targetMap
        );

        return movedTotal;
    }

    private long transferNormalItems(
            Inventory sourceInventory,
            Set<String> sourceBlockKeys,
            Map<String, List<StorageTarget>> targetMap
    ) {
        long movedTotal = 0L;

        for (int slot = 0; slot < sourceInventory.getSize(); slot++) {

            ItemStack sourceItem =
                    sourceInventory.getItem(slot);

            if (sourceItem == null
                    || sourceItem.getType() == Material.AIR
                    || sourceItem.getAmount() <= 0) {
                continue;
            }

            if (nbt.isStorage(sourceItem)) {
                continue;
            }

            List<StorageTarget> targets =
                    targetMap.get(
                            createItemKey(sourceItem)
                    );

            if (targets == null || targets.isEmpty()) {
                continue;
            }

            int beforeAmount =
                    sourceItem.getAmount();

            for (StorageTarget target : targets) {

                if (sourceItem.getAmount() <= 0) {
                    break;
                }

                if (target.isSamePhysicalInventory(sourceBlockKeys)) {
                    continue;
                }

                ItemStack destinationStorage =
                        target.currentItem();

                if (!nbt.isStorage(destinationStorage)) {
                    continue;
                }

                if (!bridge.canAccept(
                        destinationStorage,
                        sourceItem
                )) {
                    continue;
                }

                long moved =
                        bridge.storeItemStackIntoStorage(
                                target.inventory(),
                                destinationStorage,
                                sourceItem
                        );

                if (moved > 0L) {
                    playTransferEffect(target.inventory());
                }
            }

            int moved =
                    beforeAmount
                            - Math.max(
                            0,
                            sourceItem.getAmount()
                    );

            if (moved <= 0) {
                continue;
            }

            if (sourceItem.getAmount() <= 0) {
                sourceInventory.setItem(slot, null);
            } else {
                sourceInventory.setItem(slot, sourceItem);
            }

            movedTotal += moved;
        }

        return movedTotal;
    }

    private long transferStorageItems(
            Inventory sourceInventory,
            Set<String> sourceBlockKeys,
            Map<String, List<StorageTarget>> targetMap
    ) {
        long movedTotal = 0L;

        for (int sourceSlot = 0;
             sourceSlot < sourceInventory.getSize();
             sourceSlot++) {

            ItemStack sourceStorage =
                    sourceInventory.getItem(sourceSlot);

            if (!nbt.isStorage(sourceStorage)) {
                continue;
            }

            StorageData sourceData =
                    nbt.read(sourceStorage);

            if (sourceData == null
                    || sourceData.getStoredItem() == null
                    || sourceData.getAmount() <= 1L) {
                continue;
            }

            List<StorageTarget> targets =
                    targetMap.get(
                            createItemKey(
                                    sourceData.getStoredItem()
                            )
                    );

            if (targets == null || targets.isEmpty()) {
                continue;
            }

            long requested =
                    sourceData.getAmount()
                            / RedstoneContainerSettings.STORAGE_TRANSFER_DIVISOR;

            if (requested <= 0L) {
                continue;
            }

            long remaining = requested;

            for (StorageTarget target : targets) {

                if (remaining <= 0L) {
                    break;
                }

                if (target.isSamePhysicalInventory(sourceBlockKeys)) {
                    continue;
                }

                ItemStack destinationStorage =
                        target.currentItem();

                if (!nbt.isStorage(destinationStorage)) {
                    continue;
                }

                if (destinationStorage == sourceStorage) {
                    continue;
                }

                long moved =
                        bridge.moveStorageToStorage(
                                sourceInventory,
                                sourceStorage,
                                target.inventory(),
                                destinationStorage,
                                remaining
                        );

                if (moved <= 0L) {
                    continue;
                }

                playTransferEffect(target.inventory());

                remaining -= moved;
                movedTotal += moved;
            }
        }

        return movedTotal;
    }

    private Map<String, List<StorageTarget>> buildStorageTargetMap(
            Inventory sourceInventory,
            Set<String> sourceBlockKeys,
            List<Inventory> networkInventories
    ) {
        Map<String, List<StorageTarget>> map =
                new HashMap<>();

        for (Inventory inventory : networkInventories) {

            if (inventory == null) {
                continue;
            }

            Set<String> inventoryBlockKeys =
                    getInventoryBlockKeys(inventory);

            if (sharesAnyBlock(
                    sourceBlockKeys,
                    inventoryBlockKeys
            )) {
                continue;
            }

            for (int slot = 0;
                 slot < inventory.getSize();
                 slot++) {

                ItemStack item =
                        inventory.getItem(slot);

                if (!nbt.isStorage(item)) {
                    continue;
                }

                StorageData data =
                        nbt.read(item);

                if (data == null
                        || data.getStoredItem() == null) {
                    continue;
                }

                if (bridge.getRemainingCapacity(item) <= 0L) {
                    continue;
                }

                String key =
                        createItemKey(
                                data.getStoredItem()
                        );

                map.computeIfAbsent(
                        key,
                        ignored -> new ArrayList<>()
                ).add(
                        new StorageTarget(
                                inventory,
                                slot,
                                inventoryBlockKeys
                        )
                );
            }
        }

        return map;
    }

    private void playTransferEffect(Inventory inventory) {

        if (inventory == null
                || inventory.getHolder() == null) {
            return;
        }

        InventoryHolder holder =
                inventory.getHolder();

        if (holder instanceof BlockState state) {
            playLidAnimation(state);
            return;
        }

        if (holder instanceof DoubleChest doubleChest) {

            InventoryHolder left =
                    doubleChest.getLeftSide();

            if (left instanceof BlockState leftState) {
                playLidAnimation(leftState);
            }
        }
    }

    private void playLidAnimation(BlockState state) {

        if (!(state instanceof Lidded lidded)) {
            return;
        }

        lidded.open();

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        lidded::close,
                        5L
                );
    }

    private String createItemKey(ItemStack item) {

        if (item == null
                || item.getType() == Material.AIR) {
            return "AIR";
        }

        ItemStack clean = item.clone();
        clean.setAmount(1);

        return clean.getType().name()
                + "#"
                + clean.hasItemMeta()
                + "#"
                + clean.getItemMeta();
    }

    private Set<String> getInventoryBlockKeys(
            Inventory inventory
    ) {
        Set<String> keys =
                new HashSet<>();

        if (inventory == null
                || inventory.getHolder() == null) {
            return keys;
        }

        InventoryHolder holder =
                inventory.getHolder();

        if (holder instanceof BlockState state) {

            keys.add(
                    toBlockKey(state)
            );

            return keys;
        }

        if (holder instanceof DoubleChest doubleChest) {

            InventoryHolder left =
                    doubleChest.getLeftSide();

            InventoryHolder right =
                    doubleChest.getRightSide();

            if (left instanceof BlockState leftState) {
                keys.add(
                        toBlockKey(leftState)
                );
            }

            if (right instanceof BlockState rightState) {
                keys.add(
                        toBlockKey(rightState)
                );
            }
        }

        return keys;
    }

    private boolean sharesAnyBlock(
            Set<String> a,
            Set<String> b
    ) {
        if (a == null
                || b == null
                || a.isEmpty()
                || b.isEmpty()) {
            return false;
        }

        for (String key : a) {
            if (b.contains(key)) {
                return true;
            }
        }

        return false;
    }

    private String toBlockKey(BlockState state) {

        return state.getWorld().getName()
                + ":"
                + state.getX()
                + ":"
                + state.getY()
                + ":"
                + state.getZ();
    }

    private record StorageTarget(
            Inventory inventory,
            int slot,
            Set<String> inventoryBlockKeys
    ) {

        private ItemStack currentItem() {
            return inventory.getItem(slot);
        }

        private boolean isSamePhysicalInventory(
                Set<String> sourceBlockKeys
        ) {
            if (sourceBlockKeys == null
                    || sourceBlockKeys.isEmpty()) {
                return false;
            }

            for (String key : sourceBlockKeys) {
                if (inventoryBlockKeys.contains(key)) {
                    return true;
                }
            }

            return false;
        }
    }
}