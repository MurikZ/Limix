package com.example.limix.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface BlackoutScheduleDao {

    @Insert
    void insert(BlackoutScheduleEntity entity);

    @Update
    void update(BlackoutScheduleEntity entity);

    @Delete
    void delete(BlackoutScheduleEntity entity);

    @Query("SELECT * FROM blackout_schedule ORDER BY start_hour, start_minute")
    List<BlackoutScheduleEntity> getAllSchedules();

    @Query("SELECT * FROM blackout_schedule WHERE is_active = 1 " +
            "ORDER BY start_hour, start_minute")
    List<BlackoutScheduleEntity> getActiveSchedules();

    @Query("DELETE FROM blackout_schedule WHERE id = :id")
    void deleteById(int id);
}
