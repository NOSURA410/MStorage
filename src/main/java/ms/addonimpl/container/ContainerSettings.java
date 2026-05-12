package ms.addonimpl.container;

public final class ContainerSettings {

    private ContainerSettings() {
    }

    // PDCキー
    public static final String KEY_MODE = "mst_mode";

    // モード値
    public static final byte MODE_HAND = 0;
    public static final byte MODE_CONTAINER = 1;

    // 1回のコンテナ操作で処理する最大スロット数
    public static final int MAX_CONTAINER_SLOTS_PER_OPERATION = 54;

    // 1回の補充で最大何スタックまで入れるか
    public static final int MAX_FILL_STACKS_PER_OPERATION = 54;
}