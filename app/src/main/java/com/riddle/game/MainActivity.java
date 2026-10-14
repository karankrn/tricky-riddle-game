package com.riddle.game;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvRiddle, tvLevel, tvHint;
    private Button[] optionButtons = new Button[4];
    private Button btn5050, btnHint;

    private RewardedAd rewardedAd;
    private final String TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";

    private final String[] riddles = {
        "I speak without a mouth and hear without ears. I have no body, but I come alive with wind. What am I?",
        "I have cities, but no houses. I have mountains, but no trees. I have water, but no fish. What am I?",
        "What has to be broken before you can use it?"
    };

    // 4 choices for each question
    private final String[][] options = {
        {"Echo", "Shadow", "River", "Mirror"},
        {"Globe", "Book", "Map", "Painting"},
        {"Glass", "Egg", "Promise", "Lock"}
    };

    // Zero-based index of the correct answer: 0 -> Echo, 2 -> Map, 1 -> Egg
    private final int[] correctAnswers = {0, 2, 1};

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

        optionButtons[0] = findViewById(R.id.btnOption1);
        optionButtons[1] = findViewById(R.id.btnOption2);
        optionButtons[2] = findViewById(R.id.btnOption3);
        optionButtons[3] = findViewById(R.id.btnOption4);

        btn5050 = findViewById(R.id.btn5050);
        btnHint = findViewById(R.id.btnHint);

        MobileAds.initialize(this, initializationStatus -> {});
        loadRewardedAd();

        // Assign click listener to each option button
        for (int i = 0; i < optionButtons.length; i++) {
            final int selectedIndex = i;
            optionButtons[i].setOnClickListener(v -> checkAnswer(selectedIndex));
        }

        btn5050.setOnClickListener(v -> showAdForReward("5050"));
        btnHint.setOnClickListener(v -> showAdForReward("hint"));

        loadCurrentLevel();
    }

    private void loadCurrentLevel() {
        tvLevel.setText("Level " + (currentLevel + 1));
        tvRiddle.setText(riddles[currentLevel]);
        tvHint.setText("");

        // Reset and populate option buttons
        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText(options[currentLevel][i]);
            optionButtons[i].setVisibility(View.VISIBLE);
            optionButtons[i].setEnabled(true);
        }

        btn5050.setEnabled(true);
    }

    private void checkAnswer(int selectedIndex) {
        if (selectedIndex == correctAnswers[currentLevel]) {
            Toast.makeText(this, "Correct! Next Level Unlocked.", Toast.LENGTH_SHORT).show();
            currentLevel++;
            if (currentLevel < riddles.length) {
                loadCurrentLevel();
            } else {
                tvRiddle.setText("Congratulations! You completed all levels.");
                tvHint.setText("");
                for (Button btn : optionButtons) {
                    btn.setVisibility(View.GONE);
                }
                btn5050.setEnabled(false);
                btnHint.setEnabled(false);
            }
        } else {
            Toast.makeText(this, "Wrong answer, try again or use a lifeline!", Toast.LENGTH_SHORT).show();
        }
    }

    private void apply5050() {
        int correctIndex = correctAnswers[currentLevel];
        List<Integer> wrongIndices = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            if (i != correctIndex && optionButtons[i].getVisibility() == View.VISIBLE) {
                wrongIndices.add(i);
            }
        }

        // Hide 2 random wrong options
        Collections.shuffle(wrongIndices);
        if (wrongIndices.size() >= 2) {
            optionButtons[wrongIndices.get(0)].setVisibility(View.INVISIBLE);
            optionButtons[wrongIndices.get(1)].setVisibility(View.INVISIBLE);
        }
        btn5050.setEnabled(false);
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

    private void showAdForReward(String type) {
        if (rewardedAd != null) {
            rewardedAd.show(this, rewardItem -> {
                if ("5050".equals(type)) {
                    apply5050();
                    Toast.makeText(MainActivity.this, "50:50 Lifeline applied!", Toast.LENGTH_SHORT).show();
                } else if ("hint".equals(type)) {
                    tvHint.setText(hints[currentLevel]);
                    Toast.makeText(MainActivity.this, "Hint unlocked!", Toast.LENGTH_SHORT).show();
                }
            });
            loadRewardedAd();
        } else {
            Toast.makeText(this, "Ad is loading, please try again in a few seconds.", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
        }
    }
    }
