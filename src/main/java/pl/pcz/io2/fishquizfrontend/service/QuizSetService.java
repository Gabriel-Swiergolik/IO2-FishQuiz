package pl.pcz.io2.fishquizfrontend.service;

import pl.pcz.io2.fishquizfrontend.dao.DataAccessException;
import pl.pcz.io2.fishquizfrontend.dao.QuizDAO;
import pl.pcz.io2.fishquizfrontend.dto.QuizDTO;
import pl.pcz.io2.fishquizfrontend.mapper.QuizMapper;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.model.Quiz;

import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Zestawy fiszek (Quiz): tworzenie, zmiana nazwy, usuwanie i przypisywanie fiszek.
 * Bez JavaFX UI; metody blokują (baza), więc kontrolery wołają je przez FxTasks.
 */
public class QuizSetService {
    private static final Comparator<String> POLISH_ORDER = Collator.getInstance(Locale.of("pl", "PL"))::compare;

    private final QuizDAO quizDAO;
    private final ActionLogger actionLogger;

    /**
     * Serwis na podanych zależnościach (w aplikacji tworzy go {@link pl.pcz.io2.fishquizfrontend.AppContext}).
     * @param quizDAO dostęp do zestawów
     * @param actionLogger log akcji użytkownika
     */
    public QuizSetService(QuizDAO quizDAO, ActionLogger actionLogger) {
        this.quizDAO = quizDAO;
        this.actionLogger = actionLogger;
    }

    /**
     * Zestawy alfabetycznie po nazwie (bez fiszek, te wczytuje flashcardIds).
     * @return zestawy posortowane wg polskiego alfabetu
     * @throws DataAccessException gdy operacja na bazie się nie uda
     */
    public List<QuizDTO> findAll() {
        return quizDAO.findAll().stream()
                .map(QuizMapper::toDto)
                .sorted(Comparator.comparing(QuizDTO::getTitle, POLISH_ORDER))
                .toList();
    }

    /**
     * Tworzy nowy, pusty zestaw.
     * @param title nazwa (białe znaki na brzegach są obcinane)
     * @return zapisany zestaw z nadanym id
     * @throws IllegalArgumentException gdy nazwa jest pusta
     * @throws DataAccessException gdy operacja na bazie się nie uda
     */
    public QuizDTO create(String title) {
        Quiz quiz = new Quiz(0, requireTitle(title));
        quizDAO.create(quiz);
        actionLogger.logAction("CREATE_SET", describe(quiz));
        return QuizMapper.toDto(quiz);
    }

    /**
     * Zmienia nazwę zestawu.
     * @param id identyfikator zestawu
     * @param title nowa nazwa (białe znaki na brzegach są obcinane)
     * @throws IllegalArgumentException gdy nazwa jest pusta
     * @throws DataAccessException gdy zapis się nie uda albo zestaw został w międzyczasie usunięty
     */
    public void rename(int id, String title) {
        String newTitle = requireTitle(title);
        Quiz quiz = quizDAO.read(id)
                .orElseThrow(() -> new DataAccessException("Zestaw został w międzyczasie usunięty"));
        quiz.setTitle(newTitle);
        quizDAO.update(quiz);
        actionLogger.logAction("RENAME_SET", describe(quiz));
    }

    /**
     * Usuwa zestaw i jego powiązania; same fiszki zostają.
     * @param id identyfikator zestawu
     * @throws DataAccessException gdy operacja na bazie się nie uda
     */
    public void delete(int id) {
        quizDAO.delete(id);
        actionLogger.logAction("DELETE_SET", "#" + id);
    }

    /**
     * Identyfikatory fiszek zestawu.
     * @param quizId id zestawu
     * @return id fiszek w kolejności z zestawu
     * @throws DataAccessException gdy operacja na bazie się nie uda
     */
    public Set<Integer> flashcardIds(int quizId) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (Flashcard card : quizDAO.getFlashcardsForQuiz(quizId)) {
            ids.add(card.getId());
        }
        return ids;
    }

    /**
     * Ustawia skład zestawu na selectedIds: nowe fiszki dopisuje na koniec (w podanej kolejności),
     * odznaczone usuwa, a pozostałe zachowują swoje miejsce.
     * @param quizId id zestawu
     * @param selectedIds id fiszek, które mają być w zestawie
     * @throws DataAccessException gdy operacja na bazie się nie uda
     */
    public void updateFlashcards(int quizId, Collection<Integer> selectedIds) {
        Set<Integer> current = flashcardIds(quizId);
        Set<Integer> selected = new LinkedHashSet<>(selectedIds);
        int added = 0;
        int removed = 0;
        for (Integer id : current) {
            if (!selected.contains(id)) {
                quizDAO.removeFlashcardFromQuiz(quizId, id);
                removed++;
            }
        }
        for (Integer id : selected) {
            if (!current.contains(id) && quizDAO.addFlashcardToQuiz(quizId, id)) {
                added++;
            }
        }
        actionLogger.logAction("ASSIGN", "zestaw #" + quizId + ": dodano " + added + ", usunięto " + removed);
    }

    private static String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Nazwa zestawu jest wymagana");
        }
        return title.strip();
    }

    private static String describe(Quiz quiz) {
        return "#" + quiz.getId() + " " + quiz.getTitle();
    }
}
