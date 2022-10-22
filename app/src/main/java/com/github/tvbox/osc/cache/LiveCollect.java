package com.github.tvbox.osc.cache;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.ArrayList;

@Entity(tableName = "liveCollect")
public class LiveCollect implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;
    @ColumnInfo(name = "updateTime")
    public long updateTime;
    @ColumnInfo(name = "channelIndex")
    public int channelIndex;
    @ColumnInfo(name = "channelNum")
    public int channelNum;
    @ColumnInfo(name = "channelName")
    public String channelName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}