package com.gmail.mezymc.stats.listeners;

import com.gmail.mezymc.stats.StatType;
import com.gmail.mezymc.stats.UhcStats;
import com.gmail.val59000mc.events.UhcWinEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class UhcStatListener implements Listener{

    private UhcStats uhcStats;

    public UhcStatListener(UhcStats uhcStats){
        this.uhcStats = uhcStats;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        final Player player = e.getEntity();

        Bukkit.getScheduler().runTaskAsynchronously(uhcStats, new Runnable() {
            @Override
            public void run() {
                uhcStats.addOneToPlayerStats(player.getUniqueId(), player.getName(), StatType.DEATH);
            }
        });

        if (e.getEntity().getKiller() != null){
            Bukkit.getScheduler().runTaskAsynchronously(uhcStats, new Runnable() {
                @Override
                public void run() {
                    uhcStats.addOneToPlayerStats(player.getKiller().getUniqueId(), player.getKiller().getName(), StatType.KILL);
                }
            });
        }
    }

    @EventHandler
    public void onGameWin(final UhcWinEvent e){
        Bukkit.getScheduler().runTaskAsynchronously(uhcStats, new Runnable() {
            @Override
            public void run() {
                e.getWinners().forEach(uhcPlayer -> uhcStats.addOneToPlayerStats(uhcPlayer.getUuid(), uhcPlayer.getName(), StatType.WIN));
            }
        });
    }

}