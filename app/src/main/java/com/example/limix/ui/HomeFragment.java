package com.example.limix.ui;

import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.limix.R;
import com.example.limix.data.*;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment implements AppUsageAdapter.OnAppClickListener,SetLimitDialog.OnLimitSetListener {

    public HomeFragment() {}

    private TextView tvLives, tvLimmies, tvStreak, tvBestStreak;
    private TextView tvTotalTime, tvLimitPercent, tvTotalLimmies, tvRank;
    private TextView tvAiInsight, tvViewAll;
    private ProgressBar circularProgress;
    private RecyclerView recyclerApps;
    private AppUsageAdapter adapter;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvLives = view.findViewById(R.id.tvLives);
        tvLimmies = view.findViewById(R.id.tvLimmies);
        tvStreak = view.findViewById(R.id.tvStreak);
        tvBestStreak = view.findViewById(R.id.tvBestStreak);
        tvTotalTime = view.findViewById(R.id.tvTotalTime);
        tvLimitPercent = view.findViewById(R.id.tvLimitPercent);
        tvTotalLimmies = view.findViewById(R.id.tvTotalLimmies);
        tvRank = view.findViewById(R.id.tvRank);
        tvAiInsight = view.findViewById(R.id.tvAiInsight);
        tvViewAll = view.findViewById(R.id.tvViewAll);
        circularProgress = view.findViewById(R.id.circularProgress);

        recyclerApps = view.findViewById(R.id.recyclerApps);
        recyclerApps.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerApps.setNestedScrollingEnabled(false);
        adapter = new AppUsageAdapter(new ArrayList<>(), this);
        recyclerApps.setAdapter(adapter);

        tvViewAll.setOnClickListener(v ->
                Toast.makeText(requireContext(),
                        "Скоро: полный список", Toast.LENGTH_SHORT).show());

        loadData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        executor.execute(() -> {
            try {
                Context ctx = requireContext();
                LimixDatabase db = LimixDatabase.getInstance(ctx);
                String today = getTodayDate();

                UserStatsEntity stats = db.userStatsDao().getUserStats();

                if (stats == null) {
                    // Первый запуск — создаём запись
                    stats = new UserStatsEntity(today, today, 0, 3, 0);
                    db.userStatsDao().insert(stats);
                    Log.d("LIMIX", "First launch — created user stats");
                } else {
                    Log.d("LIMIX", "lastResetDate: " + stats.lastResetDate + " today: " + today);

                    if (!today.equals(stats.lastResetDate)) {
                        // Новый день — восстанавливаем жизни и сбрасываем штраф лимми
                        Log.d("LIMIX", "New day detected — resetting lives and penalty");
                        db.userStatsDao().resetDayStats(today);

                        // Проверяем стрик
                        String yesterday = getYesterdayDate();
                        if (!yesterday.equals(stats.lastSuccessDate)) {
                            db.userStatsDao().resetStreak();
                            Log.d("LIMIX", "Streak reset");
                        }

                        // Сбрасываем флаг срыва
                        requireActivity()
                                .getSharedPreferences("limix_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putBoolean("had_unlock_today", false)
                                .apply();

                        // Перечитываем обновлённые данные
                        stats = db.userStatsDao().getUserStats();
                    }
                }

                List<AppItem> apps = getAppsWithUsage(db);

                long totalMs = 0;
                for (AppItem app : apps) totalMs += app.getUsageTodayMs();

                long totalLimit = 0;
                long totalUsageWithLimit = 0;
                for (AppItem app : apps) {
                    if (app.getDailyLimitMs() > 0) {
                        totalLimit += app.getDailyLimitMs();
                        totalUsageWithLimit += app.getUsageTodayMs();
                    }
                }
                int limitPercent = totalLimit > 0
                        ? (int) ((totalUsageWithLimit * 100) / totalLimit) : -1;

                UserStatsEntity finalStats = stats;
                long finalTotalMs = totalMs;
                int finalLimitPercent = limitPercent;

                mainHandler.post(() -> {
                    updateStatsUI(finalStats);
                    updateTotalTime(finalTotalMs);
                    updateMetrics(finalLimitPercent, finalStats.limmies);
                    updateAiInsight(finalTotalMs);
                    adapter.updateApps(apps);
                });

            } catch (Exception e) {
                Log.e("LIMIX", "loadData error: " + e.getMessage(), e);
            }
        });
    }

    // Обновляет жизни, лимми, стрик
    private void updateStatsUI(UserStatsEntity stats) {
        StringBuilder hearts = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            hearts.append(i < stats.lives ? "❤️" : "🖤");
        }
        tvLives.setText(hearts.toString());

        // Show limmies with penalty indicator
        String limmiesText = String.valueOf(stats.limmies);
        if (stats.limmiesPenalty > 0) {
            limmiesText += " (−" + stats.limmiesPenalty + "%)";
        }
        tvLimmies.setText(limmiesText);
        tvStreak.setText(stats.streak + " day focus streak");
        tvBestStreak.setText("Best: " + stats.streak);
    }

    // Обновляет большой круг с общим временем
    private void updateTotalTime(long totalMs) {
        long totalMinutes = totalMs / 60000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        // Форматируем текст
        String timeText;
        if (hours > 0) {
            timeText = hours + "ч " + minutes + "м";
        } else {
            timeText = minutes + "м";
        }
        tvTotalTime.setText(timeText);

        // Прогресс круга — считаем от дневного лимита 6 часов
        // если лимит не установлен используем 6 часов как эталон
        int progress = (int) Math.min((totalMs * 100) / (6 * 60 * 60 * 1000L), 100);
        circularProgress.setProgress(progress);
    }

    // Обновляет метрики под стриком
    private void updateMetrics(int limitPercent, int limmies) {
        if (limitPercent >= 0) {
            tvLimitPercent.setText(limitPercent + "%");
        } else {
            tvLimitPercent.setText("—");
        }
        tvTotalLimmies.setText(String.valueOf(limmies));
        tvRank.setText("—"); // рейтинг добавим когда подключим Firebase
    }

    // Простой локальный инсайт без ИИ — пока заглушка
    private void updateAiInsight(long totalMs) {
        long hours = totalMs / 3600000;
        String insight;

        if (hours == 0) {
            insight = "Отличное начало дня! Продолжай в том же духе.";
        } else if (hours < 2) {
            insight = "Хороший результат — меньше 2 часов экранного времени.";
        } else if (hours < 4) {
            insight = "Уже " + hours + "ч за экраном. Попробуй сделать перерыв.";
        } else {
            insight = "Более " + hours + "ч сегодня. Время отложить телефон.";
        }

        tvAiInsight.setText(insight);
    }

    private List<AppItem> getAppsWithUsage(LimixDatabase db) {
        List<AppItem> result = new ArrayList<>();
        PackageManager pm = requireContext().getPackageManager();
        String today = getTodayDate();

        // Читаем наши данные из daily_usage — они считаются с полуночи
        List<DailyUsageEntity> dailyUsage = db.dailyUsageDao()
                .getAllForDate(today);

        // Строим map для быстрого поиска
        Map<String, Long> usageMap = new HashMap<>();
        for (DailyUsageEntity entity : dailyUsage) {
            usageMap.put(entity.packageName, entity.usageTodayMs);
        }

        Log.d("LIMIX", "Daily usage records: " + dailyUsage.size());

        // Берём список всех установленных приложений
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolvedApps = pm.queryIntentActivities(
                intent, PackageManager.MATCH_ALL);

        String myPackage = requireContext().getPackageName();

        for (ResolveInfo resolveInfo : resolvedApps) {
            String packageName = resolveInfo.activityInfo.packageName;
            if (packageName.equals(myPackage)) continue;

            try {
                String appName = resolveInfo.loadLabel(pm).toString();
                Drawable icon = resolveInfo.loadIcon(pm);

                // Берём время из нашей таблицы — считается с полуночи
                long usage = usageMap.getOrDefault(packageName, 0L);

                AppUsageEntity entity = db.appUsageDao()
                        .getByPackageName(packageName);
                long limit = entity != null ? entity.dailyLimitMs : -1;

                result.add(new AppItem(packageName, appName,
                        icon, usage, limit));
            } catch (Exception ignored) {}
        }

        // Сортируем — самые используемые вверху
        result.sort((a, b) ->
                Long.compare(b.getUsageTodayMs(), a.getUsageTodayMs()));

        return result;
    }
    private Map<String, Long> getUsageMap() {
        Map<String, Long> usageMap = new HashMap<>();

        if (!hasUsagePermission()) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            return usageMap;
        }

        UsageStatsManager usageManager = (UsageStatsManager) requireContext()
                .getSystemService(Context.USAGE_STATS_SERVICE);

        // Начало сегодняшнего дня 00:00:00
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();
        long now = System.currentTimeMillis();

        // Используем queryUsageStats вместо queryAndAggregateUsageStats
        List<UsageStats> usageStatsList = usageManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDay,
                now
        );

        if (usageStatsList != null) {
            for (UsageStats usageStats : usageStatsList) {
                long timeInForeground = usageStats.getTotalTimeInForeground();
                String packageName = usageStats.getPackageName();

                // Проверяем, что время использования попадает в сегодняшний день
                if (timeInForeground > 0) {
                    // Проверяем, когда был последний раз использовано приложение
                    if (usageStats.getLastTimeUsed() >= startOfDay) {
                        usageMap.put(packageName, timeInForeground);
                        Log.d("LIMIX", packageName + ": " + (timeInForeground / 60000) + "м");
                    }
                }
            }
        }

        return usageMap;
    }
    private boolean hasUsagePermission() {
        AppOpsManager appOps = (AppOpsManager) requireContext()
                .getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                requireContext().getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
    }
    private String getYesterdayDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -1);

        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(calendar.getTime());
    }

    @Override
    public void onAppClick(AppItem app) {
        // Создаём диалог и передаём данные приложения
        SetLimitDialog dialog = SetLimitDialog.newInstance(app);
        dialog.setOnLimitSetListener(this);
        // show требует FragmentManager и тег для идентификации
        dialog.show(getParentFragmentManager(), "SetLimitDialog");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    // Пользователь нажал "Сохранить" в диалоге
    @Override
    public void onLimitSet(String packageName, long limitMs) {
        executor.execute(() -> {
            LimixDatabase db = LimixDatabase.getInstance(requireContext());
            String today = getTodayDate();

            // Проверяем есть ли уже запись для этого приложения
            AppUsageEntity entity = db.appUsageDao().getByPackageName(packageName);

            if (entity != null) {
                // Запись есть — обновляем лимит
                entity.dailyLimitMs = limitMs;
                db.appUsageDao().update(entity);
            } else {
                // Записи нет — создаём новую
                entity = new AppUsageEntity(packageName, packageName, 0, limitMs, today);
                db.appUsageDao().insert(entity);
            }

            // Обновляем экран
            mainHandler.post(() -> {
                Toast.makeText(requireContext(),
                        "Лимит установлен: " + (limitMs / 60000) + " мин",
                        Toast.LENGTH_SHORT).show();
                loadData(); // перезагружаем список
            });
        });
    }

    // Пользователь нажал "Убрать лимит"
    @Override
    public void onLimitRemoved(String packageName) {
        executor.execute(() -> {
            LimixDatabase db = LimixDatabase.getInstance(requireContext());
            AppUsageEntity entity = db.appUsageDao().getByPackageName(packageName);

            if (entity != null) {
                // Ставим -1 — означает лимит не установлен
                entity.dailyLimitMs = -1;
                db.appUsageDao().update(entity);
            }

            mainHandler.post(() -> {
                Toast.makeText(requireContext(),
                        "Лимит убран", Toast.LENGTH_SHORT).show();
                loadData();
            });
        });
    }

}