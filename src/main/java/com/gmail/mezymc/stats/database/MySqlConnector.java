package com.gmail.mezymc.stats.database;

import com.gmail.mezymc.stats.GameMode;
import com.gmail.mezymc.stats.StatType;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MySqlConnector implements DatabaseConnector{

    private String ip, username, password, database;
    private int port;

    public MySqlConnector(String ip, String username, String password, String database, int port){
        this.ip = ip;
        this.username = username;
        this.password = password;
        this.database = database;
        this.port = port;
    }

    @Override
    public List<Position> getTop10(StatType statType, GameMode gameMode){
        try {
            Connection connection = getSqlConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT `id`, `"+statType.getColumnName()+"` FROM `"+gameMode.getTableName()+"` ORDER BY `"+statType.getColumnName()+"` DESC LIMIT 10");
            List<Position> positions = new ArrayList<>();

            int pos = 1;
            while (resultSet.next()){
                Position position = new Position(
                        pos,
                        resultSet.getString("id"),
                        resultSet.getInt(statType.getColumnName())
                );

                positions.add(position);
                pos++;
            }

            resultSet.close();
            statement.close();
            connection.close();

            return positions;
        }catch (SQLException ex){
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean doesTableExists(String tableName){
        Connection connection;
        Statement statement;
        try{
            connection = getSqlConnection();
            statement = connection.createStatement();
        }catch (SQLException ex){
            ex.printStackTrace();
            throw new RuntimeException("Failed to create statement!");
        }

        try{
            statement.executeQuery("SELECT 1 FROM `"+tableName+"` LIMIT 1;").close();
            statement.close();
            connection.close();
            return true;
        }catch (SQLException ex){
            return false;
        }
    }

    @Override
    public void createTable(String name, DatabaseColumn... databaseColumns){
        StringBuilder sb = new StringBuilder("CREATE TABLE `"+database+"`.`"+name+"` (");
        boolean first = true;

        for (DatabaseColumn databaseColumn : databaseColumns){
            if (first){
                first = false;
            }else{
                sb.append(", ");
            }

            sb.append(databaseColumn.toString());
        }
        sb.append(") ENGINE = InnoDB;");

        try{
            Connection connection = getSqlConnection();
            Statement statement = connection.createStatement();
            statement.execute(sb.toString());
            statement.close();
            connection.close();
        }catch (SQLException ex){
            Bukkit.getLogger().warning("[UhcStats] Failed to create table!");
            ex.printStackTrace();
        }
    }

    @Override
    public void pushStats(String playerId, GameMode gameMode, Map<StatType, Integer> stats){
        try {
            Connection connection = getSqlConnection();
            Statement statement = connection.createStatement();
            for (StatType statType : stats.keySet()){
                statement.executeUpdate(
                        "UPDATE `"+gameMode.getTableName()+"` SET `" + statType.getColumnName() + "`=" + stats.get(statType) + " WHERE `id`='"+playerId+"'"
                );
            }
            statement.close();
            connection.close();
        }catch (SQLException ex){
            Bukkit.getLogger().warning("[UhcStats] Failed to push stats for: " + playerId);
            ex.printStackTrace();
        }
    }

    @Override
    public Map<StatType, Integer> loadStats(String playerId, GameMode gameMode){
        Map<StatType, Integer> stats = getEmptyStatMap();

        try {
            Connection connection = getSqlConnection();
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT * FROM `"+gameMode.getTableName()+"` WHERE `id`='"+playerId+"'");

            if (result.next()){
                // collect stats
                for (StatType statType : StatType.values()){
                    stats.put(statType, result.getInt(statType.getColumnName()));
                }
            }else{
                // Player not found, insert player to table.
                insertPlayerToTable(connection, playerId, gameMode);
            }

            result.close();
            statement.close();
            connection.close();
        }catch (SQLException ex){
            ex.printStackTrace();
        }

        return stats;
    }

    @Override
    public boolean checkConnection() {
        try{
            getSqlConnection();
            return true;
        }catch (SQLException ex){
            ex.printStackTrace();
            return false;
        }
    }

    private Connection getSqlConnection() throws SQLException{
        Validate.isTrue(!Bukkit.isPrimaryThread(), "You may only open an connection to the database on a asynchronous thread!");
        return DriverManager.getConnection("jdbc:mysql://" + ip + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false", username, password);
    }

    private void insertPlayerToTable(Connection connection, String playerId, GameMode gameMode){
        try {
            StringBuilder sb = new StringBuilder("INSERT INTO `"+gameMode.getTableName()+"` (`id`");
            for (StatType statType : StatType.values()){
                sb.append(", `" + statType.getColumnName() + "`");
            }

            sb.append(") VALUES ('"+playerId+"'");

            for (int i = 0; i < StatType.values().length; i++) {
                sb.append(", 0");
            }

            sb.append(")");

            Statement statement = connection.createStatement();
            statement.execute(sb.toString());

            statement.close();
        }catch (SQLException ex){
            Bukkit.getLogger().warning("[UhcStats] Failed to update stats for: " + playerId);
            ex.printStackTrace();
        }
    }

    private Map<StatType, Integer> getEmptyStatMap(){
        Map<StatType, Integer> stats = new HashMap<>();

        for (StatType statType : StatType.values()){
            stats.put(statType, 0);
        }

        return stats;
    }

}