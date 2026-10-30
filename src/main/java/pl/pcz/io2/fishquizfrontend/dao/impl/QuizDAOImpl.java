package pl.pcz.io2.fishquizfrontend.dao.impl;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.support.ConnectionSource;
import pl.pcz.io2.fishquizfrontend.dao.DataAccessException;
import pl.pcz.io2.fishquizfrontend.dao.QuizDAO;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.model.Quiz;
import pl.pcz.io2.fishquizfrontend.model.QuizFlashcard;
import pl.pcz.io2.fishquizdb.DatabaseManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link QuizDAO} na ORMLite. Skład zestawu jest w tabeli {@link QuizFlashcard}.
 */
public class QuizDAOImpl implements QuizDAO {
    private final ConnectionSource connectionSource;
    private final Dao<Quiz, Integer> quizDao;
    private final Dao<QuizFlashcard, Integer> quizFlashcardDao;
    private final Dao<Flashcard, Integer> flashcardDao;

    /**
     * Tworzy DAO ORMLite na połączeniu z {@link DatabaseManager#getConnectionSource()}.
     * @throws DataAccessException gdy nie udało się przygotować DAO
     */
    public QuizDAOImpl() {
        connectionSource = DatabaseManager.getConnectionSource();
        try {
            quizDao = DaoManager.createDao(connectionSource, Quiz.class);
            quizFlashcardDao = DaoManager.createDao(connectionSource, QuizFlashcard.class);
            flashcardDao = DaoManager.createDao(connectionSource, Flashcard.class);
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się przygotować dostępu do quizów", e);
        }
    }

    /**
     * Zapisuje nowy zestaw; ORMLite ustawia wygenerowane id w obiekcie.
     * @param entity zestaw do zapisania
     * @throws DataAccessException gdy zapis się nie uda
     */
    @Override
    public void create(Quiz entity) {
        try {
            quizDao.create(entity);
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się zapisać quizu", e);
        }
    }

    /**
     * Odczytuje zestaw o podanym id.
     * @param id identyfikator
     * @return zestaw albo pusty {@code Optional}, gdy nie istnieje
     * @throws DataAccessException gdy odczyt się nie uda
     */
    @Override
    public Optional<Quiz> read(Integer id) {
        try {
            return Optional.ofNullable(quizDao.queryForId(id));
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się odczytać quizu o id " + id, e);
        }
    }

    /**
     * Zapisuje zmiany zestawu (wg id).
     * @param entity zestaw ze zmianami
     * @throws DataAccessException gdy zapis się nie uda
     */
    @Override
    public void update(Quiz entity) {
        try {
            quizDao.update(entity);
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się zaktualizować quizu o id " + entity.getId(), e);
        }
    }

    /**
     * Usuwa zestaw i jego przypisania w jednej transakcji; same fiszki zostają.
     * @param id identyfikator zestawu
     * @throws DataAccessException gdy usunięcie się nie uda (nic nie zostaje usunięte)
     */
    @Override
    public void delete(Integer id) {
        try {
            // Najpierw powiązania z fiszkami, potem sam quiz; fiszki zostają
            TransactionManager.callInTransaction(connectionSource, () -> {
                DeleteBuilder<QuizFlashcard, Integer> links = quizFlashcardDao.deleteBuilder();
                links.where().eq(QuizFlashcard.QUIZ_ID_FIELD, id);
                links.delete();
                quizDao.deleteById(id);
                return null;
            });
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się usunąć quizu o id " + id, e);
        }
    }

    /**
     * @return wszystkie zestawy
     * @throws DataAccessException gdy odczyt się nie uda
     */
    @Override
    public List<Quiz> findAll() {
        try {
            return quizDao.queryForAll();
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się wczytać quizów", e);
        }
    }

    @Override
    public List<Flashcard> getFlashcardsForQuiz(int quizId) {
        try {
            // Kolumny powiązań to zwykłe inty (nie pola foreign), więc zamiast joina są dwa zapytania
            List<QuizFlashcard> links = quizFlashcardDao.queryBuilder()
                    .orderBy(QuizFlashcard.ORDER_INDEX_FIELD, true)
                    .orderBy(QuizFlashcard.ID_FIELD, true)
                    .where().eq(QuizFlashcard.QUIZ_ID_FIELD, quizId)
                    .query();
            if (links.isEmpty()) {
                return new ArrayList<>();
            }
            List<Integer> ids = links.stream().map(QuizFlashcard::getFlashcardId).toList();
            Map<Integer, Flashcard> byId = flashcardDao.queryBuilder()
                    .where().in(Flashcard.ID_FIELD, ids)
                    .query().stream()
                    .collect(Collectors.toMap(Flashcard::getId, Function.identity()));
            // Kolejność z tabeli powiązań; powiązania do nieistniejących fiszek pomijamy
            List<Flashcard> result = new ArrayList<>();
            for (Integer id : ids) {
                Flashcard card = byId.get(id);
                if (card != null) {
                    result.add(card);
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się wczytać fiszek quizu o id " + quizId, e);
        }
    }

    @Override
    public boolean addFlashcardToQuiz(int quizId, int flashcardId) {
        try {
            return TransactionManager.callInTransaction(connectionSource, () -> {
                long existing = quizFlashcardDao.queryBuilder()
                        .where().eq(QuizFlashcard.QUIZ_ID_FIELD, quizId)
                        .and().eq(QuizFlashcard.FLASHCARD_ID_FIELD, flashcardId)
                        .countOf();
                if (existing > 0) {
                    return false;
                }
                QuizFlashcard last = quizFlashcardDao.queryBuilder()
                        .orderBy(QuizFlashcard.ORDER_INDEX_FIELD, false)
                        .limit(1L)
                        .where().eq(QuizFlashcard.QUIZ_ID_FIELD, quizId)
                        .queryForFirst();
                int nextIndex = last == null ? 0 : last.getOrderIndex() + 1;
                quizFlashcardDao.create(new QuizFlashcard(quizId, flashcardId, nextIndex));
                return true;
            });
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się dodać fiszki " + flashcardId + " do quizu " + quizId, e);
        }
    }

    @Override
    public void removeFlashcardFromQuiz(int quizId, int flashcardId) {
        try {
            DeleteBuilder<QuizFlashcard, Integer> links = quizFlashcardDao.deleteBuilder();
            links.where().eq(QuizFlashcard.QUIZ_ID_FIELD, quizId)
                    .and().eq(QuizFlashcard.FLASHCARD_ID_FIELD, flashcardId);
            links.delete();
        } catch (SQLException e) {
            throw new DataAccessException("Nie udało się usunąć fiszki " + flashcardId + " z quizu " + quizId, e);
        }
    }
}
