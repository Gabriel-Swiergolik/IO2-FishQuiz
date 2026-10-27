package pl.pcz.io2.fishquizfrontend.dto;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class QuizDTO {
    private final StringProperty title = new SimpleStringProperty();
    private final ObservableList<FlashcardDTO> flashcards = FXCollections.observableArrayList();

    public QuizDTO() {}

    public QuizDTO(String title) {
        this.title.set(title);
    }

    public StringProperty titleProperty() { return title; }
    public String getTitle() { return title.get(); }
    public void setTitle(String value) { title.set(value); }

    public ObservableList<FlashcardDTO> getFlashcards() { return flashcards; }
}