package com.example.limix.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "app_usage")
public class AppUsageEntity {

    // Strictness levels for block behavior
    public static final int STRICTNESS_NORMAL = 1; // Can unlock (costs 1 life)
    public static final int STRICTNESS_STRICT = 2; // 30-second delay before unlock
    public static final int STRICTNESS_MAX = 3;    // Cannot unlock, only close

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "package_name")
    public String packageName;

    @ColumnInfo(name = "app_name")
    public String appName;

    @ColumnInfo(name = "usage_today_ms")
    public long usageTodayMs;

    @ColumnInfo(name = "daily_limit_ms")
    public long dailyLimitMs;

    @ColumnInfo(name = "last_updated_date")
    public String lastUpdatedDate;

    // 1=NORMAL (unlock for 1 life), 2=STRICT (30s wait), 3=MAX (no unlock)
    @ColumnInfo(name = "strictness_level", defaultValue = "1")
    public int strictnessLevel;

    public AppUsageEntity(String packageName, String appName,
                          long usageTodayMs, long dailyLimitMs,
                          String lastUpdatedDate) {
        this.packageName = packageName;
        this.appName = appName;
        this.usageTodayMs = usageTodayMs;
        this.dailyLimitMs = dailyLimitMs;
        this.lastUpdatedDate = lastUpdatedDate;
        this.strictnessLevel = STRICTNESS_NORMAL;
    }
}
