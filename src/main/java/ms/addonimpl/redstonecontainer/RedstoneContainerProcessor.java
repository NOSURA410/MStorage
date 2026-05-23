package ms.addonimpl.redstonecontainer;

import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class RedstoneContainerProcessor {

    private final ContainerChainScanner scanner;
    private final ContainerStorageTransferService transferService;

    public RedstoneContainerProcessor(
            ContainerChainScanner scanner,
            ContainerStorageTransferService transferService
    ) {
        this.scanner = scanner;
        this.transferService = transferService;
    }

    public long process(Block sourceBlock) {
        if (sourceBlock == null) {
            return 0L;
        }

        if (!sourceBlock.getChunk().isLoaded()) {
            return 0L;
        }

        if (!scanner.isContainerBlock(sourceBlock)) {
            return 0L;
        }

        Inventory sourceInventory =
                scanner.getInventory(sourceBlock);

        if (sourceInventory == null) {
            return 0L;
        }

        if (sourceInventory.getSize()
                > RedstoneContainerSettings.MAX_SOURCE_SLOTS) {
            return 0L;
        }

        List<Inventory> networkInventories =
                scanner.scanConnectedInventories(sourceBlock);

        if (networkInventories.isEmpty()) {
            return 0L;
        }

        return transferService.transferAll(
                sourceInventory,
                networkInventories
        );
    }
}