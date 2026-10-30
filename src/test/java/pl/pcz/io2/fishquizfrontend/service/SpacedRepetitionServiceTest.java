package pl.pcz.io2.fishquizfrontend.service;

import org.junit.jupiter.api.Test;
import pl.pcz.io2.fishquizfrontend.model.AnswerQuality;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SpacedRepetitionServiceTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    private static final double DELTA = 1e-9;

    private final SpacedRepetitionService service = new SpacedRepetitionService(
            Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));

    private Flashcard freshCard() {
        return new Flashcard(0, "kot", "cat", null);
    }

    // junit-jupiter-params nie jest w zależnościach, więc oceny sprawdzamy pomocniczą metodą
    private void assertFreshCardAfter(AnswerQuality quality, int repetitions, int interval, double easiness) {
        Flashcard card = freshCard();

        Flashcard result = service.calculateNextReview(card, quality);

        assertSame(card, result);
        assertEquals(repetitions, card.getRepetitions(), quality + ": repetitions");
        assertEquals(interval, card.getInterval(), quality + ": interval");
        assertEquals(easiness, card.getEasinessFactor(), DELTA, quality + ": EF");
        assertEquals(TODAY.plusDays(interval), card.getNextReviewDate(), quality + ": nextReviewDate");
    }

    @Test
    void passingGradesOnFreshCard() {
        assertFreshCardAfter(AnswerQuality.PERFECT, 1, 1, 2.6);
        assertFreshCardAfter(AnswerQuality.EASY, 1, 1, 2.5);
        assertFreshCardAfter(AnswerQuality.GOOD, 1, 1, 2.36);
    }

    @Test
    void failingGradesOnFreshCard() {
        assertFreshCardAfter(AnswerQuality.HARD, 0, 1, 2.18);
        assertFreshCardAfter(AnswerQuality.INCORRECT, 0, 1, 1.96);
        assertFreshCardAfter(AnswerQuality.BLACKOUT, 0, 1, 1.7);
    }

    @Test
    void intervalsGrowOneSixThenTimesEasiness() {
        Flashcard card = freshCard();

        service.calculateNextReview(card, AnswerQuality.EASY);
        assertEquals(1, card.getInterval());
        service.calculateNextReview(card, AnswerQuality.EASY);
        assertEquals(6, card.getInterval());
        // EASY nie zmienia EF (2.5), więc 6 * 2.5 = 15, potem round(15 * 2.5) = 38
        service.calculateNextReview(card, AnswerQuality.EASY);
        assertEquals(15, card.getInterval());
        service.calculateNextReview(card, AnswerQuality.EASY);
        assertEquals(38, card.getInterval());

        assertEquals(4, card.getRepetitions());
        assertEquals(2.5, card.getEasinessFactor(), DELTA);
        assertEquals(TODAY.plusDays(38), card.getNextReviewDate());
    }

    @Test
    void intervalUsesEasinessFromBeforeTheAnswer() {
        Flashcard card = freshCard();
        card.setRepetitions(2);
        card.setInterval(6);

        // Stary EF 2.5: round(6 * 2.5) = 15; nowy EF 2.6 dotyczy dopiero następnej powtórki
        service.calculateNextReview(card, AnswerQuality.PERFECT);

        assertEquals(15, card.getInterval());
        assertEquals(2.6, card.getEasinessFactor(), DELTA);
    }

    @Test
    void failureResetsMatureCard() {
        Flashcard card = freshCard();
        card.setRepetitions(5);
        card.setInterval(40);
        card.setEasinessFactor(2.5);

        service.calculateNextReview(card, AnswerQuality.INCORRECT);

        assertEquals(0, card.getRepetitions());
        assertEquals(1, card.getInterval());
        assertEquals(1.96, card.getEasinessFactor(), DELTA);
        assertEquals(TODAY.plusDays(1), card.getNextReviewDate());

        // Po resecie znów 1, potem 6
        service.calculateNextReview(card, AnswerQuality.GOOD);
        assertEquals(1, card.getInterval());
        service.calculateNextReview(card, AnswerQuality.GOOD);
        assertEquals(6, card.getInterval());
    }

    @Test
    void easinessNeverDropsBelowFloor() {
        Flashcard card = freshCard();

        for (int i = 0; i < 5; i++) {
            service.calculateNextReview(card, AnswerQuality.BLACKOUT);
        }

        assertEquals(1.3, card.getEasinessFactor(), DELTA);
    }
}
