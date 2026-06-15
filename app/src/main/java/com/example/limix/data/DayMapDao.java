package com.example.limix.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.room.OnConflictStrategy;
import java.util.List;

@Dao
public interface DayMapDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DayMapEntity entity);

    @Update
    void update(DayMapEntity entity);

    @Delete
    void delete(DayMapEntity entity);

    // Карта на конкретный день отсортированная по позиции
    // :date — параметр, передаём строку вида "2024-01-15"
    @Query("SELECT * FROM day_map WHERE date = :date ORDER BY position ASC")
    List<DayMapEntity> getDayMap(String date);

    // Сколько задач уже добавлено сегодня
    // нужно чтобы не дать добавить больше 4 на бесплатном плане
    @Query("SELECT COUNT(*) FROM day_map WHERE date = :date")
    int getCountForDate(String date);

    // Отмечаем что пользователь зашёл в приложение
    // вызывается автоматически когда UsageStats фиксирует использование
    @Query("UPDATE day_map SET is_visited = 1 WHERE package_name = :packageName " +
            "AND date = :date")
    void markAsVisited(String packageName, String date);

    // Пользователь вручную отметил задачу выполненной
    @Query("UPDATE day_map SET is_completed = 1 WHERE id = :id")
    void markAsCompleted(int id);

    // Сколько задач ещё не выполнено
    // если 0 — день успешен, начисляем лимми
    @Query("SELECT COUNT(*) FROM day_map WHERE date = :date AND is_completed = 0")
    int getUncompletedCount(String date);
}