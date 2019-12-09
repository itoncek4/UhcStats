package com.gmail.mezymc.stats;

import com.gmail.mezymc.stats.commands.StatCommandExecutor;
import com.gmail.mezymc.stats.listeners.UhcStatListener;
import com.gmail.mezymc.stats.placeholders.PlaceholderCount;
import com.gmail.mezymc.stats.placeholders.PlaceholderTop10;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;

public class UhcStats extends JavaPlugin{

    public static final long UPDATE_DELAY = 1000*60;

    private boolean isUhcServer;
    private String sqlIp, sqlUsername, sqlPassword, sqlDatabase;
    private int sqlPort;
    private boolean onlineMode;

    private Set<StatsEntry> statsEntries;

    @Override
    public void onEnable() {
        loadConfig();
        new Metrics(this);
        statsEntries = new HashSet<>();

        isUhcServer = getServer().getPluginManager().getPlugin("UhcCore") != null;

        if (isUhcServer){
            getServer().getPluginManager().registerEvents(new UhcStatListener(this), this);
        }

        getServer().getPluginManager().registerEvents(new StatCommandExecutor(this, getConfig().getConfigurationSection("stats-command")), this);

        Connection connection;

        try {
            connection = getSqlConnection();
        }catch (SQLException ex){
            Bukkit.getLogger().warning("[UhcStats] Failed to connect to database! Disabling plugin!");
            ex.printStackTrace();
            getPluginLoader().disablePlugin(this);
            return;
        }

        if (isUhcServer) {
            // check if database exists
            if (!checkIfTableExists(connection)){
                Bukkit.getLogger().warning("[UhcStats] Table does not exist! Creating uhc_stats ...");
                createTable(connection);
            }
        }

        try {
            connection.close();
        }catch (SQLException ex){
            ex.printStackTrace();
        }

        registerPlaceholders();
    }

    public boolean isUhcServer() {
        return isUhcServer;
    }

    public Map<StatType, Integer> getPlayerStats(UUID uuid, String name){
        Map<StatType, Integer> stats = getEmptyStatMap();

        try {
            Connection connection = getSqlConnection();
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT * FROM `uhc_stats` WHERE `"+(onlineMode?"uuid":"name")+"`='"+getPlayerId(uuid, name)+"'");

            if (result.next()){
                // collect stats
                for (StatType statType : StatType.values()){
                    stats.put(statType, result.getInt(statType.getColumnName()));
                }
            }else {
                // insert to table
                insertPlayerToTable(uuid, name);
            }

            result.close();
            statement.close();
            connection.close();
        }catch (SQLException ex){
            Bukkit.getLogger().warning("[UhcStats] Failed to collect stats for: " + uuid.toString());
            ex.printStackTrace();
        }

        return stats;
    }

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

    public void addOneToPlayerStats(UUID uuid, String name, StatType statType){
        Map<StatType, Integer> stats = getPlayerStats(uuid, name);

        try {
            Connection connection = getSqlConnection();
            Statement statement = connection.createStatement();
            statement.executeUpdate("UPDATE `uhc_stats` SET `" + statType.getColumnName() + "`=" + (stats.get(statType) + 1) + " WHERE `"+(onlineMode?"uuid":"name")+"`='" + getPlayerId(uuid, name)+"'");
        }catch (SQLException ex){
            Bukkit.getLogger().warning("[UhcStats] Failed to update stats for: " + uuid.toString());
            ex.printStackTrace();
        }
    }

    private void registerPlaceholders(){
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            Bukkit.getLogger().warning("[UhcStats] PlaceholderAPI is not installed so there will be no placeholders.");
            return;
        }

        Bukkit.getLogger().info("[UhcStats] PlaceholderAPI found, registering placeholders ...");

        new PlaceholderCount(this).register();

        for (StatType statType : StatType.values()){
            new PlaceholderTop10(this, statType).register();
            new PlaceholderTop10(this, statType).register();
        }
    }

    private String getPlayerId(UUID uuid, String name){
        return onlineMode?uuid.toString():name;
    }

    private void insertPlayerToTable(UUID uuid, String name){
        try {
            Connection connection = getSqlConnection();
            StringBuilder sb = new StringBuilder("INSERT INTO `uhc_stats` (`uuid`, `name`");
            for (StatType statType : StatType.values()){
                sb.append(", `" + statType.getColumnName() + "`");
            }
            sb.append(") VALUES ('"+uuid.toString()+"', '"+name+"'");
            for (StatType statType : StatType.values()){
                sb.append(", 0");
            }

            sb.append(")");

            Statement statement = connection.createStatement();
            statement.execute(sb.toString());

            statement.close();
            connection.close();;
        }catch (SQLException ex){
            Bukkit.getLogger().warning("[UhcStats] Failed to update stats for: " + uuid.toString());
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

    private void loadConfig(){
        saveDefaultConfig();

        sqlIp = getConfig().getString("sql.ip", "localhost");
        sqlUsername = getConfig().getString("sql.username", "localhost");
        sqlPassword = getConfig().getString("sql.password", "password123");
        sqlDatabase = getConfig().getString("sql.database", "minecraft");
        sqlPort = getConfig().getInt("sql.port", 3306);
        onlineMode = getConfig().getBoolean("online-mode", true);
    }

    private Connection getSqlConnection() throws SQLException{
        return DriverManager.getConnection("jdbc:mysql://" + sqlIp + ":" + sqlPort + "/" + sqlDatabase + "?autoReconnect=true&useSSL=false", sqlUsername, sqlPassword);
    }

    private boolean checkIfTableExists(Connection connection) {
        Statement statement;
        try {
            statement = connection.createStatement();
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to open database connection!");
        }

        boolean result;
        try {
            statement.executeQuery("SELECT 1 FROM `uhc_stats` LIMIT 1;").close();
            result = true;
        } catch (Exception ex) {
            result = false;
        } finally {
            try {
                statement.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

        }

        return result;
    }

    private void createTable(Connection connection){
        StringBuilder sb = new StringBuilder("CREATE TABLE `"+sqlDatabase+"`.`uhc_stats` (`uuid` TEXT NOT NULL, `name` TEXT NOT NULL");
        for (StatType statType : StatType.values()){
            sb.append(", `" + statType.getColumnName() + "` INT NOT NULL DEFAULT '0'");
        }
        sb.append(") ENGINE = InnoDB;");

        try{
            Statement statement = connection.createStatement();
            statement.execute(sb.toString());
            statement.close();
            connection.close();
        }catch (SQLException ex){
            Bukkit.getLogger().warning("[UhcStats] Failed to create table!");
            ex.printStackTrace();
        }
    }

}