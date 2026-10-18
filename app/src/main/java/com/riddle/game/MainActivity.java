package com.riddle.game;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LeaderboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        TextView tvLeaderboard = findViewById(R.id.tvLeaderboardContent);
        Button btnBack = findViewById(R.id.btnBack);

        SharedPreferences prefs = getSharedPreferences("TrickyRiddlePrefs", MODE_PRIVATE);
        String topPlayer = prefs.getString("top_player", "Player");
        int highScore = prefs.getInt("high_score", 0);

        String text = "🥇 Rank 1\nPlayer: " + topPlayer + "\nScore: " + highScore + " pts\n\nKeep playing to beat your record!";
        tvLeaderboard.setText(text);

        btnBack.setOnClickListener(v -> finish());
    }
}
