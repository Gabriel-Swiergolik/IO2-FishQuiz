package pl.pcz.io2.fishquizfrontend.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import pl.pcz.io2.fishquizfrontend.dto.FlashcardDTO;
import pl.pcz.io2.fishquizfrontend.service.CsvImportExportService;
import pl.pcz.io2.fishquizfrontend.service.FlashcardService;
import pl.pcz.io2.fishquizfrontend.util.FxTasks;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

// Zakładka "Przeglądanie" (flashcard-list.fxml)
public class FlashcardListController {
    private static final int BACK_PREVIEW_LENGTH = 60;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final FlashcardService flashcardService;
    private final FxTasks fxTasks;

    @FXML private TextField filterField;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button importButton;
    @FXML private Button exportButton;
    @FXML private TableView<FlashcardDTO> table;
    @FXML private TableColumn<FlashcardDTO, String> frontColumn;
    @FXML private TableColumn<FlashcardDTO, String> backColumn;
    @FXML private TableColumn<FlashcardDTO, String> imageColumn;
    @FXML private TableColumn<FlashcardDTO, LocalDate> reviewColumn;
    @FXML private Label placeholderLabel;
    @FXML private Label countLabel;
    @FXML private Label statusLabel;

    private final ObservableList<FlashcardDTO> cards = FXCollections.observableArrayList();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private Consumer<FlashcardDTO> onEditRequested;

    public FlashcardListController(FlashcardService flashcardService, FxTasks fxTasks) {
        this.flashcardService = flashcardService;
        this.fxTasks = fxTasks;
    }

    @FXML
    private void initialize() {
        frontColumn.setCellValueFactory(cell -> cell.getValue().frontTextProperty());
        backColumn.setCellValueFactory(cell -> Bindings.createStringBinding(
                () -> preview(cell.getValue().getBackText()), cell.getValue().backTextProperty()));
        imageColumn.setCellValueFactory(cell -> Bindings.createStringBinding(
                () -> cell.getValue().getImagePath() == null ? "" : "✓", cell.getValue().imagePathProperty()));
        reviewColumn.setCellValueFactory(cell -> cell.getValue().nextReviewDateProperty());
        reviewColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty ? null : date == null ? "—" : DATE_FORMAT.format(date));
            }
        });

        // Lista → filtr z pola wyszukiwania → sortowanie po kliknięciu nagłówka
        FilteredList<FlashcardDTO> filtered = new FilteredList<>(cards, card -> true);
        filterField.textProperty().addListener((obs, old, text) -> filtered.setPredicate(card -> matches(card, text)));
        SortedList<FlashcardDTO> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        placeholderLabel.textProperty().bind(Bindings.when(Bindings.isEmpty(cards))
                .then("Brak fiszek. Dodaj pierwszą w zakładce „Dodawanie fiszek” albo zaimportuj plik CSV.")
                .otherwise("Żadna fiszka nie pasuje do wyszukiwania."));
        countLabel.textProperty().bind(Bindings.size(cards).asString("Fiszek: %d"));

        BooleanBinding noSelection = table.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(busy.or(noSelection));
        deleteButton.disableProperty().bind(busy.or(noSelection));
        importButton.disableProperty().bind(busy);
        exportButton.disableProperty().bind(busy);

        table.setRowFactory(view -> {
            TableRow<FlashcardDTO> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    requestEdit(row.getItem());
                }
            });
            return row;
        });
        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE && !busy.get()) {
                onDelete();
            }
        });
    }

    // Wołane przez HomeController, gdy użytkownik chce edytować fiszkę w formularzu
    public void setOnEditRequested(Consumer<FlashcardDTO> onEditRequested) {
        this.onEditRequested = onEditRequested;
    }

    public void refresh() {
        fxTasks.run(busy, "Nie udało się wczytać fiszek.", flashcardService::findAll, cards::setAll);
    }

    @FXML
    private void onEdit() {
        FlashcardDTO selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) {
            requestEdit(selected);
        }
    }

    @FXML
    private void onDelete() {
        FlashcardDTO selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Usuwanie fiszki");
        confirm.setHeaderText("Usunąć fiszkę „" + selected.getFrontText() + "”?");
        confirm.setContentText("Tej operacji nie można cofnąć.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        int id = selected.getId();
        fxTasks.run(busy, "Nie udało się usunąć fiszki.", () -> {
            flashcardService.delete(id);
            return flashcardService.findAll();
        }, list -> {
            cards.setAll(list);
            statusLabel.setText("Usunięto fiszkę „" + selected.getFrontText() + "”.");
        });
    }

    @FXML
    private void onImport() {
        File file = csvChooser("Importuj fiszki z pliku CSV").showOpenDialog(window());
        if (file == null) {
            return;
        }
        fxTasks.run(busy, "Nie udało się zaimportować pliku CSV.", () -> flashcardService.importCsv(file), result -> {
            refresh();
            statusLabel.setText("Import z pliku " + file.getName() + " zakończony.");
            showImportSummary(result);
        });
    }

    @FXML
    private void onExport() {
        FileChooser chooser = csvChooser("Eksportuj fiszki do pliku CSV");
        chooser.setInitialFileName("fiszki.csv");
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        fxTasks.run(busy, "Nie udało się wyeksportować fiszek do pliku CSV.", () -> flashcardService.exportCsv(file),
                count -> statusLabel.setText("Wyeksportowano fiszek: " + count + " (plik " + file.getName() + ")."));
    }

    private void requestEdit(FlashcardDTO card) {
        if (onEditRequested != null) {
            onEditRequested.accept(card);
        }
    }

    private void showImportSummary(CsvImportExportService.ImportResult result) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Import CSV");
        alert.setHeaderText("Zaimportowano: " + result.flashcards().size()
                + ", pominięto: " + result.skippedRows().size());
        if (result.skippedRows().isEmpty()) {
            alert.setContentText("Wszystkie wiersze zostały zaimportowane.");
        } else {
            alert.setContentText("Pominięte wiersze są opisane w szczegółach.");
            TextArea details = new TextArea(String.join("\n", result.skippedRows()));
            details.setEditable(false);
            details.setPrefRowCount(8);
            alert.getDialogPane().setExpandableContent(details);
        }
        alert.show();
    }

    private static FileChooser csvChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Pliki CSV (*.csv)", "*.csv"));
        return chooser;
    }

    private Window window() {
        return table.getScene().getWindow();
    }

    private static boolean matches(FlashcardDTO card, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.strip().toLowerCase(Locale.ROOT);
        return contains(card.getFrontText(), q) || contains(card.getBackText(), q);
    }

    private static boolean contains(String text, String lowerQuery) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }

    // Pierwsza linia rewersu, skrócona do szerokości kolumny
    private static String preview(String text) {
        if (text == null) {
            return "";
        }
        String firstLine = text.lines().findFirst().orElse("");
        boolean truncated = firstLine.length() > BACK_PREVIEW_LENGTH || !firstLine.equals(text);
        if (firstLine.length() > BACK_PREVIEW_LENGTH) {
            firstLine = firstLine.substring(0, BACK_PREVIEW_LENGTH);
        }
        return truncated ? firstLine.strip() + "…" : firstLine;
    }
}
