package com.example.limix.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.limix.R;
import com.example.limix.data.OnboardingSlide;
import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.SlideViewHolder> {

    // Список всех слайдов которые будем показывать
    private List<OnboardingSlide> slides;

    public OnboardingAdapter(List<OnboardingSlide> slides) {
        this.slides = slides;
    }

    // Вызывается когда нужно создать новый ViewHolder
    // LayoutInflater превращает XML в View объект
    @NonNull
    @Override
    public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding_slide, parent, false);
        return new SlideViewHolder(view);
    }

    // Вызывается когда нужно заполнить данными конкретный слайд
    // position — индекс слайда (0, 1, 2...)
    @Override
    public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
        OnboardingSlide slide = slides.get(position);
        holder.tvEmoji.setText(slide.getEmoji());
        holder.tvTitle.setText(slide.getTitle());
        holder.tvDescription.setText(slide.getDescription());
    }

    // Сколько всего слайдов
    @Override
    public int getItemCount() {
        return slides.size();
    }

    // ViewHolder — хранит ссылки на View элементы одного слайда
    // без него система искала бы findViewById каждый раз — это медленно
    static class SlideViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvTitle, tvDescription;

        SlideViewHolder(View itemView) {
            super(itemView);
            // Находим элементы один раз и сохраняем ссылки
            tvEmoji = itemView.findViewById(R.id.tvEmoji);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
        }
    }
}