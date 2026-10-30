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

// Praca na bazie i plikach poza wątkiem JavaFX. Jeden wątek roboczy: SQLite i tak obsługuje
// jednego piszącego, a połączenie z DatabaseManager nie jest używane równolegle.
public class FxTasks {
    private static final Logger log = LoggerFactory.getLogger(FxTasks.class);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "fishquiz-db");
        // Nie blokuje zamknięcia aplikacji, gdyby shutdown() nie został wywołany
        thread.setDaemon(true);
        return thread;
    });

    // work: w tle; onSuccess: w wątku JavaFX; błąd: okno z errorMessage.
    // busy (może być null) jest true na czas pracy, żeby kontroler mógł zablokować przyciski.
    // Wywoływać z wątku JavaFX.
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

    // Czeka chwilę na rozpoczęte zapisy, żeby nie zamknąć bazy w trakcie
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
