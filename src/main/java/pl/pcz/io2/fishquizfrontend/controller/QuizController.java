package pl.pcz.io2.fishquizfrontend.controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import pl.pcz.io2.fishquizfrontend.dto.QuizDTO;
import pl.pcz.io2.fishquizfrontend.model.AnswerQuality;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.service.ImageManager;
import pl.pcz.io2.fishquizfrontend.service.QuizSession;
import pl.pcz.io2.fishquizfrontend.service.QuizSetService;
import pl.pcz.io2.fishquizfrontend.util.ErrorDialogs;
import pl.pcz.io2.fishquizfrontend.util.FxTasks;

import java.io.IOException;
import java.util.List;

// Zakładka "Quiz" (quiz-view.fxml): powtórka fiszek na dziś albo jednego zestawu z oceną wg SM-2.
// Stan sesji (kolejka, pozycja, oceny) trzyma QuizSession.
public class QuizController {
    private final QuizSession session;
    private final QuizSetService quizSetService;
    private final ImageManager imageManager;
    private final FxTasks fxTasks;
    // Do okna zestawów, żeby jego kontroler też dostał zależności z AppContext
    private final Callback<Class<?>, Object> controllerFactory;

    @FXML private VBox startPane;
    @FXML private ComboBox<QuizDTO> setCombo;
    @FXML private CheckBox dueOnlyCheck;
    @FXML private Button manageSetsButton;
    @FXML private Button startButton;
    @FXML private Label startInfoLabel;
    @FXML private Node cardPane;
    @FXML private Label progressLabel;
    @FXML private Label frontLabel;
    @FXML private Button showAnswerButton;
    @FXML private VBox answerBox;
    @FXML private Label backLabel;
    @FXML private ImageView answerImage;
    @FXML private TilePane gradePane;
    @FXML private VBox summaryPane;
    @FXML private Label summaryLabel;

    private final BooleanProperty busy = new SimpleBooleanProperty();
    // Pierwsza pozycja listy zestawów (id 0): wszystkie fiszki do powtórki, jak przed zestawami
    private final QuizDTO allDue = new QuizDTO("Wszystkie fiszki do powtórki");

    public QuizController(QuizSession session, QuizSetService quizSetService, ImageManager imageManager,
                          FxTasks fxTasks, Callback<Class<?>, Object> controllerFactory) {
        this.session = session;
        this.quizSetService = quizSetService;
        this.imageManager = imageManager;
        this.fxTasks = fxTasks;
        this.controllerFactory = controllerFactory;
    }

    @FXML
    private void initialize() {
        for (AnswerQuality quality : AnswerQuality.values()) {
            Button button = new Button(quality.getValue() + " – " + label(quality));
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> grade(quality));
            gradePane.getChildren().add(button);
        }
        gradePane.disableProperty().bind(busy);
        startButton.disableProperty().bind(busy);
        setCombo.disableProperty().bind(busy);
        manageSetsButton.disableProperty().bind(busy);

        setCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(QuizDTO set) {
                return set == null ? "" : set.getTitle();
            }

