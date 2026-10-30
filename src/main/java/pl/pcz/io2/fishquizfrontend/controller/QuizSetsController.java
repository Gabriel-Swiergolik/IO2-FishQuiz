package pl.pcz.io2.fishquizfrontend.controller;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.util.StringConverter;
import pl.pcz.io2.fishquizfrontend.dto.FlashcardDTO;
import pl.pcz.io2.fishquizfrontend.dto.QuizDTO;
import pl.pcz.io2.fishquizfrontend.service.FlashcardService;
import pl.pcz.io2.fishquizfrontend.service.QuizSetService;
import pl.pcz.io2.fishquizfrontend.util.FxTasks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// Okno "Zarządzaj zestawami" (quiz-sets-dialog.fxml): zestawy po lewej, a po prawej
// lista wszystkich fiszek z polami wyboru dla wybranego zestawu
public class QuizSetsController {
    private final QuizSetService quizSetService;
    private final FlashcardService flashcardService;
    private final FxTasks fxTasks;

    @FXML private ListView<QuizDTO> setList;
    @FXML private Button newButton;
    @FXML private Button renameButton;
    @FXML private Button deleteButton;
    @FXML private Label cardsTitleLabel;
    @FXML private ListView<FlashcardDTO> cardList;
    @FXML private Button saveButton;
    @FXML private Label statusLabel;

    private final ObservableList<QuizDTO> sets = FXCollections.observableArrayList();
    private final ObservableList<FlashcardDTO> cards = FXCollections.observableArrayList();
    // Stan pól wyboru: id fiszki → czy jest w wybranym zestawie
    private final Map<Integer, BooleanProperty> checked = new HashMap<>();
    private final BooleanProperty busy = new SimpleBooleanProperty();

    private record Data(List<QuizDTO> sets, List<FlashcardDTO> cards) {}

    public QuizSetsController(QuizSetService quizSetService, FlashcardService flashcardService, FxTasks fxTasks) {
        this.quizSetService = quizSetService;
        this.flashcardService = flashcardService;
        this.fxTasks = fxTasks;
    }

