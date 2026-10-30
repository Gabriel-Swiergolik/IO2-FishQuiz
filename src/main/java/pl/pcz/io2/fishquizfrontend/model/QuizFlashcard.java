package pl.pcz.io2.fishquizfrontend.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Tabela łącząca quiz z fiszkami ({@code quiz_flashcards}). ORMLite nie ma złożonych kluczy, więc jest osobne id,
 * a para (quizId, flashcardId) jest unikalna.
 */
@DatabaseTable(tableName = "quiz_flashcards")
public class QuizFlashcard {
    /** Nazwa kolumny id, używana w zapytaniach DAO. */
    public static final String ID_FIELD = "id";
    /** Nazwa kolumny z id zestawu, używana w zapytaniach DAO. */
    public static final String QUIZ_ID_FIELD = "quizId";
    /** Nazwa kolumny z id fiszki, używana w zapytaniach DAO. */
    public static final String FLASHCARD_ID_FIELD = "flashcardId";
    /** Nazwa kolumny z pozycją fiszki w zestawie, używana w zapytaniach DAO. */
    public static final String ORDER_INDEX_FIELD = "orderIndex";

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false, uniqueCombo = true)
    private int quizId;

    @DatabaseField(canBeNull = false, uniqueCombo = true)
    private int flashcardId;

    @DatabaseField
    private int orderIndex;

    /** Puste powiązanie (wymagane przez ORMLite). */
    public QuizFlashcard() {}
    /**
     * Powiązanie fiszki z zestawem.
     * @param quizId id zestawu
     * @param flashcardId id fiszki
     * @param orderIndex pozycja fiszki w zestawie
     */
    public QuizFlashcard(int quizId, int flashcardId, int orderIndex) {
        this.quizId = quizId;
        this.flashcardId = flashcardId;
        this.orderIndex = orderIndex;
    }

    /** @return identyfikator powiązania */
    public int getId() { return id; }
    /** @param id identyfikator powiązania */
    public void setId(int id) { this.id = id; }
    /** @return id zestawu */
    public int getQuizId() { return quizId; }
    /** @param quizId id zestawu */
    public void setQuizId(int quizId) { this.quizId = quizId; }
    /** @return id fiszki */
    public int getFlashcardId() { return flashcardId; }
    /** @param flashcardId id fiszki */
    public void setFlashcardId(int flashcardId) { this.flashcardId = flashcardId; }
    /** @return pozycja fiszki w zestawie (rosnąco = kolejność w quizie) */
    public int getOrderIndex() { return orderIndex; }
    /** @param orderIndex pozycja fiszki w zestawie */
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}
