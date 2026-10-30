package pl.pcz.io2.fishquizfrontend.util;

import javafx.beans.property.BooleanProperty;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Praca na bazie i plikach poza wątkiem JavaFX. Jeden wątek roboczy: SQLite i tak obsługuje
 * jednego piszącego, a połączenie z DatabaseManager nie jest używane równolegle.
 * Błędy pracy w tle pokazuje {@link ErrorDialogs}.
 */
public class FxTasks {
    private static final Logger log = LoggerFactory.getLogger(FxTasks.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "fishquiz-db");
        // Nie blokuje zamknięcia aplikacji, gdyby shutdown() nie został wywołany
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Uruchamia pracę w tle i przekazuje wynik do wątku JavaFX. Wywoływać z wątku JavaFX.
     * Na czas pracy {@code busy} ma wartość {@code true}, żeby kontroler mógł zablokować przyciski.
     * @param busy flaga zajętości (może być {@code null})
     * @param errorMessage opis błędu po polsku, pokazywany w oknie, gdy praca rzuci wyjątek
     * @param work praca na bazie lub plikach, wykonywana w wątku "fishquiz-db"
     * @param onSuccess obsługa wyniku w wątku JavaFX
     * @param <T> typ wyniku
     */
    public <T> void run(BooleanProperty busy, String errorMessage, Callable<T> work, Consumer<T> onSuccess) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        if (busy != null) {
            busy.set(true);
        }
        task.setOnSucceeded(e -> {
            if (busy != null) {
                busy.set(false);
            }
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            if (busy != null) {
                busy.set(false);
            }
            ErrorDialogs.show(errorMessage, task.getException());
        });
        executor.execute(task);
    }

    /** Czeka do 5 s na rozpoczęte zapisy, żeby nie zamknąć bazy w trakcie, a potem przerywa resztę. */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Zadania w tle nie zakończyły się w ciągu 5 s");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
