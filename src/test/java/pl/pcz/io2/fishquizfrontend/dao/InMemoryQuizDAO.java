package pl.pcz.io2.fishquizfrontend.dao;

import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.model.Quiz;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Atrapa QuizDAO w pamięci dla testów serwisów. Fiszki bierze z InMemoryFlashcardDAO,
// więc (jak w bazie) powiązania z usuniętymi fiszkami są pomijane.
public class InMemoryQuizDAO implements QuizDAO {
    private final InMemoryFlashcardDAO flashcards;
    private final Map<Integer, Quiz> rows = new LinkedHashMap<>();
    // id zestawu → id fiszek w kolejności dodania
    private final Map<Integer, List<Integer>> links = new LinkedHashMap<>();
    private int nextId = 1;

    public InMemoryQuizDAO(InMemoryFlashcardDAO flashcards) {
        this.flashcards = flashcards;
    }

    @Override
    public void create(Quiz entity) {
        entity.setId(nextId++);
        rows.put(entity.getId(), copy(entity));
    }

    @Override
    public Optional<Quiz> read(Integer id) {
        return Optional.ofNullable(rows.get(id)).map(InMemoryQuizDAO::copy);
    }

    @Override
    public void update(Quiz entity) {
        if (rows.containsKey(entity.getId())) {
            rows.put(entity.getId(), copy(entity));
        }
    }

    @Override
    public void delete(Integer id) {
        rows.remove(id);
        links.remove(id);
    }

    @Override
    public List<Quiz> findAll() {
        return rows.values().stream().map(InMemoryQuizDAO::copy).toList();
    }

    @Override
    public List<Flashcard> getFlashcardsForQuiz(int quizId) {
        List<Flashcard> result = new ArrayList<>();
        for (Integer id : links.getOrDefault(quizId, List.of())) {
            flashcards.read(id).ifPresent(result::add);
        }
        return result;
    }

    @Override
    public boolean addFlashcardToQuiz(int quizId, int flashcardId) {
        List<Integer> ids = links.computeIfAbsent(quizId, key -> new ArrayList<>());
        if (ids.contains(flashcardId)) {
            return false;
        }
        ids.add(flashcardId);
        return true;
    }

    @Override
    public void removeFlashcardFromQuiz(int quizId, int flashcardId) {
        links.getOrDefault(quizId, new ArrayList<>()).remove(Integer.valueOf(flashcardId));
    }

    private static Quiz copy(Quiz source) {
        return new Quiz(source.getId(), source.getTitle());
    }
}
