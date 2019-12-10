package com.gmail.mezymc.stats.listeners;

import com.gmail.mezymc.stats.StatsManager;
import com.gmail.mezymc.stats.UhcStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ConnectionListener implements Listener{

    private StatsManager statsManager;

    public ConnectionListener(StatsManager statsManager){
        this.statsManager = statsManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){
        final Player player = e.getPlayer();
        Bukkit.getScheduler().runTaskAsynchronously(UhcStats.getPlugin(), () -> {
            statsManager.getStatsPlayer(player, true, true);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){
        statsManager.playerLeavesTheGame(e.getPlayer());
    }

}