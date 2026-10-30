package pl.pcz.io2.fishquizfrontend.controller;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import pl.pcz.io2.fishquizfrontend.model.AnswerQuality;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.service.ImageManager;
import pl.pcz.io2.fishquizfrontend.service.QuizSession;
import pl.pcz.io2.fishquizfrontend.util.FxTasks;

import java.util.List;

// Zakładka "Quiz" (quiz-view.fxml): powtórka fiszek na dziś z oceną wg SM-2.
// Stan sesji (kolejka, pozycja, oceny) trzyma QuizSession.
public class QuizController {
    private final QuizSession session;
    private final ImageManager imageManager;
    private final FxTasks fxTasks;

    @FXML private VBox startPane;
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

    public QuizController(QuizSession session, ImageManager imageManager, FxTasks fxTasks) {
        this.session = session;
        this.imageManager = imageManager;
        this.fxTasks = fxTasks;
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
        answerBox.managedProperty().bind(answerBox.visibleProperty());
        showAnswerButton.managedProperty().bind(showAnswerButton.visibleProperty());
        answerImage.managedProperty().bind(answerImage.visibleProperty());
        showOnly(startPane);
    }

    @FXML
    private void onStart() {
        startInfoLabel.setText("");
        fxTasks.run(busy, "Nie udało się wczytać fiszek do powtórki.", session::start, count -> {
            if (count == 0) {
                startInfoLabel.setText("Brak fiszek do powtórki");
            } else {
                showCurrentCard();
            }
        });
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
