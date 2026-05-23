package ms.addonimpl.redstonecontainer;

public final class RedstoneContainerSettings {

    /*
     * 搬出元上限
     *
     * 1LC = 54 slots
     */
    public static final int MAX_SOURCE_SLOTS = 54;

    /*
     * 搬入先探索上限
     *
     * 40LC = 54 * 40 = 2160 slots
     */
    public static final int MAX_NETWORK_SLOTS = 54 * 41;

    /*
     * Queue / Tick制御
     */
    public static final int MAX_QUEUE_SIZE = 256;
    public static final int MAX_TASKS_PER_TICK = 4;

    /*
     * レッドストーン信号後、搬送開始までの遅延
     */
    public static final long SIGNAL_DELAY_TICKS = 2L;

    /*
     * クールダウン
     */
    public static final long SUCCESS_COOLDOWN_TICKS = 40L;
    public static final long FAILED_COOLDOWN_TICKS = 10L;

    /*
     * 搬送中ロックの最大保持時間
     */
    public static final long LOCK_TIMEOUT_TICKS = 100L;

    /*
     * MS → MS 搬送時、搬出元MSから移動する割合
     *
     * 2 = 半分
     */
    public static final long STORAGE_TRANSFER_DIVISOR = 2L;

    private RedstoneContainerSettings() {
    }
}