    @FXML
    private void initialize() {
        setList.setItems(sets);
        setList.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(QuizDTO set, boolean empty) {
                super.updateItem(set, empty);
                setText(empty || set == null ? null : set.getTitle());
            }
        });
        setList.setPlaceholder(new Label("Brak zestawów. Utwórz pierwszy przyciskiem „Nowy…”."));
        setList.getSelectionModel().selectedItemProperty().addListener((obs, old, set) -> loadAssignment(set));

        cardList.setItems(cards);
        cardList.setCellFactory(CheckBoxListCell.forListView(card -> checked.get(card.getId()), new StringConverter<>() {
            @Override
            public String toString(FlashcardDTO card) {
                return card == null ? "" : card.getFrontText() + " — " + firstLine(card.getBackText());
            }

            @Override
            public FlashcardDTO fromString(String text) {
                throw new UnsupportedOperationException();
            }
        }));
        cardList.setPlaceholder(new Label("Brak fiszek. Dodaj je w zakładce „Dodawanie fiszek”."));

        BooleanBinding noSelection = setList.getSelectionModel().selectedItemProperty().isNull();
        newButton.disableProperty().bind(busy);
        renameButton.disableProperty().bind(busy.or(noSelection));
        deleteButton.disableProperty().bind(busy.or(noSelection));
        saveButton.disableProperty().bind(busy.or(noSelection));
        cardList.disableProperty().bind(noSelection);

        cardsTitleLabel.setText("Wybierz zestaw");
        fxTasks.run(busy, "Nie udało się wczytać zestawów i fiszek.",
                () -> new Data(quizSetService.findAll(), flashcardService.findAll()), data -> {
                    checked.clear();
                    data.cards().forEach(card -> checked.put(card.getId(), new SimpleBooleanProperty()));
                    cards.setAll(data.cards());
                    sets.setAll(data.sets());
                    if (!sets.isEmpty()) {
                        setList.getSelectionModel().selectFirst();
                    }
                });
    }

    @FXML
    private void onNew() {
        askTitle("Nowy zestaw", "").ifPresent(title ->
                fxTasks.run(busy, "Nie udało się utworzyć zestawu.", () -> quizSetService.create(title), created -> {
                    reloadSets(created.getId());
                    statusLabel.setText("Utworzono zestaw „" + created.getTitle() + "”.");
                }));
    }

    @FXML
    private void onRename() {
        QuizDTO selected = setList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        int id = selected.getId();
        askTitle("Zmiana nazwy zestawu", selected.getTitle()).ifPresent(title ->
                fxTasks.run(busy, "Nie udało się zmienić nazwy zestawu.", () -> {
                    quizSetService.rename(id, title);
                    return null;
                }, ignored -> {
                    reloadSets(id);
                    statusLabel.setText("Zmieniono nazwę na „" + title.strip() + "”.");
                }));
    }

    @FXML
    private void onDelete() {
        QuizDTO selected = setList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(setList.getScene().getWindow());
        confirm.setTitle("Usuwanie zestawu");
        confirm.setHeaderText("Usunąć zestaw „" + selected.getTitle() + "”?");
        confirm.setContentText("Fiszki z tego zestawu zostaną w aplikacji, znika tylko sam zestaw.");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        int id = selected.getId();
        fxTasks.run(busy, "Nie udało się usunąć zestawu.", () -> {
            quizSetService.delete(id);
            return null;
        }, ignored -> {
            reloadSets(0);
            statusLabel.setText("Usunięto zestaw „" + selected.getTitle() + "”.");
        });
    }

    @FXML
    private void onSaveAssignment() {
        QuizDTO selected = setList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        int id = selected.getId();
        // Kolejność z listy: nowo zaznaczone fiszki trafiają na koniec zestawu w tej kolejności
        List<Integer> ids = cards.stream()
                .map(FlashcardDTO::getId)
                .filter(cardId -> checked.get(cardId).get())
                .toList();
        fxTasks.run(busy, "Nie udało się zapisać fiszek zestawu.", () -> {
            quizSetService.updateFlashcards(id, ids);
            return null;
        }, ignored -> statusLabel.setText("Zapisano zestaw „" + selected.getTitle() + "”: fiszek " + ids.size() + "."));
    }

    // Zaznacza fiszki należące do wybranego zestawu
    private void loadAssignment(QuizDTO set) {
        checked.values().forEach(property -> property.set(false));
        if (set == null) {
            cardsTitleLabel.setText("Wybierz zestaw");
            return;
        }
        int id = set.getId();
        cardsTitleLabel.setText("Fiszki w zestawie „" + set.getTitle() + "”");
        fxTasks.run(busy, "Nie udało się wczytać fiszek zestawu.", () -> quizSetService.flashcardIds(id), ids -> {
            QuizDTO current = setList.getSelectionModel().getSelectedItem();
            // Użytkownik mógł w międzyczasie wybrać inny zestaw
            if (current != null && current.getId() == id) {
                applyChecks(ids);
            }
        });
    }

    private void applyChecks(Set<Integer> ids) {
        checked.forEach((cardId, property) -> property.set(ids.contains(cardId)));
    }

    // Po zmianie zestawów wczytuje listę od nowa i zaznacza zestaw o podanym id (0 = żaden)
    private void reloadSets(int selectId) {
        fxTasks.run(busy, "Nie udało się wczytać zestawów.", quizSetService::findAll, list -> {
            sets.setAll(list);
            sets.stream().filter(set -> set.getId() == selectId).findFirst()
                    .ifPresent(set -> setList.getSelectionModel().select(set));
        });
    }

    // Pyta o nazwę zestawu; pusta nazwa → ostrzeżenie i brak wyniku
    private Optional<String> askTitle(String header, String initial) {
        TextInputDialog dialog = new TextInputDialog(initial);
        dialog.initOwner(setList.getScene().getWindow());
        dialog.setTitle("Zestaw fiszek");
        dialog.setHeaderText(header);
        dialog.setContentText("Nazwa:");
        Optional<String> title = dialog.showAndWait();
        if (title.isPresent() && title.get().isBlank()) {
            Alert warning = new Alert(Alert.AlertType.WARNING);
            warning.initOwner(setList.getScene().getWindow());
            warning.setTitle("Zestaw fiszek");
            warning.setHeaderText("Nazwa zestawu jest wymagana.");
            warning.show();
            return Optional.empty();
        }
        return title;
    }

    private static String firstLine(String text) {
        return text == null ? "" : text.lines().findFirst().orElse("");
    }
}
