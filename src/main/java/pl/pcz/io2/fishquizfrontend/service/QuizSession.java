package pl.pcz.io2.fishquizfrontend.service;

import pl.pcz.io2.fishquizfrontend.dao.FlashcardDAO;
import pl.pcz.io2.fishquizfrontend.dao.QuizDAO;
import pl.pcz.io2.fishquizfrontend.model.AnswerQuality;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stan jednej sesji powtórki: kolejka fiszek do powtórki, bieżąca pozycja i oceny.
 * Kolejka to wszystkie fiszki do powtórki albo fiszki jednego zestawu (Quiz).
 * Bez JavaFX; QuizController woła start() i grade() przez FxTasks (baza).
 */
public class QuizSession {
    private final FlashcardDAO flashcardDAO;
    private final QuizDAO quizDAO;
    private final SpacedRepetitionService spacedRepetitionService;
    private final ActionLogger actionLogger;
    private final Clock clock;

    private final List<Flashcard> queue = new ArrayList<>();
    private final Map<AnswerQuality, Integer> counts = new EnumMap<>(AnswerQuality.class);
    private int index;

    /**
     * Sesja z zegarem systemowym.
     * @param flashcardDAO dostęp do fiszek
     * @param quizDAO dostęp do zestawów
     * @param spacedRepetitionService algorytm SM-2
     * @param actionLogger log akcji użytkownika
     */
    public QuizSession(FlashcardDAO flashcardDAO, QuizDAO quizDAO, SpacedRepetitionService spacedRepetitionService,
                       ActionLogger actionLogger) {
        this(flashcardDAO, quizDAO, spacedRepetitionService, actionLogger, Clock.systemDefaultZone());
    }

    /**
     * Zegar do wstrzyknięcia w testach (ten sam powinien dostać SpacedRepetitionService).
     * @param flashcardDAO dostęp do fiszek
     * @param quizDAO dostęp do zestawów
     * @param spacedRepetitionService algorytm SM-2
     * @param actionLogger log akcji użytkownika
     * @param clock zegar wyznaczający "dziś"
     */
    public QuizSession(FlashcardDAO flashcardDAO, QuizDAO quizDAO, SpacedRepetitionService spacedRepetitionService,
                       ActionLogger actionLogger, Clock clock) {
        this.flashcardDAO = flashcardDAO;
        this.quizDAO = quizDAO;
        this.spacedRepetitionService = spacedRepetitionService;
        this.actionLogger = actionLogger;
        this.clock = clock;
    }

    /**
     * Rozpoczyna nową sesję z fiszkami do powtórki na dziś.
     * @return liczba fiszek w kolejce
     * @throws pl.pcz.io2.fishquizfrontend.dao.DataAccessException gdy operacja na bazie się nie uda
     */
    public int start() {
        return begin(flashcardDAO.getDueFlashcards(LocalDate.now(clock)));
    }

    /**
     * Nowa sesja z fiszkami zestawu w jego kolejności.
     * @param quizId id zestawu
     * @param dueOnly {@code true}: tylko fiszki do powtórki na dziś; {@code false}: wszystkie
     * (ocena i tak przesuwa ich termin wg SM-2)
     * @return liczba fiszek w kolejce
     * @throws pl.pcz.io2.fishquizfrontend.dao.DataAccessException gdy operacja na bazie się nie uda
     */
    public int start(int quizId, boolean dueOnly) {
        LocalDate today = LocalDate.now(clock);
        List<Flashcard> cards = quizDAO.getFlashcardsForQuiz(quizId);
        if (dueOnly) {
            cards = cards.stream()
                    .filter(card -> card.getNextReviewDate() != null && !card.getNextReviewDate().isAfter(today))
                    .toList();
        }
        return begin(cards);
    }

    private int begin(List<Flashcard> cards) {
        queue.clear();
        queue.addAll(cards);
        counts.clear();
        index = 0;
        return queue.size();
    }

    /**
     * Sprawdza, czy sesja się skończyła.
     * @return {@code true}, gdy wszystkie fiszki z kolejki zostały ocenione lub kolejka jest pusta
     */
    public boolean isFinished() {
        return index >= queue.size();
    }

    /**
     * Fiszka, którą teraz pokazujemy.
     * @return bieżąca fiszka do pokazania
     * @throws IllegalStateException gdy sesja jest zakończona
     */
    public Flashcard current() {
        if (isFinished()) {
            throw new IllegalStateException("Sesja powtórki jest zakończona");
        }
        return queue.get(index);
    }

    /**
     * Pozycja bieżącej fiszki w kolejce (do licznika „Fiszka 3 z 10”).
     * @return numer bieżącej fiszki liczony od 1
     */
    public int position() {
        return index + 1;
    }

    /**
     * Długość kolejki tej sesji.
     * @return liczba fiszek w kolejce
     */
    public int size() {
        return queue.size();
    }

    /**
     * Ocena bieżącej fiszki: SM-2 → zapis → następna fiszka. Przy błędzie zapisu zostajemy na tej samej fiszce.
     * Fiszka usunięta w trakcie sesji jest pomijana bez oceny.
     * @param quality ocena odpowiedzi
     * @throws IllegalStateException gdy sesja jest zakończona
     * @throws pl.pcz.io2.fishquizfrontend.dao.DataAccessException gdy operacja na bazie się nie uda
     */
    public void grade(AnswerQuality quality) {
        Flashcard shown = current();
        // Świeży odczyt, żeby nie nadpisać zmian z formularza zrobionych w trakcie sesji
        Optional<Flashcard> stored = flashcardDAO.read(shown.getId());
        if (stored.isPresent()) {
            Flashcard card = stored.get();
            spacedRepetitionService.calculateNextReview(card, quality);
            flashcardDAO.update(card);
            actionLogger.logAction("REVIEW", "#" + card.getId() + " " + card.getFrontText()
                    + ": ocena " + quality.getValue() + ", następna powtórka " + card.getNextReviewDate());
            counts.merge(quality, 1, Integer::sum);
        }
        // Fiszkę usuniętą w trakcie sesji po prostu pomijamy
        index++;
    }

    /**
     * Liczba ocen w tej sesji (bez fiszek pominiętych, bo usuniętych).
     * @return liczba ocenionych fiszek w tej sesji
     */
    public int reviewedCount() {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Liczba odpowiedzi z daną oceną (do podsumowania).
     * @param quality ocena
     * @return ile razy w tej sesji wybrano tę ocenę
     */
    public int countFor(AnswerQuality quality) {
        return counts.getOrDefault(quality, 0);
    }

    /**
     * Odsetek poprawnych odpowiedzi w tej sesji.
     * @return odsetek odpowiedzi z oceną >= 3 (zaokrąglony); 0, gdy nic nie oceniono
     */
    public int percentCorrect() {
        int reviewed = reviewedCount();
        if (reviewed == 0) {
            return 0;
        }
        int correct = counts.entrySet().stream()
                .filter(entry -> entry.getKey().getValue() >= 3)
                .mapToInt(Map.Entry::getValue)
                .sum();
        return (int) Math.round(100.0 * correct / reviewed);
    }
}
