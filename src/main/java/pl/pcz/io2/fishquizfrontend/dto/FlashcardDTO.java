package pl.pcz.io2.fishquizfrontend.dto;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

import java.time.LocalDate;

/**
 * Fiszka w widoku: pola jako właściwości JavaFX, do wiązania z kontrolkami.
 * Konwersja z i do {@link pl.pcz.io2.fishquizfrontend.model.Flashcard} w
 * {@link pl.pcz.io2.fishquizfrontend.mapper.FlashcardMapper}.
 */
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

    /** Pusta fiszka (id 0). */
    public FlashcardDTO() {}

    /**
     * Fiszka z treścią i obrazem do wyświetlenia.
     * @param front awers
     * @param back rewers
     * @param img obraz do wyświetlenia albo {@code null}
     */
    public FlashcardDTO(String front, String back, Image img) {
        this.frontText.set(front);
        this.backText.set(back);
        this.image.set(img);
    }

    /** @return właściwość JavaFX: identyfikator; 0 dla fiszki jeszcze niezapisanej (do wiązania w widoku) */
    public IntegerProperty idProperty() { return id; }
    /** @return identyfikator; 0 dla fiszki jeszcze niezapisanej */
    public int getId() { return id.get(); }
    /** @param value identyfikator; 0 dla fiszki jeszcze niezapisanej */
    public void setId(int value) { id.set(value); }

    /** @return właściwość JavaFX: awers (pytanie) (do wiązania w widoku) */
    public StringProperty frontTextProperty() { return frontText; }
    /** @return awers (pytanie) */
    public String getFrontText() { return frontText.get(); }
    /** @param value awers (pytanie) */
    public void setFrontText(String value) { frontText.set(value); }

    /** @return właściwość JavaFX: rewers (odpowiedź) (do wiązania w widoku) */
    public StringProperty backTextProperty() { return backText; }
    /** @return rewers (odpowiedź) */
    public String getBackText() { return backText.get(); }
    /** @param value rewers (odpowiedź) */
    public void setBackText(String value) { backText.set(value); }

    /** @return właściwość JavaFX: nazwa pliku obrazu zapisywana w bazie albo {@code null} (do wiązania w widoku) */
    public StringProperty imagePathProperty() { return imagePath; }
    /** @return nazwa pliku obrazu zapisywana w bazie albo {@code null} */
    public String getImagePath() { return imagePath.get(); }
    /** @param value nazwa pliku obrazu zapisywana w bazie albo {@code null} */
    public void setImagePath(String value) { imagePath.set(value); }

    /** @return właściwość JavaFX: wczytany obraz, tylko do wyświetlania (do wiązania w widoku) */
    public ObjectProperty<Image> imageProperty() { return image; }
    /** @return wczytany obraz, tylko do wyświetlania */
    public Image getImage() { return image.get(); }
    /** @param value wczytany obraz, tylko do wyświetlania */
    public void setImage(Image value) { image.set(value); }

    /** @return właściwość JavaFX: data następnej powtórki, tylko do wyświetlania (do wiązania w widoku) */
    public ObjectProperty<LocalDate> nextReviewDateProperty() { return nextReviewDate; }
    /** @return data następnej powtórki, tylko do wyświetlania */
    public LocalDate getNextReviewDate() { return nextReviewDate.get(); }
    /** @param value data następnej powtórki, tylko do wyświetlania */
    public void setNextReviewDate(LocalDate value) { nextReviewDate.set(value); }
}
