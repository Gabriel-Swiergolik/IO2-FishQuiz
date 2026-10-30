package pl.pcz.io2.fishquizfrontend.mapper;

import pl.pcz.io2.fishquizfrontend.dto.QuizDTO;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.model.Quiz;

import java.util.List;

/**
 * Przepisywanie danych między Quiz a QuizDTO. Skład quizu zapisuje się przez QuizDAO, nie przez mapper.
 */
public final class QuizMapper {
    private QuizMapper() {}

    /**
     * Kopiuje id i nazwę; lista fiszek w DTO zostaje pusta.
     * @param quiz zestaw z bazy
     * @return nowy DTO
     */
    public static QuizDTO toDto(Quiz quiz) {
        QuizDTO dto = new QuizDTO(quiz.getTitle());
        dto.setId(quiz.getId());
        return dto;
    }

    /**
     * Kopiuje id, nazwę i fiszki zestawu.
     * @param quiz zestaw z bazy
     * @param flashcards fiszki w kolejności z {@code QuizDAO.getFlashcardsForQuiz}
     * @return nowy DTO z fiszkami w tej samej kolejności
     */
    public static QuizDTO toDto(Quiz quiz, List<Flashcard> flashcards) {
        QuizDTO dto = toDto(quiz);
        for (Flashcard flashcard : flashcards) {
            dto.getFlashcards().add(FlashcardMapper.toDto(flashcard));
        }
        return dto;
    }

    /**
     * Tworzy model z id i nazwą z DTO (bez fiszek).
     * @param dto dane z widoku
     * @return nowy model
     */
    public static Quiz toModel(QuizDTO dto) {
        return new Quiz(dto.getId(), dto.getTitle());
    }
}
