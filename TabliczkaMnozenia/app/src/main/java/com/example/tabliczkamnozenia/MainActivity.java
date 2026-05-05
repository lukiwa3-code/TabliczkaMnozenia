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
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends Activity {

    private static final int MIN_TABLE = 1;
    private static final int MAX_TABLE = 10;
    private static final int MIN_MULTIPLIER = 1;
    private static final int MAX_MULTIPLIER = 10;
    private static final int REQUIRED_STREAK = 5;

    private static final int MODE_MENU = 0;
    private static final int MODE_LEARNING = 1;
    private static final int MODE_TEST = 2;

    private static final int TEST_QUESTION_COUNT = 20;
    private static final int TEST_MIN_NUMBER = 1;
    private static final int TEST_MAX_NUMBER = 10;

    private final Random random = new Random();
    private final ArrayList<Integer> currentSeries = new ArrayList<>();
    private final ArrayList<Question> testQuestions = new ArrayList<>();

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

    private TextView testStatusTextView;

    private int currentMode = MODE_MENU;
    private int selectedTable = 0;
    private int currentMultiplier = 0;
    private int currentSeriesIndex = 0;

    private int currentLeftNumber = 0;
    private int currentRightNumber = 0;

    private int currentTestIndex = 0;
    private int currentTestCorrectAnswers = 0;

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
            LinearLayout row = createMenuRow();

            Button button = createMenuButton("Mnożenie przez " + table);
            TextView status = createStatusTextView();

            final int chosenTable = table;
            button.setOnClickListener(v -> startLearningQuiz(chosenTable));

            tableButtons[table] = button;
            statusTextViews[table] = status;

            row.addView(button);
            row.addView(status);
            menuContainer.addView(row);
        }

        addTestButtonToMenu();
    }

    private void addTestButtonToMenu() {
        LinearLayout row = createMenuRow();

        Button button = createMenuButton("Test: 20 losowych zadań");
        TextView status = createStatusTextView();

        button.setOnClickListener(v -> startTest());

        testStatusTextView = status;

        row.addView(button);
        row.addView(status);
        menuContainer.addView(row);
    }

    private LinearLayout createMenuRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);

        return row;
    }

    private Button createMenuButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(18);
        button.setAllCaps(false);
        button.setMinHeight(dp(58));

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        button.setLayoutParams(buttonParams);

        return button;
    }

    private TextView createStatusTextView() {
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

        return status;
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
        currentMode = MODE_MENU;
        selectedTable = 0;
        currentMultiplier = 0;
        currentSeriesIndex = 0;
        currentLeftNumber = 0;
        currentRightNumber = 0;
        currentTestIndex = 0;
        currentTestCorrectAnswers = 0;
        currentSeries.clear();
        testQuestions.clear();

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

        refreshTestStatus();
    }

    private void refreshTestStatus() {
        if (testStatusTextView == null) {
            return;
        }

        int bestScore = prefs.getInt(keyTestBestScore(), -1);
        int lastScore = prefs.getInt(keyTestLastScore(), -1);
        String lastDate = prefs.getString(keyTestLastDate(), "");
        String completedDate = prefs.getString(keyTestCompletedDate(), "");

        if (!completedDate.isEmpty()) {
            testStatusTextView.setText("✅\nzaliczony\n" + completedDate);
            testStatusTextView.setTextColor(Color.rgb(0, 128, 0));
        } else if (lastScore >= 0) {
            testStatusTextView.setText("ostatnio\n" + lastScore + "/20\n" + lastDate);
            testStatusTextView.setTextColor(Color.DKGRAY);
        } else if (bestScore >= 0) {
            testStatusTextView.setText("rekord\n" + bestScore + "/20");
            testStatusTextView.setTextColor(Color.DKGRAY);
        } else {
            testStatusTextView.setText("20 pytań\nz 1×1 do\n10×10");
            testStatusTextView.setTextColor(Color.DKGRAY);
        }
    }

    private void startLearningQuiz(int table) {
        currentMode = MODE_LEARNING;
        selectedTable = table;
        currentMultiplier = 0;
        currentSeriesIndex = 0;
        currentLeftNumber = 0;
        currentRightNumber = 0;
        currentSeries.clear();
        testQuestions.clear();

        subtitleTextView.setVisibility(View.GONE);
        menuContainer.setVisibility(View.GONE);
        quizContainer.setVisibility(View.VISIBLE);

        quizTitleTextView.setText("Mnożenie przez " + table);

        if (isCompleted(table)) {
            showCompletedLearningScreen();
        } else {
            startNewLearningSeries();
        }
    }

    private void showCompletedLearningScreen() {
        String date = getCompletionDate(selectedTable);

        progressTextView.setText("Ten test jest już zaliczony.");
        questionTextView.setText("✅ Zaliczone dnia: " + date);
        resultTextView.setText("Wróć do menu i wybierz inną kategorię albo test 20 zadań.");
        resultTextView.setTextColor(Color.rgb(0, 128, 0));

        answerEditText.setVisibility(View.GONE);
        checkButton.setVisibility(View.GONE);

        nextButton.setVisibility(View.VISIBLE);
        nextButton.setText("Wróć do menu");
        nextButton.setOnClickListener(v -> showMenu());
    }

    private void startNewLearningSeries() {
        currentSeries.clear();
        currentSeriesIndex = 0;

        for (int multiplier = MIN_MULTIPLIER; multiplier <= MAX_MULTIPLIER; multiplier++) {
            if (getStreak(selectedTable, multiplier) < REQUIRED_STREAK) {
                currentSeries.add(multiplier);
            }
        }

        if (currentSeries.isEmpty()) {
            saveCompletionIfNeeded(selectedTable);
            showCompletedLearningScreen();
            return;
        }

        Collections.shuffle(currentSeries, random);
        showCurrentLearningQuestionFromSeries();
    }

    private void showCurrentLearningQuestionFromSeries() {
        if (currentSeries.isEmpty()) {
            startNewLearningSeries();
            return;
        }

        if (currentSeriesIndex >= currentSeries.size()) {
            startNewLearningSeries();
            return;
        }

        currentMultiplier = currentSeries.get(currentSeriesIndex);
        currentLeftNumber = currentMultiplier;
        currentRightNumber = selectedTable;

        prepareQuestionScreen();

        questionTextView.setText(currentLeftNumber + " × " + currentRightNumber + " = ?");

        updateLearningProgressText();
    }

    private void updateLearningProgressText() {
        int learned = countLearnedTasks(selectedTable);
        int streak = getStreak(selectedTable, currentMultiplier);

        String seriesInfo = "Seria: zadanie " + (currentSeriesIndex + 1) + "/" + currentSeries.size();

        progressTextView.setText(
                seriesInfo + "\n" +
                "Nauczone zadania: " + learned + "/10\n" +
                "To zadanie: " + streak + "/" + REQUIRED_STREAK +
                " dobrych odpowiedzi pod rząd"
        );
    }

    private void startTest() {
        currentMode = MODE_TEST;
        selectedTable = 0;
        currentMultiplier = 0;
        currentSeriesIndex = 0;
        currentTestIndex = 0;
        currentTestCorrectAnswers = 0;
        currentSeries.clear();
        testQuestions.clear();

        subtitleTextView.setVisibility(View.GONE);
        menuContainer.setVisibility(View.GONE);
        quizContainer.setVisibility(View.VISIBLE);

        quizTitleTextView.setText("Test: 20 losowych zadań");

        createRandomTestQuestions();
        showCurrentTestQuestion();
    }

    private void createRandomTestQuestions() {
        ArrayList<Question> allQuestions = new ArrayList<>();

        for (int left = TEST_MIN_NUMBER; left <= TEST_MAX_NUMBER; left++) {
            for (int right = TEST_MIN_NUMBER; right <= TEST_MAX_NUMBER; right++) {
                allQuestions.add(new Question(left, right));
            }
        }

        Collections.shuffle(allQuestions, random);

        for (int i = 0; i < TEST_QUESTION_COUNT && i < allQuestions.size(); i++) {
            testQuestions.add(allQuestions.get(i));
        }
    }

    private void showCurrentTestQuestion() {
        if (currentTestIndex >= testQuestions.size()) {
            finishTest();
            return;
        }

        Question question = testQuestions.get(currentTestIndex);
        currentLeftNumber = question.left;
        currentRightNumber = question.right;

        prepareQuestionScreen();

        questionTextView.setText(currentLeftNumber + " × " + currentRightNumber + " = ?");
        updateTestProgressText();
    }

    private void updateTestProgressText() {
        progressTextView.setText(
                "Pytanie: " + (currentTestIndex + 1) + "/" + testQuestions.size() + "\n" +
                "Dobrych odpowiedzi: " + currentTestCorrectAnswers + "/" + testQuestions.size() + "\n" +
                "Zakres: od 1×1 do 10×10"
        );
    }

    private void prepareQuestionScreen() {
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
    }

    private void checkAnswer() {
        if (currentMode == MODE_LEARNING) {
            checkLearningAnswer();
        } else if (currentMode == MODE_TEST) {
            checkTestAnswer();
        }
    }

    private Integer readUserAnswer() {
        String answerText = answerEditText.getText().toString().trim();

        if (answerText.isEmpty()) {
            resultTextView.setText("❌ Wpisz odpowiedź.");
            resultTextView.setTextColor(Color.rgb(190, 0, 0));
            return null;
        }

        try {
            return Integer.parseInt(answerText);
        } catch (NumberFormatException e) {
            resultTextView.setText("❌ To nie wygląda jak liczba. Wpisz sam wynik, np. 25.");
            resultTextView.setTextColor(Color.rgb(190, 0, 0));
            return null;
        }
    }

    private void checkLearningAnswer() {
        if (selectedTable == 0 || currentMultiplier == 0) {
            return;
        }

        Integer userAnswerObject = readUserAnswer();

        if (userAnswerObject == null) {
            return;
        }

        int userAnswer = userAnswerObject;
        int correctAnswer = currentLeftNumber * currentRightNumber;
        hideKeyboard();

        if (userAnswer == correctAnswer) {
            int oldStreak = getStreak(selectedTable, currentMultiplier);
            int newStreak = Math.min(REQUIRED_STREAK, oldStreak + 1);
            saveStreak(selectedTable, currentMultiplier, newStreak);

            resultTextView.setText(
                    "✅ Dobrze! " + currentLeftNumber + "×" + currentRightNumber +
                    " to " + correctAnswer + "."
            );
            resultTextView.setTextColor(Color.rgb(0, 128, 0));
        } else {
            saveStreak(selectedTable, currentMultiplier, 0);

            resultTextView.setText(
                    "❌ Źle, " + currentLeftNumber + "×" + currentRightNumber +
                    " to " + correctAnswer + ", a nie jak podałeś " + userAnswer + "."
            );
            resultTextView.setTextColor(Color.rgb(190, 0, 0));
        }

        updateLearningProgressText();

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
        } else if (isLastQuestionInCurrentLearningSeries()) {
            nextButton.setText("Rozpocznij kolejną serię");
            nextButton.setOnClickListener(v -> startNewLearningSeries());
        } else {
            nextButton.setText("Następne pytanie");
            nextButton.setOnClickListener(v -> {
                currentSeriesIndex++;
                showCurrentLearningQuestionFromSeries();
            });
        }

        nextButton.setVisibility(View.VISIBLE);
    }

    private void checkTestAnswer() {
        if (currentTestIndex >= testQuestions.size()) {
            return;
        }

        Integer userAnswerObject = readUserAnswer();

        if (userAnswerObject == null) {
            return;
        }

        int userAnswer = userAnswerObject;
        int correctAnswer = currentLeftNumber * currentRightNumber;
        hideKeyboard();

        if (userAnswer == correctAnswer) {
            currentTestCorrectAnswers++;

            resultTextView.setText(
                    "✅ Dobrze! " + currentLeftNumber + "×" + currentRightNumber +
                    " to " + correctAnswer + "."
            );
            resultTextView.setTextColor(Color.rgb(0, 128, 0));
        } else {
            resultTextView.setText(
                    "❌ Źle, " + currentLeftNumber + "×" + currentRightNumber +
                    " to " + correctAnswer + ", a nie jak podałeś " + userAnswer + "."
            );
            resultTextView.setTextColor(Color.rgb(190, 0, 0));
        }

        answerEditText.setEnabled(false);
        checkButton.setEnabled(false);

        updateTestProgressText();

        if (currentTestIndex >= testQuestions.size() - 1) {
            nextButton.setText("Pokaż wynik testu");
            nextButton.setOnClickListener(v -> finishTest());
        } else {
            nextButton.setText("Następne pytanie");
            nextButton.setOnClickListener(v -> {
                currentTestIndex++;
                showCurrentTestQuestion();
            });
        }

        nextButton.setVisibility(View.VISIBLE);
    }

    private void finishTest() {
        currentMode = MODE_TEST;
        hideKeyboard();

        saveTestResult(currentTestCorrectAnswers);

        answerEditText.setVisibility(View.GONE);
        checkButton.setVisibility(View.GONE);

        questionTextView.setText(currentTestCorrectAnswers + "/" + TEST_QUESTION_COUNT);
        progressTextView.setText("Test zakończony. Zakres: od 1×1 do 10×10.");

        if (currentTestCorrectAnswers == TEST_QUESTION_COUNT) {
            String date = prefs.getString(keyTestCompletedDate(), "");

            resultTextView.setText(
                    "✅ Perfekcyjnie! Test zaliczony dnia " + date + ".\n" +
                    "Wynik: " + currentTestCorrectAnswers + "/" + TEST_QUESTION_COUNT + "."
            );
            resultTextView.setTextColor(Color.rgb(0, 128, 0));
        } else {
            resultTextView.setText(
                    "❌ Jeszcze nie zaliczone.\n" +
                    "Wynik: " + currentTestCorrectAnswers + "/" + TEST_QUESTION_COUNT + ".\n" +
                    "Spróbuj jeszcze raz. Matematyczny smok już sapie za rogiem."
            );
            resultTextView.setTextColor(Color.rgb(190, 0, 0));
        }

        nextButton.setVisibility(View.VISIBLE);
        nextButton.setText("Wróć do menu");
        nextButton.setOnClickListener(v -> showMenu());
    }

    private void saveTestResult(int score) {
        String today = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date());
        int bestScore = prefs.getInt(keyTestBestScore(), -1);

        SharedPreferences.Editor editor = prefs.edit()
                .putInt(keyTestLastScore(), score)
                .putString(keyTestLastDate(), today);

        if (score > bestScore) {
            editor.putInt(keyTestBestScore(), score);
        }

        if (score == TEST_QUESTION_COUNT && prefs.getString(keyTestCompletedDate(), "").isEmpty()) {
            editor.putString(keyTestCompletedDate(), today);
        }

        editor.apply();
    }

    private boolean isLastQuestionInCurrentLearningSeries() {
        return currentSeriesIndex >= currentSeries.size() - 1;
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

    private String keyTestLastScore() {
        return "test_last_score";
    }

    private String keyTestLastDate() {
        return "test_last_date";
    }

    private String keyTestBestScore() {
        return "test_best_score";
    }

    private String keyTestCompletedDate() {
        return "test_completed_date";
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

    private static class Question {
        final int left;
        final int right;

        Question(int left, int right) {
            this.left = left;
            this.right = right;
        }
    }
}
