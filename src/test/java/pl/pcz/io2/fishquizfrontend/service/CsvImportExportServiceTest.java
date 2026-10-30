package pl.pcz.io2.fishquizfrontend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvImportExportServiceTest {
    @TempDir
    Path tempDir;

    private final CsvImportExportService service = new CsvImportExportService();

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
        assertEquals("﻿front,back,imagePath\r\n"
                + "kot,cat,\r\n"
                + "\"a,b\",\"powiedział \"\"tak\"\"\",x.png\r\n", content);
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
                + "a,b,c,d\n"                // linia 4: za dużo pól
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
