package com.example.limix.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "daily_usage")
public class DailyUsageEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Пакет приложения
    @ColumnInfo(name = "package_name")
    public String packageName;

    // Значение UsageStatsManager на 00:00 сегодняшнего дня
    // это наша точка отсчёта — всё что было до полуночи
    @ColumnInfo(name = "baseline_ms")
    public long baselineMs;

    // Текущее время использования с полуночи
    // = UsageStatsManager.current - baseline_ms
    @ColumnInfo(name = "usage_today_ms")
    public long usageTodayMs;

    // Дата формата "2024-01-15"
    @ColumnInfo(name = "date")
    public String date;

    public DailyUsageEntity(String packageName, long baselineMs,
                            long usageTodayMs, String date) {
        this.packageName = packageName;
        this.baselineMs = baselineMs;
        this.usageTodayMs = usageTodayMs;
        this.date = date;
    }
}