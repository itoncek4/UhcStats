package com.gmail.mezymc.stats.placeholders;

import com.gmail.mezymc.stats.StatType;
import com.gmail.mezymc.stats.UhcStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.List;

public class PlaceholderTop10 extends PlaceholderExpansion{

    private List<RankingObject> top10 = null;
    private long lastUpdated;

    private UhcStats uhcStats;
    private StatType statType;

    public PlaceholderTop10(UhcStats uhcStats, StatType statType){
        this.uhcStats = uhcStats;
        this.statType = statType;
    }

    @Override
    public String getIdentifier() {
        return "uhc-top10-"+statType.getColumnName();
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
    public String onPlaceholderRequest(Player p, String params) {
        if (top10 == null || System.currentTimeMillis()-lastUpdated < UhcStats.UPDATE_DELAY){
            // update top 10
            //top10 = uhcStats.getTop10(statType);
            lastUpdated = System.currentTimeMillis();
        }

        String[] args = params.split("-");

        if (args.length < 2){
            return "Invalid placeholder!";
        }

        boolean name = args[0].equalsIgnoreCase("name");

        if (!name && !args[0].equalsIgnoreCase("value")){
            return "Invalid placeholder!";
        }

        int pos;

        try {
            pos = Integer.valueOf(args[1]);
        }catch (Exception ex){
            return "Invalid placeholder!";
        }

        if (top10.size() < pos){
            return name?"-":"0";
        }

        RankingObject rankingObject = top10.get(pos-1);
        return name?rankingObject.name:String.valueOf(rankingObject.amount);
    }

    public static class RankingObject{

        String name;
        int amount;

        public RankingObject(String name, int amount){
            this.name = name;
            this.amount = amount;
        }

    }

}