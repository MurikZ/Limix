package com.example.limix.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;
import androidx.room.OnConflictStrategy;
import java.util.List;

@Dao
public interface DailyUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DailyUsageEntity entity);

    @Update
    void update(DailyUsageEntity entity);

    // Получаем запись для конкретного приложения на конкретный день
    @Query("SELECT * FROM daily_usage WHERE package_name = :packageName " +
            "AND date = :date LIMIT 1")
    DailyUsageEntity getByPackageAndDate(String packageName, String date);

    // Все записи за конкретный день — для главного экрана
    @Query("SELECT * FROM daily_usage WHERE date = :date " +
            "ORDER BY usage_today_ms DESC")
    List<DailyUsageEntity> getAllForDate(String date);

    // Удаляем старые записи — старше 30 дней не нужны
    @Query("DELETE FROM daily_usage WHERE date < :cutoffDate")
    void deleteOldRecords(String cutoffDate);

    // Удаляем все записи за конкретный день — для сброса устаревших baseline
    @Query("DELETE FROM daily_usage WHERE date = :date")
    void deleteForDate(String date);
}