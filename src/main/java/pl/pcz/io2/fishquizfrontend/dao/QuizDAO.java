package pl.pcz.io2.fishquizfrontend.dao;

import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.model.Quiz;
import java.util.List;

public interface QuizDAO extends GenericDAO<Quiz, Integer> {
    // Fiszki quizu w kolejności orderIndex
    List<Flashcard> getFlashcardsForQuiz(int quizId);

    // Dopisuje fiszkę na koniec quizu; false, jeśli już w nim jest
    boolean addFlashcardToQuiz(int quizId, int flashcardId);

    // Usuwa fiszkę z quizu (sama fiszka zostaje)
    void removeFlashcardFromQuiz(int quizId, int flashcardId);
}
