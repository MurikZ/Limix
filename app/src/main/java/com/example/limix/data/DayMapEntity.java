package com.example.limix.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "day_map")
public class DayMapEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // Пакет приложения например "org.telegram.messenger"
    @ColumnInfo(name = "package_name")
    public String packageName;

    // Название приложения например "Telegram"
    @ColumnInfo(name = "app_name")
    public String appName;

    // Что пользователь хочет сделать в этом приложении
    // например "Написать Ивану по поводу проекта"
    @ColumnInfo(name = "task_description")
    public String taskDescription;

    // Порядок в карте — 1, 2, 3, 4
    // пользователь сам расставляет приоритеты
    @ColumnInfo(name = "position")
    public int position;

    // Зашёл ли пользователь в приложение сегодня
    // обновляется автоматически через UsageStats
    @ColumnInfo(name = "is_visited")
    public boolean isVisited;

    // Пользователь сам отметил задачу выполненной
    // нельзя проверить автоматически — доверяем пользователю
    @ColumnInfo(name = "is_completed")
    public boolean isCompleted;

    // Дата формата "2024-01-15"
    // карта сбрасывается каждый день — создаётся новая
    @ColumnInfo(name = "date")
    public String date;

    public DayMapEntity(String packageName, String appName,
                        String taskDescription, int position,
                        boolean isVisited, boolean isCompleted, String date) {
        this.packageName = packageName;
        this.appName = appName;
        this.taskDescription = taskDescription;
        this.position = position;
        this.isVisited = isVisited;
        this.isCompleted = isCompleted;
        this.date = date;
    }
}