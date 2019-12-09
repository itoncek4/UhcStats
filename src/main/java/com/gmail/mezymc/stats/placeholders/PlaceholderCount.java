package com.gmail.mezymc.stats.placeholders;

import com.gmail.mezymc.stats.StatType;
import com.gmail.mezymc.stats.UhcStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PlaceholderCount extends PlaceholderExpansion{

    private UhcStats uhcStats;

    public PlaceholderCount(UhcStats uhcStats){
        this.uhcStats = uhcStats;
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
        return uhcStats.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player p, String params){

        StatType statType;

        try {
            statType = StatType.valueOf(params.toUpperCase());
        }catch (Exception ex){
            return "Invalid stat-type";
        }

        return String.valueOf(uhcStats.getCashedStats(p.getUniqueId(), p.getName()).getStats().get(statType));
    }

}