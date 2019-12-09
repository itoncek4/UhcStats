package com.gmail.mezymc.stats;

import java.util.Map;
import java.util.UUID;

public class StatsEntry{

    private UUID uuid;
    private String name;
    private Map<StatType, Integer> stats;
    private long lastUpdated;

    public StatsEntry(UUID uuid, String name){
        this.uuid = uuid;
        this.name = name;
        stats = null;
        lastUpdated = 0;
    }

    public Map<StatType, Integer> getStats(){
        if (isOlderThan(UhcStats.UPDATE_DELAY)){
            stats = UhcStats.getPlugin(UhcStats.class).getPlayerStats(uuid, name);
        }
        return stats;
    }

    public boolean isOlderThan(long time){
        return lastUpdated + time < System.currentTimeMillis();
    }

    public boolean equals(UUID uuid){
        return this.uuid.equals(uuid);
    }

}