package com.gmail.mezymc.stats;

import com.gmail.mezymc.stats.scoreboards.LeaderBoard;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class UhcStats extends JavaPlugin{

    public static final long UPDATE_DELAY = 1000*60;

    @Override
    public void onEnable(){
        // Add bStats
        new Metrics(this);

        // Load StatsManager
        StatsManager statsManager = new StatsManager();

        if (!statsManager.loadConfig()){
            getPluginLoader().disablePlugin(this);
            return;
        }

        statsManager.registerListeners();

        Bukkit.getScheduler().runTaskAsynchronously(this,() -> {
            // Failed to connect to database, disable plugin!
            if (!statsManager.loadStatsTables()){
                getPluginLoader().disablePlugin(UhcStats.this);
            }
        });

        statsManager.registerPlaceholders();
    }

    @Override
    public void onDisable() {
        for (LeaderBoard leaderBoard : StatsManager.getStatsManager().getLeaderBoards()){
            leaderBoard.unload();
        }
    }

    public static UhcStats getPlugin(){
        return getPlugin(UhcStats.class);
    }

}