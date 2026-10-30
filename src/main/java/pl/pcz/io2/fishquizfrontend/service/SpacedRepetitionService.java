package pl.pcz.io2.fishquizfrontend.service;

import pl.pcz.io2.fishquizfrontend.model.AnswerQuality;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;

import java.time.Clock;
import java.time.LocalDate;

/**
 * Algorytm SM-2: na podstawie oceny odpowiedzi (0–5) ustala kolejną datę powtórki.
 * Tylko logika; zapis zmienionej fiszki do bazy należy do wywołującego.
 */
public class SpacedRepetitionService {
    private static final double MIN_EASINESS_FACTOR = 1.3;

    private final Clock clock;

    /** Serwis z zegarem systemowym. */
    public SpacedRepetitionService() {
        this(Clock.systemDefaultZone());
    }

    /**
     * Serwis z podanym zegarem (do wstrzyknięcia w testach).
     * @param clock zegar wyznaczający "dziś"
     */
    public SpacedRepetitionService(Clock clock) {
        this.clock = clock;
    }

    /**
     * Przelicza stan SM-2 po ocenie. Ocena poniżej 3 zaczyna naukę od nowa (odstęp 1 dzień);
     * w przeciwnym razie odstęp wynosi 1, potem 6 dni, potem poprzedni odstęp razy EF.
     * EF zmienia się przy każdej ocenie, ale nie spada poniżej 1.3. Zapis do bazy należy do wywołującego.
     * @param flashcard fiszka, zmieniana w miejscu
     * @param quality ocena odpowiedzi
     * @return ta sama fiszka (po zmianie)
     */
    public Flashcard calculateNextReview(Flashcard flashcard, AnswerQuality quality) {
        int q = quality.getValue();
        double easiness = flashcard.getEasinessFactor();

        if (q < 3) {
            // Błędna odpowiedź: nauka od początku
            flashcard.setRepetitions(0);
            flashcard.setInterval(1);
        } else {
            int repetitions = flashcard.getRepetitions();
            int interval;
            if (repetitions == 0) {
                interval = 1;
            } else if (repetitions == 1) {
                interval = 6;
            } else {
                // Odstęp liczony ze starym EF, jak w standardowym SM-2
                interval = (int) Math.round(flashcard.getInterval() * easiness);
            }
            flashcard.setInterval(interval);
            flashcard.setRepetitions(repetitions + 1);
        }

        // EF zmienia się przy każdej ocenie, ale nie spada poniżej 1.3
        double newEasiness = easiness + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02));
        flashcard.setEasinessFactor(Math.max(MIN_EASINESS_FACTOR, newEasiness));

        flashcard.setNextReviewDate(LocalDate.now(clock).plusDays(flashcard.getInterval()));
        return flashcard;
    }
}
