package pl.pcz.io2.fishquizdb;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pcz.io2.fishquizfrontend.dao.DataAccessException;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;
import pl.pcz.io2.fishquizfrontend.model.Quiz;
import pl.pcz.io2.fishquizfrontend.model.QuizFlashcard;

import java.sql.SQLException;

public class DatabaseManager {
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DEFAULT_DATABASE_URL = "jdbc:sqlite:database.db";
    private static ConnectionSource connectionSource;

    public static void init(){
        init(DEFAULT_DATABASE_URL);
    }

    // Pozwala podać inny plik bazy (np. tymczasowy w testach)
    // Przy błędzie rzuca DataAccessException i zostawia stan "bez połączenia", żeby można było spróbować ponownie
    public static void init(String databaseUrl){
        if (connectionSource != null){
            return;
        }
        try{
            connectionSource = new JdbcConnectionSource(databaseUrl);
            createTables(connectionSource);
            log.info("Połączono z bazą danych: {}", new java.io.File(databaseUrl.replace("jdbc:sqlite:", "")).getAbsolutePath());
        }catch (SQLException e){
            log.error("Błąd połączenia z bazą danych {}", databaseUrl, e);
            if (connectionSource != null){
                connectionSource.closeQuietly();
                connectionSource = null;
            }
            throw new DataAccessException("Nie udało się otworzyć bazy danych: " + databaseUrl, e);
        }
    }

    // Tworzy brakujące tabele; istniejące (i ich dane) zostają bez zmian
    private static void createTables(ConnectionSource source) throws SQLException {
        TableUtils.createTableIfNotExists(source, Flashcard.class);
        TableUtils.createTableIfNotExists(source, Quiz.class);
        TableUtils.createTableIfNotExists(source, QuizFlashcard.class);
    }

    public static ConnectionSource getConnectionSource(){
        if (connectionSource == null){
            init();
        }
        return connectionSource;
    }

    public static void close(){
        if (connectionSource != null){
            try{
                connectionSource.close();
                log.info("Zamknięto połączenie z bazą danych");
            } catch (Exception e){
                log.error("Błąd podczas zamykania połączenia z bazą danych", e);
            } finally {
                // Reset, żeby można było ponownie wywołać init() (np. w kolejnym teście)
                connectionSource = null;
            }
        }
    }

    public static void main(String[] args){
        init();
        close();

    }
}
