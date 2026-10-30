package pl.pcz.io2.fishquizfrontend;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import pl.pcz.io2.fishquizdb.DatabaseManager;
import pl.pcz.io2.fishquizfrontend.dao.DataAccessException;

public class FishQuizApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Baza w start(), a nie w init(): Alert musi być pokazany w wątku JavaFX
        try {
            DatabaseManager.init();
        } catch (DataAccessException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Błąd bazy danych");
            alert.setHeaderText("Nie udało się otworzyć lokalnej bazy danych.");
            alert.setContentText("Aplikacja zostanie zamknięta.\n\n" + e.getMessage());
            alert.showAndWait();
            Platform.exit();
            return;
        }

        TabPane tabPane = new TabPane();

        Tab addTab = new Tab("Dodawanie fiszek");
        addTab.setClosable(false); // Blokada zamykania zakładki
        VBox addLayout = new VBox(15);
        addLayout.setPadding(new Insets(20));
        addLayout.getChildren().addAll(
                new Label("Wprowadź dane nowej fiszki:"),
                new TextField("Awers (np. słówko)"),
                new TextArea("Rewers (np. tłumaczenie i opis)"),
                new Button("Zapisz w lokalnej bazie")
        );
        addTab.setContent(addLayout);

        Tab viewTab = new Tab("Przeglądanie");
        viewTab.setClosable(false);
        VBox viewLayout = new VBox(15);
        viewLayout.setAlignment(Pos.CENTER);
        viewLayout.getChildren().addAll(
                new Label("Tutaj pojawi się tabela lub siatka z Twoimi fiszkami."),
                new Button("Wczytaj fiszki")
        );
        viewTab.setContent(viewLayout);

        Tab quizTab = new Tab("Quiz");
        quizTab.setClosable(false);
        VBox quizLayout = new VBox(15);
        quizLayout.setAlignment(Pos.CENTER);
        quizLayout.getChildren().addAll(
                new Label("Gotowy na powtórkę interwałową?"),
                new Button("Rozpocznij test")
        );
        quizTab.setContent(quizLayout);

        tabPane.getTabs().addAll(addTab, viewTab, quizTab);
        Scene scene = new Scene(tabPane, 600, 400);
        primaryStage.setTitle("System Fiszek - Zespół IO_2");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    @Override
    public void stop() {
        DatabaseManager.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
