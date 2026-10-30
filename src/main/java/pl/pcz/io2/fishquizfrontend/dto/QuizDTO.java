package pl.pcz.io2.fishquizfrontend.dto;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Zestaw fiszek w widoku: id i nazwa jako właściwości JavaFX oraz lista fiszek.
 * Konwersja w {@link pl.pcz.io2.fishquizfrontend.mapper.QuizMapper}.
 */
public class QuizDTO {
    // 0 = quiz jeszcze niezapisany w bazie
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final ObservableList<FlashcardDTO> flashcards = FXCollections.observableArrayList();

    /** Pusty zestaw (id 0, bez nazwy). */
    public QuizDTO() {}

    /**
     * Zestaw o podanej nazwie (id 0).
     * @param title nazwa zestawu
     */
    public QuizDTO(String title) {
        this.title.set(title);
    }

    /** @return właściwość JavaFX: identyfikator; 0 dla zestawu jeszcze niezapisanego (do wiązania w widoku) */
    public IntegerProperty idProperty() { return id; }
    /** @return identyfikator; 0 dla zestawu jeszcze niezapisanego */
    public int getId() { return id.get(); }
    /** @param value identyfikator; 0 dla zestawu jeszcze niezapisanego */
    public void setId(int value) { id.set(value); }

    /** @return właściwość JavaFX: nazwa zestawu (do wiązania w widoku) */
    public StringProperty titleProperty() { return title; }
    /** @return nazwa zestawu */
    public String getTitle() { return title.get(); }
    /** @param value nazwa zestawu */
    public void setTitle(String value) { title.set(value); }

    /** @return modyfikowalna lista fiszek zestawu w kolejności z bazy */
    public ObservableList<FlashcardDTO> getFlashcards() { return flashcards; }
}
