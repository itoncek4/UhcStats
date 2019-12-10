package com.gmail.mezymc.stats;

import com.gmail.mezymc.stats.utils.SqlUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.Connection;

public class GameMode{

    public static final GameMode DEFAULT = new GameMode("uhc", "UHC", new ItemStack(Material.IRON_SWORD));

    private String key, name;
    private ItemStack displayItem;

    public GameMode(String key, String name, ItemStack displayItem){
        this.key = key;
        this.name = name;
        this.displayItem = displayItem;

        ItemMeta meta = displayItem.getItemMeta();
        meta.setDisplayName(name);
        this.displayItem.setItemMeta(meta);
    }

    public void createTable(Connection connection, String database){
        // Exists so no need to create
        if (SqlUtils.tableExists(connection, getTableName())){
            return;
        }

        Bukkit.getLogger().warning("[UhcStats] Table "+getTableName()+" does not exist! Creating ...");
        SqlUtils.createTable(connection, database, getTableName());
    }

    public String getKey() {
        return key;
    }

    public ItemStack getDisplayItem() {
        return displayItem;
    }

    String getTableName(){
        return key.replace('-', '_') + "_stats";
    }

}