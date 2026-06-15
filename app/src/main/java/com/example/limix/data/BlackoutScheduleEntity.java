package com.example.limix.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "blackout_schedule")
public class BlackoutScheduleEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "label")
    public String label;

    @ColumnInfo(name = "start_hour")
    public int startHour;

    @ColumnInfo(name = "start_minute")
    public int startMinute;

    @ColumnInfo(name = "end_hour")
    public int endHour;

    @ColumnInfo(name = "end_minute")
    public int endMinute;

    @ColumnInfo(name = "is_active")
    public boolean isActive;

    public BlackoutScheduleEntity(String label, int startHour, int startMinute,
                                   int endHour, int endMinute, boolean isActive) {
        this.label = label;
        this.startHour = startHour;
        this.startMinute = startMinute;
        this.endHour = endHour;
        this.endMinute = endMinute;
        this.isActive = isActive;
    }
}
