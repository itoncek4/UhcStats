package com.gmail.mezymc.stats.database;

import com.gmail.mezymc.stats.GameMode;
import com.gmail.mezymc.stats.StatType;

import java.util.List;
import java.util.Map;

public interface DatabaseConnector{

    List<Position> getTop10(StatType statType, GameMode gameMode);
    boolean doesTableExists(String tableName);
    void createTable(String name, DatabaseColumn... databaseColumns);
    void pushStats(String playerId, GameMode gameMode, Map<StatType, Integer> stats);
    Map<StatType, Integer> loadStats(String playerId, GameMode gameMode);
    boolean checkConnection();

}