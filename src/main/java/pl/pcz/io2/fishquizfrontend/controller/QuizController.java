package pl.pcz.io2.fishquizfrontend.controller;

import javafx.fxml.FXML;
import pl.pcz.io2.fishquizfrontend.dao.QuizDAO;
import pl.pcz.io2.fishquizfrontend.dao.impl.QuizDAOImpl;
import pl.pcz.io2.fishquizfrontend.model.AnswerQuality;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.service.SpacedRepetitionService;

public class QuizController {
    private final QuizDAO quizDAO = new QuizDAOImpl();
    private final SpacedRepetitionService spacedRepetitionService = new SpacedRepetitionService();

    @FXML
    public void startQuiz() {
        // to do start quizu
    }

    @FXML
    public void nextCard() {
        // to do nastepna karta
    }

    @FXML
    public void submitAnswer(Flashcard currentCard, AnswerQuality quality) {
        spacedRepetitionService.calculateNextReview(currentCard, quality);
    }
}