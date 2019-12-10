package com.gmail.mezymc.stats.listeners;

import com.gmail.mezymc.stats.StatsManager;
import com.gmail.mezymc.stats.StatsPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class StatCommandListener implements Listener{

    private String command;

    public StatCommandListener(String command){
        this.command = command;
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e){
        String commandString = e.getMessage().toLowerCase();

        if (commandString.startsWith(command + " ")){
            commandString = commandString.replace(command + " ", "");
            e.setCancelled(true);
        }else if (commandString.startsWith(command)){
            commandString = commandString.replace(command, "");
            e.setCancelled(true);
        }else{
            return;
        }

        String[] args = commandString.split(" ");
        if (commandString.equals("")){
            args = new String[0];
        }

        StatsManager statsManager = StatsManager.getStatsManager();
        Player player = e.getPlayer();
        StatsPlayer statsPlayer = statsManager.getStatsPlayer(player);

        if (statsPlayer == null){
            player.sendMessage("Failed to load stats!");
            return;
        }

        player.openInventory(statsManager.loadStatsUI(statsPlayer));
    }

}