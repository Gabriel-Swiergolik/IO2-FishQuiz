package pl.pcz.io2.fishquizfrontend.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlashcardValidatorTest {

    @Test
    void filledFrontAndBackAreValid() {
        FlashcardValidator.Result result = FlashcardValidator.validate("kot", "cat");

        assertTrue(result.isValid());
        assertFalse(result.frontMissing());
        assertFalse(result.backMissing());
    }

    @Test
    void emptyFrontIsReported() {
        FlashcardValidator.Result result = FlashcardValidator.validate("", "cat");

        assertFalse(result.isValid());
        assertTrue(result.frontMissing());
        assertFalse(result.backMissing());
    }

    @Test
    void whitespaceOnlyCountsAsMissing() {
        FlashcardValidator.Result result = FlashcardValidator.validate("kot", " \n\t ");

        assertFalse(result.isValid());
        assertFalse(result.frontMissing());
        assertTrue(result.backMissing());
    }

    @Test
    void nullsAreMissing() {
        FlashcardValidator.Result result = FlashcardValidator.validate(null, null);

        assertTrue(result.frontMissing());
        assertTrue(result.backMissing());
    }
}
