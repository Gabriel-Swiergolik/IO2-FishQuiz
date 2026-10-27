package pl.pcz.io2.fishquizdb;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

public class DatabaseManager {
    private static final String DATABASE_URL = "jdbc:sqlite:database.db";
    private static ConnectionSource connectionSource;

    public static void init(){
        try{
            if (connectionSource == null){
                connectionSource = new JdbcConnectionSource(DATABASE_URL);
                System.out.println("Połączono z bazą danych");
                System.out.println("Gdzie powstała baza: " + new java.io.File("database.db").getAbsolutePath());
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
            }
        }
    }

    public static void main(String[] args){
        init();
        close();

    }
}
