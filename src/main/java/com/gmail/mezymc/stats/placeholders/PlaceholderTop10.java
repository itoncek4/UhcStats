package com.gmail.mezymc.stats.placeholders;

import com.gmail.mezymc.stats.GameMode;
import com.gmail.mezymc.stats.StatType;
import com.gmail.mezymc.stats.StatsManager;
import com.gmail.mezymc.stats.UhcStats;
import com.gmail.mezymc.stats.database.DatabaseConnector;
import com.gmail.mezymc.stats.database.Position;
import com.gmail.mezymc.stats.utils.MojangUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlaceholderTop10 extends PlaceholderExpansion{

    private List<Position> top10 = null;
    private long lastUpdated;

    private DatabaseConnector databaseConnector;
    private GameMode gameMode;
    private StatType statType;

    public PlaceholderTop10(DatabaseConnector databaseConnector, GameMode gameMode, StatType statType){
        this.databaseConnector = databaseConnector;
        this.gameMode = gameMode;
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
        return UhcStats.getPlugin().getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player p, String params) {
        if (top10 == null || System.currentTimeMillis()-lastUpdated > UhcStats.UPDATE_DELAY){
            // update top 10
            Bukkit.getScheduler().runTaskAsynchronously(UhcStats.getPlugin(), new Runnable() {
                @Override
                public void run(){
                    top10 = databaseConnector.getTop10(statType, gameMode);
                }
            });

            lastUpdated = System.currentTimeMillis();

            if (top10 == null){
                top10 = new ArrayList<>();
            }
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

        Position position = top10.get(pos-1);

        if (name){
            if (position.getPlayerName() == null){
                if (StatsManager.getStatsManager().isOnlineMode()){
                    position.setPlayerName("Loading");
                    Bukkit.getScheduler().runTaskAsynchronously(UhcStats.getPlugin(), new Runnable() {
                        @Override
                        public void run() {
                            position.setPlayerName(MojangUtils.getPlayerName(UUID.fromString(position.getId())));
                        }
                    });
                }else{
                    position.setPlayerName(position.getId());
                }
            }

            return position.getPlayerName();
        }else{
            return String.valueOf(position.getValue());
        }
    }

}