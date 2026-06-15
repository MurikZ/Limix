package com.example.limix.ui;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.DialogFragment;
import com.example.limix.R;
import com.example.limix.data.AppItem;

public class SetLimitDialog extends DialogFragment {

    // Интерфейс — HomeFragment реализует его чтобы получить результат
    public interface OnLimitSetListener {
        void onLimitSet(String packageName, long limitMs);
        void onLimitRemoved(String packageName);
    }

    private AppItem app;
    private OnLimitSetListener listener;

    // Текущее выбранное значение в минутах
    private int currentMinutes = 30;

    // Создаём диалог с данными приложения
    public static SetLimitDialog newInstance(AppItem app) {
        SetLimitDialog dialog = new SetLimitDialog();
        dialog.app = app;
        return dialog;
    }

    public void setOnLimitSetListener(OnLimitSetListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_set_limit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Находим элементы
        ImageView ivIcon = view.findViewById(R.id.ivDialogAppIcon);
        TextView tvAppName = view.findViewById(R.id.tvDialogAppName);
        TextView tvCurrentUsage = view.findViewById(R.id.tvDialogCurrentUsage);
        TextView tvLimitValue = view.findViewById(R.id.tvLimitValue);
        SeekBar seekBar = view.findViewById(R.id.seekBarLimit);
        Button btnSave = view.findViewById(R.id.btnSaveLimit);
        Button btnRemove = view.findViewById(R.id.btnRemoveLimit);

        TextView preset15 = view.findViewById(R.id.preset15);
        TextView preset30 = view.findViewById(R.id.preset30);
        TextView preset60 = view.findViewById(R.id.preset60);
        TextView preset120 = view.findViewById(R.id.preset120);

        // Заполняем данными приложения
        ivIcon.setImageDrawable(app.getIcon());
        tvAppName.setText(app.getAppName());
        tvCurrentUsage.setText("Сегодня: " + app.getUsageFormatted());

        // Если уже есть лимит — показываем его
        if (app.getDailyLimitMs() > 0) {
            currentMinutes = (int) (app.getDailyLimitMs() / 60000);
            // SeekBar: progress = (minutes - 5) / 5
            // минимум 5 минут, шаг 5 минут
            seekBar.setProgress((currentMinutes - 5) / 5);
            tvLimitValue.setText(formatMinutes(currentMinutes));
        }

        // Слушатель SeekBar
        // SeekBar.max = 35, значит значения от 0 до 35
        // переводим: minutes = (progress * 5) + 5
        // получаем от 5 до 180 минут с шагом 5
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentMinutes = (progress * 5) + 5;
                tvLimitValue.setText(formatMinutes(currentMinutes));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Быстрые пресеты — нажатие устанавливает значение
        preset15.setOnClickListener(v -> setPreset(seekBar, tvLimitValue, 15));
        preset30.setOnClickListener(v -> setPreset(seekBar, tvLimitValue, 30));
        preset60.setOnClickListener(v -> setPreset(seekBar, tvLimitValue, 60));
        preset120.setOnClickListener(v -> setPreset(seekBar, tvLimitValue, 120));

        // Сохранить лимит
        btnSave.setOnClickListener(v -> {
            if (listener != null) {
                // Переводим минуты в миллисекунды
                long limitMs = currentMinutes * 60000L;
                listener.onLimitSet(app.getPackageName(), limitMs);
            }
            dismiss(); // закрываем диалог
        });

        // Убрать лимит
        btnRemove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onLimitRemoved(app.getPackageName());
            }
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Настраиваем размер диалога — 90% ширины экрана
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            // Прозрачный фон окна — наш кастомный background виден
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    // Устанавливает значение пресета в SeekBar и TextView
    private void setPreset(SeekBar seekBar, TextView tvValue, int minutes) {
        currentMinutes = minutes;
        seekBar.setProgress((minutes - 5) / 5);
        tvValue.setText(formatMinutes(minutes));
    }

    // Форматирует минуты в читаемый вид
    // 30 → "30 мин", 60 → "1ч", 90 → "1ч 30м"
    private String formatMinutes(int minutes) {
        if (minutes < 60) {
            return minutes + " мин";
        }
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (mins == 0) return hours + "ч";
        return hours + "ч " + mins + "м";
    }
}