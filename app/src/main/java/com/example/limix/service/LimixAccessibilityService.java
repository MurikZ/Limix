package com.example.limix.service;

import android.accessibilityservice.AccessibilityService;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import com.example.limix.data.*;
import com.example.limix.ui.BlockActivity;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LimixAccessibilityService extends AccessibilityService {

    private static final String TAG = "LimixService";

    // Minimum time between block screens for the same app (prevents flicker during transitions)
    private static final long BLOCK_COOLDOWN_MS = 3000L;

    // Tracks last time block was shown per package
    private final Map<String, Long> lastBlockTime = new HashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;

        CharSequence pkg = event.getPackageName();
        if (pkg == null) return;

        String packageName = pkg.toString();
        if (isSystemOrSelfPackage(packageName)) return;

        executor.execute(() -> checkAllBlocks(packageName));
    }

    private void checkAllBlocks(String packageName) {
        try {
            // Anti-flicker: skip if we just showed a block for this app within cooldown
            Long lastTime = lastBlockTime.get(packageName);
            long now = System.currentTimeMillis();
            if (lastTime != null && now - lastTime < BLOCK_COOLDOWN_MS) return;

            // Priority 1: Blackout schedule (blocks everything)
            if (isBlackoutActive()) {
                triggerBlock(packageName, BlockActivity.TYPE_BLACKOUT, null, null, 1);
                return;
            }

            // Priority 2: Day map enforcement (blocks all apps except current task)
            DayMapBlockResult dayMapResult = checkDayMapEnforcement(packageName);
            if (dayMapResult != null) {
                triggerBlock(packageName, BlockActivity.TYPE_DAY_MAP,
                        dayMapResult.taskAppName, dayMapResult.taskDesc, 1);
                return;
            }

            // Priority 3: App time limit exceeded
            checkLimitBlock(packageName);

        } catch (Exception e) {
            Log.e(TAG, "checkAllBlocks error: " + e.getMessage(), e);
        }
    }

    private boolean isBlackoutActive() {
        SharedPreferences prefs = getSharedPreferences("limix_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("blackout_enabled", false)) return false;

        LimixDatabase db = LimixDatabase.getInstance(this);
        List<BlackoutScheduleEntity> schedules = db.blackoutScheduleDao().getActiveSchedules();
        if (schedules.isEmpty()) return false;

        Calendar now = Calendar.getInstance();
        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        for (BlackoutScheduleEntity s : schedules) {
            int startMin = s.startHour * 60 + s.startMinute;
            int endMin = s.endHour * 60 + s.endMinute;
            if (currentMinutes >= startMin && currentMinutes <= endMin) return true;
        }
        return false;
    }

    // Returns non-null if the day map is active and user opened the wrong app
    private DayMapBlockResult checkDayMapEnforcement(String openedPackage) {
        SharedPreferences prefs = getSharedPreferences("limix_prefs", MODE_PRIVATE);
        if (!prefs.getBoolean("day_map_enforce_on", false)) return null;

        LimixDatabase db = LimixDatabase.getInstance(this);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        List<DayMapEntity> tasks = db.dayMapDao().getDayMap(today);
        if (tasks.isEmpty()) return null;

        // Find the first uncompleted task — that's the current goal
        DayMapEntity currentTask = null;
        for (DayMapEntity task : tasks) {
            if (!task.isCompleted) {
                currentTask = task;
                break;
            }
        }

        if (currentTask == null) return null; // All tasks done — no restriction
        if (openedPackage.equals(currentTask.packageName)) return null; // Opened correct app

        // Opened wrong app while day map enforcement is on
        return new DayMapBlockResult(currentTask.appName, currentTask.taskDescription);
    }

    private void checkLimitBlock(String packageName) {
        LimixDatabase db = LimixDatabase.getInstance(this);
        AppUsageEntity entity = db.appUsageDao().getByPackageName(packageName);

        if (entity == null || entity.dailyLimitMs <= 0) return;

        long usageMs = getTodayUsage(packageName);
        if (usageMs < entity.dailyLimitMs) return;

        // Limit exceeded — block on every launch
        int strictness = entity.strictnessLevel > 0 ? entity.strictnessLevel : 1;
        triggerBlock(packageName, BlockActivity.TYPE_LIMIT, null, null, strictness);
    }

    private void triggerBlock(String packageName, int blockType,
                               String taskAppName, String taskDesc, int strictness) {
        lastBlockTime.put(packageName, System.currentTimeMillis());

        String appName = getAppName(packageName);
        long usageMs = getTodayUsage(packageName);

        Intent intent = new Intent(this, BlockActivity.class);
        intent.putExtra(BlockActivity.EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(BlockActivity.EXTRA_APP_NAME, appName);
        intent.putExtra(BlockActivity.EXTRA_USAGE_MS, usageMs);
        intent.putExtra(BlockActivity.EXTRA_BLOCK_TYPE, blockType);
        intent.putExtra(BlockActivity.EXTRA_STRICTNESS, strictness);
        if (taskAppName != null) intent.putExtra(BlockActivity.EXTRA_TASK_APP_NAME, taskAppName);
        if (taskDesc != null) intent.putExtra(BlockActivity.EXTRA_TASK_DESC, taskDesc);

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        Log.d(TAG, "Block triggered: type=" + blockType + " pkg=" + packageName
                + " strictness=" + strictness);
    }

    private String getAppName(String packageName) {
        try {
            return getPackageManager()
                    .getApplicationLabel(getPackageManager().getApplicationInfo(packageName, 0))
                    .toString();
        } catch (Exception e) {
            return packageName;
        }
    }

    private long getTodayUsage(String packageName) {
        UsageStatsManager usageManager = (UsageStatsManager)
                getSystemService(Context.USAGE_STATS_SERVICE);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Map<String, UsageStats> stats = usageManager.queryAndAggregateUsageStats(
                cal.getTimeInMillis(), System.currentTimeMillis());

        UsageStats appStats = stats.get(packageName);
        return appStats != null ? appStats.getTotalTimeInForeground() : 0;
    }

    private boolean isSystemOrSelfPackage(String pkg) {
        return pkg.startsWith("com.android") ||
                pkg.startsWith("com.example.limix") ||
                pkg.equals("android") ||
                pkg.startsWith("com.google.android.inputmethod") ||
                pkg.startsWith("com.samsung.android.app.launcher") ||
                pkg.startsWith("com.miui.home") ||
                pkg.startsWith("com.huawei.android.launcher");
    }

    static class DayMapBlockResult {
        final String taskAppName;
        final String taskDesc;

        DayMapBlockResult(String taskAppName, String taskDesc) {
            this.taskAppName = taskAppName;
            this.taskDesc = taskDesc;
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "AccessibilityService interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "AccessibilityService connected");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
