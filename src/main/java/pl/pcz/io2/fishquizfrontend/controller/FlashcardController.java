package pl.pcz.io2.fishquizfrontend.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import pl.pcz.io2.fishquizfrontend.dto.FlashcardDTO;
import pl.pcz.io2.fishquizfrontend.service.FlashcardService;
import pl.pcz.io2.fishquizfrontend.service.FlashcardValidator;
import pl.pcz.io2.fishquizfrontend.service.ImageManager;
import pl.pcz.io2.fishquizfrontend.util.FxTasks;

import java.io.File;

/** Zakładka "Dodawanie fiszek" (flashcard-form.fxml): nowa fiszka albo edycja istniejącej. */
public class FlashcardController {
    private static final String INVALID = "invalid";

    private final FlashcardService flashcardService;
    private final ImageManager imageManager;
    private final FxTasks fxTasks;

    @FXML private Label titleLabel;
    @FXML private TextField frontField;
    @FXML private TextArea backArea;
    @FXML private Button chooseImageButton;
    @FXML private Button removeImageButton;
    @FXML private ImageView imagePreview;
    @FXML private Label noImageLabel;
    @FXML private Button saveButton;
    @FXML private Button cancelEditButton;
    @FXML private Label statusLabel;

    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final BooleanProperty editing = new SimpleBooleanProperty();
    // Stan formularza: id edytowanej fiszki (0 = nowa), nowo wybrany plik, usunięcie obrazka
    private int editingId;
    private File pendingImage;
    private boolean imageRemoved;
    private Runnable onEditFinished;

    /**
     * Kontroler tworzony przez fabrykę z {@link pl.pcz.io2.fishquizfrontend.AppContext}.
     * @param flashcardService zapis fiszek
     * @param imageManager podgląd obrazów
     * @param fxTasks praca w tle
     */
    public FlashcardController(FlashcardService flashcardService, ImageManager imageManager, FxTasks fxTasks) {
        this.flashcardService = flashcardService;
        this.imageManager = imageManager;
        this.fxTasks = fxTasks;
    }

    @FXML
    private void initialize() {
        titleLabel.textProperty().bind(Bindings.when(editing).then("Edycja fiszki").otherwise("Nowa fiszka"));
        cancelEditButton.visibleProperty().bind(editing);
        cancelEditButton.managedProperty().bind(editing);
        saveButton.disableProperty().bind(busy);
        chooseImageButton.disableProperty().bind(busy);
        removeImageButton.disableProperty().bind(busy.or(imagePreview.imageProperty().isNull()));
        noImageLabel.visibleProperty().bind(imagePreview.imageProperty().isNull());
        noImageLabel.managedProperty().bind(noImageLabel.visibleProperty());
        imagePreview.managedProperty().bind(imagePreview.imageProperty().isNotNull());

        // Podświetlenie błędu znika, gdy użytkownik zacznie pisać
        frontField.textProperty().addListener((obs, old, text) -> markInvalid(frontField, false));
        backArea.textProperty().addListener((obs, old, text) -> markInvalid(backArea, false));
    }

    /**
     * Wołane po zapisaniu edycji albo anulowaniu (HomeController wraca wtedy do listy).
     * @param onEditFinished akcja do wywołania (może być {@code null})
     */
    public void setOnEditFinished(Runnable onEditFinished) {
        this.onEditFinished = onEditFinished;
    }

    /**
     * Wypełnia formularz fiszką z listy; zapis zaktualizuje ją zamiast tworzyć nową.
     * @param dto edytowana fiszka (z listy)
     */
    public void loadForEdit(FlashcardDTO dto) {
        clearForm();
        editingId = dto.getId();
        editing.set(true);
        frontField.setText(dto.getFrontText());
        backArea.setText(dto.getBackText());
        // Brak pliku → null, czyli "Brak obrazka"
        imagePreview.setImage(imageManager.loadImage(dto.getImagePath()));
    }

    /** Wybór obrazka z dysku; plik jest kopiowany dopiero przy zapisie fiszki. */
    @FXML
    private void onChooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Wybierz obrazek");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Obrazy (png, jpg, gif, bmp)", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"));
        File file = chooser.showOpenDialog(frontField.getScene().getWindow());
        if (file == null) {
            return;
        }
        Image image = new Image(file.toURI().toString());
        if (image.isError()) {
            showStatus("Nie udało się wczytać obrazka " + file.getName() + ".", true);
            return;
        }
        pendingImage = file;
        imageRemoved = false;
        imagePreview.setImage(image);
        showStatus("", false);
    }

    @FXML
    private void onRemoveImage() {
        pendingImage = null;
        imageRemoved = true;
        imagePreview.setImage(null);
    }

    /** Sprawdza formularz i zapisuje fiszkę w tle; po edycji wraca do listy. */
    @FXML
    private void onSave() {
        FlashcardValidator.Result validation = FlashcardValidator.validate(frontField.getText(), backArea.getText());
        markInvalid(frontField, validation.frontMissing());
        markInvalid(backArea, validation.backMissing());
        if (!validation.isValid()) {
            showStatus("Uzupełnij awers i rewers.", true);
            return;
        }

        FlashcardDTO dto = new FlashcardDTO();
        dto.setId(editingId);
        dto.setFrontText(frontField.getText());
        dto.setBackText(backArea.getText());
        File image = pendingImage;
        boolean removeImage = imageRemoved;
        boolean wasEditing = editing.get();

        fxTasks.run(busy, "Nie udało się zapisać fiszki.",
                () -> flashcardService.save(dto, image, removeImage),
                saved -> {
                    clearForm();
                    showStatus("Zapisano fiszkę „" + saved.getFrontText() + "”.", false);
                    if (wasEditing && onEditFinished != null) {
                        onEditFinished.run();
                    }
                });
    }

    @FXML
    private void onCancelEdit() {
        clearForm();
        if (onEditFinished != null) {
            onEditFinished.run();
        }
    }

    private void clearForm() {
        editingId = 0;
        editing.set(false);
        pendingImage = null;
        imageRemoved = false;
        frontField.clear();
        backArea.clear();
        imagePreview.setImage(null);
        markInvalid(frontField, false);
        markInvalid(backArea, false);
        showStatus("", false);
    }

    private void showStatus(String text, boolean error) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().removeAll("status-error", "status-ok");
        statusLabel.getStyleClass().add(error ? "status-error" : "status-ok");
    }

    private static void markInvalid(Node field, boolean invalid) {
        field.getStyleClass().remove(INVALID);
        if (invalid) {
            field.getStyleClass().add(INVALID);
        }
    }
}
