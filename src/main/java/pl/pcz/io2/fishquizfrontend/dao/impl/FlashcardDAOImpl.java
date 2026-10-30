package pl.pcz.io2.fishquizfrontend.dao.impl;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.support.ConnectionSource;
import pl.pcz.io2.fishquizfrontend.dao.DataAccessException;
import pl.pcz.io2.fishquizfrontend.dao.FlashcardDAO;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.model.QuizFlashcard;
import pl.pcz.io2.fishquizdb.DatabaseManager;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class FlashcardDAOImpl implements FlashcardDAO {
    private final ConnectionSource connectionSource;
    private final Dao<Flashcard, Integer> flashcardDao;
    private final Dao<QuizFlashcard, Integer> quizFlashcardDao;

    public FlashcardDAOImpl() {
        connectionSource = DatabaseManager.getConnectionSource();
        try {
            flashcardDao = DaoManager.createDao(connectionSource, Flashcard.class);
            quizFlashcardDao = DaoManager.createDao(connectionSource, QuizFlashcard.class);
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się przygotować dostępu do fiszek", e);
        }
    }

    @Override
    public void create(Flashcard entity) {
        try {
            // ORMLite sam ustawia wygenerowane id w obiekcie
            flashcardDao.create(entity);
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się zapisać fiszki", e);
        }
    }

    @Override
    public void createAll(List<Flashcard> flashcards) {
        try {
            // Jedna transakcja zamiast osobnego zatwierdzenia dla każdej fiszki
            TransactionManager.callInTransaction(connectionSource, () -> {
                for (Flashcard flashcard : flashcards) {
                    flashcardDao.create(flashcard);
                }
                return null;
            });
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się zapisać " + flashcards.size() + " fiszek", e);
        }
    }

    @Override
    public Optional<Flashcard> read(Integer id) {
        try {
            return Optional.ofNullable(flashcardDao.queryForId(id));
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się odczytać fiszki o id " + id, e);
        }
    }

    @Override
    public void update(Flashcard entity) {
        try {
            flashcardDao.update(entity);
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się zaktualizować fiszki o id " + entity.getId(), e);
        }
    }

    @Override
    public void delete(Integer id) {
        try {
            // Najpierw powiązania z quizami, potem sama fiszka; razem albo wcale
            TransactionManager.callInTransaction(connectionSource, () -> {
                DeleteBuilder<QuizFlashcard, Integer> links = quizFlashcardDao.deleteBuilder();
                links.where().eq(QuizFlashcard.FLASHCARD_ID_FIELD, id);
                links.delete();
                flashcardDao.deleteById(id);
                return null;
            });
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się usunąć fiszki o id " + id, e);
        }
    }

    @Override
    public List<Flashcard> findAll() {
        try {
            return flashcardDao.queryForAll();
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się wczytać fiszek", e);
        }
    }

    @Override
    public List<Flashcard> getDueFlashcards(LocalDate today) {
        try {
            // Daty są zapisane jako tekst ISO, więc porównanie "<=" odpowiada porównaniu dat
            return flashcardDao.queryBuilder()
                    .orderBy(Flashcard.NEXT_REVIEW_DATE_FIELD, true)
                    .orderBy(Flashcard.ID_FIELD, true)
                    .where().le(Flashcard.NEXT_REVIEW_DATE_FIELD, today)
                    .query();
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się wczytać fiszek do powtórki", e);
        }
    }
}
