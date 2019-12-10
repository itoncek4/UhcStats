package com.gmail.mezymc.stats.utils;

import com.gmail.mezymc.stats.StatType;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlUtils{

    public static boolean tableExists(Connection connection, String tableName){
        Statement statement;
        try{
            statement = connection.createStatement();
        }catch (SQLException ex){
            ex.printStackTrace();
            throw new RuntimeException("Failed to create statement!");
        }

        try{
            statement.executeQuery("SELECT 1 FROM `"+tableName+"` LIMIT 1;").close();
            return true;
        }catch (SQLException ex){
            return false;
        }
    }

    public static void createTable(Connection connection, String database, String tableName){
        StringBuilder sb = new StringBuilder("CREATE TABLE `"+database+"`.`"+tableName+"` (`id` TEXT NOT NULL");
        for (StatType statType : StatType.values()){
            sb.append(", `" + statType.getColumnName() + "` INT NOT NULL DEFAULT '0'");
        }
        sb.append(") ENGINE = InnoDB;");

        try{
            Statement statement = connection.createStatement();
            statement.execute(sb.toString());
            statement.close();
        }catch (SQLException ex){
            Bukkit.getLogger().warning("[UhcStats] Failed to create table!");
            ex.printStackTrace();
        }
    }

}