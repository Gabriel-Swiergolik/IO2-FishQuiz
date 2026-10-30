package pl.pcz.io2.fishquizfrontend.service;

/** Sprawdzenie formularza fiszki: awers i rewers są wymagane (same spacje = puste). */
public final class FlashcardValidator {
    private FlashcardValidator() {}

    /**
     * Wynik sprawdzenia formularza.
     * @param frontMissing awers jest pusty
     * @param backMissing rewers jest pusty
     */
    public record Result(boolean frontMissing, boolean backMissing) {
        /** @return {@code true}, gdy awers i rewers są wypełnione */
        public boolean isValid() {
            return !frontMissing && !backMissing;
        }
    }

    /**
     * Sprawdza awers i rewers.
     * @param front awers (może być {@code null})
     * @param back rewers (może być {@code null})
     * @return które pola są puste
     */
    public static Result validate(String front, String back) {
        return new Result(isBlank(front), isBlank(back));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
