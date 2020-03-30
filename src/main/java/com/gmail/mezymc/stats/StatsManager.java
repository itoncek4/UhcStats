package com.gmail.mezymc.stats;

import com.gmail.mezymc.stats.database.DatabaseColumn;
import com.gmail.mezymc.stats.database.DatabaseConnector;
import com.gmail.mezymc.stats.database.MySqlConnector;
import com.gmail.mezymc.stats.database.Position;
import com.gmail.mezymc.stats.listeners.ConnectionListener;
import com.gmail.mezymc.stats.listeners.GuiListener;
import com.gmail.mezymc.stats.listeners.StatCommandListener;
import com.gmail.mezymc.stats.listeners.UhcStatListener;
import com.gmail.mezymc.stats.placeholders.PlaceholderTop10;
import com.gmail.mezymc.stats.scoreboards.BoardPosition;
import com.gmail.mezymc.stats.scoreboards.LeaderBoard;
import com.gmail.mezymc.stats.scoreboards.LeaderboardUpdateThread;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StatsManager{

    private static StatsManager statsManager;

    private YamlConfiguration cfg;

    private DatabaseConnector databaseConnector;

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

        for (StatsPlayer statsPlayer : cachedPlayers){
            // No stats that need to be pushed
            if (!statsPlayer.isNeedsPush()){
                continue;
            }

            pushGameModeStats(statsPlayer, serverGameMode);
            statsPlayer.setNeedsPush(false);
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

        // SQL Details not yet set, disable plugin
        if (cfg.getString("sql.password", "password123").equals("password123")){
            Bukkit.getLogger().warning("[UhcStats] SQL details not set! Disabling plugin!");
            return false;
        }

        databaseConnector = new MySqlConnector(
                cfg.getString("sql.ip", "localhost"),
                cfg.getString("sql.username", "localhost"),
                cfg.getString("sql.password", "password123"),
                cfg.getString("sql.database", "minecraft"),
                cfg.getInt("sql.port", 3306)
        );

        onlineMode = cfg.getBoolean("online-mode", true);

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

                try{
                    itemStack = new ItemStack(Material.valueOf(displayItem));
                }catch (IllegalArgumentException ex){
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
        // Check connection
        if (!databaseConnector.checkConnection()){
            Bukkit.getLogger().warning("[UhcStats] Failed to connect to database! Disabling plugin!");
            return false;
        }

        // Check tables
        for (GameMode gameMode : gameModes){
            createTable(gameMode);
        }

        return true;
    }

    void createTable(GameMode gameMode){
        if (databaseConnector.doesTableExists(gameMode.getTableName())){
            return;
        }

        databaseConnector.createTable(
                gameMode.getTableName(),
                new DatabaseColumn("id", DatabaseColumn.DataType.TEXT),
                new DatabaseColumn("kill", DatabaseColumn.DataType.INT),
                new DatabaseColumn("death", DatabaseColumn.DataType.INT),
                new DatabaseColumn("win", DatabaseColumn.DataType.INT)
        );
    }

    void registerPlaceholders(){
        if (Bukkit.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            Bukkit.getLogger().warning("[UhcStats] PlaceholderAPI is not installed so there will be no placeholders.");
            return;
        }

        Bukkit.getLogger().info("[UhcStats] PlaceholderAPI found, registering placeholders ...");

        for (StatType statType : StatType.values()){
            new PlaceholderTop10(databaseConnector, serverGameMode, statType).register();
            new PlaceholderTop10(databaseConnector, serverGameMode, statType).register();
        }
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

    private StatsPlayer loadStatsPlayer(String id){
        StatsPlayer player = new StatsPlayer(id);

        for (GameMode gameMode : gameModes){
            player.setGameModeStats(gameMode, databaseConnector.loadStats(id, gameMode));
        }

        return player;
    }

    public List<BoardPosition> getTop10(LeaderBoard leaderBoard, StatType statType, GameMode gameMode){
        List<BoardPosition> boardPositions = new ArrayList<>();
        List<Position> positions = databaseConnector.getTop10(statType, gameMode);

        int i = 1;
        for (Position position : positions){
            BoardPosition boardPosition = leaderBoard.getBoardPosition(i);
            if (boardPosition == null){
                boardPosition = new BoardPosition(leaderBoard, i, position.getId(), position.getValue());
            }else{
                boardPosition.update(position.getId(), position.getValue());
            }
            boardPositions.add(boardPosition);

            i++;
        }

        return boardPositions;
    }

    private void pushGameModeStats(StatsPlayer statsPlayer, GameMode gameMode){
        databaseConnector.pushStats(statsPlayer.getId(), gameMode, statsPlayer.getGameModeStats(gameMode));
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