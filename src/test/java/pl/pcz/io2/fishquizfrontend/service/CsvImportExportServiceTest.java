package pl.pcz.io2.fishquizfrontend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvImportExportServiceTest {
    private static final byte[] IMAGE = {(byte) 0x89, 'P', 'N', 'G', 0, 1, 2, (byte) 0xFF};

    @TempDir
    Path tempDir;

    private Path imagesDir;
    private CsvImportExportService service;

    @BeforeEach
    void setUp() {
        imagesDir = tempDir.resolve("images");
        service = new CsvImportExportService(new ImageManager(imagesDir));
    }

    private long imageFileCount() throws IOException {
        if (!Files.isDirectory(imagesDir)) return 0;
        try (Stream<Path> files = Files.list(imagesDir)) {
            return files.count();
        }
    }

    private File file(String content) throws IOException {
        Path file = tempDir.resolve("import.csv");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file.toFile();
    }

    private List<String> fronts(CsvImportExportService.ImportResult result) {
        return result.flashcards().stream().map(Flashcard::getFrontText).toList();
    }

    @Test
    void roundTripKeepsCommasQuotesNewlinesAndPolishLetters() {
        List<Flashcard> cards = List.of(
                new Flashcard(1, "zażółć, gęślą", "jaźń \"w cudzysłowie\"\nnowa linia\r\ni CRLF", "abc.png"),
                new Flashcard(2, "ĄĆĘŁŃÓŚŹŻ", "ąćęłńóśźż", null),
                new Flashcard(3, "\"", ",", ""));
        File file = tempDir.resolve("fiszki.csv").toFile();

        service.exportToCsv(cards, file);
        CsvImportExportService.ImportResult result = service.importFromCsv(file);

        assertTrue(result.skippedRows().isEmpty(), result.skippedRows().toString());
        assertEquals(3, result.flashcards().size());
        for (int i = 0; i < cards.size(); i++) {
            Flashcard expected = cards.get(i);
            Flashcard actual = result.flashcards().get(i);
            assertEquals(expected.getFrontText(), actual.getFrontText());
            assertEquals(expected.getBackText(), actual.getBackText());
        }
        assertEquals("abc.png", result.flashcards().get(0).getImagePath());
        // Brak obrazu wraca jako null, także dla pustego tekstu
        assertNull(result.flashcards().get(1).getImagePath());
        assertNull(result.flashcards().get(2).getImagePath());
    }

    @Test
    void exportWritesBomHeaderAndQuotedRows() throws IOException {
        File file = tempDir.resolve("fiszki.csv").toFile();

        service.exportToCsv(List.of(
                new Flashcard(1, "kot", "cat", null),
                new Flashcard(2, "a,b", "powiedział \"tak\"", "x.png")), file);

        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        // x.png nie istnieje, więc pole imageData jest puste
        assertEquals("﻿front,back,imagePath,imageData\r\n"
                + "kot,cat,,\r\n"
                + "\"a,b\",\"powiedział \"\"tak\"\"\",x.png,\r\n", content);
    }

    @Test
    void exportEmbedsImageAsBase64() throws IOException {
        Files.createDirectories(imagesDir);
        Files.write(imagesDir.resolve("abc.png"), IMAGE);
        File file = tempDir.resolve("fiszki.csv").toFile();

        service.exportToCsv(List.of(
                new Flashcard(1, "kot", "cat", "abc.png"),
                new Flashcard(2, "pies", "dog", "brak-pliku.png")), file);

        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        assertEquals("kot,cat,abc.png," + Base64.getEncoder().encodeToString(IMAGE), lines.get(1));
        assertEquals("pies,dog,brak-pliku.png,", lines.get(2));
    }

    @Test
    void importRestoresEmbeddedImageIntoEmptyImagesDir() throws IOException {
        String data = Base64.getEncoder().encodeToString(IMAGE);

        CsvImportExportService.ImportResult result = service.importFromCsv(
                file("front,back,imagePath,imageData\r\nkot,cat,stary.png," + data + "\r\n"));

        assertTrue(result.skippedRows().isEmpty(), result.skippedRows().toString());
        String path = result.flashcards().get(0).getImagePath();
        assertNotEquals("stary.png", path);
        assertTrue(path.endsWith(".png"));
        assertArrayEquals(IMAGE, Files.readAllBytes(imagesDir.resolve(path)));
        assertEquals(List.of(path), result.savedImages());
    }

    @Test
    void legacyThreeAndTwoColumnFilesImportAsBefore() throws IOException {
        CsvImportExportService.ImportResult result = service.importFromCsv(
                file("front,back,imagePath\r\nkot,cat,abc.png\r\npies,dog,\r\nmysz,mouse\r\n"));

        assertTrue(result.skippedRows().isEmpty(), result.skippedRows().toString());
        assertEquals(List.of("kot", "pies", "mysz"), fronts(result));
        assertEquals("abc.png", result.flashcards().get(0).getImagePath());
        assertNull(result.flashcards().get(1).getImagePath());
        assertNull(result.flashcards().get(2).getImagePath());
        assertTrue(result.savedImages().isEmpty());
        assertEquals(0, imageFileCount());
    }

    @Test
    void invalidEmbeddedImageRowsAreSkipped() throws IOException {
        String data = Base64.getEncoder().encodeToString(IMAGE);
        String csv = "front,back,imagePath,imageData\n"
                + "kot,cat,a.png,to-nie-jest-base64!\n"   // linia 2: złe Base64
                + "pies,dog,notatki.txt," + data + "\n"    // linia 3: nieobsługiwany format
                + "mysz,mouse,," + data + "\n"             // linia 4: brak nazwy pliku
                + "ryba,fish,b.png," + data + "\n";        // linia 5: OK

        CsvImportExportService.ImportResult result = service.importFromCsv(file(csv));

        assertEquals(List.of("ryba"), fronts(result));
        assertEquals(3, result.skippedRows().size());
        assertEquals("Wiersz 2: niepoprawne dane obrazu (Base64)", result.skippedRows().get(0));
        assertTrue(result.skippedRows().get(1).startsWith("Wiersz 3: Nieobsługiwany format obrazu"),
                result.skippedRows().get(1));
        assertTrue(result.skippedRows().get(2).startsWith("Wiersz 4: dane obrazu bez nazwy pliku"),
                result.skippedRows().get(2));
        // Plik powstał tylko dla poprawnego wiersza
        assertEquals(1, imageFileCount());
    }

    @Test
    void importedCardsAreNewWithDefaultState() throws IOException {
        CsvImportExportService.ImportResult result = service.importFromCsv(file("front,back,imagePath\r\nkot,cat,\r\n"));

        Flashcard card = result.flashcards().get(0);
        assertEquals(0, card.getId());
        assertEquals(0, card.getInterval());
        assertEquals(0, card.getRepetitions());
        assertEquals(2.5, card.getEasinessFactor());
    }

    @Test
    void malformedRowsAreSkippedAndReportedWithLineNumbers() throws IOException {
        String csv = "front,back,imagePath\n"
                + "kot,cat,\n"               // linia 2: OK
                + "tylko-jedno-pole\n"       // linia 3: za mało pól
                + "a,b,c,d,e\n"              // linia 4: za dużo pól
                + ",pusty awers,\n"          // linia 5: pusty awers
                + "\"zły\"x,cudzysłów,\n"    // linia 6: znak po cudzysłowie
                + "\"wiele\nlinii\",ok,\n"   // linie 7-8: OK
                + "pies,dog\n";              // linia 9: OK bez imagePath

        CsvImportExportService.ImportResult result = service.importFromCsv(file(csv));

        assertEquals(List.of("kot", "wiele\nlinii", "pies"), fronts(result));
        assertEquals(4, result.skippedRows().size());
        assertTrue(result.skippedRows().get(0).startsWith("Wiersz 3:"), result.skippedRows().get(0));
        assertTrue(result.skippedRows().get(1).startsWith("Wiersz 4:"), result.skippedRows().get(1));
        assertTrue(result.skippedRows().get(2).startsWith("Wiersz 5:"), result.skippedRows().get(2));
        assertTrue(result.skippedRows().get(3).startsWith("Wiersz 6:"), result.skippedRows().get(3));
    }

    @Test
    void unclosedQuoteSkipsOnlyTheBrokenRow() throws IOException {
        String csv = "front,back,imagePath\r\nkot,cat,\r\npies,\"bez końca\r\nmysz,mouse,\r\n";

        CsvImportExportService.ImportResult result = service.importFromCsv(file(csv));

        // Niezamknięty cudzysłów pochłania resztę pliku, więc wcześniejsze wiersze zostają
        assertEquals(List.of("kot"), fronts(result));
        assertEquals(1, result.skippedRows().size());
        assertTrue(result.skippedRows().get(0).startsWith("Wiersz 3:"), result.skippedRows().get(0));
    }

    @Test
    void importWorksWithoutHeaderAndWithLfAndBlankLines() throws IOException {
        CsvImportExportService.ImportResult result = service.importFromCsv(file("kot,cat\n\npies,dog,p.png\n\n"));

        assertTrue(result.skippedRows().isEmpty(), result.skippedRows().toString());
        assertEquals(List.of("kot", "pies"), fronts(result));
        assertEquals("p.png", result.flashcards().get(1).getImagePath());
    }

    @Test
    void emptyFileGivesEmptyResult() throws IOException {
        CsvImportExportService.ImportResult result = service.importFromCsv(file(""));

        assertTrue(result.flashcards().isEmpty());
        assertTrue(result.skippedRows().isEmpty());
    }
}
