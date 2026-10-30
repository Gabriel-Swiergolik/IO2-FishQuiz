package pl.pcz.io2.fishquizfrontend.dao.impl;

import pl.pcz.io2.fishquizfrontend.dao.QuizDAO;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.model.Quiz;
import pl.pcz.io2.fishquizdb.DatabaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuizDAOImpl implements QuizDAO {
    @Override
    public void create(Quiz entity) {}

    @Override
    public Optional<Quiz> read(Integer id) { return Optional.empty(); }

    @Override
    public void update(Quiz entity) {}

    @Override
    public void delete(Integer id) {}

    @Override
    public List<Quiz> findAll() { return new ArrayList<>(); }

    @Override
    public List<Flashcard> getFlashcardsForQuiz(int quizId) { return new ArrayList<>(); }
}