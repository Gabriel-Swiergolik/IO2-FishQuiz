package pl.pcz.io2.fishquizfrontend.dao;

import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.model.Quiz;
import java.util.List;

public interface QuizDAO extends GenericDAO<Quiz, Integer> {
    List<Flashcard> getFlashcardsForQuiz(int quizId);
}