package com.gmail.mezymc.stats;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GameMode{

    public static final GameMode DEFAULT = new GameMode("uhc", "UHC", new ItemStack(Material.IRON_SWORD));

    private String key;
    private ItemStack displayItem;

    public GameMode(String key, String name, ItemStack displayItem){
        this.key = key;
        this.displayItem = displayItem;

        ItemMeta meta = displayItem.getItemMeta();
        meta.setDisplayName(name);
        this.displayItem.setItemMeta(meta);
    }

    public String getKey() {
        return key;
    }

    public ItemStack getDisplayItem() {
        return displayItem;
    }

    public String getTableName(){
        return key.replace('-', '_') + "_stats";
    }

}