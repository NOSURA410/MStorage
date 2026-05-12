package ms.model;

public enum StorageMode {

    HAND((byte) 0),
    CONTAINER((byte) 1);

    private final byte value;

    StorageMode(byte value) {
        this.value = value;
    }

    public byte toByte() {
        return value;
    }

    public static StorageMode fromByte(byte b) {
        if (b == 1) {
            return CONTAINER;
        }
        return HAND;
    }

    // 旧データ互換（String → Mode）
    public static StorageMode fromString(String s) {
        if (s == null || s.isBlank()) {
            return HAND;
        }

        try {
            return StorageMode.valueOf(s.toUpperCase());
        } catch (Exception e) {
            return HAND;
        }
    }
}