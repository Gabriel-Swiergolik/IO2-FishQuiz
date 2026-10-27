package pl.pcz.io2.fishquizfrontend.dto;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;

public class FlashcardDTO {
    private final StringProperty frontText = new SimpleStringProperty();
    private final StringProperty backText = new SimpleStringProperty();
    private final ObjectProperty<Image> image = new SimpleObjectProperty<>();

    public FlashcardDTO() {}

    public FlashcardDTO(String front, String back, Image img) {
        this.frontText.set(front);
        this.backText.set(back);
        this.image.set(img);
    }

    public StringProperty frontTextProperty() { return frontText; }
    public String getFrontText() { return frontText.get(); }
    public void setFrontText(String value) { frontText.set(value); }

    public StringProperty backTextProperty() { return backText; }
    public String getBackText() { return backText.get(); }
    public void setBackText(String value) { backText.set(value); }

    public ObjectProperty<Image> imageProperty() { return image; }
    public Image getImage() { return image.get(); }
    public void setImage(Image value) { image.set(value); }
}