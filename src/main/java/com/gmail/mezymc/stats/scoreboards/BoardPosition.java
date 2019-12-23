package com.gmail.mezymc.stats.scoreboards;

import com.gmail.mezymc.stats.StatsManager;
import com.gmail.mezymc.stats.utils.MojangUtils;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.UUID;

public class BoardPosition {

    private LeaderBoard parent;
    private int position;
    private String playerId;
    private int value;
    private ArmorStand armorStand;
    private String playerName;

    public BoardPosition(LeaderBoard parent, int position, String playerId, int value){
        this.parent = parent;
        this.position = position;
        this.playerId = playerId;
        this.value = value;

        if (StatsManager.getStatsManager().isOnlineMode()){
            // fetch player name from uuid
            playerName = MojangUtils.getPlayerName(UUID.fromString(playerId));
        }
    }

    public int getPosition() {
        return position;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int i){
        value = i;
    }

    private String getPlayerName(){
        if (playerName == null){
            return playerId;
        }
        return playerName;
    }

    public void update(String playerId, int value){
        if (!this.playerId.equals(playerId)){
            this.playerId = playerId;
            if (StatsManager.getStatsManager().isOnlineMode()){
                // fetch player name from uuid
                playerName = MojangUtils.getPlayerName(UUID.fromString(playerId));
            }
        }
        this.value = value;
    }

    public void updateText() {
        ArmorStand armorStand = getArmorStand();
        if (armorStand != null) {
            String format = parent.getFormat();
            format = format.replace("%number%", String.valueOf(getPosition()));
            format = format.replace("%player%", getPlayerName());
            format = format.replace("%count%", String.valueOf(getValue()));
            getArmorStand().setCustomName(format);
        }
    }

    public void remove(){
        armorStand.remove();
    }

    public Location getLocation(){
        Location parentLoc = parent.getLocation();
        return new Location(parentLoc.getWorld(), parentLoc.getX(), parentLoc.getY() - getPosition()*.3 - .3, parentLoc.getZ());
    }

    private ArmorStand getArmorStand(){
        if (armorStand == null){
            spawnArmorStand();
        }
        return armorStand;
    }

    public void setArmorStand(ArmorStand armorStand) {
        this.armorStand = armorStand;
    }

    public boolean ownsArmorStand(ArmorStand armorStand){
        return this.armorStand.equals(armorStand);
    }

    private void spawnArmorStand(){
        Location location = getLocation();

        for (Entity entity : location.getWorld().getNearbyEntities(location,1,1,1)){
            if (entity.getType() == EntityType.ARMOR_STAND && entity.getLocation().equals(location)){
                armorStand = (ArmorStand) entity;
            }
        }

        if (armorStand == null){
            LeaderboardUpdateThread.runSync(new Runnable() {
                @Override
                public void run() {
                    armorStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
                    armorStand.setGravity(false);
                    armorStand.setVisible(false);
                    armorStand.setCustomNameVisible(true);
                    armorStand.setCustomName("Loading ...");

                    updateText();
                }
            });

        }else {
            armorStand.setGravity(false);
            armorStand.setVisible(false);
            armorStand.setCustomNameVisible(true);
            armorStand.setCustomName("Loading ...");
        }
    }

}