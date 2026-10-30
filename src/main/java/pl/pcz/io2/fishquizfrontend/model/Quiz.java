package pl.pcz.io2.fishquizfrontend.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Zestaw fiszek (tabela {@code quizzes}). Fiszki są przypisane przez {@link QuizFlashcard}.
 */
@DatabaseTable(tableName = "quizzes")
public class Quiz {
    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(canBeNull = false)
    private String title;

    /** Pusty zestaw (wymagany przez ORMLite). */
    public Quiz() {}
    /**
     * Zestaw o podanej nazwie.
     * @param id identyfikator; 0 dla zestawu jeszcze niezapisanego
     * @param title nazwa zestawu
     */
    public Quiz(int id, String title) {
        this.id = id;
        this.title = title;
    }

    /** @return identyfikator nadany przez bazę; 0 przed zapisem */
    public int getId() { return id; }
    /** @param id identyfikator zestawu */
    public void setId(int id) { this.id = id; }
    /** @return nazwa zestawu */
    public String getTitle() { return title; }
    /** @param title nazwa zestawu */
    public void setTitle(String title) { this.title = title; }
}