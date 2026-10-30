package pl.pcz.io2.fishquizfrontend.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import pl.pcz.io2.fishquizdb.LocalDatePersister;

import java.time.LocalDate;

/**
 * Fiszka (tabela {@code flashcards}): awers, rewers, opcjonalny obraz i stan algorytmu SM-2.
 *
 * <p>Nowa fiszka ma współczynnik łatwości 2.5 i termin powtórki na dziś.
 */
@DatabaseTable(tableName = "flashcards")
public class Flashcard {
    /** Nazwa kolumny id, używana w zapytaniach DAO. */
    public static final String ID_FIELD = "id";
    /** Nazwa kolumny z datą następnej powtórki, używana w zapytaniach DAO. */
    public static final String NEXT_REVIEW_DATE_FIELD = "nextReviewDate";

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false)
    private String frontText;

    @DatabaseField(canBeNull = false)
    private String backText;

    @DatabaseField
    private String imagePath;

    @DatabaseField(persisterClass = LocalDatePersister.class)
    private LocalDate nextReviewDate;

    @DatabaseField
    private int interval;

    @DatabaseField
    private int repetitions;

    @DatabaseField
    private double easinessFactor;

    /** Pusta fiszka z domyślnym stanem SM-2 (wymagany też przez ORMLite). */
    public Flashcard() {
        this.easinessFactor = 2.5;
        this.nextReviewDate = LocalDate.now();
    }

    /**
     * Fiszka z treścią i domyślnym stanem SM-2.
     * @param id identyfikator; 0 dla fiszki jeszcze niezapisanej
     * @param frontText awers (pytanie)
     * @param backText rewers (odpowiedź)
     * @param imagePath nazwa pliku obrazu w katalogu obrazów albo {@code null}
     */
    public Flashcard(int id, String frontText, String backText, String imagePath) {
        this();
        this.id = id;
        this.frontText = frontText;
        this.backText = backText;
        this.imagePath = imagePath;
    }

    /** @return identyfikator nadany przez bazę; 0 przed zapisem */
    public int getId() { return id; }
    /** @param id identyfikator fiszki */
    public void setId(int id) { this.id = id; }

    /** @return awers (pytanie) */
    public String getFrontText() { return frontText; }
    /** @param frontText awers (pytanie) */
    public void setFrontText(String frontText) { this.frontText = frontText; }

    /** @return rewers (odpowiedź) */
    public String getBackText() { return backText; }
    /** @param backText rewers (odpowiedź) */
    public void setBackText(String backText) { this.backText = backText; }

    /** @return nazwa pliku obrazu w katalogu obrazów albo {@code null}, gdy fiszka nie ma obrazu */
    public String getImagePath() { return imagePath; }
    /** @param imagePath nazwa pliku obrazu albo {@code null} */
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    /** @return data następnej powtórki */
    public LocalDate getNextReviewDate() { return nextReviewDate; }
    /** @param nextReviewDate data następnej powtórki */
    public void setNextReviewDate(LocalDate nextReviewDate) { this.nextReviewDate = nextReviewDate; }

    /** @return bieżący odstęp między powtórkami w dniach (SM-2) */
    public int getInterval() { return interval; }
    /** @param interval odstęp między powtórkami w dniach */
    public void setInterval(int interval) { this.interval = interval; }

    /** @return liczba kolejnych poprawnych powtórek (SM-2) */
    public int getRepetitions() { return repetitions; }
    /** @param repetitions liczba kolejnych poprawnych powtórek */
    public void setRepetitions(int repetitions) { this.repetitions = repetitions; }

    /** @return współczynnik łatwości EF (SM-2, co najmniej 1.3) */
    public double getEasinessFactor() { return easinessFactor; }
    /** @param easinessFactor współczynnik łatwości EF */
    public void setEasinessFactor(double easinessFactor) { this.easinessFactor = easinessFactor; }
}