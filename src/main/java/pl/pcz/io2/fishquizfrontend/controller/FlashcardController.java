package pl.pcz.io2.fishquizfrontend.controller;

import javafx.fxml.FXML;
import pl.pcz.io2.fishquizfrontend.dao.FlashcardDAO;
import pl.pcz.io2.fishquizfrontend.dao.impl.FlashcardDAOImpl;
import pl.pcz.io2.fishquizfrontend.dto.FlashcardDTO;
import pl.pcz.io2.fishquizfrontend.service.ActionLogger;
import pl.pcz.io2.fishquizfrontend.service.CsvImportExportService;
import pl.pcz.io2.fishquizfrontend.service.ImageManager;

public class FlashcardController {
    private final FlashcardDAO flashcardDAO = new FlashcardDAOImpl();
    private final ImageManager imageManager = new ImageManager();
    private final ActionLogger actionLogger = new ActionLogger();
    private final CsvImportExportService csvService = new CsvImportExportService();

    @FXML
    public void addFlashcard(FlashcardDTO dto) {
        actionLogger.logAction("CREATE", dto.getFrontText());
    }

    @FXML
    public void editFlashcard(FlashcardDTO dto) {
        actionLogger.logAction("UPDATE", dto.getFrontText());
    }

    @FXML
    public void deleteFlashcard(int id) {
        flashcardDAO.delete(id);
        actionLogger.logAction("DELETE", String.valueOf(id));
    }

    @FXML
    public void chooseImage() {
        // to do otwieranie zdjecia
    }
}