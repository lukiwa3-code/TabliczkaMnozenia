# Tabliczka mnożenia — Android

Prosta aplikacja Android w Java + XML.

## Co robi aplikacja

- Ma przyciski: Mnożenie przez 1, 2, 3 ... 9.
- Po wejściu w kategorię pokazuje losowe pytania, np. `3 × 7 = ?`.
- Zadanie jest nauczone, gdy użytkownik odpowie dobrze 5 razy pod rząd.
- Błąd zeruje licznik dla tego konkretnego działania.
- Wyniki i daty zaliczeń zapisują się w `SharedPreferences`.
- Po ponownym otwarciu aplikacji nadal widać zaliczone testy.

## Jak uruchomić najłatwiej

1. Otwórz Android Studio.
2. Utwórz nowy projekt: **Empty Views Activity** albo zwykły pusty projekt Android.
3. Język wybierz: **Java**.
4. Nazwa projektu: `TabliczkaMnozenia`.
5. Package name: `com.example.tabliczkamnozenia`.
6. Wklej pliki z tego repozytorium w te same miejsca.
7. Uruchom aplikację zielonym przyciskiem **Run**.

## Ważne pliki

- `app/src/main/java/com/example/tabliczkamnozenia/MainActivity.java`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle`
