package pl.pcz.io2.fishquizfrontend.util;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

/** Wspólne okno błędu: krótki opis po polsku, szczegóły (stos wywołań) do rozwinięcia. */
public final class ErrorDialogs {
    private static final Logger log = LoggerFactory.getLogger(ErrorDialogs.class);

    private ErrorDialogs() {}

    /**
     * Zapisuje błąd w logu i pokazuje okno błędu. Można wołać z dowolnego wątku.
     * @param message krótki opis po polsku (nagłówek okna)
     * @param error przyczyna; jej komunikat i stos wywołań są w oknie (może być {@code null})
     */
    public static void show(String message, Throwable error) {
        log.error(message, error);
        if (Platform.isFxApplicationThread()) {
            showAlert(message, error);
        } else {
            Platform.runLater(() -> showAlert(message, error));
        }
    }

    /**
     * Ostatnia linia obrony: wyjątek, którego nikt nie złapał (w dowolnym wątku), też trafia do okna błędu.
     */
    public static void installUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            try {
                show("Nieoczekiwany błąd aplikacji.", error);
            } catch (Throwable dialogError) {
                // Samo okno się nie udało (np. brak toolkitu): zostaje log, bez ponownego wywołania
                log.error("Nie udało się pokazać okna błędu", dialogError);
            }
        });
    }

    private static void showAlert(String message, Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Błąd");
        alert.setHeaderText(message);
        alert.setContentText(error == null || error.getMessage() == null ? "Nieznana przyczyna." : error.getMessage());

        if (error != null) {
            // Stos wywołań tylko do okna; na konsolę trafia przez log.error w show()
            StringWriter details = new StringWriter();
            error.printStackTrace(new PrintWriter(details));
            TextArea area = new TextArea(details.toString());
            area.setEditable(false);
            area.setWrapText(false);
            area.setPrefRowCount(12);
            alert.getDialogPane().setExpandableContent(area);
        }
        // show() zamiast showAndWait(): showAndWait jest zabronione w trakcie animacji i układania widoku
        alert.show();
    }
}
