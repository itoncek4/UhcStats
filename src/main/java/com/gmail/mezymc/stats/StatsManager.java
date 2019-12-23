package com.gmail.mezymc.stats;

import com.gmail.mezymc.stats.listeners.ConnectionListener;
import com.gmail.mezymc.stats.listeners.GuiListener;
import com.gmail.mezymc.stats.listeners.StatCommandListener;
import com.gmail.mezymc.stats.listeners.UhcStatListener;
import com.gmail.mezymc.stats.scoreboards.BoardPosition;
import com.gmail.mezymc.stats.scoreboards.LeaderBoard;
import com.gmail.mezymc.stats.scoreboards.LeaderboardUpdateThread;
import com.gmail.val59000mc.exceptions.ParseException;
import com.gmail.val59000mc.utils.JsonItemUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

public class StatsManager{

    private static StatsManager statsManager;

    private YamlConfiguration cfg;

    private String sqlIp, sqlUsername, sqlPassword, sqlDatabase;
    private int sqlPort;

    private String guiTitle;

    private Set<GameMode> gameModes;
    private Set<StatsPlayer> cachedPlayers;
    private Set<LeaderBoard> leaderBoards;
    private boolean isUhcServer;
    private boolean onlineMode;
    private GameMode serverGameMode;

    public StatsManager(){
        statsManager = this;
        cachedPlayers = new HashSet<>();
    }

    public static StatsManager getStatsManager() {
        return statsManager;
    }

    public String getGuiTitle() {
        return guiTitle;
    }

    /**
     * This method will get a StatsPlayer
     * @param id The player ID, depending on online or offline more this is their name/uuid
     * @param load When true and the player is not cashed it will be loaded. Make sure you only run this asynchronous!
     * @param cache When true the StatsPlayer will stay in memory.
     * @return Returns a StatsPlayer. May be null when load is false and player is not cashed!
     */
    public StatsPlayer getStatsPlayer(String id, boolean load, boolean cache){
        for (StatsPlayer player : cachedPlayers){
            if (player.getId().equals(id)){
                return player;
            }
        }

        // Don't load new stats player.
        if (!load){
            return null;
        }

        StatsPlayer player = loadStatsPlayer(id);
        if (cache){
            cachedPlayers.add(player);
        }

        return player;
    }

    /**
     * This method will get a StatsPlayer
     * @param uuid The player UUID
     * @param name The player name
     * @param load When true and the player is not cashed it will be loaded. Make sure you only run this asynchronous!
     * @param cache When true the StatsPlayer will stay in memory.
     * @return Returns a StatsPlayer. May be null when load is false and player is not cashed!
     */
    public StatsPlayer getStatsPlayer(UUID uuid, String name, boolean load, boolean cache){
        String id = onlineMode ? uuid.toString() : name;
        return getStatsPlayer(id, load, cache);
    }

    /**
     * This method will get a StatsPlayer
     * @param uuid The player UUID
     * @param name The player name
     * @return Returns a StatsPlayer. May be null when load is false and player is not cashed!
     */
    public StatsPlayer getStatsPlayer(UUID uuid, String name){
        return getStatsPlayer(uuid, name, false, false);
    }

    /**
     * This method will get a StatsPlayer
     * @param player The Bukkit Player object.
     * @param load When true and the player is not cashed it will be loaded. Make sure you only run this asynchronous!
     * @param cache When true the StatsPlayer will stay in memory.
     * @return Returns a StatsPlayer. May be null when load is false and player is not cashed!
     */
    public StatsPlayer getStatsPlayer(Player player, boolean load, boolean cache){
        return getStatsPlayer(player.getUniqueId(), player.getName(), load, cache);
    }

    /**
     * This method will get a StatsPlayer
     * @param player The Bukkit Player object.
     * @return Returns a StatsPlayer. May be null when load is false and player is not cashed!
     */
    public StatsPlayer getStatsPlayer(Player player){
        return getStatsPlayer(player.getUniqueId(), player.getName(), false, false);
    }

    public boolean isOnlineMode() {
        return onlineMode;
    }

    public void playerLeavesTheGame(Player player){
        StatsPlayer statsPlayer = getStatsPlayer(player);

        // StatsPlayer is not online
        if (statsPlayer == null){
            return;
        }

        // Stats player has gained stats in current game, push stats when the game has finished.
        if (statsPlayer.isNeedsPush()){
            return;
        }

        cachedPlayers.remove(statsPlayer);
    }

    public void pushAllStats(){
        // No Uhc Server so no stats collected.
        if (!isUhcServer){
            return;
        }

        Connection connection;

        try {
            connection = getSqlConnection();
        }catch (SQLException ex){
            ex.printStackTrace();
            return;
        }

        for (StatsPlayer statsPlayer : cachedPlayers){
            // No stats that need to be pushed
            if (!statsPlayer.isNeedsPush()){
                continue;
            }

            pushGameModeStats(connection, statsPlayer, serverGameMode);
            statsPlayer.setNeedsPush(false);
        }

        try {
            connection.close();
        }catch (SQLException ex){
            ex.printStackTrace();
        }
    }

