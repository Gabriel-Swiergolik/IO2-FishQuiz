package pl.pcz.io2.fishquizfrontend.dto;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class QuizDTO {
    // 0 = quiz jeszcze niezapisany w bazie
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final ObservableList<FlashcardDTO> flashcards = FXCollections.observableArrayList();

    public QuizDTO() {}

    public QuizDTO(String title) {
        this.title.set(title);
    }

    public IntegerProperty idProperty() { return id; }
    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }

    public StringProperty titleProperty() { return title; }
    public String getTitle() { return title.get(); }
    public void setTitle(String value) { title.set(value); }

    public ObservableList<FlashcardDTO> getFlashcards() { return flashcards; }
}
