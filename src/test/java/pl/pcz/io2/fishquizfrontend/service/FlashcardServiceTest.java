package pl.pcz.io2.fishquizfrontend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.pcz.io2.fishquizdb.DbTestSupport;
import pl.pcz.io2.fishquizfrontend.dao.DataAccessException;
import pl.pcz.io2.fishquizfrontend.dao.FlashcardDAO;
import pl.pcz.io2.fishquizfrontend.dao.InMemoryFlashcardDAO;
import pl.pcz.io2.fishquizfrontend.dao.impl.FlashcardDAOImpl;
import pl.pcz.io2.fishquizfrontend.dto.FlashcardDTO;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static java.nio.charset.StandardCharsets.UTF_8;

class FlashcardServiceTest extends DbTestSupport {
    private static final LocalDate REVIEW = LocalDate.of(2026, 5, 1);

    private Path imagesDir;
    private ImageManager imageManager;
    private FlashcardDAO dao;
    private FlashcardService service;

    @BeforeEach
    void setUp() {
        imagesDir = tempDir.resolve("images");
        imageManager = new ImageManager(imagesDir);
        dao = new FlashcardDAOImpl();
        service = new FlashcardService(dao, imageManager, new CsvImportExportService(), new ActionLogger());
    }

    private File sourceImage(String name) throws IOException {
        Path file = tempDir.resolve(name);
        Files.write(file, new byte[]{1, 2, 3});
        return file.toFile();
    }

    private static FlashcardDTO form(int id, String front, String back) {
        FlashcardDTO dto = new FlashcardDTO();
        dto.setId(id);
        dto.setFrontText(front);
        dto.setBackText(back);
        return dto;
    }

    private long imageFileCount() throws IOException {
        if (!Files.isDirectory(imagesDir)) return 0;
        try (Stream<Path> files = Files.list(imagesDir)) {
            return files.count();
        }
    }

    @Test
    void createSavesNewCardWithCopiedImage() throws IOException {
        Flashcard saved = service.save(form(0, "  kot ", "cat\n"), sourceImage("kot.png"), false);

        assertTrue(saved.getId() > 0);
        Flashcard stored = dao.read(saved.getId()).orElseThrow();
        // Spacje na brzegach są obcinane
        assertEquals("kot", stored.getFrontText());
        assertEquals("cat", stored.getBackText());
        assertTrue(Files.isRegularFile(imagesDir.resolve(stored.getImagePath())));
        assertEquals(2.5, stored.getEasinessFactor());
    }

    @Test
    void createWithoutImageLeavesPathEmpty() {
        Flashcard saved = service.save(form(0, "kot", "cat"), null, false);

        assertNull(dao.read(saved.getId()).orElseThrow().getImagePath());
    }

    @Test
    void invalidFormIsRejectedAndNothingSaved() throws IOException {
        File image = sourceImage("kot.png");

        assertThrows(IllegalArgumentException.class, () -> service.save(form(0, " ", "cat"), image, false));
        assertTrue(dao.findAll().isEmpty());
        assertEquals(0, imageFileCount());
    }

    @Test
    void editUpdatesTextsAndKeepsSm2State() {
        Flashcard card = new Flashcard(0, "kot", "cat", null);
        card.setInterval(6);
        card.setRepetitions(2);
        card.setEasinessFactor(2.6);
        card.setNextReviewDate(REVIEW);
        dao.create(card);

        service.save(form(card.getId(), "pies", "dog"), null, false);

        Flashcard stored = dao.read(card.getId()).orElseThrow();
        assertEquals("pies", stored.getFrontText());
        assertEquals("dog", stored.getBackText());
        assertEquals(6, stored.getInterval());
        assertEquals(2, stored.getRepetitions());
        assertEquals(2.6, stored.getEasinessFactor());
        assertEquals(REVIEW, stored.getNextReviewDate());
        assertEquals(1, dao.findAll().size());
    }

    @Test
    void editWithoutImageChangeKeepsImage() throws IOException {
        Flashcard card = service.save(form(0, "kot", "cat"), sourceImage("kot.png"), false);

        service.save(form(card.getId(), "kot", "cat (zwierzę)"), null, false);

        String path = dao.read(card.getId()).orElseThrow().getImagePath();
        assertEquals(card.getImagePath(), path);
        assertTrue(Files.isRegularFile(imagesDir.resolve(path)));
    }

    @Test
    void replacingImageDeletesOldFile() throws IOException {
        Flashcard card = service.save(form(0, "kot", "cat"), sourceImage("kot.png"), false);
        String oldPath = card.getImagePath();

        service.save(form(card.getId(), "kot", "cat"), sourceImage("kot2.jpg"), false);

        String newPath = dao.read(card.getId()).orElseThrow().getImagePath();
        assertNotEquals(oldPath, newPath);
        assertFalse(Files.exists(imagesDir.resolve(oldPath)));
        assertTrue(Files.isRegularFile(imagesDir.resolve(newPath)));
        assertEquals(1, imageFileCount());
    }

