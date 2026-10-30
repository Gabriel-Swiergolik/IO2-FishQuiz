package pl.pcz.io2.fishquizfrontend.model;

/**
 * Ocena odpowiedzi w skali SM-2 (0–5). Oceny od 3 w górę ({@link #GOOD} i wyżej) liczą się jako poprawne.
 */
public enum AnswerQuality {
    /** 0, w quizie „Nie pamiętam”: brak odpowiedzi. */
    BLACKOUT(0),
    /** 1, „Źle”: odpowiedź błędna. */
    INCORRECT(1),
    /** 2, „Trudno”: w SM-2 jeszcze ocena negatywna (poniżej 3). */
    HARD(2),
    /** 3, „Dobrze”: odpowiedź poprawna. */
    GOOD(3),
    /** 4, „Łatwo”: odpowiedź poprawna bez większego wysiłku. */
    EASY(4),
    /** 5, „Idealnie”: odpowiedź poprawna bez wahania. */
    PERFECT(5);

    private final int value;

    /**
     * Ocena o podanej wartości liczbowej.
     * @param value ocena liczbowa 0–5
     */
    AnswerQuality(int value) {
        this.value = value;
    }

    /** @return ocena liczbowa 0–5 używana w algorytmie SM-2 */
    public int getValue() {
        return value;
    }
}