package pl.pcz.io2.fishquizfrontend.dao;

// Niesprawdzany wyjątek warstwy danych: opakowuje SQLException z ORMLite.
// DAO i DatabaseManager go rzucają, kontrolery łapią i pokazują Alert (patrz decyzja 0001).
public class DataAccessException extends RuntimeException {
    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
