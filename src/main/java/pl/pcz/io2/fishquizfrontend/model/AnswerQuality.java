package pl.pcz.io2.fishquizfrontend.model;

public enum AnswerQuality {
    BLACKOUT(0),
    INCORRECT(1),
    HARD(2),
    GOOD(3),
    EASY(4),
    PERFECT(5);

    private final int value;

    AnswerQuality(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}