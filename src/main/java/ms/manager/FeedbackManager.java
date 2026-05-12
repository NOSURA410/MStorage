package ms.manager;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class FeedbackManager {

    public void success(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);
    }

    public void fail(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
    }

    public void actionBar(Player player, String message) {
        player.sendActionBar(message);
    }
}