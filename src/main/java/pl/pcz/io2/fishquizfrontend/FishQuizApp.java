package pl.pcz.io2.fishquizfrontend;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import pl.pcz.io2.fishquizdb.DatabaseManager;
import pl.pcz.io2.fishquizfrontend.dao.DataAccessException;
import pl.pcz.io2.fishquizfrontend.util.ErrorDialogs;

import java.io.IOException;

/** Aplikacja JavaFX: otwiera bazę, tworzy {@link AppContext} i pokazuje główne okno (main-view.fxml). */
public class FishQuizApp extends Application {
    private AppContext context;

    /**
     * Otwiera bazę i pokazuje główne okno. Gdy bazy nie da się otworzyć, pokazuje błąd i zamyka aplikację.
     * @param primaryStage główne okno
     * @throws IOException gdy nie udało się wczytać main-view.fxml
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        ErrorDialogs.installUncaughtExceptionHandler();

        // Baza w start(), a nie w init(): Alert musi być pokazany w wątku JavaFX
        try {
            DatabaseManager.init();
            context = new AppContext();
        } catch (DataAccessException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd bazy danych");
            alert.setHeaderText("Nie udało się otworzyć lokalnej bazy danych.");
            alert.setContentText("Aplikacja zostanie zamknięta.\n\n" + e.getMessage());
            alert.showAndWait();
            Platform.exit();
            return;
        }

        // Fabryka przechodzi też na pliki z fx:include, więc wszystkie kontrolery dostają zależności z AppContext
        FXMLLoader loader = new FXMLLoader(FishQuizApp.class.getResource("main-view.fxml"));
        loader.setControllerFactory(context.controllerFactory());
        Parent root = loader.load();

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("System Fiszek - Zespół IO_2");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /** Kończy zadania w tle, a potem zamyka bazę. */
    @Override
    public void stop() {
        // Najpierw dokończ zapisy w tle, potem zamknij bazę
        if (context != null) {
            context.shutdown();
        }
        DatabaseManager.close();
    }

    /**
     * Uruchamia aplikację JavaFX ({@code Application.launch}).
     * @param args argumenty wiersza poleceń
     */
    public static void main(String[] args) {
        launch(args);
    }
}
