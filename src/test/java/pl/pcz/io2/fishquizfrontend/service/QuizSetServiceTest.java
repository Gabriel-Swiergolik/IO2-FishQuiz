package pl.pcz.io2.fishquizfrontend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.pcz.io2.fishquizdb.DbTestSupport;
import pl.pcz.io2.fishquizfrontend.dao.DataAccessException;
import pl.pcz.io2.fishquizfrontend.dao.impl.FlashcardDAOImpl;
import pl.pcz.io2.fishquizfrontend.dao.impl.QuizDAOImpl;
import pl.pcz.io2.fishquizfrontend.dto.QuizDTO;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizSetServiceTest extends DbTestSupport {
    private FlashcardDAOImpl flashcardDao;
    private QuizDAOImpl quizDao;
    private QuizSetService service;

    @BeforeEach
    void setUp() {
        flashcardDao = new FlashcardDAOImpl();
        quizDao = new QuizDAOImpl();
        service = new QuizSetService(quizDao, new ActionLogger());
    }

    private Flashcard card(String front) {
        Flashcard card = new Flashcard(0, front, front + "-tył", null);
        flashcardDao.create(card);
        return card;
    }

    private List<Integer> setOrder(int quizId) {
        return quizDao.getFlashcardsForQuiz(quizId).stream().map(Flashcard::getId).toList();
    }

    @Test
    void createStripsTitleAndPersists() {
        QuizDTO created = service.create("  Zwierzęta ");

        assertNotEquals(0, created.getId());
        assertEquals("Zwierzęta", created.getTitle());
        assertEquals("Zwierzęta", quizDao.read(created.getId()).orElseThrow().getTitle());
    }

    @Test
    void blankTitleIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.create("   "));
        assertThrows(IllegalArgumentException.class, () -> service.create(null));
        QuizDTO set = service.create("Kolory");
        assertThrows(IllegalArgumentException.class, () -> service.rename(set.getId(), ""));
        assertEquals(1, quizDao.findAll().size());
    }

    @Test
    void findAllIsSortedByPolishTitle() {
        service.create("Żywność");
        service.create("Zwierzęta");
        service.create("Ćwiczenia");
        service.create("Czasowniki");

        List<String> titles = service.findAll().stream().map(QuizDTO::getTitle).toList();

        assertEquals(List.of("Czasowniki", "Ćwiczenia", "Zwierzęta", "Żywność"), titles);
    }

    @Test
    void renameChangesTitle() {
        QuizDTO set = service.create("Zwierzeta");

        service.rename(set.getId(), " Zwierzęta ");

        assertEquals("Zwierzęta", quizDao.read(set.getId()).orElseThrow().getTitle());
    }

    @Test
    void renamingDeletedSetFails() {
        QuizDTO set = service.create("Zwierzęta");
        service.delete(set.getId());

        assertThrows(DataAccessException.class, () -> service.rename(set.getId(), "Nowa nazwa"));
    }

    @Test
    void deleteKeepsCards() {
        Flashcard cat = card("kot");
        QuizDTO set = service.create("Zwierzęta");
        service.updateFlashcards(set.getId(), List.of(cat.getId()));

        service.delete(set.getId());

        assertTrue(service.findAll().isEmpty());
        assertTrue(flashcardDao.read(cat.getId()).isPresent());
        assertTrue(service.flashcardIds(set.getId()).isEmpty());
    }

    @Test
    void updateFlashcardsAddsRemovesAndKeepsOrder() {
        Flashcard cat = card("kot");
        Flashcard dog = card("pies");
        Flashcard fish = card("ryba");
        QuizDTO set = service.create("Zwierzęta");

        service.updateFlashcards(set.getId(), List.of(dog.getId(), cat.getId()));
        assertEquals(List.of(dog.getId(), cat.getId()), setOrder(set.getId()));

        // pies zostaje na swoim miejscu, kot znika, ryba trafia na koniec
        service.updateFlashcards(set.getId(), List.of(fish.getId(), dog.getId()));
        assertEquals(List.of(dog.getId(), fish.getId()), setOrder(set.getId()));
        assertEquals(Set.of(dog.getId(), fish.getId()), service.flashcardIds(set.getId()));
    }

    @Test
    void setsAreIndependent() {
        Flashcard cat = card("kot");
        QuizDTO animals = service.create("Zwierzęta");
        QuizDTO pets = service.create("Domowe");

        service.updateFlashcards(animals.getId(), List.of(cat.getId()));
        service.updateFlashcards(pets.getId(), List.of(cat.getId()));
        service.updateFlashcards(animals.getId(), List.of());

        assertTrue(service.flashcardIds(animals.getId()).isEmpty());
        assertEquals(Set.of(cat.getId()), service.flashcardIds(pets.getId()));
    }

    @Test
    void deletedCardDisappearsFromSet() {
        Flashcard cat = card("kot");
        Flashcard dog = card("pies");
        QuizDTO set = service.create("Zwierzęta");
        service.updateFlashcards(set.getId(), List.of(cat.getId(), dog.getId()));

        flashcardDao.delete(cat.getId());

        assertEquals(Set.of(dog.getId()), service.flashcardIds(set.getId()));
    }
}
