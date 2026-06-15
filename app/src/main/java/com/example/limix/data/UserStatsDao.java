package com.example.limix.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Query;
import androidx.room.OnConflictStrategy;

@Dao
public interface UserStatsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserStatsEntity entity);

    @Update
    void update(UserStatsEntity entity);

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    UserStatsEntity getUserStats();

    // Lose 1 life AND increase limmies penalty by 25% (max 75%)
    // Called every time user dismisses a block screen
    @Query("UPDATE user_stats SET " +
            "lives = MAX(0, lives - 1), " +
            "lives_lost_today = lives_lost_today + 1, " +
            "limmies_penalty = MIN(75, limmies_penalty + 25) " +
            "WHERE id = 1")
    void decrementLivesAndPenalize();

    // Legacy — kept for compatibility
    @Query("UPDATE user_stats SET lives = MAX(0, lives - 1) WHERE id = 1")
    void decrementLives();

    @Query("UPDATE user_stats SET limmies = limmies + :amount WHERE id = 1")
    void addLimmies(int amount);

    // Full midnight reset: restore lives AND clear penalty
    @Query("UPDATE user_stats SET lives = 3, last_reset_date = :date, " +
            "lives_lost_today = 0, limmies_penalty = 0 WHERE id = 1")
    void resetDayStats(String date);

    // Legacy reset (lives only) — kept for compatibility
    @Query("UPDATE user_stats SET lives = 3, last_reset_date = :date WHERE id = 1")
    void resetLives(String date);

    @Query("UPDATE user_stats SET streak = streak + 1, last_success_date = :date WHERE id = 1")
    void incrementStreak(String date);

    @Query("UPDATE user_stats SET streak = 0 WHERE id = 1")
    void resetStreak();
}
