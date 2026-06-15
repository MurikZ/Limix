package com.example.limix.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;
import androidx.room.OnConflictStrategy;
import java.util.List;

// @Dao — говорит Room что это интерфейс доступа к таблице app_usage
@Dao
public interface AppUsageDao {

    // Вставляем новую запись
    // REPLACE — если запись уже есть, заменяем полностью
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppUsageEntity entity);

    // Обновляем существующую запись
    @Update
    void update(AppUsageEntity entity);

    // Все приложения отсортированные по времени — самые используемые вверху
    @Query("SELECT * FROM app_usage ORDER BY usage_today_ms DESC")
    List<AppUsageEntity> getAllApps();

    // Конкретное приложение по package name
    // LIMIT 1 — берём только первую запись, package_name уникален
    @Query("SELECT * FROM app_usage WHERE package_name = :packageName LIMIT 1")
    AppUsageEntity getByPackageName(String packageName);

    // Только приложения с установленным лимитом
    @Query("SELECT * FROM app_usage WHERE daily_limit_ms > 0 " +
            "ORDER BY usage_today_ms DESC")
    List<AppUsageEntity> getAppsWithLimits();

    // Сбрасываем время использования в начале нового дня
    @Query("UPDATE app_usage SET usage_today_ms = 0")
    void resetDailyUsage();
}