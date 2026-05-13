package ms.addonimpl.hopper;

public final class HopperSettings {

    private HopperSettings() {
    }

    // 1回のホッパー処理で最大何個までMSへ収納するか
    public static final int MAX_TRANSFER_PER_OPERATION = 64;

    // 同一ホッパーの二重処理防止tick
    public static final long HOPPER_COOLDOWN_TICKS = 1L;

    // 搬入処理を有効化
    public static final boolean ENABLE_IMPORT_TO_STORAGE = true;

    // 搬出処理を有効化
    public static final boolean ENABLE_EXPORT_FROM_STORAGE = true;

    // MSストレージ本体のホッパー移動禁止
    public static final boolean BLOCK_STORAGE_ITEM_MOVE = true;
}