package com.github.tvbox.osc.cache;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * @author pj567
 * @date :2021/1/7
 * @description:
 */
@Dao
public interface LiveCollectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(LiveCollect record);

    @Query("select * from liveCollect  order by updateTime desc")
    List<LiveCollect> getAll();

    @Query("select * from liveCollect where `id`=:id")
    LiveCollect getLiveCollect(int id);

    @Query("select * from liveCollect where `channelName`=:channelName")
    LiveCollect getLiveCollect(String channelName);

    @Query("delete from liveCollect where `id`=:id")
    void delete(int id);

    @Query("delete from liveCollect where `channelName`=:channelName")
    void delete(String channelName);

    @Delete
    int delete(LiveCollect record);
}