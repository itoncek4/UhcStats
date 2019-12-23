package com.gmail.mezymc.stats.scoreboards;

import com.gmail.mezymc.stats.GameMode;
import com.gmail.mezymc.stats.StatType;
import com.gmail.mezymc.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.List;

public class LeaderBoard{

    private String key;
    private StatType statType;
    private List<BoardPosition> boardPositions;
    private GameMode gameMode;
    private Location location;
    private String format;

    private ArmorStand armorStand1;
    private ArmorStand armorStand2;

    public LeaderBoard(String key, StatType statType, GameMode gameMode){
        this.key = key;
        this.statType = statType;
        this.gameMode = gameMode;
    }

    public StatType getStatType() {
        return statType;
    }

    public Location getLocation() {
        return location;
    }

    public void instantiate(ConfigurationSection cfg){

        location = new Location(
                Bukkit.getWorld(cfg.getString("location.world")),
                cfg.getDouble("location.x"),
                cfg.getDouble("location.y"),
                cfg.getDouble("location.z")
        );

        String title = cfg.getString("title");
        format = cfg.getString("lines");

        title = ChatColor.translateAlternateColorCodes('&', title);
        format = ChatColor.translateAlternateColorCodes('&', format);

        armorStand1 = spawnArmorStand(
                new Location(location.getWorld(), location.getX(), location.getY() - .3, location.getZ()),
                title
        );
        armorStand2 = null;
    }

    public String getFormat(){
        return format;
    }

    private ArmorStand spawnArmorStand(Location location, String text){

        ArmorStand armorStand = null;

        for (Entity entity : location.getWorld().getEntities()){
            if (entity.getType() == EntityType.ARMOR_STAND && entity.getLocation().equals(location)){
                armorStand = (ArmorStand) entity;
            }
        }

        if (armorStand == null){
            armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        }

        armorStand.setGravity(false);
        armorStand.setVisible(false);
        armorStand.setCustomNameVisible(true);
        armorStand.setCustomName(text);

        return armorStand;
    }

    public void unload(){
        armorStand1.remove();
        if (armorStand2 != null) {
            armorStand2.remove();
        }

        if (boardPositions == null){
            return;
        }

        for (BoardPosition boardPosition : boardPositions){
            boardPosition.remove();
        }
    }

    public void update(){
        boardPositions = StatsManager.getStatsManager().getTop10(this, statType, gameMode);
        boardPositions.forEach(BoardPosition::updateText);
    }

    public BoardPosition getBoardPosition(int position){
        if (boardPositions == null) return null;
        for (BoardPosition boardPosition : boardPositions){
            if (boardPosition.getPosition() == position){
                return boardPosition;
            }
        }
        return null;
    }

}