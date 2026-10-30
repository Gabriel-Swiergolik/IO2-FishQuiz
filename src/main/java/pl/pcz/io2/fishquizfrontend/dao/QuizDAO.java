package pl.pcz.io2.fishquizfrontend.dao;

import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.model.Quiz;
import java.util.List;

/**
 * Dostęp do zestawów (quizów) i ich składu. Usunięcie zestawu usuwa przypisania, ale nie same fiszki.
 */
public interface QuizDAO extends GenericDAO<Quiz, Integer> {
    /**
     * Fiszki quizu w kolejności orderIndex.
     * @param quizId id zestawu
     * @return fiszki zestawu; pusta lista, gdy zestaw jest pusty lub nie istnieje
     * @throws DataAccessException gdy odczyt się nie uda
     */
    List<Flashcard> getFlashcardsForQuiz(int quizId);

    /**
     * Dopisuje fiszkę na koniec quizu.
     * @param quizId id zestawu
     * @param flashcardId id fiszki
     * @return {@code false}, jeśli fiszka już jest w zestawie
     * @throws DataAccessException gdy zapis się nie uda
     */
    boolean addFlashcardToQuiz(int quizId, int flashcardId);

    /**
     * Usuwa fiszkę z quizu (sama fiszka zostaje).
     * @param quizId id zestawu
     * @param flashcardId id fiszki
     * @throws DataAccessException gdy usunięcie się nie uda
     */
    void removeFlashcardFromQuiz(int quizId, int flashcardId);
}
