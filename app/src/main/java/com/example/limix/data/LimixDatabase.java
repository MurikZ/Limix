package com.example.limix.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {
        AppUsageEntity.class,
        UserStatsEntity.class,
        DayMapEntity.class,
        DailyUsageEntity.class,
        BlackoutScheduleEntity.class
}, version = 3)
public abstract class LimixDatabase extends RoomDatabase {

    private static volatile LimixDatabase instance;

    public abstract AppUsageDao appUsageDao();
    public abstract UserStatsDao userStatsDao();
    public abstract DayMapDao dayMapDao();
    public abstract DailyUsageDao dailyUsageDao();
    public abstract BlackoutScheduleDao blackoutScheduleDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS daily_usage (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "package_name TEXT, " +
                            "baseline_ms INTEGER NOT NULL DEFAULT 0, " +
                            "usage_today_ms INTEGER NOT NULL DEFAULT 0, " +
                            "date TEXT)"
            );
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Add limmies penalty system to user_stats
            database.execSQL(
                    "ALTER TABLE user_stats ADD COLUMN lives_lost_today INTEGER NOT NULL DEFAULT 0");
            database.execSQL(
                    "ALTER TABLE user_stats ADD COLUMN limmies_penalty INTEGER NOT NULL DEFAULT 0");

            // Add strictness level to app_usage
            database.execSQL(
                    "ALTER TABLE app_usage ADD COLUMN strictness_level INTEGER NOT NULL DEFAULT 1");

            // Create blackout schedule table
            database.execSQL(
                    "CREATE TABLE IF NOT EXISTS blackout_schedule (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "label TEXT, " +
                            "start_hour INTEGER NOT NULL DEFAULT 0, " +
                            "start_minute INTEGER NOT NULL DEFAULT 0, " +
                            "end_hour INTEGER NOT NULL DEFAULT 23, " +
                            "end_minute INTEGER NOT NULL DEFAULT 59, " +
                            "is_active INTEGER NOT NULL DEFAULT 1)"
            );
        }
    };

    public static LimixDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (LimixDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    LimixDatabase.class,
                                    "limix_database"
                            )
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .build();
                }
            }
        }
        return instance;
    }
}
