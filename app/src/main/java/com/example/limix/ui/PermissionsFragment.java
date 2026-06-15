package com.example.limix.ui;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.limix.R;
import com.example.limix.utils.PermissionHelper;

public class PermissionsFragment extends Fragment {

    // Обязательный пустой конструктор
    public PermissionsFragment() {}

    private TextView iconUsage, iconNotification,iconAccessibility;
    private Button btnUsage, btnNotification, btnContinue,btnAccessibility;

    // Только объявляем переменную — не инициализируем здесь
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализируем здесь — фрагмент уже прикреплён к Activity
        // registerForActivityResult можно вызывать только до onStart
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    // Вызывается когда пользователь ответил на запрос уведомлений
                    updatePermissionUI();
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_permissions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        iconUsage = view.findViewById(R.id.iconUsage);
        iconNotification = view.findViewById(R.id.iconNotification);
        btnUsage = view.findViewById(R.id.btnUsage);
        btnNotification = view.findViewById(R.id.btnNotification);
        btnContinue = view.findViewById(R.id.btnContinue);

        // Открывает системные настройки статистики
        btnUsage.setOnClickListener(v ->
                PermissionHelper.openUsageStatsSettings(requireContext()));

        // Показывает системный pop-up для уведомлений
        btnNotification.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        });

        // Переход на экран ИИ
        btnContinue.setOnClickListener(v ->
                Navigation.findNavController(view)
                        .navigate(R.id.action_permissions_to_ai));


        iconAccessibility = view.findViewById(R.id.iconAccessibility);
        btnAccessibility = view.findViewById(R.id.btnAccessibility);

        // Кнопка открывает настройки специальных возможностей
        btnAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(
                    android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });
        updatePermissionUI();
    }
    // Проверяет включён ли наш AccessibilityService
    private boolean isAccessibilityServiceEnabled() {
        String serviceName = requireContext().getPackageName() +
                "/" +
                "com.example.limix.service.LimixAccessibilityService";
        try {
            int enabled = android.provider.Settings.Secure.getInt(
                    requireContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            if (enabled != 1) return false;

            String services = android.provider.Settings.Secure.getString(
                    requireContext().getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return services != null && services.contains(serviceName);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновляем когда пользователь возвращается из настроек
        updatePermissionUI();
    }

    private void updatePermissionUI() {
        boolean hasUsage = PermissionHelper.hasUsageStatsPermission(requireContext());
        boolean hasNotification = PermissionHelper.hasNotificationPermission(requireContext());

        if (hasUsage) {
            iconUsage.setText("✓");
            iconUsage.setTextColor(0xFF00CC66);
            btnUsage.setEnabled(false);
            btnUsage.setAlpha(0.4f);
        } else {
            iconUsage.setText("○");
            iconUsage.setTextColor(0xFFFF6600);
            btnUsage.setEnabled(true);
            btnUsage.setAlpha(1f);
        }

        if (hasNotification) {
            iconNotification.setText("✓");
            iconNotification.setTextColor(0xFF00CC66);
            btnNotification.setEnabled(false);
            btnNotification.setAlpha(0.4f);
        } else {
            iconNotification.setText("○");
            iconNotification.setTextColor(0xFFFF6600);
            btnNotification.setEnabled(true);
            btnNotification.setAlpha(1f);
        }

        if (hasUsage) {
            btnContinue.setEnabled(true);
            btnContinue.setAlpha(1f);
        } else {
            btnContinue.setEnabled(false);
            btnContinue.setAlpha(0.4f);
        }
        boolean hasAccessibility = isAccessibilityServiceEnabled();

        if (hasAccessibility) {
            iconAccessibility.setText("✓");
            iconAccessibility.setTextColor(0xFF00CC66);
            btnAccessibility.setEnabled(false);
            btnAccessibility.setAlpha(0.4f);
        } else {
            iconAccessibility.setText("○");
            iconAccessibility.setTextColor(0xFFFF6600);
            btnAccessibility.setEnabled(true);
            btnAccessibility.setAlpha(1f);
        }
    }
}