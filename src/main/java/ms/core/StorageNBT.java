package ms.core;

import ms.model.StorageData;
import ms.model.StorageMode;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;

public class StorageNBT {

    public static final int CURRENT_VERSION = 1;

    private static final byte MODE_HAND_BYTE = 0;
    private static final byte MODE_CONTAINER_BYTE = 1;

    private final NamespacedKey keyStorage;
    private final NamespacedKey keyItem;
    private final NamespacedKey keyAmount;
    private final NamespacedKey keyMode;
    private final NamespacedKey keyOwner;
    private final NamespacedKey keyVersion;

    // ★ 追加
    private final NamespacedKey keyUid;

    public StorageNBT(JavaPlugin plugin) {
        this.keyStorage = new NamespacedKey(plugin, "mst_storage");
        this.keyItem = new NamespacedKey(plugin, "mst_item");
        this.keyAmount = new NamespacedKey(plugin, "mst_amount");
        this.keyMode = new NamespacedKey(plugin, "mst_mode");
        this.keyOwner = new NamespacedKey(plugin, "mst_owner");
        this.keyVersion = new NamespacedKey(plugin, "mst_version");

        // ★ 追加
        this.keyUid = new NamespacedKey(plugin, "mst_uid");
    }

    public boolean isStorage(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(keyStorage, PersistentDataType.BYTE)) {
            return false;
        }

