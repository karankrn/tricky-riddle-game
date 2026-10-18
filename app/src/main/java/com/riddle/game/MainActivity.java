package com.riddle.game;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvPlayerName, tvScore, tvLevel, tvHearts, tvRiddle, tvHint;
    private Button[] optionButtons = new Button[4];
    private Button btnHint, btnSkip, btnPause;
    private LinearLayout layoutMenuOverlay;
    private Button btnResume, btnRestart, btnViewLeaderboard, btnChangeAlias;

    private int score = 0;
    private int hearts = 3;
    private int currentLevelIndex = 0;
    private String playerName = "Player";

    private int countHint = 1;
    private int countSkip = 1;

    private RewardedAd rewardedAd;
    private InterstitialAd interstitialAd;
    private final String TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917";
    private final String TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";

    // Question structure
    private static class RiddleItem {
        String question;
        String[] options;
        int correctIndex;
        String hint;

        RiddleItem(String question, String[] options, int correctIndex, String hint) {
            this.question = question;
            this.options = options;
            this.correctIndex = correctIndex;
            this.hint = hint;
        }
    }

    private List<RiddleItem> riddleList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind Views
        tvPlayerName = findViewById(R.id.tvPlayerName);
        tvScore = findViewById(R.id.tvScore);
        tvLevel = findViewById(R.id.tvLevel);
        tvHearts = findViewById(R.id.tvHearts);
        tvRiddle = findViewById(R.id.tvRiddle);
        tvHint = findViewById(R.id.tvHint);

        optionButtons[0] = findViewById(R.id.btnOption1);
        optionButtons[1] = findViewById(R.id.btnOption2);
        optionButtons[2] = findViewById(R.id.btnOption3);
        optionButtons[3] = findViewById(R.id.btnOption4);

        btnHint = findViewById(R.id.btnHint);
        btnSkip = findViewById(R.id.btnSkip);
        btnPause = findViewById(R.id.btnPause);

        layoutMenuOverlay = findViewById(R.id.layoutMenuOverlay);
        btnResume = findViewById(R.id.btnResume);
        btnRestart = findViewById(R.id.btnRestart);
        btnViewLeaderboard = findViewById(R.id.btnViewLeaderboard);
        btnChangeAlias = findViewById(R.id.btnChangeAlias);

        loadPlayerAlias();
        setupQuestionBank();

        // Check if alias is missing (first launch)
        if (playerName.equals("Player")) {
            promptPlayerAlias(true);
        } else {
            initGame();
        }

        // Ads initialization & start ad
        new Thread(() -> {
            try {
                MobileAds.initialize(MainActivity.this, initializationStatus -> {});
                runOnUiThread(() -> {
                    loadInterstitialAd();
                    loadRewardedAd();
                });
            } catch (Exception ignored) {}
        }).start();

        // Option click handlers
        for (int i = 0; i < optionButtons.length; i++) {
            final int index = i;
            optionButtons[i].setOnClickListener(v -> checkAnswer(index));
        }

        // Lifelines
        btnHint.setOnClickListener(v -> handleLifeline("hint"));
        btnSkip.setOnClickListener(v -> handleLifeline("skip"));

        // Pause Menu Handlers
        btnPause.setOnClickListener(v -> layoutMenuOverlay.setVisibility(View.VISIBLE));
        btnResume.setOnClickListener(v -> layoutMenuOverlay.setVisibility(View.GONE));
        btnRestart.setOnClickListener(v -> {
            layoutMenuOverlay.setVisibility(View.GONE);
            restartGameFromBeginning();
        });
        btnViewLeaderboard.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LeaderboardActivity.class));
        });
        btnChangeAlias.setOnClickListener(v -> promptPlayerAlias(false));
    }

    private void setupQuestionBank() {
        riddleList.clear();
        riddleList.add(new RiddleItem(
            "🗣️ I speak without a mouth and hear without ears. I have no body, but I come alive with wind. What am I?",
            new String[]{"Echo", "Shadow", "River", "Mirror"}, 0,
            "💡 Clue: It repeats your own voice back to you!"
        ));
        riddleList.add(new RiddleItem(
            "🗺️ I have cities, but no houses. I have mountains, but no trees. I have water, but no fish. What am I?",
            new String[]{"Globe", "Book", "Map", "Painting"}, 2,
            "💡 Clue: You unfold it to explore roads and places!"
        ));
        riddleList.add(new RiddleItem(
            "🍳 What has to be broken before you can use it?",
            new String[]{"Glass", "Egg", "Promise", "Lock"}, 1,
            "💡 Clue: Crack it open to make an omelette!"
        ));
        riddleList.add(new RiddleItem(
            "🕯️ I am tall when I am young, and I am short when I am old. What am I?",
            new String[]{"Pencil", "Candle", "Tree", "Giraffe"}, 1,
            "💡 Clue: It melts and gives light!"
        ));
    }

    private void initGame() {
        Collections.shuffle(riddleList);
        currentLevelIndex = 0;
        score = 0;
        hearts = 3;
        countHint = 1;
        countSkip = 1;
        updateUI();
        loadCurrentRiddle();
    }

    private void restartGameFromBeginning() {
        Toast.makeText(this, "Starting over!", Toast.LENGTH_SHORT).show();
        initGame();
    }

    private void loadCurrentRiddle() {
        if (currentLevelIndex >= riddleList.size()) {
            saveScore(score);
            tvRiddle.setText("🎉 Congratulations " + playerName + "! You beat all riddles!");
            tvHint.setText("");
            for (Button b : optionButtons) b.setVisibility(View.GONE);
            btnHint.setEnabled(false);
            btnSkip.setEnabled(false);
            return;
        }

        RiddleItem item = riddleList.get(currentLevelIndex);
        tvLevel.setText("Level " + (currentLevelIndex + 1));
        tvRiddle.setText(item.question);
        tvHint.setText("");

        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText(item.options[i]);
            optionButtons[i].setVisibility(View.VISIBLE);
            optionButtons[i].setEnabled(true);
        }
        updateUI();
    }

    private void checkAnswer(int selectedIndex) {
        RiddleItem current = riddleList.get(currentLevelIndex);
        if (selectedIndex == current.correctIndex) {
            score += 100;
            saveScore(score);
            Toast.makeText(this, "🌟 Correct! +100", Toast.LENGTH_SHORT).show();
            currentLevelIndex++;
            loadCurrentRiddle();
        } else {
            hearts--;
            updateHeartsUI();
            if (hearts <= 0) {
                showGameOverDialog();
            } else {
                Toast.makeText(this, "❌ Wrong! Lost 1 Heart!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showGameOverDialog() {
        new AlertDialog.Builder(this)
            .setTitle("💔 Game Over")
            .setMessage("You lost all 3 hearts! The game will restart with randomized riddles.")
            .setCancelable(false)
            .setPositiveButton("Try Again", (dialog, which) -> restartGameFromBeginning())
            .show();
    }

    private void updateUI() {
        tvPlayerName.setText("👤 " + playerName);
        tvScore.setText("⭐ Score: " + score);
        updateHeartsUI();
        btnHint.setText(countHint > 0 ? "💡 Hint (" + countHint + ")" : "💡 Hint (+1 🎬)");
        btnSkip.setText(countSkip > 0 ? "🚀 Skip (" + countSkip + ")" : "🚀 Skip (+1 🎬)");
    }

    private void updateHeartsUI() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hearts; i++) sb.append("❤️");
        for (int i = hearts; i < 3; i++) sb.append("🖤");
        tvHearts.setText(sb.toString());
    }

    private void handleLifeline(String type) {
        if ("hint".equals(type)) {
            if (countHint > 0) {
                countHint--;
                tvHint.setText(riddleList.get(currentLevelIndex).hint);
                updateUI();
            } else {
                showAdForLifeline("hint");
            }
        } else if ("skip".equals(type)) {
            if (countSkip > 0) {
                countSkip--;
                currentLevelIndex++;
                loadCurrentRiddle();
            } else {
                showAdForLifeline("skip");
            }
        }
    }

    private void promptPlayerAlias(boolean startAfter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Your Hero Name");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("e.g. BrainyKid");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                playerName = name;
                SharedPreferences.Editor editor = getSharedPreferences("TrickyRiddlePrefs", MODE_PRIVATE).edit();
                editor.putString("player_alias", playerName);
                editor.apply();
                tvPlayerName.setText("👤 " + playerName);
            }
            if (startAfter) {
                showStartInterstitial();
                initGame();
            }
        });

        if (!startAfter) {
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        }
        builder.setCancelable(!startAfter);
        builder.show();
    }

    private void loadPlayerAlias() {
        SharedPreferences prefs = getSharedPreferences("TrickyRiddlePrefs", MODE_PRIVATE);
        playerName = prefs.getString("player_alias", "Player");
    }

    private void saveScore(int currentScore) {
        SharedPreferences prefs = getSharedPreferences("TrickyRiddlePrefs", MODE_PRIVATE);
        int topScore = prefs.getInt("high_score", 0);
        if (currentScore > topScore) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("high_score", currentScore);
            editor.putString("top_player", playerName);
            editor.apply();
        }
    }

    // Google AdMob Rewarded and Interstitial implementation
    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, TEST_INTERSTITIAL_ID, adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd ad) {
                interstitialAd = ad;
            }
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                interstitialAd = null;
            }
        });
    }

    private void showStartInterstitial() {
        if (interstitialAd != null) {
            interstitialAd.show(MainActivity.this);
            interstitialAd = null;
        }
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, TEST_REWARDED_ID, adRequest, new RewardedAdLoadCallback() {
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
                if ("hint".equals(type)) countHint++;
                else if ("skip".equals(type)) countSkip++;
                updateUI();
                Toast.makeText(MainActivity.this, "Lifeline recharged! 🎁", Toast.LENGTH_SHORT).show();
            });
            loadRewardedAd();
        } else {
            Toast.makeText(this, "Ad is loading, try again in a few seconds!", Toast.LENGTH_SHORT).show();
            loadRewardedAd();
        }
    }
        }
