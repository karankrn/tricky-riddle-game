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

    private TextView tvScore, tvLevel, tvRiddle, tvHint;
    private Button[] optionButtons = new Button[4];
    private Button btn5050, btnHint, btnSkip;

    private int score = 0;
    private int currentLevel = 0;

    // 3 Lifelines starting counts
    private int count5050 = 1;
    private int countHint = 1;
    private int countSkip = 1;

    private RewardedAd rewardedAd;
    private final String TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";

    private final String[] riddles = {
        "🗣️ I speak without a mouth and hear without ears. I have no body, but I come alive with wind. What am I?",
        "🗺️ I have cities, but no houses. I have mountains, but no trees. I have water, but no fish. What am I?",
        "🍳 What has to be broken before you can use it?"
    };

    private final String[][] options = {
        {"Echo", "Shadow", "River", "Mirror"},
        {"Globe", "Book", "Map", "Painting"},
        {"Glass", "Egg", "Promise", "Lock"}
    };

    private final int[] correctAnswers = {0, 2, 1};

    private final String[] hints = {
        "💡 Clue: It repeats your own voice back to you!",
        "💡 Clue: You unfold it to explore roads and places!",
        "💡 Clue: Crack it open to make an omelette!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvScore = findViewById(R.id.tvScore);
        tvLevel = findViewById(R.id.tvLevel);
        tvRiddle = findViewById(R.id.tvRiddle);
        tvHint = findViewById(R.id.tvHint);

        optionButtons[0] = findViewById(R.id.btnOption1);
        optionButtons[1] = findViewById(R.id.btnOption2);
        optionButtons[2] = findViewById(R.id.btnOption3);
        optionButtons[3] = findViewById(R.id.btnOption4);

        btn5050 = findViewById(R.id.btn5050);
        btnHint = findViewById(R.id.btnHint);
        btnSkip = findViewById(R.id.btnSkip);

        // Safe ad initialization
        try {
            MobileAds.initialize(this, initializationStatus -> {});
            loadRewardedAd();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < optionButtons.length; i++) {
            final int index = i;
            optionButtons[i].setOnClickListener(v -> checkAnswer(index));
        }

        btn5050.setOnClickListener(v -> handleLifeline("5050"));
        btnHint.setOnClickListener(v -> handleLifeline("hint"));
        btnSkip.setOnClickListener(v -> handleLifeline("skip"));

        updateLifelineButtons();
        loadCurrentLevel();
    }

    private void loadCurrentLevel() {
        tvScore.setText("⭐ Score: " + score);
        tvLevel.setText("Level " + (currentLevel + 1));
        tvRiddle.setText(riddles[currentLevel]);
        tvHint.setText("");

        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText(options[currentLevel][i]);
            optionButtons[i].setVisibility(View.VISIBLE);
            optionButtons[i].setEnabled(true);
        }
    }

    private void checkAnswer(int selectedIndex) {
        if (selectedIndex == correctAnswers[currentLevel]) {
            score += 100;
            Toast.makeText(this, "🌟 Awesome Job! +100 Points!", Toast.LENGTH_SHORT).show();
            advanceLevel();
        } else {
            Toast.makeText(this, "❌ Oops, try another guess or use a lifeline!", Toast.LENGTH_SHORT).show();
        }
    }

    private void advanceLevel() {
        currentLevel++;
        if (currentLevel < riddles.length) {
            loadCurrentLevel();
        } else {
            tvScore.setText("⭐ Final Score: " + score);
            tvRiddle.setText("🎉 Hurray! You solved all the riddles!");
            tvHint.setText("");
            for (Button b : optionButtons) {
                b.setVisibility(View.GONE);
            }
            btn5050.setEnabled(false);
            btnHint.setEnabled(false);
            btnSkip.setEnabled(false);
        }
    }

    private void updateLifelineButtons() {
        btn5050.setText(count5050 > 0 ? "🪄 50:50 (" + count5050 + ")" : "🪄 50:50 (+1 🎬)");
        btnHint.setText(countHint > 0 ? "💡 Hint (" + countHint + ")" : "💡 Hint (+1 🎬)");
        btnSkip.setText(countSkip > 0 ? "🚀 Skip (" + countSkip + ")" : "🚀 Skip (+1 🎬)");
    }

    private void handleLifeline(String type) {
        if ("5050".equals(type)) {
            if (count5050 > 0) {
                count5050--;
                apply5050();
                updateLifelineButtons();
            } else {
                showAdForLifeline("5050");
            }
        } else if ("hint".equals(type)) {
            if (countHint > 0) {
                countHint--;
                tvHint.setText(hints[currentLevel]);
                updateLifelineButtons();
            } else {
                showAdForLifeline("hint");
            }
        } else if ("skip".equals(type)) {
            if (countSkip > 0) {
                countSkip--;
                updateLifelineButtons();
                advanceLevel();
            } else {
                showAdForLifeline("skip");
            }
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
        Collections.shuffle(wrongIndices);
        if (wrongIndices.size() >= 2) {
            optionButtons[wrongIndices.get(0)].setVisibility(View.INVISIBLE);
            optionButtons[wrongIndices.get(1)].setVisibility(View.INVISIBLE);
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

    private void showAdForLifeline(String type) {
        if (rewardedAd != null) {
            rewardedAd.show(this, rewardItem -> {
                if ("5050".equals(type)) count5050++;
                else if ("hint".equals(type)) countHint++;
                else if ("skip".equals(type)) countSkip++;
                updateLifelineButtons();
                Toast.makeText(MainActivity.this, "Lifeline recharged! 🎁", Toast.LENGTH_SHORT).show();
            });
            loadRewardedAd();
        } else {
            Toast.makeText(this, "Ad loading, please try again in a moment!", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
        }
    }
                           }