    boolean loadConfig(){
        File file = new File(UhcStats.getPlugin().getDataFolder() + "/config.yml");
        if (!file.exists()){
            UhcStats.getPlugin().saveDefaultConfig();
        }

        cfg = new YamlConfiguration();

        try {
            cfg.load(file);
        }catch (IOException | InvalidConfigurationException ex){
            Bukkit.getLogger().warning("[UhcStats] Failed to load config! Disabling plugin!");
            ex.printStackTrace();
            return false;
        }

        sqlIp = cfg.getString("sql.ip", "localhost");
        sqlUsername = cfg.getString("sql.username", "localhost");
        sqlPassword = cfg.getString("sql.password", "password123");
        sqlDatabase = cfg.getString("sql.database", "minecraft");
        sqlPort = cfg.getInt("sql.port", 3306);
        onlineMode = cfg.getBoolean("online-mode", true);

        // SQL Details not yet set, disable plugin
        if (sqlPassword.equals("password123")){
            Bukkit.getLogger().warning("[UhcStats] SQL details not set! Disabling plugin!");
            return false;
        }

        guiTitle = ChatColor.translateAlternateColorCodes('&', cfg.getString("gui-title", "&6&lUHC Stats"));

        // Check if UhcServer
        isUhcServer = Bukkit.getServer().getPluginManager().getPlugin("UhcCore") != null;

        // Load GameModes
        Bukkit.getLogger().info("[UhcStats] Loading GameModes");
        gameModes = new HashSet<>();
        ConfigurationSection section = cfg.getConfigurationSection("gamemodes");

        if (section == null){
            gameModes.add(GameMode.DEFAULT);
            Bukkit.getLogger().info("[UhcStats] No GameModes found!");
        }else{
            for (String key : section.getKeys(false)){
                String name = section.getString(key + ".name", null);
                String displayItem = section.getString(key + ".display-item", null);

                if (name == null || displayItem == null){
                    Bukkit.getLogger().severe("[UhcStats] Failed to load GameMode: " + key + " missing data.");
                    continue;
                }

                ItemStack itemStack;

                try {
                    itemStack = JsonItemUtils.getItemFromJson(displayItem);
                }catch (ParseException ex){
                    Bukkit.getLogger().severe("[UhcStats] Invalid display item! " + displayItem);
                    ex.printStackTrace();
                    continue;
                }

                name = ChatColor.translateAlternateColorCodes('&', name);
                gameModes.add(new GameMode(key, name, itemStack));
            }

            Bukkit.getLogger().info("[UhcStats] Loaded " + gameModes.size() + " GameModes!");

            Bukkit.getScheduler().runTaskLater(UhcStats.getPlugin(), () -> loadLeaderBoards(cfg), 10);
        }


        // Load server GameMode
        if (isUhcServer){
            if (gameModes.size() == 1){
                serverGameMode = gameModes.iterator().next();
            }else{
                serverGameMode = getGameMode(section.getString("server-gamemode"));
            }

            Bukkit.getLogger().info("[UhcStats] Server GameMode is: " + serverGameMode.getKey());
        }

        return true;
    }

    void registerListeners(){
        Bukkit.getPluginManager().registerEvents(new ConnectionListener(this), UhcStats.getPlugin());
        Bukkit.getPluginManager().registerEvents(new GuiListener(), UhcStats.getPlugin());
        Bukkit.getPluginManager().registerEvents(new StatCommandListener(cfg.getString("stats-command", "/stats")), UhcStats.getPlugin());

        if (isUhcServer){
            Bukkit.getPluginManager().registerEvents(new UhcStatListener(this), UhcStats.getPlugin());
        }
    }

    boolean loadStatsTables(){
        Connection connection;

        // Check connection
        try {
            connection = getSqlConnection();
        }catch (SQLException ex){
            Bukkit.getLogger().warning("[UhcStats] Failed to connect to database! Disabling plugin!");
            ex.printStackTrace();
            return false;
        }

        // Check tables
        for (GameMode gameMode : gameModes){
            gameMode.createTable(connection, sqlDatabase);
        }

        try {
            connection.close();
        }catch (SQLException ex){
            ex.printStackTrace();
        }
        return true;
    }

    void registerPlaceholders(){
        if (Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            Bukkit.getLogger().warning("[UhcStats] PlaceholderAPI is not installed so there will be no placeholders.");
            return;
        }

        Bukkit.getLogger().info("[UhcStats] PlaceholderAPI found, registering placeholders ...");

        //new PlaceholderCount(this).register();
/*
        for (StatType statType : StatType.values()){
            new PlaceholderTop10(this, statType).register();
            new PlaceholderTop10(this, statType).register();
        }
*/
    }

    public GameMode getGameMode(String key){
        for (GameMode gameMode : gameModes){
            if (gameMode.getKey().equals(key)){
                return gameMode;
            }
        }
        return null;
    }

    public GameMode getServerGameMode() {
        return serverGameMode;
    }

