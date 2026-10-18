package com.riddle.game;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
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

    private TextView tvPlayerName, tvScore, tvLevel, tvHearts, tvRiddle, tvHint, tvMascotDialogue, tvMenuStats;
    private ImageView ivMascot;
    private Button[] optionButtons = new Button[4];
    private Button btnHint, btnSkip, btnPause;
    private LinearLayout layoutMenuOverlay;
    private Button btnResume, btnRestart, btnViewLeaderboard, btnChangeAlias, btnToggleSound, btnQuitGame;
    private AdView adViewBanner;

    private int score = 0;
    private int hearts = 3;
    private int currentLevelIndex = 0;
    private String playerName = "Player";
    private boolean isSoundOn = true;

    private int countHint = 1;
    private int countSkip = 1;

    private ToneGenerator toneGenerator;

    private RewardedAd rewardedAd;
    private InterstitialAd interstitialAd;
    private final String TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917";
    private final String TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712";

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

        // Core Game Views
        tvPlayerName = findViewById(R.id.tvPlayerName);
        tvScore = findViewById(R.id.tvScore);
        tvLevel = findViewById(R.id.tvLevel);
        tvHearts = findViewById(R.id.tvHearts);
        tvRiddle = findViewById(R.id.tvRiddle);
        tvHint = findViewById(R.id.tvHint);
        tvMascotDialogue = findViewById(R.id.tvMascotDialogue);
        ivMascot = findViewById(R.id.ivMascot);

        optionButtons[0] = findViewById(R.id.btnOption1);
        optionButtons[1] = findViewById(R.id.btnOption2);
        optionButtons[2] = findViewById(R.id.btnOption3);
        optionButtons[3] = findViewById(R.id.btnOption4);

        btnHint = findViewById(R.id.btnHint);
        btnSkip = findViewById(R.id.btnSkip);
        btnPause = findViewById(R.id.btnPause);

        // Menu Views
        layoutMenuOverlay = findViewById(R.id.layoutMenuOverlay);
        tvMenuStats = findViewById(R.id.tvMenuStats);
        btnResume = findViewById(R.id.btnResume);
        btnRestart = findViewById(R.id.btnRestart);
        btnViewLeaderboard = findViewById(R.id.btnViewLeaderboard);
        btnChangeAlias = findViewById(R.id.btnChangeAlias);
        btnToggleSound = findViewById(R.id.btnToggleSound);
        btnQuitGame = findViewById(R.id.btnQuitGame);

        adViewBanner = findViewById(R.id.adViewBanner);

        // Sound Generator
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 80);
        } catch (Exception ignored) {}

        loadPlayerAlias();
        setupQuestionBank();

        if (playerName.equals("Player")) {
            promptPlayerAlias(true);
        } else {
            initGame();
        }

        // Ads initialization in background
        new Thread(() -> {
            try {
                MobileAds.initialize(MainActivity.this, initializationStatus -> {});
                runOnUiThread(() -> {
                    loadInterstitialAd();
                    loadRewardedAd();
                    loadBannerAd();
                });
            } catch (Exception ignored) {}
        }).start();

        // Option click handling
        for (int i = 0; i < optionButtons.length; i++) {
            final int index = i;
            optionButtons[i].setOnClickListener(v -> checkAnswer(index, optionButtons[index]));
        }

        // Lifelines
        btnHint.setOnClickListener(v -> handleLifeline("hint"));
        btnSkip.setOnClickListener(v -> handleLifeline("skip"));

        // Pause Menu Handlers
        btnPause.setOnClickListener(v -> openPauseMenu());
        btnResume.setOnClickListener(v -> layoutMenuOverlay.setVisibility(View.GONE));

        btnToggleSound.setOnClickListener(v -> {
            isSoundOn = !isSoundOn;
            btnToggleSound.setText(isSoundOn ? "🔊 Sound: ON" : "🔇 Sound: OFF");
            Toast.makeText(this, isSoundOn ? "Sound Enabled!" : "Sound Muted!", Toast.LENGTH_SHORT).show();
        });

        btnRestart.setOnClickListener(v -> {
            layoutMenuOverlay.setVisibility(View.GONE);
            restartGameFromBeginning();
        });

        btnViewLeaderboard.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LeaderboardActivity.class));
        });

        btnChangeAlias.setOnClickListener(v -> promptPlayerAlias(false));
        btnQuitGame.setOnClickListener(v -> finish());
    }

    private void openPauseMenu() {
        StringBuilder heartStr = new StringBuilder();
        for (int i = 0; i < hearts; i++) heartStr.append("❤️");
        for (int i = hearts; i < 3; i++) heartStr.append("🖤");

        if (tvMenuStats != null) {
            tvMenuStats.setText("Level " + (currentLevelIndex + 1) + " • Score: " + score + " • " + heartStr);
        }
        layoutMenuOverlay.setVisibility(View.VISIBLE);
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
        riddleList.add(new RiddleItem(
            "🪙 What has a head and a tail but no body?",
            new String[]{"A Coin", "A Snake", "A Worm", "A Comet"}, 0,
            "💡 Clue: You flip it to make a decision!"
        ));
        riddleList.add(new RiddleItem(
            "✏️ What gets shorter the more it talks and works on paper?",
            new String[]{"Eraser", "Pencil", "Ruler", "Crayon"}, 1,
            "💡 Clue: You sharpen it when it gets dull!"
        ));
        riddleList.add(new RiddleItem(
            "🚪 What has many keys but can never open a single door?",
            new String[]{"Piano", "Lockbox", "Keychain", "Clock"}, 0,
            "💡 Clue: You press its keys to play sweet music!"
        ));
        riddleList.add(new RiddleItem(
            "⏰ What has two hands and a face, but no arms or legs?",
            new String[]{"Robot", "Doll", "Clock", "Mirror"}, 2,
            "💡 Clue: It ticks all day long on the wall!"
        ));
        riddleList.add(new RiddleItem(
            "🧽 What is full of holes but still holds water?",
            new String[]{"Sieve", "Sponge", "Net", "Bucket"}, 1,
            "💡 Clue: You use it to wash dishes or clean the bath!"
        ));
        riddleList.add(new RiddleItem(
            "🍌 What fruit wears a yellow jacket that you peel before you eat?",
            new String[]{"Lemon", "Banana", "Pineapple", "Mango"}, 1,
            "💡 Clue: Monkeys love to eat this fruit!"
        ));
        riddleList.add(new RiddleItem(
            "🍕 What is round like a ball, sliced like a triangle, and comes in a square box?",
            new String[]{"Pancake", "Pizza", "Cake", "Pie"}, 1,
            "💡 Clue: Covered in gooey cheese and your favorite toppings!"
        ));
        riddleList.add(new RiddleItem(
            "🐢 I carry my house everywhere on my back. What am I?",
            new String[]{"Snail", "Turtle", "Crab", "Armadillo"}, 1,
            "💡 Clue: Moves slowly and won the race against the hare!"
        ));
        riddleList.add(new RiddleItem(
            "🌈 What appears after the rain with seven bright colors?",
            new String[]{"Sun", "Cloud", "Rainbow", "Kite"}, 2,
            "💡 Clue: Red, Orange, Yellow, Green, Blue, Indigo, Violet!"
        ));
        riddleList.add(new RiddleItem(
            "🎈 Fill me with air and I float up high, but poke me and I go POP! What am I?",
            new String[]{"A Balloon", "A Kite", "A Cloud", "A Bubble"}, 0,
            "💡 Clue: Tied with ribbons at birthday parties!"
        ));
        riddleList.add(new RiddleItem(
            "☂️ What goes up into the air the moment rain comes down?",
            new String[]{"A Cloud", "An Umbrella", "A Rainbow", "The Sun"}, 1,
            "💡 Clue: You hold it over your head to stay dry!"
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
        initGame();
    }

    private void loadCurrentRiddle() {
        if (currentLevelIndex >= riddleList.size()) {
            saveScore(score);
            tvRiddle.setText("🎉 Hurray " + playerName + "! You solved every riddle!");
            tvHint.setText("");
            if (tvMascotDialogue != null) tvMascotDialogue.setText("🏆 You are a Master Brain!");
            for (Button b : optionButtons) b.setVisibility(View.GONE);
            btnHint.setEnabled(false);
            btnSkip.setEnabled(false);
            playSuccessSound();
            return;
        }

        RiddleItem item = riddleList.get(currentLevelIndex);
        tvLevel.setText("🏆 Level " + (currentLevelIndex + 1));
        tvRiddle.setText(item.question);
        tvHint.setText("");
        if (tvMascotDialogue != null) {
            tvMascotDialogue.setText("Can you crack this puzzle, " + playerName + "?");
        }

        for (int i = 0; i < 4; i++) {
            optionButtons[i].setText(item.options[i]);
            optionButtons[i].setVisibility(View.VISIBLE);
            optionButtons[i].setEnabled(true);
        }
        updateUI();
    }

    private void checkAnswer(int selectedIndex, Button clickedBtn) {
        RiddleItem current = riddleList.get(currentLevelIndex);
        if (selectedIndex == current.correctIndex) {
            playSuccessSound();
            score += 100;
            saveScore(score);
            animateScorePulse();
            animateMascotBounce();
            if (tvMascotDialogue != null) tvMascotDialogue.setText("🎉 Woohoo! Super smart!");
            Toast.makeText(this, "🌟 Awesome! +100 Points!", Toast.LENGTH_SHORT).show();
            currentLevelIndex++;
            loadCurrentRiddle();
        } else {
            playErrorSound();
            animateShake(clickedBtn);
            hearts--;
            updateHeartsUI();
            if (tvMascotDialogue != null) tvMascotDialogue.setText("🧐 Oops! Try another guess!");

            if (hearts <= 0) {
                showReviveOrGameOverDialog();
            } else {
                Toast.makeText(this, "❌ Ouch! Lost 1 Heart!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showReviveOrGameOverDialog() {
        new AlertDialog.Builder(this)
            .setTitle("💔 Out of Hearts!")
            .setMessage("Watch a short video to revive with 2 Hearts and protect your score!")
            .setCancelable(false)
            .setPositiveButton("📺 Revive (Watch Ad)", (dialog, which) -> showAdToRevive())
            .setNegativeButton("Restart from Level 1", (dialog, which) -> restartGameFromBeginning())
            .show();
    }

    private void showAdToRevive() {
        if (rewardedAd != null) {
            rewardedAd.show(this, rewardItem -> {
                hearts = 2;
                updateHeartsUI();
                if (tvMascotDialogue != null) tvMascotDialogue.setText("💖 Second Chance! Go for it!");
                Toast.makeText(MainActivity.this, "💖 Revived with 2 Hearts!", Toast.LENGTH_SHORT).show();
            });
            loadRewardedAd();
        } else {
            restartGameFromBeginning();
            loadRewardedAd();
        }
    }

    private void animateShake(View view) {
        TranslateAnimation shake = new TranslateAnimation(0, 15, 0, 0);
        shake.setDuration(400);
        shake.setInterpolator(new CycleInterpolator(4));
        view.startAnimation(shake);
    }

    private void animateScorePulse() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tvScore, "scaleX", 1.0f, 1.25f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tvScore, "scaleY", 1.0f, 1.25f, 1.0f);
        scaleX.setDuration(350);
        scaleY.setDuration(350);
        scaleX.start();
        scaleY.start();
    }

    private void animateMascotBounce() {
        if (ivMascot != null) {
            ObjectAnimator bounce = ObjectAnimator.ofFloat(ivMascot, "translationY", 0f, -18f, 0f);
            bounce.setDuration(300);
            bounce.start();
        }
    }

    private void playSuccessSound() {
        if (isSoundOn && toneGenerator != null) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 180);
        }
    }

    private void playErrorSound() {
        if (isSoundOn && toneGenerator != null) {
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 250);
        }
    }

    private void updateUI() {
        tvPlayerName.setText("👤 Hero: " + playerName);
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
                if (tvMascotDialogue != null) tvMascotDialogue.setText("💡 Here is your secret clue!");
                updateUI();
            } else {
                showAdForLifeline("hint");
            }
        } else if ("skip".equals(type)) {
            if (countSkip > 0) {
                countSkip--;
                currentLevelIndex++;
                if (tvMascotDialogue != null) tvMascotDialogue.setText("🚀 Rocket skip used!");
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

        builder.setPositiveButton("Let's Play!", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                playerName = name;
                SharedPreferences.Editor editor = getSharedPreferences("TrickyRiddlePrefs", MODE_PRIVATE).edit();
                editor.putString("player_alias", playerName);
                editor.apply();
                tvPlayerName.setText("👤 Hero: " + playerName);
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

    private void loadBannerAd() {
        if (adViewBanner != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adViewBanner.loadAd(adRequest);
        }
    }

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

    @Override
    protected void onDestroy() {
        if (adViewBanner != null) {
            adViewBanner.destroy();
        }
        if (toneGenerator != null) {
            toneGenerator.release();
        }
        super.onDestroy();
    }
}