    @Test
    void removingImageClearsPathAndDeletesFile() throws IOException {
        Flashcard card = service.save(form(0, "kot", "cat"), sourceImage("kot.png"), false);

        service.save(form(card.getId(), "kot", "cat"), null, true);

        assertNull(dao.read(card.getId()).orElseThrow().getImagePath());
        assertEquals(0, imageFileCount());
    }

    @Test
    void imageSharedWithAnotherCardIsNotDeleted() throws IOException {
        Flashcard first = service.save(form(0, "kot", "cat"), sourceImage("kot.png"), false);
        // Np. po eksporcie i imporcie CSV druga fiszka wskazuje ten sam plik
        Flashcard second = new Flashcard(0, "kot (kopia)", "cat", first.getImagePath());
        dao.create(second);

        service.save(form(first.getId(), "kot", "cat"), null, true);

        assertTrue(Files.isRegularFile(imagesDir.resolve(second.getImagePath())));
    }

    @Test
    void editOfDeletedCardFails() {
        Flashcard card = service.save(form(0, "kot", "cat"), null, false);
        dao.delete(card.getId());

        assertThrows(DataAccessException.class, () -> service.save(form(card.getId(), "kot", "cat"), null, false));
    }

    @Test
    void newImageIsRemovedWhenDatabaseWriteFails() throws IOException {
        InMemoryFlashcardDAO failing = new InMemoryFlashcardDAO();
        failing.failWrites = true;
        FlashcardService failingService = new FlashcardService(failing, imageManager, new CsvImportExportService(), new ActionLogger());
        File image = sourceImage("kot.png");

        assertThrows(DataAccessException.class, () -> failingService.save(form(0, "kot", "cat"), image, false));
        assertEquals(0, imageFileCount());
    }

    @Test
    void findAllMapsCardsToDtos() {
        Flashcard card = new Flashcard(0, "kot", "cat", null);
        card.setNextReviewDate(REVIEW);
        dao.create(card);

        List<FlashcardDTO> all = service.findAll();

        assertEquals(1, all.size());
        assertEquals(card.getId(), all.get(0).getId());
        assertEquals("kot", all.get(0).getFrontText());
        assertEquals(REVIEW, all.get(0).getNextReviewDate());
    }

    @Test
    void deleteRemovesCardAndItsImage() throws IOException {
        Flashcard card = service.save(form(0, "kot", "cat"), sourceImage("kot.png"), false);

        service.delete(card.getId());

        assertTrue(dao.read(card.getId()).isEmpty());
        assertEquals(0, imageFileCount());
    }

    @Test
    void deleteKeepsImageUsedByAnotherCard() throws IOException {
        Flashcard first = service.save(form(0, "kot", "cat"), sourceImage("kot.png"), false);
        Flashcard second = new Flashcard(0, "kot (kopia)", "cat", first.getImagePath());
        dao.create(second);

        service.delete(first.getId());

        assertTrue(Files.isRegularFile(imagesDir.resolve(second.getImagePath())));
    }

    @Test
    void deleteOfMissingCardDoesNothing() {
        service.delete(12345);

        assertTrue(dao.findAll().isEmpty());
    }

    @Test
    void importSavesValidRowsAndReportsSkipped() throws IOException {
        Path csv = tempDir.resolve("fiszki.csv");
        Files.writeString(csv, "front,back,imagePath\nkot,cat,\n,pusty awers,\n\"pies, duży\",dog,pies.png\n", UTF_8);

        CsvImportExportService.ImportResult result = service.importCsv(csv.toFile());

        assertEquals(2, result.flashcards().size());
        assertEquals(1, result.skippedRows().size());
        List<String> fronts = dao.findAll().stream().map(Flashcard::getFrontText).sorted().toList();
        assertEquals(List.of("kot", "pies, duży"), fronts);
    }

    @Test
    void exportThenImportRestoresCards() {
        service.save(form(0, "kot", "cat"), null, false);
        service.save(form(0, "żółw", "turtle, \"slow\"\nanimal"), null, false);
        File csv = tempDir.resolve("eksport.csv").toFile();

        assertEquals(2, service.exportCsv(csv));
        for (FlashcardDTO dto : service.findAll()) {
            service.delete(dto.getId());
        }
        service.importCsv(csv);

        List<String> backs = dao.findAll().stream().map(Flashcard::getBackText).sorted().toList();
        assertEquals(List.of("cat", "turtle, \"slow\"\nanimal"), backs);
    }
}
