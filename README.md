# IO2-FishQuiz

System Fiszek: desktopowa aplikacja do nauki z fiszek z powtórkami w odstępach (spaced repetition, SM-2).
Projekt zespołu IO_2 (PCz). Stos: Java 21, JavaFX 21, SQLite przez ORMLite, Maven (wrapper w repo).

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
| Paczka (jar) | `./mvnw package` | `.\mvnw.cmd package` |

## Baza danych
Aplikacja używa pliku SQLite `database.db` tworzonego w katalogu roboczym (plik jest w `.gitignore`).
Testy korzystają z tymczasowej bazy i nie dotykają `database.db`.
