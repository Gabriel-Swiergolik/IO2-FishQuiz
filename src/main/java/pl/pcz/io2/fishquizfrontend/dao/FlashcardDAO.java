package pl.pcz.io2.fishquizfrontend.dao;

import pl.pcz.io2.fishquizfrontend.model.Flashcard;

import java.time.LocalDate;
import java.util.List;

public interface FlashcardDAO extends GenericDAO<Flashcard, Integer> {
    // Fiszki do powtórki (nextReviewDate <= today), od najstarszej daty
    List<Flashcard> getDueFlashcards(LocalDate today);

    // Zapis wielu fiszek w jednej transakcji (import CSV): wszystkie albo żadna
    void createAll(List<Flashcard> flashcards);

    default List<Flashcard> getDueFlashcards() {
        return getDueFlashcards(LocalDate.now());
    }
}
