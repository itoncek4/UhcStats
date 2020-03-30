package com.gmail.mezymc.stats.database;

public class Position{

    private int position, value;
    private String id;

    public Position(int position, String id, int value){
        this.position = position;
        this.id = id;
        this.value = value;
    }

    public int getPosition() {
        return position;
    }

    public String getId() {
        return id;
    }

    public int getValue() {
        return value;
    }

}