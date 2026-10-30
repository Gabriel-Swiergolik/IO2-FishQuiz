package pl.pcz.io2.fishquizfrontend.dao;

import pl.pcz.io2.fishquizfrontend.model.Flashcard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// Atrapa FlashcardDAO w pamięci dla testów serwisów. Jak prawdziwa baza oddaje kopie,
// więc zmiana zwróconego obiektu nie zmienia "zapisanych" danych bez update().
public class InMemoryFlashcardDAO implements FlashcardDAO {
    private final Map<Integer, Flashcard> rows = new LinkedHashMap<>();
    private int nextId = 1;
    // true → create/update/delete rzucają DataAccessException (symulacja błędu bazy)
    public boolean failWrites;
    public int updateCount;

    @Override
    public void create(Flashcard entity) {
        failIfNeeded();
        entity.setId(nextId++);
        rows.put(entity.getId(), copy(entity));
    }

    @Override
    public void createAll(List<Flashcard> flashcards) {
        failIfNeeded();
        flashcards.forEach(this::create);
    }

    @Override
    public Optional<Flashcard> read(Integer id) {
        return Optional.ofNullable(rows.get(id)).map(InMemoryFlashcardDAO::copy);
    }

    @Override
    public void update(Flashcard entity) {
        failIfNeeded();
        updateCount++;
        if (rows.containsKey(entity.getId())) {
            rows.put(entity.getId(), copy(entity));
        }
    }

    @Override
    public void delete(Integer id) {
        failIfNeeded();
        rows.remove(id);
    }

    @Override
    public List<Flashcard> findAll() {
        return rows.values().stream().map(InMemoryFlashcardDAO::copy).toList();
    }

    @Override
    public List<Flashcard> getDueFlashcards(LocalDate today) {
        return rows.values().stream()
                .filter(card -> card.getNextReviewDate() != null && !card.getNextReviewDate().isAfter(today))
                .sorted(Comparator.comparing(Flashcard::getNextReviewDate).thenComparing(Flashcard::getId))
                .map(InMemoryFlashcardDAO::copy)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void failIfNeeded() {
        if (failWrites) {
            throw new DataAccessException("Symulowany błąd zapisu");
        }
    }

    private static Flashcard copy(Flashcard source) {
        Flashcard card = new Flashcard(source.getId(), source.getFrontText(), source.getBackText(), source.getImagePath());
        card.setNextReviewDate(source.getNextReviewDate());
        card.setInterval(source.getInterval());
        card.setRepetitions(source.getRepetitions());
        card.setEasinessFactor(source.getEasinessFactor());
        return card;
    }
}
