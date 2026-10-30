package pl.pcz.io2.fishquizdb;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

public class DatabaseManager {
    private static final String DEFAULT_DATABASE_URL = "jdbc:sqlite:database.db";
    private static ConnectionSource connectionSource;

    public static void init(){
        init(DEFAULT_DATABASE_URL);
    }

    // Pozwala podać inny plik bazy (np. tymczasowy w testach)
    public static void init(String databaseUrl){
        try{
            if (connectionSource == null){
                connectionSource = new JdbcConnectionSource(databaseUrl);
                System.out.println("Połączono z bazą danych");
                System.out.println("Gdzie powstała baza: " + new java.io.File(databaseUrl.replace("jdbc:sqlite:", "")).getAbsolutePath());
                // Place for future tables
            }
        }catch (SQLException e){
            System.err.println("Błąd połaczenia z bazą danych: " + e.getMessage());
            e.printStackTrace();
        }
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
                System.out.println("Zamknięto połączenie z bazą danych");
            } catch (Exception e){
                System.err.println("Błąd podczas zamykania połączenia: " + e.getMessage());
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
