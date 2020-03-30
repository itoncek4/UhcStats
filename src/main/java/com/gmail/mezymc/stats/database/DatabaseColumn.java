package com.gmail.mezymc.stats.database;

public class DatabaseColumn{

    public enum DataType{
        TEXT,
        INT
    }

    private String name;
    private DataType dataType;

    public DatabaseColumn(String name, DataType dataType){
        this.name = name;
        this.dataType = dataType;
    }

    public String getName() {
        return name;
    }

    public DataType getDataType() {
        return dataType;
    }

    @Override
    public String toString() {
        return "`"+name+"` "+dataType+" NOT NULL" + (dataType == DataType.INT ? " DEFAULT '0'" : "");
    }
}