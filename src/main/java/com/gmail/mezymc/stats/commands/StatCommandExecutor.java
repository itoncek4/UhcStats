package com.gmail.mezymc.stats.commands;

import com.gmail.mezymc.stats.StatType;
import com.gmail.mezymc.stats.UhcStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Map;

public class StatCommandExecutor implements Listener{

    private UhcStats uhcStats;
    private ConfigurationSection section;
    private String command;

    public StatCommandExecutor(UhcStats uhcStats, ConfigurationSection section){
        this.uhcStats = uhcStats;
        this.section = section;
        command = section.getString("command", "stats");
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
        }else {
            return;
        }

        String[] args = commandString.split(" ");
        if (commandString.equals("")){
            args = new String[0];
        }

        processCommand(e.getPlayer(), args);
    }

    private void processCommand(Player player, String[] args) {
        if (args.length == 0){
            Map<StatType, Integer> stats = uhcStats.getCashedStats(player.getUniqueId(), player.getName()).getStats();

            for (String message : section.getStringList("messages-own")){
                player.sendMessage(parsePlaceholders(player, message, stats));
            }
            return;
        }

        if (args.length == 1){
            Player argPlayer = Bukkit.getPlayer(args[0]);

            if (argPlayer == null){
                player.sendMessage(ChatColor.RED + "That player is not online!");
                return;
            }

            Map<StatType, Integer> stats = uhcStats.getCashedStats(argPlayer.getUniqueId(), argPlayer.getName()).getStats();

            // get player stats
            for (String message : section.getStringList("messages-other")){
                player.sendMessage(parsePlaceholders(argPlayer, message, stats));
            }
            return;
        }

        player.sendMessage(ChatColor.RED + "Invalid usage!");
    }

    private String parsePlaceholders(Player player, String string, Map<StatType, Integer> stats){
        string = ChatColor.translateAlternateColorCodes('&', string);

        if (string.contains("%player%")){
            string = string.replace("%player%", player.getName());
        }

        for (StatType statType : stats.keySet()){
            if (string.contains("%uhc_"+statType.getColumnName()+"%")){
                string = string.replace("%uhc_"+statType.getColumnName()+"%", String.valueOf(stats.get(statType)));
            }
        }

        return string;
    }

}