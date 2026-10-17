package pl.pcz.io2.fishquizfrontend.model;

public class QuizFlashcard {
    private int quizId;
    private int flashcardId;
    private int orderIndex;

    public QuizFlashcard() {}
    public QuizFlashcard(int quizId, int flashcardId, int orderIndex) {
        this.quizId = quizId;
        this.flashcardId = flashcardId;
        this.orderIndex = orderIndex;
    }

    public int getQuizId() { return quizId; }
    public void setQuizId(int quizId) { this.quizId = quizId; }
    public int getFlashcardId() { return flashcardId; }
    public void setFlashcardId(int flashcardId) { this.flashcardId = flashcardId; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}