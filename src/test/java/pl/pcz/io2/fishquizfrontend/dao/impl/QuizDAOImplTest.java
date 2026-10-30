package pl.pcz.io2.fishquizfrontend.dao.impl;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.pcz.io2.fishquizdb.DatabaseManager;
import pl.pcz.io2.fishquizdb.DbTestSupport;
import pl.pcz.io2.fishquizfrontend.dao.DataAccessException;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.model.Quiz;
import pl.pcz.io2.fishquizfrontend.model.QuizFlashcard;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizDAOImplTest extends DbTestSupport {
    private QuizDAOImpl dao;
    private FlashcardDAOImpl flashcardDao;

    @BeforeEach
    void createDaos() {
        dao = new QuizDAOImpl();
        flashcardDao = new FlashcardDAOImpl();
    }

    private Quiz quiz(String title) {
        Quiz quiz = new Quiz(0, title);
        dao.create(quiz);
        return quiz;
    }

    private Flashcard card(String front) {
        Flashcard card = new Flashcard(0, front, front + "-tył", null);
        flashcardDao.create(card);
        return card;
    }

    private List<Integer> cardIds(int quizId) {
        return dao.getFlashcardsForQuiz(quizId).stream().map(Flashcard::getId).toList();
    }

    private List<QuizFlashcard> allLinks() throws SQLException {
        Dao<QuizFlashcard, Integer> links = DaoManager.createDao(DatabaseManager.getConnectionSource(), QuizFlashcard.class);
        return links.queryForAll();
    }

    @Test
    void createSetsGeneratedId() {
        Quiz first = quiz("Zwierzęta");
        Quiz second = quiz("Kolory");

        assertTrue(first.getId() > 0);
        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void readReturnsSavedQuiz() {
        Quiz saved = quiz("Zwierzęta");

        Optional<Quiz> read = dao.read(saved.getId());

        assertTrue(read.isPresent());
        assertEquals("Zwierzęta", read.get().getTitle());
    }

    @Test
    void readMissingIdReturnsEmpty() {
        assertTrue(dao.read(12345).isEmpty());
    }

    @Test
    void updatePersistsChanges() {
        Quiz saved = quiz("Zwierzęta");
        saved.setTitle("Zwierzęta domowe");

        dao.update(saved);

        assertEquals("Zwierzęta domowe", dao.read(saved.getId()).orElseThrow().getTitle());
    }

    @Test
    void findAllReturnsEveryQuiz() {
        assertTrue(dao.findAll().isEmpty());
        quiz("Zwierzęta");
        quiz("Kolory");

        assertEquals(2, dao.findAll().size());
    }

    @Test
    void quizWithoutCardsHasEmptyList() {
        Quiz quiz = quiz("Pusty");

        assertTrue(dao.getFlashcardsForQuiz(quiz.getId()).isEmpty());
    }

    @Test
    void flashcardsComeBackInInsertionOrder() {
        Quiz quiz = quiz("Zwierzęta");
        Flashcard kot = card("kot");
        Flashcard pies = card("pies");
        Flashcard mysz = card("mysz");

        // Kolejność dodawania inna niż kolejność id
        assertTrue(dao.addFlashcardToQuiz(quiz.getId(), mysz.getId()));
        assertTrue(dao.addFlashcardToQuiz(quiz.getId(), kot.getId()));
        assertTrue(dao.addFlashcardToQuiz(quiz.getId(), pies.getId()));

        assertEquals(List.of(mysz.getId(), kot.getId(), pies.getId()), cardIds(quiz.getId()));
    }

    @Test
    void removedCardDisappearsAndReAddGoesToTheEnd() {
        Quiz quiz = quiz("Zwierzęta");
        Flashcard kot = card("kot");
        Flashcard pies = card("pies");
        Flashcard mysz = card("mysz");
        dao.addFlashcardToQuiz(quiz.getId(), kot.getId());
        dao.addFlashcardToQuiz(quiz.getId(), pies.getId());
        dao.addFlashcardToQuiz(quiz.getId(), mysz.getId());

        dao.removeFlashcardFromQuiz(quiz.getId(), kot.getId());
        assertEquals(List.of(pies.getId(), mysz.getId()), cardIds(quiz.getId()));

        dao.addFlashcardToQuiz(quiz.getId(), kot.getId());
        assertEquals(List.of(pies.getId(), mysz.getId(), kot.getId()), cardIds(quiz.getId()));
        // Sama fiszka nie jest usuwana
        assertTrue(flashcardDao.read(kot.getId()).isPresent());
    }

    @Test
    void addingSameCardTwiceIsIgnored() throws SQLException {
        Quiz quiz = quiz("Zwierzęta");
        Flashcard kot = card("kot");

        assertTrue(dao.addFlashcardToQuiz(quiz.getId(), kot.getId()));
        assertFalse(dao.addFlashcardToQuiz(quiz.getId(), kot.getId()));

        assertEquals(List.of(kot.getId()), cardIds(quiz.getId()));
        assertEquals(1, allLinks().size());
    }

    @Test
    void sameCardCanBeInSeveralQuizzes() {
        Quiz zwierzeta = quiz("Zwierzęta");
        Quiz powtorka = quiz("Powtórka");
        Flashcard kot = card("kot");

        assertTrue(dao.addFlashcardToQuiz(zwierzeta.getId(), kot.getId()));
        assertTrue(dao.addFlashcardToQuiz(powtorka.getId(), kot.getId()));

        assertEquals(List.of(kot.getId()), cardIds(zwierzeta.getId()));
        assertEquals(List.of(kot.getId()), cardIds(powtorka.getId()));
    }

    @Test
    void deleteRemovesQuizAndItsLinksButKeepsCards() throws SQLException {
        Quiz deleted = quiz("Zwierzęta");
        Quiz kept = quiz("Kolory");
        Flashcard kot = card("kot");
        Flashcard czerwony = card("czerwony");
        dao.addFlashcardToQuiz(deleted.getId(), kot.getId());
        dao.addFlashcardToQuiz(deleted.getId(), czerwony.getId());
        dao.addFlashcardToQuiz(kept.getId(), czerwony.getId());

        dao.delete(deleted.getId());

        assertTrue(dao.read(deleted.getId()).isEmpty());
        assertTrue(dao.read(kept.getId()).isPresent());
        assertEquals(2, flashcardDao.findAll().size());
        List<QuizFlashcard> remaining = allLinks();
        assertEquals(1, remaining.size());
        assertEquals(kept.getId(), remaining.get(0).getQuizId());
    }

    @Test
    void linkToDeletedCardIsSkipped() {
        Quiz quiz = quiz("Zwierzęta");
        Flashcard kot = card("kot");
        Flashcard pies = card("pies");
        dao.addFlashcardToQuiz(quiz.getId(), kot.getId());
        dao.addFlashcardToQuiz(quiz.getId(), pies.getId());

        flashcardDao.delete(kot.getId());

        assertEquals(List.of(pies.getId()), cardIds(quiz.getId()));
    }

    @Test
    void sqlErrorIsWrappedInDataAccessException() {
        // title ma NOT NULL, więc baza odrzuca zapis
        Quiz invalid = new Quiz(0, null);

        DataAccessException e = assertThrows(DataAccessException.class, () -> dao.create(invalid));
        assertTrue(e.getCause() instanceof SQLException);
    }
}
