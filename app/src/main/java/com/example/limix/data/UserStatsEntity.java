package com.example.limix.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_stats")
public class UserStatsEntity {
    @PrimaryKey
    public int id = 1;

    @ColumnInfo(name = "lives")
    public int lives;

    @ColumnInfo(name = "limmies")
    public int limmies;

    @ColumnInfo(name = "streak")
    public int streak;

    @ColumnInfo(name = "last_reset_date")
    public String lastResetDate;

    @ColumnInfo(name = "last_success_date")
    public String lastSuccessDate;

    // How many lives were lost today (resets at midnight)
    @ColumnInfo(name = "lives_lost_today", defaultValue = "0")
    public int livesLostToday;

    // Limmies earning penalty: 0=full, 25=−25%, 50=−50%, 75=−75% earned per period
    @ColumnInfo(name = "limmies_penalty", defaultValue = "0")
    public int limmiesPenalty;

    public UserStatsEntity(String lastResetDate, String lastSuccessDate,
                           int streak, int lives, int limmies) {
        this.limmies = limmies;
        this.lives = lives;
        this.streak = streak;
        this.lastResetDate = lastResetDate;
        this.lastSuccessDate = lastSuccessDate;
        this.livesLostToday = 0;
        this.limmiesPenalty = 0;
    }

    // Returns effective limmies multiplier (0.25 to 1.0)
    public float getLimmiesMultiplier() {
        return Math.max(0.1f, 1.0f - (limmiesPenalty / 100.0f));
    }
}
