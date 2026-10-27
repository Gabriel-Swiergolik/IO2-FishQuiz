package pl.pcz.io2.fishquizfrontend.dao;

import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import java.util.List;

public interface FlashcardDAO extends GenericDAO<Flashcard, Integer> {
    List<Flashcard> getDueFlashcards();
}