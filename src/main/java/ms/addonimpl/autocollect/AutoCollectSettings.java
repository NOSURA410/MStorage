package ms.addonimpl.autocollect;

public final class AutoCollectSettings {

    private AutoCollectSettings() {}

    // 回収範囲（ブロック）
    public static final double RADIUS = 4.0D;

    // 実行間隔（tick）
    public static final long INTERVAL_TICKS = 5L;

    // 1tickで処理する最大プレイヤー数
    public static final int MAX_PLAYERS_PER_TICK = 8;

    // 1プレイヤーあたり最大処理Item数
    public static final int MAX_ITEMS_PER_PLAYER = 16;

    // AutoCollectフラグ（PDCキー）
    public static final String KEY_AUTO = "mst_auto";

    // フラグ値
    public static final byte AUTO_ON = 1;
    public static final byte AUTO_OFF = 0;
}