            @Override
            public QuizDTO fromString(String text) {
                throw new UnsupportedOperationException();
            }
        });
        setCombo.getItems().setAll(allDue);
        setCombo.setValue(allDue);
        // Przy "wszystkich fiszkach" i tak są tylko te do powtórki
        dueOnlyCheck.disableProperty().bind(setCombo.valueProperty().isEqualTo(allDue));
        answerBox.managedProperty().bind(answerBox.visibleProperty());
        showAnswerButton.managedProperty().bind(showAnswerButton.visibleProperty());
        answerImage.managedProperty().bind(answerImage.visibleProperty());
        showOnly(startPane);
    }

    // Wołane przez HomeController przy wejściu na zakładkę: zestawy mogły się zmienić
    public void refreshSets() {
        fxTasks.run(busy, "Nie udało się wczytać zestawów.", quizSetService::findAll, sets -> {
            int selectedId = setCombo.getValue() == null ? 0 : setCombo.getValue().getId();
            setCombo.getItems().setAll(allDue);
            setCombo.getItems().addAll(sets);
            // Zostaje wybrany ten sam zestaw; jeśli go usunięto, wracamy do wszystkich fiszek
            setCombo.setValue(sets.stream().filter(set -> set.getId() == selectedId).findFirst().orElse(allDue));
        });
    }

    @FXML
    private void onStart() {
        startInfoLabel.setText("");
        QuizDTO set = setCombo.getValue();
        boolean allDueMode = set == null || set == allDue;
        int setId = allDueMode ? 0 : set.getId();
        boolean dueOnly = dueOnlyCheck.isSelected();
        fxTasks.run(busy, "Nie udało się wczytać fiszek do powtórki.",
                () -> allDueMode ? session.start() : session.start(setId, dueOnly), count -> {
                    if (count == 0) {
                        startInfoLabel.setText(allDueMode ? "Brak fiszek do powtórki"
                                : dueOnly ? "Brak fiszek do powtórki w tym zestawie"
                                : "Ten zestaw jest pusty");
                    } else {
                        showCurrentCard();
                    }
                });
    }

    @FXML
    private void onManageSets() {
        FXMLLoader loader = new FXMLLoader(
                QuizController.class.getResource("/pl/pcz/io2/fishquizfrontend/quiz-sets-dialog.fxml"));
        loader.setControllerFactory(controllerFactory);
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            ErrorDialogs.show("Nie udało się otworzyć okna zestawów.", e);
            return;
        }
        Stage dialog = new Stage();
        dialog.initOwner(startPane.getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Zestawy fiszek");
        dialog.setScene(new Scene(root));
        dialog.showAndWait();
        startInfoLabel.setText("");
        refreshSets();
    }

    @FXML
    private void onShowAnswer() {
        Flashcard card = session.current();
        backLabel.setText(card.getBackText());
        // null, gdy fiszka nie ma obrazu albo pliku już nie ma
        Image image = imageManager.loadImage(card.getImagePath());
        answerImage.setImage(image);
        answerImage.setVisible(image != null);
        showAnswerButton.setVisible(false);
        answerBox.setVisible(true);
    }

    @FXML
    private void onBackToStart() {
        startInfoLabel.setText("");
        showOnly(startPane);
    }

    private void grade(AnswerQuality quality) {
        fxTasks.run(busy, "Nie udało się zapisać oceny fiszki.", () -> {
            session.grade(quality);
            return null;
        }, ignored -> showCurrentCard());
    }

    private void showCurrentCard() {
        if (session.isFinished()) {
            showSummary();
            return;
        }
        Flashcard card = session.current();
        progressLabel.setText("Fiszka " + session.position() + " z " + session.size());
        frontLabel.setText(card.getFrontText());
        backLabel.setText("");
        answerImage.setImage(null);
        answerBox.setVisible(false);
        showAnswerButton.setVisible(true);
        showOnly(cardPane);
        showAnswerButton.requestFocus();
    }

    private void showSummary() {
        StringBuilder text = new StringBuilder()
                .append("Przejrzane fiszki: ").append(session.reviewedCount()).append('\n')
                .append("Poprawne odpowiedzi (ocena ≥ 3): ").append(session.percentCorrect()).append("%\n");
        for (AnswerQuality quality : AnswerQuality.values()) {
            text.append('\n').append(quality.getValue()).append(" – ").append(label(quality))
                    .append(": ").append(session.countFor(quality));
        }
        summaryLabel.setText(text.toString());
        showOnly(summaryPane);
    }

    private void showOnly(Node pane) {
        for (Node candidate : List.of(startPane, cardPane, summaryPane)) {
            candidate.setVisible(candidate == pane);
        }
    }

    private static String label(AnswerQuality quality) {
        return switch (quality) {
            case BLACKOUT -> "Nie pamiętam";
            case INCORRECT -> "Źle";
            case HARD -> "Trudno";
            case GOOD -> "Dobrze";
            case EASY -> "Łatwo";
            case PERFECT -> "Idealnie";
        };
    }
}
