package com.example.limix.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.limix.MainActivity;
import com.example.limix.R;
import com.example.limix.data.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BlockActivity extends AppCompatActivity {

    // Intent extras
    public static final String EXTRA_PACKAGE_NAME  = "package_name";
    public static final String EXTRA_APP_NAME       = "app_name";
    public static final String EXTRA_USAGE_MS       = "usage_ms";
    public static final String EXTRA_BLOCK_TYPE     = "block_type";
    public static final String EXTRA_STRICTNESS     = "strictness";
    public static final String EXTRA_TASK_APP_NAME  = "task_app_name";
    public static final String EXTRA_TASK_DESC      = "task_desc";

    // Block types
    public static final int TYPE_LIMIT    = 0;  // App daily limit exceeded
    public static final int TYPE_DAY_MAP  = 1;  // Day map: wrong app opened
    public static final int TYPE_BLACKOUT = 2;  // Blackout schedule active

    private String packageName, appName, taskAppName, taskDesc;
    private long usageMs;
    private int blockType, strictness;

    private TextView tvTitle, tvSubtitle, tvBlockedAppName;
    private TextView tvTimeSpent, tvBlockLives, tvCountdown, tvWarning, tvPenaltyInfo;
    private ImageView ivBlockedAppIcon;
    private Button btnUnlock, btnClose;
    private View countdownContainer;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Security: prevent screenshots and screen recording of block screen
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_block);

        packageName  = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);
        appName      = getIntent().getStringExtra(EXTRA_APP_NAME);
        usageMs      = getIntent().getLongExtra(EXTRA_USAGE_MS, 0);
        blockType    = getIntent().getIntExtra(EXTRA_BLOCK_TYPE, TYPE_LIMIT);
        strictness   = getIntent().getIntExtra(EXTRA_STRICTNESS, 1);
        taskAppName  = getIntent().getStringExtra(EXTRA_TASK_APP_NAME);
        taskDesc     = getIntent().getStringExtra(EXTRA_TASK_DESC);

        bindViews();
        setupCloseButton();
        setupBlockTypeUI();
        loadLives();
    }

    private void bindViews() {
        tvTitle           = findViewById(R.id.tvBlockTitle);
        tvSubtitle        = findViewById(R.id.tvBlockSubtitle);
        tvBlockedAppName  = findViewById(R.id.tvBlockedAppName);
        tvTimeSpent       = findViewById(R.id.tvTimeSpent);
        tvBlockLives      = findViewById(R.id.tvBlockLives);
        tvCountdown       = findViewById(R.id.tvCountdown);
        tvWarning         = findViewById(R.id.tvWarning);
        tvPenaltyInfo     = findViewById(R.id.tvPenaltyInfo);
        ivBlockedAppIcon  = findViewById(R.id.ivBlockedAppIcon);
        btnUnlock         = findViewById(R.id.btnUnlock);
        btnClose          = findViewById(R.id.btnClose);
        countdownContainer = findViewById(R.id.countdownContainer);
    }

    private void setupCloseButton() {
        btnClose.setOnClickListener(v -> goHome());
    }

    private void setupBlockTypeUI() {
        switch (blockType) {
            case TYPE_DAY_MAP:  setupDayMapBlock();  break;
            case TYPE_BLACKOUT: setupBlackoutBlock(); break;
            default:            setupLimitBlock();
        }
    }

    // ── LIMIT block ──────────────────────────────────────────────────────────
    private void setupLimitBlock() {
        tvTitle.setText("ЛИМИТ\nИСЧЕРПАН");
        tvSubtitle.setText(appName != null ? appName.toUpperCase() : "");
        tvBlockedAppName.setText("Использовано сегодня: " + formatTime(usageMs));
        tvTimeSpent.setVisibility(View.GONE);

        loadAppIcon();

        if (strictness >= AppUsageEntity.STRICTNESS_MAX) {
            // MAX: no unlock at all
            btnUnlock.setVisibility(View.GONE);
            tvWarning.setText("СТРОГИЙ РЕЖИМ: разблокировка невозможна");
        } else if (strictness == AppUsageEntity.STRICTNESS_STRICT) {
            // STRICT: 30-second countdown before unlock
            btnUnlock.setEnabled(false);
            btnUnlock.setAlpha(0.3f);
            countdownContainer.setVisibility(View.VISIBLE);
            tvWarning.setText("Подождите, затем можно разблокировать за 1 ❤");
            startCountdown();
        } else {
            // NORMAL: immediate unlock costs 1 life
            tvWarning.setText("РАЗБЛОКИРОВКА СТОИТ 1 ЖИЗНЬ");
            btnUnlock.setOnClickListener(v -> doUnlock());
        }
    }

    // ── DAY MAP block ─────────────────────────────────────────────────────────
    private void setupDayMapBlock() {
        tvTitle.setText("ФОКУС\nВРЕМЯ");
        String target = taskAppName != null ? taskAppName : "следующее приложение";
        tvSubtitle.setText("ТЕКУЩАЯ ЦЕЛЬ: " + target.toUpperCase());

        String task = (taskDesc != null && !taskDesc.isEmpty())
                ? "Задача: " + taskDesc
                : "Открой " + target + " и выполни цель";
        tvBlockedAppName.setText(task);

        ivBlockedAppIcon.setImageResource(android.R.drawable.ic_menu_agenda);
        ivBlockedAppIcon.setAlpha(0.7f);
        tvTimeSpent.setVisibility(View.GONE);

        tvWarning.setText("ИГНОРИРОВАНИЕ СТОИТ 1 ЖИЗНЬ");
        btnUnlock.setText("ВСЁ РАВНО ОТКРЫТЬ  (−1 ❤)");
        btnUnlock.setOnClickListener(v -> doUnlock());
    }

    // ── BLACKOUT block ────────────────────────────────────────────────────────
    private void setupBlackoutBlock() {
        tvTitle.setText("РЕЖИМ\nФОКУСА");
        tvSubtitle.setText("ДОСТУП ОГРАНИЧЕН");
        tvBlockedAppName.setText("Это время выделено для важных дел");

        ivBlockedAppIcon.setImageResource(android.R.drawable.ic_lock_lock);
        ivBlockedAppIcon.setAlpha(0.6f);
        tvTimeSpent.setVisibility(View.GONE);

        btnUnlock.setVisibility(View.GONE);
        tvWarning.setText("ВСЕ ПРИЛОЖЕНИЯ ЗАБЛОКИРОВАНЫ ПО РАСПИСАНИЮ");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void loadAppIcon() {
        try {
            Drawable icon = getPackageManager().getApplicationIcon(packageName);
            ivBlockedAppIcon.setImageDrawable(icon);
            ivBlockedAppIcon.setAlpha(0.25f);
        } catch (PackageManager.NameNotFoundException e) {
            ivBlockedAppIcon.setImageResource(android.R.drawable.sym_def_app_icon);
            ivBlockedAppIcon.setAlpha(0.25f);
        }
    }

    private void startCountdown() {
        countDownTimer = new CountDownTimer(30_000L, 1000) {
            @Override
            public void onTick(long millisLeft) {
                tvCountdown.setText("Разблокировка через: " + (millisLeft / 1000) + "с");
            }

            @Override
            public void onFinish() {
                countdownContainer.setVisibility(View.GONE);
                btnUnlock.setEnabled(true);
                btnUnlock.setAlpha(1.0f);
                tvWarning.setText("РАЗБЛОКИРОВКА СТОИТ 1 ЖИЗНЬ");
                btnUnlock.setOnClickListener(v -> doUnlock());
            }
        }.start();
    }

    private void doUnlock() {
        executor.execute(() -> {
            LimixDatabase db = LimixDatabase.getInstance(this);

            // Lose 1 life AND increase limmies penalty by 25%
            db.userStatsDao().decrementLivesAndPenalize();

            getSharedPreferences("limix_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("had_unlock_today", true)
                    .apply();

            runOnUiThread(() -> {
                Intent launchIntent =
                        getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                }
                finish();
            });
        });
    }

    private void goHome() {
        Intent homeIntent = new Intent(this, MainActivity.class);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
        finish();
    }

    private void loadLives() {
        executor.execute(() -> {
            LimixDatabase db = LimixDatabase.getInstance(this);
            UserStatsEntity stats = db.userStatsDao().getUserStats();
            if (stats == null) return;

            runOnUiThread(() -> {
                // Hearts display
                StringBuilder hearts = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    hearts.append(i < stats.lives ? "❤ " : "♡ ");
                }
                tvBlockLives.setText(hearts.toString().trim());

                // Show penalty warning if lives were lost today
                if (stats.livesLostToday > 0) {
                    tvPenaltyInfo.setVisibility(View.VISIBLE);
                    tvPenaltyInfo.setText(
                            "Штраф лимми: -" + stats.limmiesPenalty + "% до конца дня");
                }

                // Disable unlock if no lives left
                if (stats.lives <= 0 && btnUnlock.getVisibility() == View.VISIBLE) {
                    btnUnlock.setEnabled(false);
                    btnUnlock.setAlpha(0.3f);
                    btnUnlock.setText("НЕТ ЖИЗНЕЙ");
                }
            });
        });
    }

    private String formatTime(long ms) {
        long totalMinutes = ms / 60_000L;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours > 0) return hours + "ч " + minutes + "м";
        return minutes + "м";
    }

    @Override
    public void onBackPressed() {
        // Intentionally blocked — user must use Close or Unlock button
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) countDownTimer.cancel();
        executor.shutdown();
    }
}
