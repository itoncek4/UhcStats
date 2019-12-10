package com.gmail.mezymc.stats.listeners;

import com.gmail.mezymc.stats.*;
import com.gmail.val59000mc.events.UhcWinEvent;
import com.gmail.val59000mc.exceptions.UhcPlayerNotOnlineException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class UhcStatListener implements Listener{

    private StatsManager statsManager;
    private GameMode gameMode;

    public UhcStatListener(StatsManager statsManager){
        this.statsManager = statsManager;
        gameMode = statsManager.getServerGameMode();
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();

        StatsPlayer statsPlayer = statsManager.getStatsPlayer(player);
        statsPlayer.addOneToStats(gameMode, StatType.DEATH);

        if (player.getKiller() != null) {
            StatsPlayer statsKiller = statsManager.getStatsPlayer(player.getKiller());
            statsKiller.addOneToStats(gameMode, StatType.KILL);
        }
    }

    @EventHandler
    public void onGameWin(UhcWinEvent e){
        e.getWinners().forEach(uhcPlayer -> {
            StatsPlayer statsPlayer = statsManager.getStatsPlayer(uhcPlayer.getUuid(), uhcPlayer.getName());
            if (statsPlayer != null){
                statsPlayer.addOneToStats(gameMode, StatType.WIN);
            }else{
                Bukkit.getLogger().warning("[UhcStats] Failed to add win to stats for " + uhcPlayer.getName());
            }
        });

        // Push all stats
        Bukkit.getScheduler().runTaskAsynchronously(UhcStats.getPlugin(), () -> statsManager.pushAllStats());
    }

}