package com.gmail.mezymc.stats.listeners;

import com.gmail.mezymc.stats.StatsManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiListener implements Listener{

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){
        if (e.getCurrentItem() == null){
            return;
        }

        StatsManager statsManager = StatsManager.getStatsManager();

        if (e.getView().getTitle().equals(statsManager.getGuiTitle())){
            e.setCancelled(true);
        }
    }

}