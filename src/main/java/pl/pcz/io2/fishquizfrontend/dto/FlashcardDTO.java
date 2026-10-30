package pl.pcz.io2.fishquizfrontend.dto;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

import java.time.LocalDate;

public class FlashcardDTO {
    // 0 = fiszka jeszcze niezapisana w bazie
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty frontText = new SimpleStringProperty();
    private final StringProperty backText = new SimpleStringProperty();
    // Ścieżka zapisywana w bazie; obraz poniżej służy tylko do wyświetlania
    private final StringProperty imagePath = new SimpleStringProperty();
    private final ObjectProperty<Image> image = new SimpleObjectProperty<>();
    // Tylko do wyświetlenia (lista); stan SM-2 zmienia wyłącznie quiz, więc nie wraca do modelu
    private final ObjectProperty<LocalDate> nextReviewDate = new SimpleObjectProperty<>();

    public FlashcardDTO() {}

    public FlashcardDTO(String front, String back, Image img) {
        this.frontText.set(front);
        this.backText.set(back);
        this.image.set(img);
    }

    public IntegerProperty idProperty() { return id; }
    public int getId() { return id.get(); }
    public void setId(int value) { id.set(value); }

    public StringProperty frontTextProperty() { return frontText; }
    public String getFrontText() { return frontText.get(); }
    public void setFrontText(String value) { frontText.set(value); }

    public StringProperty backTextProperty() { return backText; }
    public String getBackText() { return backText.get(); }
    public void setBackText(String value) { backText.set(value); }

    public StringProperty imagePathProperty() { return imagePath; }
    public String getImagePath() { return imagePath.get(); }
    public void setImagePath(String value) { imagePath.set(value); }

    public ObjectProperty<Image> imageProperty() { return image; }
    public Image getImage() { return image.get(); }
    public void setImage(Image value) { image.set(value); }

    public ObjectProperty<LocalDate> nextReviewDateProperty() { return nextReviewDate; }
    public LocalDate getNextReviewDate() { return nextReviewDate.get(); }
    public void setNextReviewDate(LocalDate value) { nextReviewDate.set(value); }
}
