package pl.pcz.io2.fishquizfrontend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.pcz.io2.fishquizfrontend.dao.DataAccessException;
import pl.pcz.io2.fishquizfrontend.dao.InMemoryFlashcardDAO;
import pl.pcz.io2.fishquizfrontend.dao.InMemoryQuizDAO;
import pl.pcz.io2.fishquizfrontend.model.AnswerQuality;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.model.Quiz;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizSessionTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    private static final Clock CLOCK = Clock.fixed(TODAY.atTime(10, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private InMemoryFlashcardDAO dao;
    private InMemoryQuizDAO quizDao;
    private QuizSession session;

    @BeforeEach
    void setUp() {
        dao = new InMemoryFlashcardDAO();
        quizDao = new InMemoryQuizDAO(dao);
        session = new QuizSession(dao, quizDao, new SpacedRepetitionService(CLOCK), new ActionLogger(), CLOCK);
    }

    private Flashcard card(String front, LocalDate nextReview) {
        Flashcard card = new Flashcard(0, front, front + "-tył", null);
        card.setNextReviewDate(nextReview);
        dao.create(card);
        return card;
    }

    // Zestaw z podanymi fiszkami w tej kolejności; zwraca jego id
    private int set(String title, Flashcard... cards) {
        Quiz quiz = new Quiz(0, title);
        quizDao.create(quiz);
        for (Flashcard card : cards) {
            quizDao.addFlashcardToQuiz(quiz.getId(), card.getId());
        }
        return quiz.getId();
    }

    @Test
    void emptyQueueWhenNothingIsDue() {
        card("jutro", TODAY.plusDays(1));

        assertEquals(0, session.start());
        assertTrue(session.isFinished());
        assertThrows(IllegalStateException.class, () -> session.current());
    }

    @Test
    void dueCardsComeInDateOrder() {
        Flashcard today = card("dziś", TODAY);
        card("jutro", TODAY.plusDays(1));
        Flashcard old = card("dawno", TODAY.minusDays(5));

        assertEquals(2, session.start());
        assertEquals(old.getId(), session.current().getId());
        assertEquals(1, session.position());
        session.grade(AnswerQuality.GOOD);
        assertEquals(today.getId(), session.current().getId());
        assertEquals(2, session.position());
        session.grade(AnswerQuality.GOOD);
        assertTrue(session.isFinished());
    }

    @Test
    void gradingSavesSm2StateAndAdvances() {
        Flashcard card = card("kot", TODAY);
        session.start();

        session.grade(AnswerQuality.PERFECT);

        Flashcard stored = dao.read(card.getId()).orElseThrow();
        assertEquals(1, stored.getRepetitions());
        assertEquals(1, stored.getInterval());
        assertEquals(2.6, stored.getEasinessFactor(), 1e-9);
        assertEquals(TODAY.plusDays(1), stored.getNextReviewDate());
        assertTrue(session.isFinished());
    }

    @Test
    void reviewedCardsAreNotDueAgainToday() {
        Flashcard perfect = card("kot", TODAY);
        Flashcard forgotten = card("pies", TODAY);
        session.start();

        session.grade(AnswerQuality.PERFECT);
        session.grade(AnswerQuality.BLACKOUT);

        // W SM-2 nowa fiszka dostaje odstęp 1 dzień niezależnie od oceny, ale różni się stanem
        Flashcard first = dao.read(perfect.getId()).orElseThrow();
        Flashcard second = dao.read(forgotten.getId()).orElseThrow();
        assertEquals(TODAY.plusDays(1), first.getNextReviewDate());
        assertEquals(TODAY.plusDays(1), second.getNextReviewDate());
        assertEquals(1, first.getRepetitions());
        assertEquals(0, second.getRepetitions());
        assertTrue(first.getEasinessFactor() > second.getEasinessFactor());
        assertEquals(0, session.start());
    }

    @Test
    void statisticsCountGradesAndPercentCorrect() {
        card("a", TODAY);
        card("b", TODAY);
        card("c", TODAY);
        session.start();

        session.grade(AnswerQuality.PERFECT);
        session.grade(AnswerQuality.GOOD);
        session.grade(AnswerQuality.HARD);

        assertEquals(3, session.reviewedCount());
        assertEquals(1, session.countFor(AnswerQuality.PERFECT));
        assertEquals(1, session.countFor(AnswerQuality.GOOD));
        assertEquals(1, session.countFor(AnswerQuality.HARD));
        assertEquals(0, session.countFor(AnswerQuality.BLACKOUT));
        assertEquals(67, session.percentCorrect());
    }

    @Test
    void percentCorrectIsZeroWithoutAnswers() {
        session.start();

        assertEquals(0, session.percentCorrect());
    }

    @Test
    void startResetsStatistics() {
        card("a", TODAY);
        session.start();
        session.grade(AnswerQuality.PERFECT);

        session.start();

        assertEquals(0, session.reviewedCount());
    }

    @Test
    void cardDeletedDuringSessionIsSkipped() {
        Flashcard deleted = card("kot", TODAY);
        card("pies", TODAY);
        session.start();
        dao.delete(deleted.getId());

        session.grade(AnswerQuality.PERFECT);

        assertEquals(0, session.reviewedCount());
        assertEquals(0, dao.updateCount);
        assertFalse(session.isFinished());
        assertEquals("pies", session.current().getFrontText());
    }

    @Test
    void editMadeDuringSessionIsKept() {
        Flashcard card = card("kot", TODAY);
        session.start();
        Flashcard edited = dao.read(card.getId()).orElseThrow();
        edited.setFrontText("kotek");
        dao.update(edited);

        session.grade(AnswerQuality.GOOD);

        Flashcard stored = dao.read(card.getId()).orElseThrow();
        assertEquals("kotek", stored.getFrontText());
        assertEquals(1, stored.getRepetitions());
    }

    @Test
    void failedSaveKeepsCurrentCard() {
        Flashcard card = card("kot", TODAY);
        session.start();
        dao.failWrites = true;

        assertThrows(DataAccessException.class, () -> session.grade(AnswerQuality.GOOD));

        assertEquals(card.getId(), session.current().getId());
        assertEquals(0, session.reviewedCount());
    }

    @Test
    void setModeKeepsSetOrder() {
        Flashcard later = card("później", TODAY.minusDays(1));
        Flashcard earlier = card("wcześniej", TODAY.minusDays(5));
        int setId = set("Zwierzęta", later, earlier);

        assertEquals(2, session.start(setId, true));
        // Kolejność zestawu, a nie data powtórki
        assertEquals(later.getId(), session.current().getId());
        session.grade(AnswerQuality.GOOD);
        assertEquals(earlier.getId(), session.current().getId());
    }

    @Test
    void setModeDueOnlySkipsFutureCardsAndCardsOutsideTheSet() {
        Flashcard due = card("kot", TODAY);
        Flashcard future = card("pies", TODAY.plusDays(3));
        card("spoza zestawu", TODAY);
        int setId = set("Zwierzęta", due, future);

        assertEquals(1, session.start(setId, true));
        assertEquals(due.getId(), session.current().getId());
    }

    @Test
    void setModeAllCardsIncludesFutureCards() {
        Flashcard due = card("kot", TODAY);
        Flashcard future = card("pies", TODAY.plusDays(3));
        int setId = set("Zwierzęta", future, due);

        assertEquals(2, session.start(setId, false));
        assertEquals(future.getId(), session.current().getId());
    }

    @Test
    void setModeWithEmptyOrUnknownSetHasNoCards() {
        card("kot", TODAY);
        int empty = set("Pusty");

        assertEquals(0, session.start(empty, false));
        assertTrue(session.isFinished());
        assertEquals(0, session.start(999, true));
    }

    @Test
    void gradingInSetModeSavesSm2State() {
        Flashcard future = card("pies", TODAY.plusDays(3));
        int setId = set("Zwierzęta", future);
        session.start(setId, false);

        session.grade(AnswerQuality.PERFECT);

        Flashcard stored = dao.read(future.getId()).orElseThrow();
        assertEquals(1, stored.getRepetitions());
        assertEquals(TODAY.plusDays(1), stored.getNextReviewDate());
        assertEquals(1, session.reviewedCount());
        assertTrue(session.isFinished());
    }
}
