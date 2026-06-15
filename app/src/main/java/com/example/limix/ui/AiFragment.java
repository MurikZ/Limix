package com.example.limix.ui;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.example.limix.R;
import com.example.limix.data.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AiFragment extends Fragment {

    // Known social-media packages
    private static final Set<String> SOCIAL_PKGS = new HashSet<>(Arrays.asList(
            "com.instagram.android", "com.zhiliaoapp.musically", "com.twitter.android",
            "com.facebook.katana", "com.vkontakte.android", "org.telegram.messenger",
            "com.snapchat.android", "com.pinterest", "com.reddit.frontpage",
            "com.linkedin.android", "me.vk.ui.main"
    ));

    // Known entertainment packages
    private static final Set<String> ENTERTAINMENT_PKGS = new HashSet<>(Arrays.asList(
            "com.google.android.youtube", "com.netflix.mediaclient",
            "com.twitch.android.app", "ru.kinopoisk",
            "com.amazon.avod.thirdpartyclient", "com.spotify.music"
    ));

    // Suggestion model
    private static class AppSuggestion {
        String packageName, appName, reason;
        int strictness;
        boolean apply = true;

        AppSuggestion(String pkg, String name, int strictness, String reason) {
            this.packageName = pkg;
            this.appName = name;
            this.strictness = strictness;
            this.reason = reason;
        }
    }

    private EditText etGoalInput;
    private Button btnAnalyze, btnApplyAll;
    private LinearLayout suggestionsContainer, dayMapTasksContainer;
    private LinearLayout blackoutContainer, schedulesList;
    private Switch switchDayMapEnforce, switchBlackoutEnabled;

    private final List<AppSuggestion> currentSuggestions = new ArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etGoalInput          = view.findViewById(R.id.etGoalInput);
        btnAnalyze           = view.findViewById(R.id.btnAnalyze);
        btnApplyAll          = view.findViewById(R.id.btnApplyAll);
        suggestionsContainer = view.findViewById(R.id.suggestionsContainer);
        switchDayMapEnforce  = view.findViewById(R.id.switchDayMapEnforce);
        switchBlackoutEnabled = view.findViewById(R.id.switchBlackoutEnabled);
        dayMapTasksContainer = view.findViewById(R.id.dayMapTasksContainer);
        blackoutContainer    = view.findViewById(R.id.blackoutContainer);
        schedulesList        = view.findViewById(R.id.schedulesList);

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("limix_prefs", Context.MODE_PRIVATE);

        // Day map enforcement toggle
        switchDayMapEnforce.setChecked(prefs.getBoolean("day_map_enforce_on", false));
        switchDayMapEnforce.setOnCheckedChangeListener((cb, checked) -> {
            prefs.edit().putBoolean("day_map_enforce_on", checked).apply();
            loadDayMapTasks();
        });

        // Blackout toggle
        boolean blackoutOn = prefs.getBoolean("blackout_enabled", false);
        switchBlackoutEnabled.setChecked(blackoutOn);
        blackoutContainer.setVisibility(blackoutOn ? View.VISIBLE : View.GONE);
        switchBlackoutEnabled.setOnCheckedChangeListener((cb, checked) -> {
            prefs.edit().putBoolean("blackout_enabled", checked).apply();
            blackoutContainer.setVisibility(checked ? View.VISIBLE : View.GONE);
        });

        // Quick-goal chips
        view.findViewById(R.id.chipStudy).setOnClickListener(v -> quickAnalyze(
                "Хочу учиться и не отвлекаться на соцсети и игры"));
        view.findViewById(R.id.chipWork).setOnClickListener(v -> quickAnalyze(
                "Рабочий день: нужна максимальная концентрация без развлечений"));
        view.findViewById(R.id.chipDetox).setOnClickListener(v -> quickAnalyze(
                "Детокс: заблокировать все соцсети и развлечения полностью"));
        view.findViewById(R.id.chipSport).setOnClickListener(v -> quickAnalyze(
                "Тренировка: убрать телефон и не отвлекаться вообще"));

        btnAnalyze.setOnClickListener(v -> analyzeGoal());
        btnApplyAll.setOnClickListener(v -> applyAllSuggestions());
        btnApplyAll.setVisibility(View.GONE);

        view.findViewById(R.id.btnAddBlackout).setOnClickListener(v -> showAddScheduleDialog());

        loadDayMapTasks();
        refreshSchedules();
    }

    private void quickAnalyze(String goal) {
        etGoalInput.setText(goal);
        analyzeGoal();
    }

    private void analyzeGoal() {
        String goal = etGoalInput.getText().toString().trim().toLowerCase(Locale.getDefault());
        if (goal.isEmpty()) {
            Toast.makeText(requireContext(), "Введи цель", Toast.LENGTH_SHORT).show();
            return;
        }
        executor.execute(() -> {
            List<AppSuggestion> suggestions = generateSuggestions(goal);
            mainHandler.post(() -> {
                displaySuggestions(suggestions);
                btnApplyAll.setVisibility(suggestions.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    private List<AppSuggestion> generateSuggestions(String goal) {
        List<AppSuggestion> result = new ArrayList<>();
        PackageManager pm = requireContext().getPackageManager();

        // Determine strictness and target categories from goal text
        int strictness;
        Set<String> categories = new HashSet<>();

        if (containsAny(goal, "детокс", "detox", "полностью", "заблокировать всё", "убрать всё")) {
            strictness = AppUsageEntity.STRICTNESS_MAX;
            categories.addAll(Arrays.asList("social", "entertainment", "games"));
        } else if (containsAny(goal, "учёб", "учиться", "study", "урок", "экзамен", "занять")) {
            strictness = AppUsageEntity.STRICTNESS_STRICT;
            categories.addAll(Arrays.asList("social", "entertainment", "games"));
        } else if (containsAny(goal, "работ", "work", "офис", "концентрац", "фокус", "продуктив")) {
            strictness = AppUsageEntity.STRICTNESS_STRICT;
            categories.addAll(Arrays.asList("social", "entertainment"));
        } else if (containsAny(goal, "спорт", "трениров", "sport", "gym", "фитнес", "убрать телефон")) {
            strictness = AppUsageEntity.STRICTNESS_MAX;
            categories.addAll(Arrays.asList("social", "entertainment", "games"));
        } else {
            strictness = AppUsageEntity.STRICTNESS_NORMAL;
            categories.add("social");
        }

        // Scan installed apps and filter by category
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL);
        String myPkg = requireContext().getPackageName();

        for (ResolveInfo info : apps) {
            String pkg = info.activityInfo.packageName;
            if (pkg.equals(myPkg)) continue;

            String cat = getCategory(pkg);
            if (!categories.contains(cat)) continue;

            String name = info.loadLabel(pm).toString();
            result.add(new AppSuggestion(pkg, name, strictness, reasonFor(cat, strictness)));
        }

        // Fallback: suggest top 5 most-used apps if no category matches
        if (result.isEmpty()) {
            LimixDatabase db = LimixDatabase.getInstance(requireContext());
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            List<DailyUsageEntity> usage = db.dailyUsageDao().getAllForDate(today);
            usage.sort((a, b) -> Long.compare(b.usageTodayMs, a.usageTodayMs));
            int count = 0;
            for (DailyUsageEntity u : usage) {
                if (count >= 5) break;
                if (u.packageName.equals(myPkg)) continue;
                try {
                    String name = pm.getApplicationLabel(
                            pm.getApplicationInfo(u.packageName, 0)).toString();
                    result.add(new AppSuggestion(u.packageName, name, strictness,
                            strictnessLabel(strictness) + " · часто используется"));
                    count++;
                } catch (Exception ignored) {}
            }
        }

        return result;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String getCategory(String pkg) {
        if (SOCIAL_PKGS.contains(pkg)) return "social";
        if (ENTERTAINMENT_PKGS.contains(pkg)) return "entertainment";
        String lower = pkg.toLowerCase(Locale.getDefault());
        if (lower.contains("game") || lower.contains("puzzle") || lower.contains("clash"))
            return "games";
        return "other";
    }

    private String reasonFor(String cat, int strictness) {
        String level = strictnessLabel(strictness);
        switch (cat) {
            case "social": return level + " · соцсети отвлекают";
            case "entertainment": return level + " · мешает фокусу";
            case "games": return level + " · снижает продуктивность";
            default: return level + " · часто используется";
        }
    }

    private String strictnessLabel(int s) {
        if (s >= AppUsageEntity.STRICTNESS_MAX) return "MAX";
        if (s == AppUsageEntity.STRICTNESS_STRICT) return "СТРОГИЙ";
        return "СТАНДАРТ";
    }

    private int strictnessColor(int s) {
        if (s >= AppUsageEntity.STRICTNESS_MAX) return 0xFFFF2244;
        if (s == AppUsageEntity.STRICTNESS_STRICT) return 0xFFFFCC00;
        return 0xFF00FF88;
    }

    private void displaySuggestions(List<AppSuggestion> suggestions) {
        currentSuggestions.clear();
        currentSuggestions.addAll(suggestions);
        suggestionsContainer.removeAllViews();

        if (suggestions.isEmpty()) {
            TextView tv = new TextView(requireContext());
            tv.setText("Приложения не найдены. Попробуй другую формулировку.");
            tv.setTextColor(0xFF888899);
            tv.setPadding(0, 24, 0, 0);
            suggestionsContainer.addView(tv);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (AppSuggestion s : suggestions) {
            View item = inflater.inflate(R.layout.item_ai_suggestion, suggestionsContainer, false);

            ((TextView) item.findViewById(R.id.tvSuggAppName)).setText(s.appName);
            ((TextView) item.findViewById(R.id.tvSuggReason)).setText(s.reason);

            TextView tvLevel = item.findViewById(R.id.tvSuggLevel);
            tvLevel.setText(strictnessLabel(s.strictness));
            tvLevel.setTextColor(strictnessColor(s.strictness));

            CheckBox cb = item.findViewById(R.id.cbApply);
            cb.setChecked(s.apply);
            cb.setOnCheckedChangeListener((v, checked) -> s.apply = checked);

            suggestionsContainer.addView(item);
        }
    }

    private void applyAllSuggestions() {
        executor.execute(() -> {
            LimixDatabase db = LimixDatabase.getInstance(requireContext());
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            int applied = 0;

            for (AppSuggestion s : currentSuggestions) {
                if (!s.apply) continue;

                AppUsageEntity entity = db.appUsageDao().getByPackageName(s.packageName);
                if (entity == null) {
                    entity = new AppUsageEntity(s.packageName, s.appName,
                            0, 60 * 60 * 1000L, today); // Default: 1h limit
                    entity.strictnessLevel = s.strictness;
                    db.appUsageDao().insert(entity);
                } else {
                    entity.strictnessLevel = s.strictness;
                    if (entity.dailyLimitMs <= 0) entity.dailyLimitMs = 60 * 60 * 1000L;
                    db.appUsageDao().update(entity);
                }
                applied++;
            }

            int finalApplied = applied;
            mainHandler.post(() -> {
                Toast.makeText(requireContext(),
                        "Применено: " + finalApplied + " блокировок", Toast.LENGTH_SHORT).show();
                btnApplyAll.setVisibility(View.GONE);
                currentSuggestions.clear();
                suggestionsContainer.removeAllViews();
            });
        });
    }

    private void loadDayMapTasks() {
        executor.execute(() -> {
            LimixDatabase db = LimixDatabase.getInstance(requireContext());
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            List<DayMapEntity> tasks = db.dayMapDao().getDayMap(today);

            mainHandler.post(() -> {
                dayMapTasksContainer.removeAllViews();
                if (tasks.isEmpty()) {
                    addInfoText(dayMapTasksContainer, "Нет задач. Добавь на главном экране.");
                    return;
                }
                for (DayMapEntity t : tasks) {
                    String status = t.isCompleted ? "✓" : (t.isVisited ? "→" : "○");
                    TextView tv = new TextView(requireContext());
                    tv.setText(status + " " + t.appName
                            + (t.taskDescription != null ? ": " + t.taskDescription : ""));
                    tv.setTextColor(t.isCompleted ? 0xFF00FF88 : 0xFFFFFFFF);
                    tv.setTextSize(14);
                    tv.setPadding(0, 8, 0, 8);
                    tv.setTypeface(android.graphics.Typeface.MONOSPACE);
                    dayMapTasksContainer.addView(tv);
                }
            });
        });
    }

    private void showAddScheduleDialog() {
        TimePickerDialog startPicker = new TimePickerDialog(requireContext(),
                (view, startH, startM) -> {
                    TimePickerDialog endPicker = new TimePickerDialog(requireContext(),
                            (v2, endH, endM) -> saveSchedule(startH, startM, endH, endM),
                            Math.min(startH + 1, 23), startM, true);
                    endPicker.setTitle("Конец блокировки");
                    endPicker.show();
                }, 22, 0, true);
        startPicker.setTitle("Начало блокировки");
        startPicker.show();
    }

    private void saveSchedule(int startH, int startM, int endH, int endM) {
        executor.execute(() -> {
            LimixDatabase db = LimixDatabase.getInstance(requireContext());
            String label = String.format(Locale.getDefault(),
                    "%02d:%02d — %02d:%02d", startH, startM, endH, endM);
            db.blackoutScheduleDao().insert(
                    new BlackoutScheduleEntity(label, startH, startM, endH, endM, true));
            mainHandler.post(this::refreshSchedules);
        });
    }

    private void refreshSchedules() {
        executor.execute(() -> {
            LimixDatabase db = LimixDatabase.getInstance(requireContext());
            List<BlackoutScheduleEntity> list = db.blackoutScheduleDao().getAllSchedules();

            mainHandler.post(() -> {
                schedulesList.removeAllViews();
                if (list.isEmpty()) {
                    addInfoText(schedulesList, "Нет расписаний");
                    return;
                }
                LayoutInflater inf = LayoutInflater.from(requireContext());
                for (BlackoutScheduleEntity s : list) {
                    LinearLayout row = new LinearLayout(requireContext());
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(android.view.Gravity.CENTER_VERTICAL);

                    TextView tvLabel = new TextView(requireContext());
                    tvLabel.setText(s.label);
                    tvLabel.setTextColor(Color.WHITE);
                    tvLabel.setTextSize(14);
                    tvLabel.setTypeface(android.graphics.Typeface.MONOSPACE);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    tvLabel.setLayoutParams(lp);

                    Button btnDel = new Button(requireContext());
                    btnDel.setText("×");
                    btnDel.setTextColor(0xFFFF2244);
                    btnDel.setBackgroundColor(Color.TRANSPARENT);
                    int schedId = s.id;
                    btnDel.setOnClickListener(v -> executor.execute(() -> {
                        LimixDatabase db2 = LimixDatabase.getInstance(requireContext());
                        db2.blackoutScheduleDao().deleteById(schedId);
                        mainHandler.post(this::refreshSchedules);
                    }));

                    row.addView(tvLabel);
                    row.addView(btnDel);
                    row.setPadding(0, 8, 0, 8);
                    schedulesList.addView(row);
                }
            });
        });
    }

    private void addInfoText(LinearLayout container, String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextColor(0xFF888899);
        tv.setTextSize(13);
        tv.setPadding(0, 8, 0, 8);
        container.addView(tv);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDayMapTasks();
        refreshSchedules();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
