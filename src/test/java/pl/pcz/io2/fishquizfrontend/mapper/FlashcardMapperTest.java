package pl.pcz.io2.fishquizfrontend.mapper;

import org.junit.jupiter.api.Test;
import pl.pcz.io2.fishquizfrontend.dto.FlashcardDTO;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FlashcardMapperTest {
    private static final LocalDate REVIEW = LocalDate.of(2026, 4, 1);

    private Flashcard learnedCard() {
        Flashcard card = new Flashcard(7, "kot", "cat", "abc.png");
        card.setInterval(15);
        card.setRepetitions(3);
        card.setEasinessFactor(2.36);
        card.setNextReviewDate(REVIEW);
        return card;
    }

    @Test
    void toDtoCopiesEditableFieldsAndId() {
        FlashcardDTO dto = FlashcardMapper.toDto(learnedCard());

        assertEquals(7, dto.getId());
        assertEquals("kot", dto.getFrontText());
        assertEquals("cat", dto.getBackText());
        assertEquals("abc.png", dto.getImagePath());
        // Data powtórki tylko do wyświetlenia na liście
        assertEquals(REVIEW, dto.getNextReviewDate());
        // Obraz wczytuje widok, nie mapper
        assertNull(dto.getImage());
    }

    @Test
    void roundTripKeepsIdTextsAndImagePath() {
        Flashcard back = FlashcardMapper.toModel(FlashcardMapper.toDto(learnedCard()));

        assertEquals(7, back.getId());
        assertEquals("kot", back.getFrontText());
        assertEquals("cat", back.getBackText());
        assertEquals("abc.png", back.getImagePath());
    }

    @Test
    void roundTripWithoutImage() {
        Flashcard card = new Flashcard(3, "pies", "dog", null);

        FlashcardDTO dto = FlashcardMapper.toDto(card);
        Flashcard back = FlashcardMapper.toModel(dto);

        assertNull(dto.getImagePath());
        assertNull(back.getImagePath());
    }

    @Test
    void toModelOfNewCardHasDefaultSm2State() {
        FlashcardDTO dto = new FlashcardDTO();
        dto.setFrontText("mysz");
        dto.setBackText("mouse");

        Flashcard card = FlashcardMapper.toModel(dto);

        assertEquals(0, card.getId());
        assertEquals(0, card.getInterval());
        assertEquals(0, card.getRepetitions());
        assertEquals(2.5, card.getEasinessFactor());
    }

    @Test
    void editKeepsSm2State() {
        Flashcard card = learnedCard();
        FlashcardDTO dto = FlashcardMapper.toDto(card);
        dto.setFrontText("kotek");
        dto.setBackText("kitten");
        dto.setImagePath("def.jpg");
        // Nawet zmienione id w DTO nie nadpisuje id edytowanej fiszki
        dto.setId(99);
        // Data z DTO też nie wraca do modelu: zmienia ją tylko SM-2
        dto.setNextReviewDate(REVIEW.plusDays(30));

        FlashcardMapper.updateModel(dto, card);

        assertEquals("kotek", card.getFrontText());
        assertEquals("kitten", card.getBackText());
        assertEquals("def.jpg", card.getImagePath());
        assertEquals(7, card.getId());
        assertEquals(15, card.getInterval());
        assertEquals(3, card.getRepetitions());
        assertEquals(2.36, card.getEasinessFactor());
        assertEquals(REVIEW, card.getNextReviewDate());
    }
}
