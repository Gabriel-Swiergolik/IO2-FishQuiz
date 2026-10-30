package pl.pcz.io2.fishquizdb;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.StringType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// ORMLite 6.1 nie obsługuje LocalDate, więc zapisujemy datę jako tekst ISO (yyyy-MM-dd).
// Taki format sortuje się tak samo jak daty, więc porównanie "<=" w zapytaniach działa poprawnie.
public class LocalDatePersister extends StringType {
    private static final LocalDatePersister singleton = new LocalDatePersister();

    private LocalDatePersister() {
        super(SqlType.STRING, new Class<?>[] { LocalDate.class });
    }

    public static LocalDatePersister getSingleton() {
        return singleton;
    }

    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
        return ((LocalDate) javaObject).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
        return LocalDate.parse((String) sqlArg, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
