package pl.pcz.io2.fishquizfrontend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pcz.io2.fishquizfrontend.dao.DataAccessException;
import pl.pcz.io2.fishquizfrontend.dao.FlashcardDAO;
import pl.pcz.io2.fishquizfrontend.dto.FlashcardDTO;
import pl.pcz.io2.fishquizfrontend.mapper.FlashcardMapper;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;

import java.io.File;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

// Operacje na fiszkach wywoływane z widoków: łączy DAO, pliki obrazów i log akcji.
// Bez JavaFX UI, więc da się to testować na tymczasowej bazie. Metody blokują (baza, pliki),
// dlatego kontrolery wołają je przez FxTasks.
public class FlashcardService {
    private static final Logger log = LoggerFactory.getLogger(FlashcardService.class);

    private final FlashcardDAO flashcardDAO;
    private final ImageManager imageManager;
    private final CsvImportExportService csvService;
    private final ActionLogger actionLogger;

    public FlashcardService(FlashcardDAO flashcardDAO, ImageManager imageManager,
                            CsvImportExportService csvService, ActionLogger actionLogger) {
        this.flashcardDAO = flashcardDAO;
        this.imageManager = imageManager;
        this.csvService = csvService;
        this.actionLogger = actionLogger;
    }

    public List<FlashcardDTO> findAll() {
        return flashcardDAO.findAll().stream().map(FlashcardMapper::toDto).toList();
    }

    // Zapis z formularza. dto.getId() == 0 → nowa fiszka, inaczej edycja istniejącej (stan SM-2 zostaje).
    // newImage: nowo wybrany plik (albo null); removeImage: użytkownik usunął obrazek.
    // Poprzedni plik obrazu jest kasowany dopiero po udanym zapisie w bazie.
    public Flashcard save(FlashcardDTO dto, File newImage, boolean removeImage) {
        if (!FlashcardValidator.validate(dto.getFrontText(), dto.getBackText()).isValid()) {
            throw new IllegalArgumentException("Awers i rewers fiszki są wymagane");
        }
        FlashcardDTO values = new FlashcardDTO();
        values.setId(dto.getId());
        values.setFrontText(dto.getFrontText().strip());
        values.setBackText(dto.getBackText().strip());

        String newPath = newImage != null ? imageManager.saveImage(newImage) : null;
        try {
            if (values.getId() == 0) {
                values.setImagePath(newPath);
                Flashcard card = FlashcardMapper.toModel(values);
                flashcardDAO.create(card);
                actionLogger.logAction("CREATE", describe(card));
                return card;
            }

            // Świeży odczyt: quiz mógł w międzyczasie zmienić stan SM-2 tej fiszki
            Flashcard card = flashcardDAO.read(values.getId())
                    .orElseThrow(() -> new DataAccessException("Fiszka została w międzyczasie usunięta"));
            String oldPath = card.getImagePath();
            values.setImagePath(newPath != null ? newPath : removeImage ? null : oldPath);
            FlashcardMapper.updateModel(values, card);
            flashcardDAO.update(card);
            actionLogger.logAction("UPDATE", describe(card));

            if (oldPath != null && !oldPath.equals(card.getImagePath())) {
                deleteImageIfUnused(oldPath, card.getId());
            }
            return card;
        } catch (RuntimeException e) {
            // Zapis się nie udał: nie zostawiamy osieroconej kopii nowego obrazu
            if (newPath != null) {
                deleteImageQuietly(newPath);
            }
            throw e;
        }
    }

    // Usuwa fiszkę i (jeśli nikt inny go nie używa) jej plik obrazu
    public void delete(int id) {
        Optional<Flashcard> card = flashcardDAO.read(id);
        flashcardDAO.delete(id);
        card.ifPresent(deleted -> {
            if (deleted.getImagePath() != null) {
                deleteImageIfUnused(deleted.getImagePath(), id);
            }
            actionLogger.logAction("DELETE", describe(deleted));
        });
    }

    // Import CSV: poprawne wiersze trafiają do bazy w jednej transakcji, pominięte są w wyniku
    public CsvImportExportService.ImportResult importCsv(File file) {
        CsvImportExportService.ImportResult result = csvService.importFromCsv(file);
        flashcardDAO.createAll(result.flashcards());
        actionLogger.logAction("IMPORT", file.getName() + ": zaimportowano " + result.flashcards().size()
                + ", pominięto " + result.skippedRows().size());
        return result;
    }

    // Zwraca liczbę wyeksportowanych fiszek
    public int exportCsv(File file) {
        List<Flashcard> flashcards = flashcardDAO.findAll();
        csvService.exportToCsv(flashcards, file);
        actionLogger.logAction("EXPORT", file.getName() + ": " + flashcards.size() + " fiszek");
        return flashcards.size();
    }

    // Kasuje plik obrazu, chyba że używa go inna fiszka (np. po eksporcie i imporcie CSV
    // dwie fiszki wskazują ten sam plik). Liczba fiszek jest mała, więc wystarczy findAll.
    private void deleteImageIfUnused(String path, int excludingId) {
        boolean usedElsewhere = flashcardDAO.findAll().stream()
                .anyMatch(other -> other.getId() != excludingId && path.equals(other.getImagePath()));
        if (!usedElsewhere) {
            deleteImageQuietly(path);
        }
    }

    // Zmiana w bazie już się udała, więc problem z plikiem tylko logujemy
    private void deleteImageQuietly(String path) {
        try {
            imageManager.deleteImage(path);
        } catch (UncheckedIOException e) {
            log.warn("Nie udało się usunąć pliku obrazu {}", path, e);
        }
    }

    private static String describe(Flashcard card) {
        return "#" + card.getId() + " " + card.getFrontText();
    }
}
