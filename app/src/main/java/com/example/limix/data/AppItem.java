package com.example.limix.data;

import android.graphics.drawable.Drawable;

public class AppItem {

    private String packageName;
    private String appName;
    private Drawable icon;       // иконка — только в памяти, не в базе
    private long usageTodayMs;
    private long dailyLimitMs;

    public AppItem(String packageName, String appName, Drawable icon,
                   long usageTodayMs, long dailyLimitMs) {
        this.packageName = packageName;
        this.appName = appName;
        this.icon = icon;
        this.usageTodayMs = usageTodayMs;
        this.dailyLimitMs = dailyLimitMs;
    }

    public String getPackageName() { return packageName; }
    public String getAppName() { return appName; }
    public Drawable getIcon() { return icon; }
    public long getUsageTodayMs() { return usageTodayMs; }
    public long getDailyLimitMs() { return dailyLimitMs; }

    // Переводим миллисекунды в читаемый формат
    // 3661000 мс → "1ч 1м"
    public String getUsageFormatted() {
        long totalMinutes = usageTodayMs / 60000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours > 0) return hours + "ч " + minutes + "м";
        return minutes + "м";
    }

    // Процент использования от лимита для прогресс-бара
    public int getUsagePercent() {
        if (dailyLimitMs <= 0) return 0;
        int percent = (int) ((usageTodayMs * 100) / dailyLimitMs);
        return Math.min(percent, 100);
    }

    // Превышен ли лимит
    public boolean isLimitExceeded() {
        return dailyLimitMs > 0 && usageTodayMs >= dailyLimitMs;
    }
}