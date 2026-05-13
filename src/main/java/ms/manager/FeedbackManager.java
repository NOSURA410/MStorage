package ms.manager;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class FeedbackManager {

    public void success(Player player) {
        if (player == null) {
            return;
        }

        player.playSound(
                player.getLocation(),
                Sound.UI_BUTTON_CLICK,
                0.2f,
                1.6f
        );
    }

    public void fail(Player player) {
        if (player == null) {
            return;
        }

        player.playSound(
                player.getLocation(),
                Sound.BLOCK_NOTE_BLOCK_BASS,
                0.2f,
                0.7f
        );
    }

    public void actionBar(Player player, String message) {
        if (player == null || message == null) {
            return;
        }

        player.sendActionBar(message);
    }
}