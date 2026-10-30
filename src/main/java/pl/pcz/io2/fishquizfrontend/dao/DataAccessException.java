package pl.pcz.io2.fishquizfrontend.dao;

/**
 * Niesprawdzany wyjątek warstwy danych: opakowuje SQLException z ORMLite.
 * DAO i DatabaseManager go rzucają, kontrolery łapią i pokazują Alert (patrz decyzja 0001).
 */
public class DataAccessException extends RuntimeException {
    /**
     * Wyjątek bez pierwotnej przyczyny (np. rekord usunięty w międzyczasie).
     * @param message komunikat po polsku, pokazywany użytkownikowi
     */
    public DataAccessException(String message) {
        super(message);
    }

    /**
     * Wyjątek opakowujący błąd bazy.
     * @param message komunikat po polsku, pokazywany użytkownikowi
     * @param cause pierwotny błąd, zwykle {@code SQLException}
     */
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