    public Inventory loadStatsUI(StatsPlayer statsPlayer){
        Inventory inv = Bukkit.createInventory(null, 9, guiTitle);

        for (GameMode gameMode : gameModes){
            Map<StatType, Integer> gameModeStats = statsPlayer.getGameModeStats(gameMode);

            ItemStack item = gameMode.getDisplayItem();
            ItemMeta meta = item.getItemMeta();
            List<String> lore = new ArrayList<>();

            for (StatType statType : StatType.values()){
                lore.add(ChatColor.YELLOW + statType.getName() + ": " + gameModeStats.get(statType));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            inv.addItem(item);
        }

        return inv;
    }

    private Connection getSqlConnection() throws SQLException{
        Validate.isTrue(!Bukkit.isPrimaryThread(), "You may only open an connection to the database on a asynchronous thread!");
        return DriverManager.getConnection("jdbc:mysql://" + sqlIp + ":" + sqlPort + "/" + sqlDatabase + "?autoReconnect=true&useSSL=false", sqlUsername, sqlPassword);
    }

    private StatsPlayer loadStatsPlayer(String id){
        StatsPlayer player = new StatsPlayer(id);

        Connection connection;

        try {
            connection = getSqlConnection();
        }catch (SQLException ex){
            ex.printStackTrace();
            return null;
        }

        for (GameMode gameMode : gameModes){
            player.setGameModeStats(gameMode, loadGameModeStats(connection, id, gameMode));
        }

        return player;
    }

    private Map<StatType, Integer> loadGameModeStats(Connection connection, String playerId, GameMode gameMode){
        Map<StatType, Integer> stats = getEmptyStatMap();

        try {
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
        }catch (SQLException ex){
            ex.printStackTrace();
        }

        return stats;
    }

    public List<BoardPosition> getTop10(LeaderBoard leaderBoard, StatType statType, GameMode gameMode){
        try {
            Connection connection = getSqlConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT `id`, `"+statType.getColumnName()+"` FROM `"+gameMode.getTableName()+"` ORDER BY `"+statType.getColumnName()+"` DESC LIMIT 10");
            List<BoardPosition> boardPositions = new ArrayList<>();

            int pos = 1;
            while (resultSet.next()){
                BoardPosition boardPosition = leaderBoard.getBoardPosition(pos);
                if (boardPosition == null) {
                    boardPosition = new BoardPosition(
                            leaderBoard,
                            pos,
                            resultSet.getString("id"),
                            resultSet.getInt(statType.getColumnName())
                    );
                }else{
                    boardPosition.update(resultSet.getString("id"), resultSet.getInt(statType.getColumnName()));
                }

                boardPositions.add(boardPosition);
                pos++;
            }

            resultSet.close();
            statement.close();
            connection.close();

            return boardPositions;
        }catch (SQLException ex){
            ex.printStackTrace();
            return null;
        }
    }

    private void pushGameModeStats(Connection connection, StatsPlayer statsPlayer, GameMode gameMode){
        Map<StatType, Integer> stats = statsPlayer.getGameModeStats(gameMode);

        try {
            Statement statement = connection.createStatement();
            for (StatType statType : stats.keySet()){
                statement.executeUpdate(
                        "UPDATE `"+gameMode.getTableName()+"` SET `" + statType.getColumnName() + "`=" + stats.get(statType) + " WHERE `id`='"+statsPlayer.getId()+"'"
                );
            }
            statement.close();
        }catch (SQLException ex){
            Bukkit.getLogger().warning("[UhcStats] Failed to push stats for: " + statsPlayer.getId());
            ex.printStackTrace();
        }
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

    private void loadLeaderBoards(YamlConfiguration cfg){
        ConfigurationSection cfgSection = cfg.getConfigurationSection("leaderboards");
        leaderBoards = new HashSet<>();

        if (cfgSection == null){
            Bukkit.getLogger().info("[UhcStats] Loaded 0 leaderboards.");
            return;
        }

        for (String key : cfgSection.getKeys(false)){
            StatType statType = StatType.valueOf(cfgSection.getString(key + ".stat-type"));
            GameMode gameMode = getGameMode(cfgSection.getString(key + ".gamemode", "uhc"));

            if (gameMode == null){
                Bukkit.getLogger().warning("[UhcStats] Failed to load " + key + " leaderboard, make sure you have it's gamemode configured in the config.");
                continue;
            }

            LeaderBoard leaderBoard = new LeaderBoard(key, statType, gameMode);
            leaderBoards.add(leaderBoard);
            leaderBoard.instantiate(cfgSection.getConfigurationSection(key));
        }

        Bukkit.getLogger().info("[UhcStats] Loaded "+leaderBoards.size()+" leaderboards.");

        // start thread
        Bukkit.getScheduler().runTaskLaterAsynchronously(UhcStats.getPlugin(), new LeaderboardUpdateThread(this), 10);
    }

    public Set<LeaderBoard> getLeaderBoards(){
        return leaderBoards;
    }

}