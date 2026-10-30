package pl.pcz.io2.fishquizfrontend.dao.impl;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.pcz.io2.fishquizdb.DatabaseManager;
import pl.pcz.io2.fishquizdb.DbTestSupport;
import pl.pcz.io2.fishquizfrontend.dao.DataAccessException;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.model.QuizFlashcard;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlashcardDAOImplTest extends DbTestSupport {
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

    private FlashcardDAOImpl dao;

    @BeforeEach
    void createDao() {
        dao = new FlashcardDAOImpl();
    }

    private Flashcard card(String front, LocalDate nextReview) {
        Flashcard card = new Flashcard(0, front, front + "-tył", null);
        card.setNextReviewDate(nextReview);
        dao.create(card);
        return card;
    }

    @Test
    void createSetsGeneratedId() {
        Flashcard first = card("kot", TODAY);
        Flashcard second = card("pies", TODAY);

        assertTrue(first.getId() > 0);
        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void readReturnsSavedCard() {
        Flashcard saved = card("kot", TODAY);

        Optional<Flashcard> read = dao.read(saved.getId());

        assertTrue(read.isPresent());
        assertEquals("kot", read.get().getFrontText());
        assertEquals("kot-tył", read.get().getBackText());
        assertEquals(TODAY, read.get().getNextReviewDate());
    }

    @Test
    void readMissingIdReturnsEmpty() {
        assertTrue(dao.read(12345).isEmpty());
    }

    @Test
    void updatePersistsChanges() {
        Flashcard saved = card("kot", TODAY);
        saved.setBackText("cat");
        saved.setInterval(6);
        saved.setNextReviewDate(TODAY.plusDays(6));

        dao.update(saved);

        Flashcard read = dao.read(saved.getId()).orElseThrow();
        assertEquals("cat", read.getBackText());
        assertEquals(6, read.getInterval());
        assertEquals(TODAY.plusDays(6), read.getNextReviewDate());
    }

    @Test
    void findAllReturnsEveryCard() {
        assertTrue(dao.findAll().isEmpty());
        card("kot", TODAY);
        card("pies", TODAY.plusDays(3));

        assertEquals(2, dao.findAll().size());
    }

    @Test
    void deleteRemovesCardAndItsQuizLinks() throws SQLException {
        Flashcard deleted = card("kot", TODAY);
        Flashcard kept = card("pies", TODAY);
        Dao<QuizFlashcard, Integer> links = DaoManager.createDao(DatabaseManager.getConnectionSource(), QuizFlashcard.class);
        links.create(new QuizFlashcard(1, deleted.getId(), 0));
        links.create(new QuizFlashcard(2, deleted.getId(), 0));
        links.create(new QuizFlashcard(1, kept.getId(), 1));

        dao.delete(deleted.getId());

        assertTrue(dao.read(deleted.getId()).isEmpty());
        assertTrue(dao.read(kept.getId()).isPresent());
        List<QuizFlashcard> remaining = links.queryForAll();
        assertEquals(1, remaining.size());
        assertEquals(kept.getId(), remaining.get(0).getFlashcardId());
    }

    @Test
    void dueFlashcardsIncludeTodayAndPastInDateOrder() {
        Flashcard tomorrow = card("jutro", TODAY.plusDays(1));
        Flashcard today = card("dziś", TODAY);
        Flashcard lastWeek = card("tydzień temu", TODAY.minusDays(7));
        Flashcard yesterday = card("wczoraj", TODAY.minusDays(1));

        List<Flashcard> due = dao.getDueFlashcards(TODAY);

        assertEquals(List.of(lastWeek.getId(), yesterday.getId(), today.getId()),
                due.stream().map(Flashcard::getId).toList());
        assertTrue(due.stream().noneMatch(c -> c.getId() == tomorrow.getId()));
    }

    @Test
    void dueFlashcardsSkipCardsWithoutDate() {
        card("bez daty", null);
        Flashcard dated = card("dziś", TODAY);

        assertEquals(List.of(dated.getId()), dao.getDueFlashcards(TODAY).stream().map(Flashcard::getId).toList());
    }

    @Test
    void sqlErrorIsWrappedInDataAccessException() {
        // frontText ma NOT NULL, więc baza odrzuca zapis
        Flashcard invalid = new Flashcard(0, null, "tył", null);

        DataAccessException e = assertThrows(DataAccessException.class, () -> dao.create(invalid));
        assertTrue(e.getCause() instanceof SQLException);
    }
}
