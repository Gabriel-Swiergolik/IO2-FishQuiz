package pl.pcz.io2.fishquizfrontend.model;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.pcz.io2.fishquizdb.DatabaseManager;
import pl.pcz.io2.fishquizdb.DbTestSupport;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelMappingTest extends DbTestSupport {
    private Dao<Flashcard, Integer> flashcardDao;
    private Dao<QuizFlashcard, Integer> quizFlashcardDao;

    @BeforeEach
    void createTables() throws SQLException {
        ConnectionSource source = DatabaseManager.getConnectionSource();
        TableUtils.createTableIfNotExists(source, Flashcard.class);
        TableUtils.createTableIfNotExists(source, QuizFlashcard.class);
        flashcardDao = DaoManager.createDao(source, Flashcard.class);
        quizFlashcardDao = DaoManager.createDao(source, QuizFlashcard.class);
    }

    @Test
    void flashcardRoundTripKeepsAllFieldsIncludingDate() throws SQLException {
        Flashcard card = new Flashcard(0, "kot", "cat", "images/kot.png");
        card.setNextReviewDate(LocalDate.of(2026, 1, 15));
        card.setInterval(6);
        card.setRepetitions(2);
        card.setEasinessFactor(2.36);

        flashcardDao.create(card);
        assertTrue(card.getId() > 0, "ORMLite powinien ustawić wygenerowane id");

        Flashcard read = flashcardDao.queryForId(card.getId());
        assertNotNull(read);
        assertEquals(card.getId(), read.getId());
        assertEquals("kot", read.getFrontText());
        assertEquals("cat", read.getBackText());
        assertEquals("images/kot.png", read.getImagePath());
        assertEquals(LocalDate.of(2026, 1, 15), read.getNextReviewDate());
        assertEquals(6, read.getInterval());
        assertEquals(2, read.getRepetitions());
        assertEquals(2.36, read.getEasinessFactor(), 1e-9);
    }

    @Test
    void nullDateAndImageAreStoredAsNull() throws SQLException {
        Flashcard card = new Flashcard(0, "pies", "dog", null);
        card.setNextReviewDate(null);
        flashcardDao.create(card);

        Flashcard read = flashcardDao.queryForId(card.getId());
        assertNull(read.getImagePath());
        assertNull(read.getNextReviewDate());
    }

    @Test
    void flashcardWithoutFrontTextIsRejected() {
        Flashcard card = new Flashcard(0, null, "cat", null);
        assertThrows(SQLException.class, () -> flashcardDao.create(card));
    }

    @Test
    void quizFlashcardRoundTripAndUniquePair() throws SQLException {
        QuizFlashcard link = new QuizFlashcard(1, 7, 3);
        quizFlashcardDao.create(link);
        assertTrue(link.getId() > 0);

        QuizFlashcard read = quizFlashcardDao.queryForId(link.getId());
        assertEquals(1, read.getQuizId());
        assertEquals(7, read.getFlashcardId());
        assertEquals(3, read.getOrderIndex());

        // Ta sama fiszka w innym quizie jest dozwolona, ta sama para już nie
        quizFlashcardDao.create(new QuizFlashcard(2, 7, 0));
        assertThrows(SQLException.class, () -> quizFlashcardDao.create(new QuizFlashcard(1, 7, 5)));
    }
}
