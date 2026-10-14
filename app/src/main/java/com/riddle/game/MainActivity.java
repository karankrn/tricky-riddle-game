package com.riddle.game;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class MainActivity extends AppCompatActivity {

    private TextView tvRiddle, tvLevel, tvHint;
    private EditText etAnswer;
    private Button btnSubmit, btnHint;

    private RewardedAd rewardedAd;
    private final String TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";

    private final String[] riddles = {
        "I speak without a mouth and hear without ears. I have no body, but I come alive with wind. What am I?",
        "I have cities, but no houses. I have mountains, but no trees. I have water, but no fish. What am I?",
        "What has to be broken before you can use it?"
    };

    private final String[] answers = {"echo", "map", "egg"};
    private final String[] hints = {
        "Hint: It repeats your own voice back to you.",
        "Hint: You hold it in your hand when traveling.",
        "Hint: You eat this for breakfast."
    };

    private int currentLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvLevel = findViewById(R.id.tvLevel);
        tvRiddle = findViewById(R.id.tvRiddle);
        tvHint = findViewById(R.id.tvHint);
        etAnswer = findViewById(R.id.etAnswer);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnHint = findViewById(R.id.btnHint);

        MobileAds.initialize(this, initializationStatus -> {});
        loadRewardedAd();

        loadCurrentLevel();

        btnSubmit.setOnClickListener(v -> checkAnswer());
        btnHint.setOnClickListener(v -> showAdForHint());
    }

    private void loadCurrentLevel() {
        tvLevel.setText("Level " + (currentLevel + 1));
        tvRiddle.setText(riddles[currentLevel]);
        tvHint.setText("");
        etAnswer.setText("");
    }

    private void checkAnswer() {
        String input = etAnswer.getText().toString().trim().toLowerCase();
        if (input.equals(answers[currentLevel])) {
            Toast.makeText(this, "Correct! Next Level Unlocked.", Toast.LENGTH_SHORT).show();
            currentLevel++;
            if (currentLevel < riddles.length) {
                loadCurrentLevel();
            } else {
                tvRiddle.setText("Congratulations! You completed all levels.");
                btnSubmit.setEnabled(false);
                btnHint.setEnabled(false);
            }
        } else {
            Toast.makeText(this, "Wrong answer, try again or use a hint!", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, TEST_AD_UNIT_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                rewardedAd = null;
            }
        });
    }

    private void showAdForHint() {
        if (rewardedAd != null) {
            rewardedAd.show(this, rewardItem -> {
                tvHint.setText(hints[currentLevel]);
                Toast.makeText(MainActivity.this, "Hint unlocked!", Toast.LENGTH_SHORT).show();
            });
            loadRewardedAd();
        } else {
            Toast.makeText(this, "Ad loading, please try again in a few seconds.", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
        }
    }
}
