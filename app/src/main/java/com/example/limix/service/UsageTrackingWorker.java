package com.example.limix.service;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
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
                Log.d(TAG, "No usage permission");
                return Result.success();
            }

            LimixDatabase db = LimixDatabase.getInstance(ctx);
            String today = getTodayDate();

            // Получаем текущие данные от UsageStatsManager
            // запрашиваем за последние 2 дня чтобы точно захватить полночь
            Map<String, Long> currentUsage = getCurrentUsage(ctx);

            Log.d(TAG, "Processing " + currentUsage.size() + " apps");

            // Обрабатываем каждое приложение
            for (Map.Entry<String, Long> entry : currentUsage.entrySet()) {
                String packageName = entry.getKey();
                long totalUsageEver = entry.getValue();

                // Ищем baseline для сегодняшнего дня
                DailyUsageEntity record = db.dailyUsageDao()
                        .getByPackageAndDate(packageName, today);

                if (record == null) {
                    // Первый раз видим это приложение сегодня
                    // Текущее значение UsageStats = всё что было до сейчас
                    // Сохраняем как baseline — это наша точка отсчёта с полуночи
                    record = new DailyUsageEntity(
                            packageName,
                            totalUsageEver, // baseline = текущее накопленное
                            0,              // время сегодня = 0 в начале
                            today);
                    db.dailyUsageDao().insert(record);
                    Log.d(TAG, "New baseline for " + packageName +
                            ": " + totalUsageEver);
                } else {
                    // Запись уже есть — обновляем время сегодня
                    // usageTodayMs = текущее накопленное - baseline
                    long usageTodayMs = totalUsageEver - record.baselineMs;

                    // Защита от отрицательных значений
                    // может случиться если Android сбросил свою статистику
                    if (usageTodayMs < 0) {
                        record.baselineMs = totalUsageEver;
                        usageTodayMs = 0;
                    }

                    record.usageTodayMs = usageTodayMs;
                    db.dailyUsageDao().update(record);

                    // Проверяем лимит
                    checkLimit(ctx, db, packageName, usageTodayMs);
                }
            }

            // Удаляем старые записи — старше 30 дней
            Calendar cutoff = Calendar.getInstance();
            cutoff.add(Calendar.DAY_OF_YEAR, -30);
            String cutoffDate = new SimpleDateFormat("yyyy-MM-dd",
                    Locale.getDefault()).format(cutoff.getTime());
            db.dailyUsageDao().deleteOldRecords(cutoffDate);

            // Earn limmies for each 15-min period without violations
            // Penalty reduces earnings (livesLostToday tracked separately)
            UserStatsEntity stats = db.userStatsDao().getUserStats();
            if (stats != null) {
                int baseEarn = 3; // base limmies per 15-min period
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
        Log.d(TAG, "Limit exceeded for " + packageName +
                " usage: " + usageTodayMs +
                " limit: " + limitEntity.dailyLimitMs);

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

    // Получает текущее суммарное время каждого приложения
    private Map<String, Long> getCurrentUsage(Context ctx) {
        Map<String, Long> result = new HashMap<>();

        UsageStatsManager usageManager = (UsageStatsManager)
                ctx.getSystemService(Context.USAGE_STATS_SERVICE);

        // Запрашиваем за последние 2 дня с запасом
        // это гарантирует что мы видим все приложения
        long twoDaysAgo = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000L);
        long now = System.currentTimeMillis();

        Map<String, UsageStats> stats = usageManager
                .queryAndAggregateUsageStats(twoDaysAgo, now);

        for (Map.Entry<String, UsageStats> entry : stats.entrySet()) {
            long time = entry.getValue().getTotalTimeInForeground();
            if (time > 0) {
                result.put(entry.getKey(), time);
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