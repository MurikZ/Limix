package com.example.limix.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.example.limix.R;
import com.example.limix.data.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LeaderboardFragment extends Fragment {

    private TextView tvMyRank, tvMyLimmies, tvMyStreak, tvMyLives;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvMyRank    = view.findViewById(R.id.tvMyRank);
        tvMyLimmies = view.findViewById(R.id.tvMyLimmies);
        tvMyStreak  = view.findViewById(R.id.tvMyStreak);
        tvMyLives   = view.findViewById(R.id.tvMyLives);

        loadStats();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStats();
    }

    private void loadStats() {
        executor.execute(() -> {
            LimixDatabase db = LimixDatabase.getInstance(requireContext());
            UserStatsEntity stats = db.userStatsDao().getUserStats();
            if (stats == null) return;

            mainHandler.post(() -> {
                tvMyLimmies.setText(String.valueOf(stats.limmies));
                tvMyStreak.setText(stats.streak + " дней");

                StringBuilder hearts = new StringBuilder();
                for (int i = 0; i < 3; i++) {
                    hearts.append(i < stats.lives ? "❤ " : "♡ ");
                }
                tvMyLives.setText(hearts.toString().trim());

                // Local rank placeholder until Firebase connected
                tvMyRank.setText("#1");
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
