package pl.pcz.io2.fishquizfrontend.service;

// Sprawdzenie formularza fiszki: awers i rewers są wymagane (same spacje = puste)
public final class FlashcardValidator {
    private FlashcardValidator() {}

    public record Result(boolean frontMissing, boolean backMissing) {
        public boolean isValid() {
            return !frontMissing && !backMissing;
        }
    }

    public static Result validate(String front, String back) {
        return new Result(isBlank(front), isBlank(back));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