        Byte flag = pdc.get(keyStorage, PersistentDataType.BYTE);
        return flag != null && flag == (byte) 1;
    }

    public ItemStack createStorage(ItemStack baseItem, UUID owner) {
        if (baseItem == null || owner == null) {
            return null;
        }

        ItemStack stored = baseItem.clone();
        stored.setAmount(1);

        ItemStack storage = stored.clone();
        storage.setAmount(1);

        StorageData data = new StorageData(
                stored,
                0L,
                StorageMode.HAND,
                owner,
                CURRENT_VERSION
        );

        if (!write(storage, data)) {
            return null;
        }

        // ★ uid付与
        ensureUid(storage);

        return storage;
    }

    public StorageData read(ItemStack item) {
        if (!isStorage(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        String itemBase64 = pdc.get(keyItem, PersistentDataType.STRING);
        Long amount = pdc.get(keyAmount, PersistentDataType.LONG);
        String ownerText = pdc.get(keyOwner, PersistentDataType.STRING);
        Integer version = pdc.get(keyVersion, PersistentDataType.INTEGER);

        if (itemBase64 == null || amount == null || ownerText == null || version == null) {
            return null;
        }

        if (amount < 0) {
            return null;
        }

        ItemStack storedItem = deserializeItem(itemBase64);
        if (storedItem == null) {
            return null;
        }

        StorageMode mode = readModeSafely(pdc);

        UUID owner;

        try {
            owner = UUID.fromString(ownerText);
        } catch (Exception e) {
            return null;
        }

        // ★ uid欠損修復
        ensureUid(item);

        meta = item.getItemMeta();
        if (meta != null) {
            item.setItemMeta(meta);
        }

        return new StorageData(
                storedItem,
                amount,
                mode,
                owner,
                version
        );
    }

    public boolean write(ItemStack item, StorageData data) {
        if (item == null
                || data == null
                || data.getStoredItem() == null
                || data.getOwner() == null) {

            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        String serialized = serializeItem(data.getStoredItem());

        if (serialized == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        pdc.set(keyStorage, PersistentDataType.BYTE, (byte) 1);
        pdc.set(keyItem, PersistentDataType.STRING, serialized);
        pdc.set(keyAmount, PersistentDataType.LONG, data.getAmount());

        byte modeByte = toModeByte(data.getMode());

        pdc.remove(keyMode);
        pdc.set(keyMode, PersistentDataType.BYTE, modeByte);

        pdc.set(
                keyOwner,
                PersistentDataType.STRING,
                data.getOwner().toString()
        );

        pdc.set(
                keyVersion,
                PersistentDataType.INTEGER,
                data.getVersion()
        );

        // ★ uid欠損修復
        if (!pdc.has(keyUid, PersistentDataType.STRING)) {
            pdc.set(
                    keyUid,
                    PersistentDataType.STRING,
                    UUID.randomUUID().toString()
            );
        }

        item.setItemMeta(meta);
        return true;
    }

    // ★ 追加
    public void ensureUid(ItemStack item) {

        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        PersistentDataContainer pdc =
                meta.getPersistentDataContainer();

        if (!pdc.has(keyUid, PersistentDataType.STRING)) {

            pdc.set(
                    keyUid,
                    PersistentDataType.STRING,
                    UUID.randomUUID().toString()
            );

            item.setItemMeta(meta);
        }
    }

    private StorageMode readModeSafely(PersistentDataContainer pdc) {

        if (pdc == null) {
            return StorageMode.HAND;
        }

        if (pdc.has(keyMode, PersistentDataType.BYTE)) {

            Byte value = pdc.get(
                    keyMode,
                    PersistentDataType.BYTE
            );

            if (value == null) {
                resetModeToHand(pdc);
                return StorageMode.HAND;
            }

            if (value == MODE_CONTAINER_BYTE) {
                return storageModeFromName("CONTAINER");
            }

            if (value == MODE_HAND_BYTE) {
                return StorageMode.HAND;
            }

            resetModeToHand(pdc);
            return StorageMode.HAND;
        }

        if (pdc.has(keyMode, PersistentDataType.STRING)) {

            String modeName =
                    pdc.get(
                            keyMode,
                            PersistentDataType.STRING
                    );

            StorageMode mode =
                    parseOldStringMode(modeName);

            pdc.remove(keyMode);

            pdc.set(
                    keyMode,
                    PersistentDataType.BYTE,
                    toModeByte(mode)
            );

            return mode;
        }

        resetModeToHand(pdc);
        return StorageMode.HAND;
    }

    private StorageMode parseOldStringMode(String modeName) {

        if (modeName == null) {
            return StorageMode.HAND;
        }

        if ("1".equals(modeName)) {
            return storageModeFromName("CONTAINER");
        }

        if ("0".equals(modeName)) {
            return StorageMode.HAND;
        }

        return storageModeFromName(modeName);
    }

    private StorageMode storageModeFromName(String modeName) {

        if (modeName == null) {
            return StorageMode.HAND;
        }

        try {
            return StorageMode.valueOf(modeName);
        } catch (Exception e) {
            return StorageMode.HAND;
        }
    }

    private byte toModeByte(StorageMode mode) {

        if (mode == null) {
            return MODE_HAND_BYTE;
        }

        if ("CONTAINER".equals(mode.name())) {
            return MODE_CONTAINER_BYTE;
        }

        return MODE_HAND_BYTE;
    }

    private void resetModeToHand(PersistentDataContainer pdc) {

        if (pdc == null) {
            return;
        }

        pdc.remove(keyMode);

        pdc.set(
                keyMode,
                PersistentDataType.BYTE,
                MODE_HAND_BYTE
        );
    }

    private String serializeItem(ItemStack item) {

        try {

            ByteArrayOutputStream byteOut =
                    new ByteArrayOutputStream();

            BukkitObjectOutputStream out =
                    new BukkitObjectOutputStream(byteOut);

            out.writeObject(item);
            out.close();

            return Base64.getEncoder()
                    .encodeToString(byteOut.toByteArray());

        } catch (Exception e) {
            return null;
        }
    }

    private ItemStack deserializeItem(String base64) {

        try {

            byte[] data =
                    Base64.getDecoder().decode(base64);

            BukkitObjectInputStream in =
                    new BukkitObjectInputStream(
                            new ByteArrayInputStream(data)
                    );

            Object object = in.readObject();

            in.close();

            if (!(object instanceof ItemStack item)) {
                return null;
            }

            item.setAmount(1);

            return item;

        } catch (Exception e) {
            return null;
        }
    }
}