package pl.pcz.io2.fishquizfrontend;

import javafx.util.Callback;
import pl.pcz.io2.fishquizfrontend.controller.FlashcardController;
import pl.pcz.io2.fishquizfrontend.controller.FlashcardListController;
import pl.pcz.io2.fishquizfrontend.controller.HomeController;
import pl.pcz.io2.fishquizfrontend.controller.QuizController;
import pl.pcz.io2.fishquizfrontend.controller.QuizSetsController;
import pl.pcz.io2.fishquizfrontend.dao.FlashcardDAO;
import pl.pcz.io2.fishquizfrontend.dao.QuizDAO;
import pl.pcz.io2.fishquizfrontend.dao.impl.FlashcardDAOImpl;
import pl.pcz.io2.fishquizfrontend.dao.impl.QuizDAOImpl;
import pl.pcz.io2.fishquizfrontend.service.ActionLogger;
import pl.pcz.io2.fishquizfrontend.service.CsvImportExportService;
import pl.pcz.io2.fishquizfrontend.service.FlashcardService;
import pl.pcz.io2.fishquizfrontend.service.ImageManager;
import pl.pcz.io2.fishquizfrontend.service.QuizSession;
import pl.pcz.io2.fishquizfrontend.service.QuizSetService;
import pl.pcz.io2.fishquizfrontend.service.SpacedRepetitionService;
import pl.pcz.io2.fishquizfrontend.util.FxTasks;

import java.util.Map;
import java.util.function.Supplier;

// Jedno miejsce, w którym powstają DAO i serwisy (po jednym egzemplarzu na aplikację).
// Kontrolery dostają je przez konstruktor z fabryki podpiętej do FXMLLoader.
// Tworzyć po DatabaseManager.init().
public class AppContext {
    private final FlashcardDAO flashcardDAO = new FlashcardDAOImpl();
    private final QuizDAO quizDAO = new QuizDAOImpl();
    private final SpacedRepetitionService spacedRepetitionService = new SpacedRepetitionService();
    private final ImageManager imageManager = new ImageManager();
    private final CsvImportExportService csvService = new CsvImportExportService();
    private final ActionLogger actionLogger = new ActionLogger();
    private final FlashcardService flashcardService = new FlashcardService(flashcardDAO, imageManager, csvService, actionLogger);
    private final QuizSetService quizSetService = new QuizSetService(quizDAO, actionLogger);
    private final FxTasks fxTasks = new FxTasks();

    private final Map<Class<?>, Supplier<Object>> controllers = Map.of(
            HomeController.class, HomeController::new,
            FlashcardController.class, () -> new FlashcardController(flashcardService, imageManager, fxTasks),
            FlashcardListController.class, () -> new FlashcardListController(flashcardService, fxTasks),
            QuizController.class, () -> new QuizController(
                    new QuizSession(flashcardDAO, quizDAO, spacedRepetitionService, actionLogger),
                    quizSetService, imageManager, fxTasks, controllerFactory()),
            QuizSetsController.class, () -> new QuizSetsController(quizSetService, flashcardService, fxTasks)
    );

    public Callback<Class<?>, Object> controllerFactory() {
        return type -> {
            Supplier<Object> supplier = controllers.get(type);
            if (supplier == null) {
                throw new IllegalArgumentException("Nieznany kontroler: " + type.getName());
            }
            return supplier.get();
        };
    }

    public void shutdown() {
        fxTasks.shutdown();
    }
}
