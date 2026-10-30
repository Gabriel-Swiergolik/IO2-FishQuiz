# IO2-FishQuiz

[![Build](https://github.com/Gabriel-Swiergolik/IO2-FishQuiz/actions/workflows/build.yml/badge.svg)](https://github.com/Gabriel-Swiergolik/IO2-FishQuiz/actions/workflows/build.yml)

System Fiszek: desktopowa aplikacja do nauki z fiszek z powtórkami w odstępach (spaced repetition, SM-2).
Projekt zespołu IO_2 (PCz). Stos: Java 21, JavaFX 21 (FXML), SQLite przez ORMLite, Maven (wrapper w repo).

## Funkcje
- **Dodawanie fiszek**: awers, rewers i opcjonalny obrazek (png, jpg, jpeg, gif, bmp). Edycja istniejącej fiszki
  nie zeruje jej postępu w powtórkach.
- **Przeglądanie**: tabela fiszek z wyszukiwaniem, sortowaniem, edycją (przycisk lub dwuklik) i usuwaniem
  (z potwierdzeniem, także klawiszem Delete).
- **Import i eksport CSV** (format poniżej) z podsumowaniem pominiętych wierszy.
- **Quiz SM-2**: powtórka fiszek na dziś, ocena odpowiedzi 0–5, wyliczenie następnej powtórki i podsumowanie sesji.
- **Zestawy fiszek**: okno „Zarządzaj zestawami…” w zakładce „Quiz” (tworzenie, zmiana nazwy, usuwanie,
  przypisywanie fiszek). Quiz można uruchomić dla jednego zestawu: tylko fiszki do powtórki albo wszystkie
  (pole „Tylko fiszki do powtórki”). Usunięcie zestawu nie usuwa fiszek.
- Operacje na bazie i plikach działają w tle; błędy pokazuje okno z opisem po polsku i szczegółami do rozwinięcia.

## Wymagania
- **JDK 21** (lub nowszy). Maven nie jest potrzebny, używamy wrappera `mvnw`.
- `JAVA_HOME` musi wskazywać na JDK 21. Starsze JDK (np. 17) zatrzymują build komunikatem
  `FishQuiz wymaga JDK 21 lub nowszego`.

Ustawienie `JAVA_HOME` dla bieżącej sesji (podmień ścieżkę na swoją instalację):

```bash
# bash / Git Bash
export JAVA_HOME="/c/Program Files/Java/jdk-21"
```

```powershell
# PowerShell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
```

## Polecenia

| Zadanie | bash | PowerShell |
|---------|------|------------|
| Uruchomienie aplikacji | `./mvnw clean javafx:run` | `.\mvnw.cmd clean javafx:run` |
| Kompilacja | `./mvnw compile` | `.\mvnw.cmd compile` |
| Testy (wszystkie) | `./mvnw test` | `.\mvnw.cmd test` |
| Jeden test | `./mvnw test -Dtest=Klasa#metoda` | `.\mvnw.cmd test "-Dtest=Klasa#metoda"` |
| Pełny build jak w CI | `./mvnw -B verify` | `.\mvnw.cmd -B verify` |
| Paczka (jar) | `./mvnw package` | `.\mvnw.cmd package` |

Testy nie potrzebują ekranu (nie uruchamiają JavaFX) i korzystają z tymczasowej bazy.

## Dane aplikacji
Oba miejsca są liczone od **katalogu roboczego** (przy `javafx:run` to katalog projektu) i są w `.gitignore`:
- `database.db`: plik SQLite z fiszkami, zestawami i stanem SM-2. Tabele powstają przy pierwszym uruchomieniu.
- `images/`: kopie obrazków dodanych do fiszek (nazwy UUID). W bazie zapisana jest sama nazwa pliku.

Testy nie dotykają `database.db` ani `images/`.

## Format CSV
- Kodowanie UTF-8; eksport dopisuje na początku BOM, import go pomija.
- Pierwszy wiersz to nagłówek `front,back,imagePath`; wiersze kończą się CRLF.
- Cytowanie wg RFC 4180: pole z `,`, `"` lub nową linią jest w cudzysłowach, a `"` w środku jest podwojony.
- `imagePath` to nazwa pliku w `images/` (same obrazki nie są eksportowane); puste pole = brak obrazka.
  Postęp SM-2 i zestawy nie są eksportowane.
- Import jest pobłażliwy: przyjmuje też końce linii LF, brak nagłówka i brak kolumny `imagePath`. Błędny wiersz
  (zła liczba pól, pusty awers lub rewers, źle postawiony cudzysłów) jest pomijany, a reszta pliku się importuje.

```csv
front,back,imagePath
kot,cat,
"Stolica Polski, miasto",Warszawa,
```

Excel z polskimi ustawieniami oczekuje separatora `;`, więc plik najlepiej otworzyć przez
Dane → Z tekstu/CSV (LibreOffice sam pyta o separator).

## CI
GitHub Actions (`.github/workflows/build.yml`) uruchamia `./mvnw -B verify` (JDK 21, Temurin) przy każdym pushu
i pull requeście do `develop` i `main`.
