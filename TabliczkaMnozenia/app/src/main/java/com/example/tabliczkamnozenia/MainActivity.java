package com.example.tabliczkamnozenia;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    private static final int MIN_TABLE = 1;
    private static final int MAX_TABLE = 9;
    private static final int MIN_MULTIPLIER = 1;
    private static final int MAX_MULTIPLIER = 10;
    private static final int REQUIRED_STREAK = 5;

    private final Random random = new Random();

    private SharedPreferences prefs;

    private TextView subtitleTextView;
    private LinearLayout menuContainer;
    private LinearLayout quizContainer;

    private TextView quizTitleTextView;
    private TextView progressTextView;
    private TextView questionTextView;
    private EditText answerEditText;
    private Button checkButton;
    private Button nextButton;
    private Button backButton;
    private TextView resultTextView;

    private final Button[] tableButtons = new Button[MAX_TABLE + 1];
    private final TextView[] statusTextViews = new TextView[MAX_TABLE + 1];

    private int selectedTable = 0;
    private int currentMultiplier = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("tabliczka_mnozenia_dane", MODE_PRIVATE);

        bindViews();
        createMenuButtons();
        setupQuizButtons();
        showMenu();
    }

    private void bindViews() {
        subtitleTextView = findViewById(R.id.subtitleTextView);
        menuContainer = findViewById(R.id.menuContainer);
        quizContainer = findViewById(R.id.quizContainer);

        quizTitleTextView = findViewById(R.id.quizTitleTextView);
        progressTextView = findViewById(R.id.progressTextView);
        questionTextView = findViewById(R.id.questionTextView);
        answerEditText = findViewById(R.id.answerEditText);
        checkButton = findViewById(R.id.checkButton);
        nextButton = findViewById(R.id.nextButton);
        backButton = findViewById(R.id.backButton);
        resultTextView = findViewById(R.id.resultTextView);
    }

    private void createMenuButtons() {
        menuContainer.removeAllViews();

        for (int table = MIN_TABLE; table <= MAX_TABLE; table++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            rowParams.setMargins(0, 0, 0, dp(10));
            row.setLayoutParams(rowParams);

            Button button = new Button(this);
            button.setText("Mnożenie przez " + table);
            button.setTextSize(18);
            button.setAllCaps(false);
            button.setMinHeight(dp(58));

            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1
            );
            button.setLayoutParams(buttonParams);

            TextView status = new TextView(this);
            status.setTextSize(14);
            status.setGravity(Gravity.CENTER_VERTICAL);
            status.setSingleLine(false);

            LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                    dp(130),
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            statusParams.setMargins(dp(12), 0, 0, 0);
            status.setLayoutParams(statusParams);

            final int chosenTable = table;
            button.setOnClickListener(v -> startQuiz(chosenTable));

            tableButtons[table] = button;
            statusTextViews[table] = status;

            row.addView(button);
            row.addView(status);
            menuContainer.addView(row);
        }
    }

    private void setupQuizButtons() {
        backButton.setOnClickListener(v -> showMenu());

        checkButton.setOnClickListener(v -> checkAnswer());

        answerEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                checkAnswer();
                return true;
            }
            return false;
        });
    }

    private void showMenu() {
        selectedTable = 0;
        currentMultiplier = 0;

        subtitleTextView.setVisibility(View.VISIBLE);
        menuContainer.setVisibility(View.VISIBLE);
        quizContainer.setVisibility(View.GONE);

        refreshMenuStatuses();
    }

    private void refreshMenuStatuses() {
        for (int table = MIN_TABLE; table <= MAX_TABLE; table++) {
            saveCompletionIfNeeded(table);

            TextView status = statusTextViews[table];

            if (isCompleted(table)) {
                String date = getCompletionDate(table);
                status.setText("✅\nzaliczone\n" + date);
                status.setTextColor(Color.rgb(0, 128, 0));
            } else {
                int learned = countLearnedTasks(table);
                status.setText(learned + "/10\nnauczone");
                status.setTextColor(Color.DKGRAY);
            }
        }
    }

    private void startQuiz(int table) {
        selectedTable = table;

        subtitleTextView.setVisibility(View.GONE);
        menuContainer.setVisibility(View.GONE);
        quizContainer.setVisibility(View.VISIBLE);

        quizTitleTextView.setText("Mnożenie przez " + table);

        if (isCompleted(table)) {
            showCompletedScreen();
        } else {
            showNextQuestion();
        }
    }

    private void showCompletedScreen() {
        String date = getCompletionDate(selectedTable);

        progressTextView.setText("Ten test jest już zaliczony.");
        questionTextView.setText("✅ Zaliczone dnia: " + date);
        resultTextView.setText("Wróć do menu i wybierz inną kategorię.");
        resultTextView.setTextColor(Color.rgb(0, 128, 0));

        answerEditText.setVisibility(View.GONE);
        checkButton.setVisibility(View.GONE);

        nextButton.setVisibility(View.VISIBLE);
        nextButton.setText("Wróć do menu");
        nextButton.setOnClickListener(v -> showMenu());
    }

    private void showNextQuestion() {
        currentMultiplier = chooseRandomNotLearnedMultiplier(selectedTable);

        if (currentMultiplier == -1) {
            saveCompletionIfNeeded(selectedTable);
            showCompletedScreen();
            return;
        }

        answerEditText.setVisibility(View.VISIBLE);
        checkButton.setVisibility(View.VISIBLE);

        answerEditText.setText("");
        answerEditText.setEnabled(true);
        answerEditText.requestFocus();

        checkButton.setEnabled(true);

        nextButton.setVisibility(View.GONE);
        nextButton.setText("Następne pytanie");

        resultTextView.setText("");
        resultTextView.setTextColor(Color.DKGRAY);

        questionTextView.setText(selectedTable + " × " + currentMultiplier + " = ?");

        updateProgressText();
    }

    private void updateProgressText() {
        int learned = countLearnedTasks(selectedTable);
        int streak = getStreak(selectedTable, currentMultiplier);

        progressTextView.setText(
                "Nauczone zadania: " + learned + "/10\n" +
                "To zadanie: " + streak + "/" + REQUIRED_STREAK +
                " dobrych odpowiedzi pod rząd"
        );
    }

    private void checkAnswer() {
        if (selectedTable == 0 || currentMultiplier == 0) {
            return;
        }

        String answerText = answerEditText.getText().toString().trim();

        if (answerText.isEmpty()) {
            resultTextView.setText("❌ Wpisz odpowiedź.");
            resultTextView.setTextColor(Color.rgb(190, 0, 0));
            return;
        }

        int userAnswer;

        try {
            userAnswer = Integer.parseInt(answerText);
        } catch (NumberFormatException e) {
            resultTextView.setText("❌ To nie wygląda jak liczba. Wpisz sam wynik, np. 25.");
            resultTextView.setTextColor(Color.rgb(190, 0, 0));
            return;
        }

        int correctAnswer = selectedTable * currentMultiplier;
        hideKeyboard();

        if (userAnswer == correctAnswer) {
            int oldStreak = getStreak(selectedTable, currentMultiplier);
            int newStreak = Math.min(REQUIRED_STREAK, oldStreak + 1);
            saveStreak(selectedTable, currentMultiplier, newStreak);

            resultTextView.setText(
                    "✅ Dobrze! " + selectedTable + "×" + currentMultiplier +
                    " to " + correctAnswer + "."
            );
            resultTextView.setTextColor(Color.rgb(0, 128, 0));
        } else {
            saveStreak(selectedTable, currentMultiplier, 0);

            resultTextView.setText(
                    "❌ Źle, " + selectedTable + "×" + currentMultiplier +
                    " to " + correctAnswer + ", a nie jak podałeś " + userAnswer + "."
            );
            resultTextView.setTextColor(Color.rgb(190, 0, 0));
        }

        updateProgressText();

        answerEditText.setEnabled(false);
        checkButton.setEnabled(false);

        if (isTableFullyLearned(selectedTable)) {
            saveCompletionIfNeeded(selectedTable);
            String date = getCompletionDate(selectedTable);

            resultTextView.setText(
                    resultTextView.getText().toString() +
                    "\n\n🎉 Cały test zaliczony dnia " + date + "!"
            );

            nextButton.setText("Wróć do menu");
            nextButton.setOnClickListener(v -> showMenu());
        } else {
            nextButton.setText("Następne pytanie");
            nextButton.setOnClickListener(v -> showNextQuestion());
        }

        nextButton.setVisibility(View.VISIBLE);
    }

    private int chooseRandomNotLearnedMultiplier(int table) {
        ArrayList<Integer> notLearned = new ArrayList<>();

        for (int multiplier = MIN_MULTIPLIER; multiplier <= MAX_MULTIPLIER; multiplier++) {
            if (getStreak(table, multiplier) < REQUIRED_STREAK) {
                notLearned.add(multiplier);
            }
        }

        if (notLearned.isEmpty()) {
            return -1;
        }

        return notLearned.get(random.nextInt(notLearned.size()));
    }

    private boolean isTableFullyLearned(int table) {
        for (int multiplier = MIN_MULTIPLIER; multiplier <= MAX_MULTIPLIER; multiplier++) {
            if (getStreak(table, multiplier) < REQUIRED_STREAK) {
                return false;
            }
        }
        return true;
    }

    private int countLearnedTasks(int table) {
        int learned = 0;

        for (int multiplier = MIN_MULTIPLIER; multiplier <= MAX_MULTIPLIER; multiplier++) {
            if (getStreak(table, multiplier) >= REQUIRED_STREAK) {
                learned++;
            }
        }

        return learned;
    }

    private void saveCompletionIfNeeded(int table) {
        if (!isTableFullyLearned(table)) {
            return;
        }

        if (isCompleted(table)) {
            return;
        }

        String today = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());

        prefs.edit()
                .putBoolean(keyCompleted(table), true)
                .putString(keyCompletedDate(table), today)
                .apply();
    }

    private boolean isCompleted(int table) {
        return prefs.getBoolean(keyCompleted(table), false);
    }

    private String getCompletionDate(int table) {
        return prefs.getString(keyCompletedDate(table), "brak daty");
    }

    private int getStreak(int table, int multiplier) {
        return prefs.getInt(keyStreak(table, multiplier), 0);
    }

    private void saveStreak(int table, int multiplier, int streak) {
        prefs.edit()
                .putInt(keyStreak(table, multiplier), streak)
                .apply();
    }

    private String keyStreak(int table, int multiplier) {
        return "streak_" + table + "_" + multiplier;
    }

    private String keyCompleted(int table) {
        return "completed_" + table;
    }

    private String keyCompletedDate(int table) {
        return "completed_date_" + table;
    }

    private void hideKeyboard() {
        View currentFocus = getCurrentFocus();

        if (currentFocus == null) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }

        currentFocus.clearFocus();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
