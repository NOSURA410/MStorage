package ms.addonimpl.autocollect;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class AutoCollectTask {

    private final AutoCollectProcessor processor;

    private BukkitTask task;
    private int cursor = 0;

    public AutoCollectTask(AutoCollectProcessor processor) {
        this.processor = processor;
    }

    public void start() {
        if (task != null) {
            return;
        }

        task = Bukkit.getScheduler().runTaskTimer(
                Bukkit.getPluginManager().getPlugin("Material_Storage"),
                this::tick,
                AutoCollectSettings.INTERVAL_TICKS,
                AutoCollectSettings.INTERVAL_TICKS
        );
    }

    public void stop() {
        if (task == null) {
            return;
        }

        task.cancel();
        task = null;
        cursor = 0;
    }

    private void tick() {
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

        if (players.isEmpty()) {
            cursor = 0;
            return;
        }

        int processed = 0;

        while (processed < AutoCollectSettings.MAX_PLAYERS_PER_TICK && !players.isEmpty()) {
            if (cursor >= players.size()) {
                cursor = 0;
            }

            Player player = players.get(cursor);
            cursor++;

            processor.process(player);
            processed++;

            if (cursor >= players.size()) {
                cursor = 0;
                break;
            }
        }
    }
}