package com.gmail.mezymc.stats.placeholders;

import com.gmail.mezymc.stats.*;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PlaceholderCount extends PlaceholderExpansion{

    private StatsManager statsManager;

    public PlaceholderCount(StatsManager statsManager){
        this.statsManager = statsManager;
    }

    @Override
    public String getIdentifier() {
        return "uhc";
    }

    @Override
    public String getAuthor() {
        return "Mezy";
    }

    @Override
    public String getVersion() {
        return UhcStats.getPlugin().getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player p, String params){

        StatType statType;

        try {
            statType = StatType.valueOf(params.toUpperCase());
        }catch (Exception ex){
            return "Invalid stat-type";
        }

        StatsPlayer statsPlayer = statsManager.getStatsPlayer(p);
        return String.valueOf(statsPlayer.getGameModeStats(GameMode.DEFAULT).get(statType));
    }

}