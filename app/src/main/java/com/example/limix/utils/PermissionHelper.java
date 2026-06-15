package com.example.limix.utils;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

public class PermissionHelper {

    // Проверяет есть ли разрешение на чтение статистики использования приложений
    // обычный checkPermission здесь не работает — это особое разрешение
    // поэтому проверяем через AppOpsManager
    public static boolean hasUsageStatsPermission(Context context) {
        AppOpsManager appOps = (AppOpsManager) context
                .getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    // Открывает системный экран настроек где пользователь может дать разрешение
    // мы не можем дать его программно — только пользователь вручную
    public static void openUsageStatsSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    // Проверяет разрешение на показ уведомлений (Android 13+)
    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(
                    android.Manifest.permission.POST_NOTIFICATIONS)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        // На Android ниже 13 разрешение не нужно — всегда true
        return true;
    }
}