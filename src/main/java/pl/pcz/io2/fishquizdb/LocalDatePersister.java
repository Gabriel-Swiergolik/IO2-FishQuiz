package pl.pcz.io2.fishquizdb;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Zapis {@code LocalDate} w kolumnie tekstowej. ORMLite 6.1 nie obsługuje LocalDate, więc zapisujemy datę jako tekst ISO (yyyy-MM-dd).
 * Taki format sortuje się tak samo jak daty, więc porównanie {@code <=} w zapytaniach działa poprawnie.
 */
public class LocalDatePersister extends StringType {
    private static final LocalDatePersister singleton = new LocalDatePersister();

    private LocalDatePersister() {
        super(SqlType.STRING, new Class<?>[] { LocalDate.class });
    }

    /**
     * Jedyna instancja; ORMLite wywołuje tę metodę, gdy pole ma {@code persisterClass = LocalDatePersister.class}.
     * @return instancja persistera
     */
    public static LocalDatePersister getSingleton() {
        return singleton;
    }

    /**
     * Zamienia datę na tekst ISO przed zapisem do bazy.
     * @param fieldType opis pola (nieużywany)
     * @param javaObject wartość pola, {@code LocalDate}
     * @return data jako tekst {@code yyyy-MM-dd}
     */
    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
        return ((LocalDate) javaObject).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * Odczytuje datę zapisaną jako tekst ISO.
     * @param fieldType opis pola (nieużywany)
     * @param sqlArg wartość z bazy, tekst {@code yyyy-MM-dd}
     * @param columnPos numer kolumny (nieużywany)
     * @return odczytana data
     */
    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
        return LocalDate.parse((String) sqlArg, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
