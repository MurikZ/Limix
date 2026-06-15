package com.example.limix.ui;

import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.limix.R;
import com.example.limix.data.AppItem;
import java.util.List;

public class AppUsageAdapter extends RecyclerView.Adapter<AppUsageAdapter.AppViewHolder> {

    private List<AppItem> apps;

    // Интерфейс для передачи нажатия наружу — в HomeFragment
    // HomeFragment решает что делать когда нажали на приложение
    public interface OnAppClickListener {
        void onAppClick(AppItem app);
    }

    private OnAppClickListener listener;

    public AppUsageAdapter(List<AppItem> apps, OnAppClickListener listener) {
        this.apps = apps;
        this.listener = listener;
    }

    // Вызываем когда пришли новые данные из базы
    // notifyDataSetChanged говорит RecyclerView перерисовать все элементы
    public void updateApps(List<AppItem> newApps) {
        this.apps = newApps;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app_usage, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        AppItem app = apps.get(position);

        // Заполняем данными
        holder.ivAppIcon.setImageDrawable(app.getIcon());
        holder.tvAppName.setText(app.getAppName());
        holder.tvUsageTime.setText(app.getUsageFormatted());
        holder.progressUsage.setProgress(app.getUsagePercent());

        // Если лимит установлен
        if (app.getDailyLimitMs() > 0) {
            long limitMinutes = app.getDailyLimitMs() / 60000;
            holder.tvLimit.setText("Лимит: " + limitMinutes + " мин");

            // Лимит превышен — красный pixel-стиль
            if (app.isLimitExceeded()) {
                holder.progressUsage.setProgressDrawable(
                        holder.itemView.getContext().getDrawable(R.drawable.pixel_progress_danger));
                holder.tvUsageTime.setTextColor(0xFFFF2244);
                holder.tvAppName.setAlpha(0.6f);
            } else {
                // В норме — оранжевый pixel-стиль
                holder.progressUsage.setProgressDrawable(
                        holder.itemView.getContext().getDrawable(R.drawable.pixel_progress_fill));
                holder.tvUsageTime.setTextColor(0xFFFF6600);
                holder.tvAppName.setAlpha(1.0f);
            }
        } else {
            holder.tvLimit.setText("Нет лимита");
            holder.progressUsage.setProgress(0);
        }

        // Передаём нажатие в HomeFragment
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onAppClick(app);
        });
    }

    @Override
    public int getItemCount() {
        return apps != null ? apps.size() : 0;
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAppIcon;
        TextView tvAppName, tvUsageTime, tvLimit;
        ProgressBar progressUsage;

        AppViewHolder(View itemView) {
            super(itemView);
            ivAppIcon = itemView.findViewById(R.id.ivAppIcon);
            tvAppName = itemView.findViewById(R.id.tvAppName);
            tvUsageTime = itemView.findViewById(R.id.tvUsageTime);
            tvLimit = itemView.findViewById(R.id.tvLimit);
            progressUsage = itemView.findViewById(R.id.progressUsage);
        }
    }
}