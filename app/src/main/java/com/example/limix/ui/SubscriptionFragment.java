package com.example.limix.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.Button;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.limix.R;

public class SubscriptionFragment extends Fragment {

    public SubscriptionFragment() {}

    private Button btnSubscribe, btnContinueFree;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_subscription, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnSubscribe = view.findViewById(R.id.btnSubscribe);
        btnContinueFree = view.findViewById(R.id.btnContinueFree);

        // Кнопка подписки — пока просто переходим дальше
        // Google Play Billing подключим отдельным шагом
        btnSubscribe.setOnClickListener(v -> {
            // Сохраняем что пользователь выбрал Premium
            // когда подключим биллинг — здесь будет реальная покупка
            SharedPreferences prefs = requireActivity()
                    .getSharedPreferences("limix_prefs", 0);
            prefs.edit().putBoolean("is_premium", true).apply();

            // Переходим на главный экран
            navigateToHome(view);
        });

        // Кнопка без подписки — сохраняем что пользователь выбрал бесплатный план
        btnContinueFree.setOnClickListener(v -> {
            SharedPreferences prefs = requireActivity()
                    .getSharedPreferences("limix_prefs", 0);
            prefs.edit().putBoolean("is_premium", false).apply();

            navigateToHome(view);
        });
    }

    private void navigateToHome(View view) {
        // popBackStack(false) очищает стек назад
        // пользователь не сможет вернуться на онбординг кнопкой назад
        Navigation.findNavController(view)
                .navigate(R.id.action_subscription_to_home);
    }
}