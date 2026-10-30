package pl.pcz.io2.fishquizfrontend.mapper;

import pl.pcz.io2.fishquizfrontend.dto.QuizDTO;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.model.Quiz;

import java.util.List;

// Przepisywanie danych między Quiz a QuizDTO. Skład quizu zapisuje się przez QuizDAO, nie przez mapper.
public final class QuizMapper {
    private QuizMapper() {}

    public static QuizDTO toDto(Quiz quiz) {
        QuizDTO dto = new QuizDTO(quiz.getTitle());
        dto.setId(quiz.getId());
        return dto;
    }

    // flashcards w kolejności z QuizDAO.getFlashcardsForQuiz
    public static QuizDTO toDto(Quiz quiz, List<Flashcard> flashcards) {
        QuizDTO dto = toDto(quiz);
        for (Flashcard flashcard : flashcards) {
            dto.getFlashcards().add(FlashcardMapper.toDto(flashcard));
        }
        return dto;
    }

    public static Quiz toModel(QuizDTO dto) {
        return new Quiz(dto.getId(), dto.getTitle());
    }
}
