package pl.pcz.io2.fishquizfrontend.mapper;

import org.junit.jupiter.api.Test;
import pl.pcz.io2.fishquizfrontend.dto.FlashcardDTO;
import pl.pcz.io2.fishquizfrontend.dto.QuizDTO;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.model.Quiz;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizMapperTest {
    @Test
    void roundTripKeepsIdAndTitle() {
        Quiz back = QuizMapper.toModel(QuizMapper.toDto(new Quiz(4, "Zwierzęta")));

        assertEquals(4, back.getId());
        assertEquals("Zwierzęta", back.getTitle());
    }

    @Test
    void toDtoWithoutCardsHasEmptyList() {
        assertTrue(QuizMapper.toDto(new Quiz(4, "Zwierzęta")).getFlashcards().isEmpty());
    }

    @Test
    void toDtoMapsCardsInGivenOrder() {
        List<Flashcard> cards = List.of(
                new Flashcard(2, "pies", "dog", null),
                new Flashcard(1, "kot", "cat", "abc.png"));

        QuizDTO dto = QuizMapper.toDto(new Quiz(4, "Zwierzęta"), cards);

        assertEquals(4, dto.getId());
        assertEquals("Zwierzęta", dto.getTitle());
        assertEquals(List.of(2, 1), dto.getFlashcards().stream().map(FlashcardDTO::getId).toList());
        assertEquals("abc.png", dto.getFlashcards().get(1).getImagePath());
    }
}
