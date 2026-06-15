package com.example.limix.ui;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.viewpager2.widget.ViewPager2;
import com.example.limix.R;
import com.example.limix.data.OnboardingSlide;
import java.util.ArrayList;
import java.util.List;

public class OnboardingFragment extends Fragment {

    private ViewPager2 viewPager;
    private Button btnNext;
    private LinearLayout dotsLayout;
    private List<OnboardingSlide> slides;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Находим элементы layout по их id
        viewPager = view.findViewById(R.id.viewPager);
        btnNext = view.findViewById(R.id.btnNext);
        dotsLayout = view.findViewById(R.id.dotsLayout);

        // Создаём список слайдов с контентом
        slides = new ArrayList<>();
        slides.add(new OnboardingSlide(
                "📱",
                "Ты теряешь часы каждый день",
                "Средний пользователь проводит в TikTok и Instagram более 3 часов в день. Это 45 дней в год."));
        slides.add(new OnboardingSlide(
                "🧠",
                "Это меняет твой мозг",
                "Бесконечный скроллинг снижает концентрацию и способность получать удовольствие от реальной жизни."));
        slides.add(new OnboardingSlide(
                "⚡",
                "Детокс работает",
                "Уже через 7 дней пользователи отмечают улучшение сна, фокуса и настроения."));

        // Передаём слайды адаптеру и подключаем к ViewPager2
        OnboardingAdapter adapter = new OnboardingAdapter(slides);
        viewPager.setAdapter(adapter);

        // Рисуем точки для первого слайда
        setupDots(0);

        // Слушатель — вызывается каждый раз когда пользователь свайпает на другой слайд
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // Обновляем точки под текущий слайд
                setupDots(position);

                // На последнем слайде меняем текст кнопки
                if (position == slides.size() - 1) {
                    btnNext.setText("НАЧАТЬ");
                } else {
                    btnNext.setText("ДАЛЕЕ");
                }
            }
        });

        // Обработчик нажатия кнопки
        btnNext.setOnClickListener(v -> {
            int currentSlide = viewPager.getCurrentItem();

            if (currentSlide < slides.size() - 1) {
                // Если не последний слайд — идём на следующий
                viewPager.setCurrentItem(currentSlide + 1);
            } else {
                // Если последний слайд — сохраняем что онбординг пройден
                // SharedPreferences — это простое хранилище ключ-значение
                // используем его чтобы запомнить что пользователь уже видел онбординг
                SharedPreferences prefs = requireActivity()
                        .getSharedPreferences("limix_prefs", MODE_PRIVATE);
                prefs.edit().putBoolean("is_first_launch", false).apply();

                // Переходим на экран ИИ
                // navigate вызывает переход который описан в nav_graph
                Navigation.findNavController(view)
                        .navigate(R.id.action_onboarding_to_ai);
            }
        });
    }

    // Метод который рисует точки-индикаторы
    // currentPage — индекс текущего слайда
    private void setupDots(int currentPage) {
        // Очищаем старые точки
        dotsLayout.removeAllViews();

        for (int i = 0; i < slides.size(); i++) {
            TextView dot = new TextView(requireContext());
            dot.setText("●");
            dot.setTextSize(10);
            dot.setPadding(8, 0, 8, 0);

            // Текущий слайд — оранжевая точка, остальные серые
            if (i == currentPage) {
                dot.setTextColor(0xFFFF6600);
            } else {
                dot.setTextColor(0xFF444444);
            }

            dotsLayout.addView(dot);
        }
    }
}