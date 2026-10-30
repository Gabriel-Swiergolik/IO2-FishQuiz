package pl.pcz.io2.fishquizfrontend.dao;

import pl.pcz.io2.fishquizfrontend.model.Flashcard;

import java.time.LocalDate;
import java.util.List;

/**
 * Dostęp do fiszek. Usunięcie fiszki usuwa też jej przypisania do zestawów.
 */
public interface FlashcardDAO extends GenericDAO<Flashcard, Integer> {
    /**
     * Fiszki do powtórki ({@code nextReviewDate <= today}), od najstarszej daty.
     * @param today dzień, dla którego liczymy zaległe powtórki
     * @return fiszki posortowane po dacie powtórki, potem po id
     * @throws DataAccessException gdy odczyt się nie uda
     */
    List<Flashcard> getDueFlashcards(LocalDate today);

    /**
     * Zapis wielu fiszek w jednej transakcji (import CSV): wszystkie albo żadna.
     * @param flashcards nowe fiszki; dostają wygenerowane id
     * @throws DataAccessException gdy zapis się nie uda (nic nie zostaje zapisane)
     */
    void createAll(List<Flashcard> flashcards);

    /**
     * Fiszki do powtórki na dziś według zegara systemowego.
     * @return jak {@link #getDueFlashcards(LocalDate)} dla dzisiejszej daty
     * @throws DataAccessException gdy odczyt się nie uda
     */
    default List<Flashcard> getDueFlashcards() {
        return getDueFlashcards(LocalDate.now());
    }
}
