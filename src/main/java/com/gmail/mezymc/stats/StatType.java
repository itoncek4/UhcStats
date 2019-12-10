package com.gmail.mezymc.stats;

public enum StatType{
    KILL("Kills"),
    DEATH("Death"),
    WIN("Wins");

    private String name;

    StatType(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getColumnName() {
        return name().toLowerCase();
    }

}