package com.example.limix.service;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.limix.BuildConfig;
import com.example.limix.data.*;
import com.example.limix.ui.BlockActivity;
import java.text.SimpleDateFormat;
import java.util.*;

public class UsageTrackingWorker extends Worker {

    private static final String TAG = "UsageWorker";

    public UsageTrackingWorker(@NonNull Context context,
                               @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context ctx = getApplicationContext();

            // Проверяем разрешение
            if (!hasUsagePermission(ctx)) {
                if (BuildConfig.DEBUG) Log.d(TAG, "No usage permission");
                return Result.success();
            }

            LimixDatabase db = LimixDatabase.getInstance(ctx);
            String today = getTodayDate();

            // Сбрасываем записи за сегодня один раз в день при первом запуске
            // это очищает устаревшие baseline-значения от старой версии логики
            SharedPreferences prefs = ctx.getSharedPreferences("limix_prefs", Context.MODE_PRIVATE);
            if (!today.equals(prefs.getString("last_reset_date", ""))) {
                db.dailyUsageDao().deleteForDate(today);
                prefs.edit().putString("last_reset_date", today).apply();
                if (BuildConfig.DEBUG) Log.d(TAG, "Daily reset: cleared stale records for " + today);
            }

            // Получаем время с полуночи напрямую через INTERVAL_DAILY
            Map<String, Long> todayUsage = getTodayUsageFromMidnight(ctx);

            if (BuildConfig.DEBUG) Log.d(TAG, "Processing " + todayUsage.size() + " apps");

            for (Map.Entry<String, Long> entry : todayUsage.entrySet()) {
                String packageName = entry.getKey();
                long usageTodayMs = entry.getValue();

                DailyUsageEntity record = db.dailyUsageDao()
                        .getByPackageAndDate(packageName, today);

                if (record == null) {
                    // baseline=0 — поле больше не используется, но оставляем в схеме
                    record = new DailyUsageEntity(packageName, 0, usageTodayMs, today);
                    db.dailyUsageDao().insert(record);
                } else {
                    record.usageTodayMs = usageTodayMs;
                    db.dailyUsageDao().update(record);
                }

                // Проверяем лимит для всех записей (в т.ч. новых)
                checkLimit(ctx, db, packageName, usageTodayMs);
            }

            // Удаляем старые записи раз в день, не каждые 15 минут
            if (!today.equals(prefs.getString("last_cleanup_date", ""))) {
                Calendar cutoff = Calendar.getInstance();
                cutoff.add(Calendar.DAY_OF_YEAR, -30);
                String cutoffDate = new SimpleDateFormat("yyyy-MM-dd",
                        Locale.getDefault()).format(cutoff.getTime());
                db.dailyUsageDao().deleteOldRecords(cutoffDate);
                prefs.edit().putString("last_cleanup_date", today).apply();
            }

            // Начисляем лимми за каждые 15 минут без нарушений
            UserStatsEntity stats = db.userStatsDao().getUserStats();
            if (stats != null) {
                int baseEarn = 3;
                float multiplier = stats.getLimmiesMultiplier();
                int earned = Math.max(0, Math.round(baseEarn * multiplier));
                if (earned > 0) {
                    db.userStatsDao().addLimmies(earned);
                }
            }

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "doWork error: " + e.getMessage(), e);
            return Result.failure();
        }
    }

    // Проверяет превышен ли лимит и показывает блокировку
    private void checkLimit(Context ctx, LimixDatabase db,
                            String packageName, long usageTodayMs) {
        AppUsageEntity limitEntity = db.appUsageDao()
                .getByPackageName(packageName);

        // Лимит не установлен — пропускаем
        if (limitEntity == null || limitEntity.dailyLimitMs <= 0) return;

        // Лимит не превышен — пропускаем
        if (usageTodayMs < limitEntity.dailyLimitMs) return;

        // Проверяем какое приложение сейчас на экране
        String currentApp = getCurrentForegroundApp(ctx);
        if (!packageName.equals(currentApp)) return;

        // Приложение открыто и лимит превышен — показываем блокировку
        if (BuildConfig.DEBUG) Log.d(TAG, "Limit exceeded, usage=" + usageTodayMs +
                " limit=" + limitEntity.dailyLimitMs);

        String appName;
        try {
            appName = ctx.getPackageManager()
                    .getApplicationLabel(ctx.getPackageManager()
                            .getApplicationInfo(packageName, 0))
                    .toString();
        } catch (Exception e) {
            appName = packageName;
        }

        Intent intent = new Intent(ctx, BlockActivity.class);
        intent.putExtra(BlockActivity.EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(BlockActivity.EXTRA_APP_NAME, appName);
        intent.putExtra(BlockActivity.EXTRA_USAGE_MS, usageTodayMs);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP);
        ctx.startActivity(intent);
    }

    // Возвращает время использования каждого приложения строго с 00:00 сегодняшнего дня
    // queryAndAggregateUsageStats агрегирует по заданному диапазону, не по внутренним bucket'ам
    private Map<String, Long> getTodayUsageFromMidnight(Context ctx) {
        UsageStatsManager usm = (UsageStatsManager)
                ctx.getSystemService(Context.USAGE_STATS_SERVICE);

        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0);
        midnight.set(Calendar.MILLISECOND, 0);

        Map<String, UsageStats> stats = usm.queryAndAggregateUsageStats(
                midnight.getTimeInMillis(),
                System.currentTimeMillis()
        );

        Map<String, Long> result = new HashMap<>();
        if (stats != null) {
            for (Map.Entry<String, UsageStats> entry : stats.entrySet()) {
                long time = entry.getValue().getTotalTimeInForeground();
                if (time > 0) result.put(entry.getKey(), time);
            }
        }
        return result;
    }

    // Определяет какое приложение сейчас на переднем плане
    private String getCurrentForegroundApp(Context ctx) {
        UsageStatsManager usageManager = (UsageStatsManager)
                ctx.getSystemService(Context.USAGE_STATS_SERVICE);

        long now = System.currentTimeMillis();
        // Запрашиваем последние 5 секунд
        List<UsageStats> stats = usageManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                now - 5000, now);

        if (stats == null || stats.isEmpty()) return "";

        // Последнее использованное приложение = текущее
        UsageStats latest = null;
        for (UsageStats s : stats) {
            if (latest == null ||
                    s.getLastTimeUsed() > latest.getLastTimeUsed()) {
                latest = s;
            }
        }

        return latest != null ? latest.getPackageName() : "";
    }

    private boolean hasUsagePermission(Context ctx) {
        AppOpsManager appOps = (AppOpsManager)
                ctx.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                ctx.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
    }
}