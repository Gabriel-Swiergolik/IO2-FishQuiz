package pl.pcz.io2.fishquizfrontend.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

// Tabela łącząca quiz z fiszkami. ORMLite nie ma złożonych kluczy, więc jest osobne id,
// a para (quizId, flashcardId) jest unikalna.
@DatabaseTable(tableName = "quiz_flashcards")
public class QuizFlashcard {
    // Nazwy kolumn używane w zapytaniach DAO
    public static final String ID_FIELD = "id";
    public static final String QUIZ_ID_FIELD = "quizId";
    public static final String FLASHCARD_ID_FIELD = "flashcardId";
    public static final String ORDER_INDEX_FIELD = "orderIndex";

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false, uniqueCombo = true)
    private int quizId;

    @DatabaseField(canBeNull = false, uniqueCombo = true)
    private int flashcardId;

    @DatabaseField
    private int orderIndex;

    public QuizFlashcard() {}
    public QuizFlashcard(int quizId, int flashcardId, int orderIndex) {
        this.quizId = quizId;
        this.flashcardId = flashcardId;
        this.orderIndex = orderIndex;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getQuizId() { return quizId; }
    public void setQuizId(int quizId) { this.quizId = quizId; }
    public int getFlashcardId() { return flashcardId; }
    public void setFlashcardId(int flashcardId) { this.flashcardId = flashcardId; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}
