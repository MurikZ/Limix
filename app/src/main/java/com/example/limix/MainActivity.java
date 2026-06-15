package com.example.limix;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.limix.service.UsageTrackingWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        bottomNav = findViewById(R.id.bottom_nav);
        NavigationUI.setupWithNavController(bottomNav, navController);

        // Проверяем первый ли это запуск
        // getBoolean возвращает true если ключ не найден — значит первый запуск
        SharedPreferences prefs = getSharedPreferences("limix_prefs", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("is_first_launch", true);

        // Если не первый запуск — пропускаем онбординг и идём сразу на главный экран
        if (!isFirstLaunch) {
            navController.navigate(R.id.homeFragment);
        }

        // Скрываем нижнюю навигацию на экранах до главного
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            if (id == R.id.onboardingFragment
                    || id == R.id.permissionsFragment
                    || id == R.id.aiFragment
                    || id == R.id.subscriptionFragment) {
                bottomNav.setVisibility(View.GONE);
            } else {
                bottomNav.setVisibility(View.VISIBLE);
            }
        });
        // Запускаем периодический Worker — раз в 15 минут
        // 15 минут минимальный интервал который поддерживает WorkManager
        // для более частого обновления используем другой подход
        PeriodicWorkRequest trackingWork =
                new PeriodicWorkRequest.Builder(
                        UsageTrackingWorker.class,
                        15, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "usage_tracking",
                ExistingPeriodicWorkPolicy.KEEP,
                trackingWork);

        Log.d("LIMIX", "WorkManager scheduled");
    }
}