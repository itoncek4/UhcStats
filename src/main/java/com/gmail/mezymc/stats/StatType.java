package com.gmail.mezymc.stats;

public enum StatType{
    KILL,
    DEATH,
    WIN;

    public String getColumnName() {
        return name().toLowerCase();
    }

}