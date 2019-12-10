package com.gmail.mezymc.stats;

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

    public static UhcStats getPlugin(){
        return getPlugin(UhcStats.class);
    }
/*
    public StatsEntry getCashedStats(UUID uuid, String name){
        Set<StatsEntry> outdated = new HashSet<>();
        StatsEntry playerStats = null;

        for (StatsEntry statsEntry : statsEntries){
            if (statsEntry.equals(uuid)){
                playerStats = statsEntry;
            }
            else if (statsEntry.isOlderThan(UPDATE_DELAY*2)){
                outdated.add(statsEntry);
            }
        }

        if (playerStats == null) {
            playerStats = new StatsEntry(uuid, name);
            statsEntries.add(playerStats);
        }

        statsEntries.removeAll(outdated);

        return playerStats;
    }

    public List<PlaceholderTop10.RankingObject> getTop10(StatType statType){
        List<PlaceholderTop10.RankingObject> top10 = new ArrayList<>();

        try {
            Connection connection = getSqlConnection();
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT * FROM `uhc_stats` ORDER BY `"+statType.getColumnName()+"` DESC LIMIT 10");

            while (result.next()){
                top10.add(new PlaceholderTop10.RankingObject(result.getString("name"), result.getInt(statType.getColumnName())));
            }

            result.close();
            statement.close();
            connection.close();
        }catch (SQLException ex){
            Bukkit.getLogger().warning("[UhcStats] Failed to collect top 10 stats!");
            ex.printStackTrace();
        }

        return top10;
    }

 */

}