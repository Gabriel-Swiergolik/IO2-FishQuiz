package pl.pcz.io2.fishquizfrontend.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

// Kontroler głównego okna (main-view.fxml). Zakładki przełącza TabPane,
// a tu tylko łączymy widoki: odświeżanie listy i przejście do edycji.
public class HomeController {
    @FXML private TabPane tabPane;
    @FXML private Tab formTab;
    @FXML private Tab listTab;
    @FXML private Tab quizTab;

    // Kontrolery z fx:include (fx:id + "Controller")
    @FXML private FlashcardController formController;
    @FXML private FlashcardListController listController;
    @FXML private QuizController quizController;

    @FXML
    private void initialize() {
        formController.setOnEditFinished(() -> tabPane.getSelectionModel().select(listTab));
        listController.setOnEditRequested(card -> {
            formController.loadForEdit(card);
            tabPane.getSelectionModel().select(formTab);
        });
        listTab.selectedProperty().addListener((obs, was, selected) -> {
            if (selected) {
                listController.refresh();
            }
        });
    }
}
