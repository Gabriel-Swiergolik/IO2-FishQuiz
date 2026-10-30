package pl.pcz.io2.fishquizfrontend.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import pl.pcz.io2.fishquizdb.LocalDatePersister;

import java.time.LocalDate;

@DatabaseTable(tableName = "flashcards")
public class Flashcard {
    // Nazwy kolumn używane w zapytaniach DAO
    public static final String ID_FIELD = "id";
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

    public Flashcard() {
        this.easinessFactor = 2.5;
        this.nextReviewDate = LocalDate.now();
    }

    public Flashcard(int id, String frontText, String backText, String imagePath) {
        this();
        this.id = id;
        this.frontText = frontText;
        this.backText = backText;
        this.imagePath = imagePath;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFrontText() { return frontText; }
    public void setFrontText(String frontText) { this.frontText = frontText; }

    public String getBackText() { return backText; }
    public void setBackText(String backText) { this.backText = backText; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public LocalDate getNextReviewDate() { return nextReviewDate; }
    public void setNextReviewDate(LocalDate nextReviewDate) { this.nextReviewDate = nextReviewDate; }

    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }

    public int getRepetitions() { return repetitions; }
    public void setRepetitions(int repetitions) { this.repetitions = repetitions; }

    public double getEasinessFactor() { return easinessFactor; }
    public void setEasinessFactor(double easinessFactor) { this.easinessFactor = easinessFactor; }
}