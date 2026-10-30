package pl.pcz.io2.fishquizdb;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import org.junit.jupiter.api.Test;
import pl.pcz.io2.fishquizfrontend.dao.DataAccessException;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.model.Quiz;
import pl.pcz.io2.fishquizfrontend.model.QuizFlashcard;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseManagerTest extends DbTestSupport {

    @Test
    void initGivesConnectionSourceAndCloseAllowsReinit() throws Exception {
        ConnectionSource first = DatabaseManager.getConnectionSource();
        assertNotNull(first);
        // init tworzy tabele, więc plik bazy już istnieje w katalogu tymczasowym
        assertTrue(Files.exists(tempDir.resolve("test.db")));

        DatabaseManager.close();
        DatabaseManager.init("jdbc:sqlite:" + tempDir.resolve("second.db"));

        ConnectionSource second = DatabaseManager.getConnectionSource();
        assertNotNull(second);
        assertNotSame(first, second);
    }

    @Test
    void initCreatesAllTablesOnEmptyDatabase() throws Exception {
        ConnectionSource source = DatabaseManager.getConnectionSource();

        assertTrue(DaoManager.createDao(source, Flashcard.class).isTableExists());
        assertTrue(DaoManager.createDao(source, Quiz.class).isTableExists());
        assertTrue(DaoManager.createDao(source, QuizFlashcard.class).isTableExists());
    }

    @Test
    void secondInitIsIdempotentAndKeepsData() throws Exception {
        String url = "jdbc:sqlite:" + tempDir.resolve("test.db");
        ConnectionSource source = DatabaseManager.getConnectionSource();
        DaoManager.createDao(source, Quiz.class).create(new Quiz(0, "Angielski"));

        // Bez close(): nic się nie dzieje, to samo połączenie
        DatabaseManager.init(url);
        assertSame(source, DatabaseManager.getConnectionSource());

        // Po close(): tabele już istnieją, dane zostają
        DatabaseManager.close();
        DatabaseManager.init(url);
        Dao<Quiz, Integer> quizDao = DaoManager.createDao(DatabaseManager.getConnectionSource(), Quiz.class);
        assertEquals(1, quizDao.countOf());
        assertEquals("Angielski", quizDao.queryForAll().get(0).getTitle());
    }

    @Test
    void initFailureThrowsAndAllowsRetry() {
        DatabaseManager.close();
        String badUrl = "jdbc:sqlite:" + tempDir.resolve("brak-katalogu").resolve("test.db");

        assertThrows(DataAccessException.class, () -> DatabaseManager.init(badUrl));

        // Po błędzie nie zostaje "pół-otwarte" połączenie, więc poprawny init działa
        DatabaseManager.init("jdbc:sqlite:" + tempDir.resolve("good.db"));
        assertTrue(Files.exists(tempDir.resolve("good.db")));
    }
}
