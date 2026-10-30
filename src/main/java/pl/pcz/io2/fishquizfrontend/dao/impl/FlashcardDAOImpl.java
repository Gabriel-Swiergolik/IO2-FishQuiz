package pl.pcz.io2.fishquizfrontend.dao.impl;

import pl.pcz.io2.fishquizfrontend.dao.FlashcardDAO;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizdb.DatabaseManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlashcardDAOImpl implements FlashcardDAO {
    @Override
    public void create(Flashcard entity) {}

    @Override
    public Optional<Flashcard> read(Integer id) { return Optional.empty(); }

    @Override
    public void update(Flashcard entity) {}

    @Override
    public void delete(Integer id) {}

    @Override
    public List<Flashcard> findAll() { return new ArrayList<>(); }

    @Override
    public List<Flashcard> getDueFlashcards() { return new ArrayList<>(); }
}