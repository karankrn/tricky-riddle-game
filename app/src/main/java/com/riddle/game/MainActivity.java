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

    private TextView tvPlayerName, tvScore, tvLevel, tvHearts, tvRiddle, tvHint, tvMascotDialogue;
    private ImageView ivMascot;
    private Button[] optionButtons = new Button[4];
    private Button btnHint, btnSkip, btnPause;
    private LinearLayout layoutMenuOverlay;
    private Button btnResume, btnRestart, btnViewLeaderboard, btnChangeAlias;
    private AdView adViewBanner;

    private int score = 0;
    private int hearts = 3;
    private int currentLevelIndex = 0;
    private String playerName = "Player";

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

        layoutMenuOverlay = findViewById(R.id.layoutMenuOverlay);
        btnResume = findViewById(R.id.btnResume);
        btnRestart = findViewById(R.id.btnRestart);
        btnViewLeaderboard = findViewById(R.id.btnViewLeaderboard);
        btnChangeAlias = findViewById(R.id.btnChangeAlias);

        adViewBanner = findViewById(R.id.adViewBanner);

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

        for (int i = 0; i < optionButtons.length; i++) {
            final int index = i;
            optionButtons[i].setOnClickListener(v -> checkAnswer(index, optionButtons[index]));
        }

        btnHint.setOnClickListener(v -> handleLifeline("hint"));
        btnSkip.setOnClickListener(v -> handleLifeline("skip"));

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

        // 1 - 5: Everyday Fun
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

        // 6 - 10: School & Home Items
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
            "✂️ What has two blades that love to kiss paper and snip shapes?",
            new String[]{"Scissors", "Knife", "Saw", "Pencil"}, 0,
            "💡 Clue: Be careful not to run while holding them!"
        ));

        // 11 - 15: Food & Treats
        riddleList.add(new RiddleItem(
            "🍿 I wear a yellow coat, jump when I get hot, and pop into a fluffy white treat. What am I?",
            new String[]{"Cornflake", "Popcorn", "Marshmallow", "Butter"}, 1,
            "💡 Clue: You eat this crunchy snack at the movie theater!"
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
            "🧅 What wears many layers of skin and makes you cry without hurting you?",
            new String[]{"Garlic", "Onion", "Potato", "Pepper"}, 1,
            "💡 Clue: You chop it in the kitchen for soups and curries!"
        ));
        riddleList.add(new RiddleItem(
            "🍦 I am cold, sweet, and come in flavors like chocolate and berry. If you eat me slowly, I drip! What am I?",
            new String[]{"Juice", "Ice Cream", "Lollipop", "Cupcake"}, 1,
            "💡 Clue: Served in a cone on hot sunny days!"
        ));

        // 16 - 20: Animals & Bugs
        riddleList.add(new RiddleItem(
            "🐸 What green creature can jump higher than a building?",
            new String[]{"Grasshopper", "Frog", "Kangaroo", "All of them (Buildings cannot jump!)"}, 3,
            "💡 Clue: Think carefully... can buildings jump?"
        ));
        riddleList.add(new RiddleItem(
            "🐝 I fly from flower to flower making sweet golden syrup. What am I?",
            new String[]{"Butterfly", "Honeybee", "Dragonfly", "Ladybug"}, 1,
            "💡 Clue: I buzz around the hive and make honey!"
        ));
        riddleList.add(new RiddleItem(
            "🐢 I carry my house everywhere on my back. When I get scared, I tuck inside! What am I?",
            new String[]{"Snail", "Turtle", "Crab", "Armadillo"}, 1,
            "💡 Clue: Known for moving slowly and winning the race against the hare!"
        ));
        riddleList.add(new RiddleItem(
            "🦒 I have four tall legs, two tiny horns, and a neck so long I eat leaves from high treetops. What am I?",
            new String[]{"Elephant", "Giraffe", "Zebra", "Camel"}, 1,
            "💡 Clue: The tallest animal on the savanna!"
        ));
        riddleList.add(new RiddleItem(
            "🦇 I sleep upside down during the day and fly around at night using my ears to see. What am I?",
            new String[]{"Owl", "Bat", "Hawk", "Flying Squirrel"}, 1,
            "💡 Clue: A nocturnal animal often spotted in caves!"
        ));

        // 21 - 25: Nature & Weather
        riddleList.add(new RiddleItem(
            "🌈 What appears after the rain, painted with seven bright colors across the sky?",
            new String[]{"Sun", "Cloud", "Rainbow", "Kite"}, 2,
            "💡 Clue: Violet, Indigo, Blue, Green, Yellow, Orange, Red!"
        ));
        riddleList.add(new RiddleItem(
            "👣 The more of me you take behind you, the more you leave on the ground. What am I?",
            new String[]{"Footsteps", "Breaths", "Minutes", "Pebbles"}, 0,
            "💡 Clue: You see them stamped in mud or beach sand!"
        ));
        riddleList.add(new RiddleItem(
            "☁️ I fly without wings, I cry without eyes. Whenever I pass through, darkness covers the skies. What am I?",
            new String[]{"A Kite", "A Cloud", "An Airplane", "The Moon"}, 1,
            "💡 Clue: It brings rain to the thirsty plants!"
        ));
        riddleList.add(new RiddleItem(
            "🔥 Feed me wood and I live and grow tall. Give me water and I instantly die. What am I?",
            new String[]{"Plant", "Fire", "Engine", "Camp"}, 1,
            "💡 Clue: It crackles, keeps you warm, but you must never touch it!"
        ));
        riddleList.add(new RiddleItem(
            "🌊 What can run forever without ever having legs?",
            new String[]{"A Car", "A River", "A Clock", "Wind"}, 1,
            "💡 Clue: Water continuously flows down to the sea!"
        ));

        // 26 - 30: Tricky Teasers
        riddleList.add(new RiddleItem(
            "👕 What has a neck and arms, but no head and no legs?",
            new String[]{"A Robot", "A Shirt", "A Chair", "A Blanket"}, 1,
            "💡 Clue: You wear it every single day!"
        ));
        riddleList.add(new RiddleItem(
            "🪑 What has four legs, but can never walk even a single step?",
            new String[]{"A Dog", "A Table", "A Horse", "A Stool"}, 1,
            "💡 Clue: You sit around it to eat lunch or do your homework!"
        ));
        riddleList.add(new RiddleItem(
            "🪞 If you smile, I smile. If you frown, I frown. What am I?",
            new String[]{"A Camera", "A Mirror", "A Painting", "A Shadow"}, 1,
            "💡 Clue: Look straight into it when brushing your teeth!"
        ));
        riddleList.add(new RiddleItem(
            "📖 What has a spine, hundreds of pages, but not a single bone?",
            new String[]{"A Skeleton", "A Book", "A Magazine", "A Diary"}, 1,
            "💡 Clue: You open it to read stories and adventures!"
        ));
        riddleList.add(new RiddleItem(
            "👟 What has a tongue, can be tied tightly, but can never speak a word?",
            new String[]{"A Shoe", "A Bell", "A Snake", "A Backpack"}, 0,
            "💡 Clue: You tie its laces before running outside to play!"
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
            tvMascotDialogue.setText("🏆 You are a Master Brain!");
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
        tvMascotDialogue.setText("Can you crack this puzzle, " + playerName + "?");

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
            tvMascotDialogue.setText("🎉 Woohoo! Super smart!");
            Toast.makeText(this, "🌟 Awesome! +100 Points!", Toast.LENGTH_SHORT).show();
            currentLevelIndex++;
            loadCurrentRiddle();
        } else {
            playErrorSound();
            animateShake(clickedBtn);
            hearts--;
            updateHeartsUI();
            tvMascotDialogue.setText("🧐 Oops! Try another guess!");

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
                tvMascotDialogue.setText("💖 Second Chance! Go for it!");
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
        if (toneGenerator != null) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 180);
        }
    }

    private void playErrorSound() {
        if (toneGenerator != null) {
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
                tvMascotDialogue.setText("💡 Here is your secret clue!");
                updateUI();
            } else {
                showAdForLifeline("hint");
            }
        } else if ("skip".equals(type)) {
            if (countSkip > 0) {
                countSkip--;
                currentLevelIndex++;
                tvMascotDialogue.setText("🚀 Rocket skip used!");
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
        AdRequest adRequest = new AdRequest.Builder().build();
        adViewBanner.loadAd(adRequest);
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
