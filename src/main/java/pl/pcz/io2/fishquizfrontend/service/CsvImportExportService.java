package pl.pcz.io2.fishquizfrontend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Eksport i import fiszek w formacie CSV.
 *
 * <p>Format pliku:
 * <ul>
 *   <li>kodowanie UTF-8; eksport dopisuje na początku BOM (żeby Excel rozpoznał UTF-8), import go pomija;</li>
 *   <li>pierwszy wiersz to nagłówek {@code front,back,imagePath,imageData};</li>
 *   <li>cytowanie wg RFC 4180: pole zawierające {@code ,}, {@code "} lub znak nowej linii jest ujęte
 *       w cudzysłowy, a {@code "} wewnątrz jest podwojony; wiersze kończą się CRLF;</li>
 *   <li>{@code imagePath} to ścieżka z {@link ImageManager}; pusta wartość oznacza brak obrazu;</li>
 *   <li>{@code imageData} to zawartość pliku obrazu w Base64 (jedna linia, bez cudzysłowów), więc plik CSV
 *       wystarcza do odtworzenia obrazów. Format obrazu wynika z rozszerzenia w {@code imagePath}.
 *       Gdy pliku obrazu nie da się odczytać przy eksporcie, pole jest puste (WARN w logu).
 *       Base64 zajmuje ok. 4/3 rozmiaru obrazu, a Excel obcina komórki dłuższe niż 32 767 znaków,
 *       więc pliku z dużymi obrazami nie należy zapisywać w Excelu.</li>
 * </ul>
 * Postęp SM-2 i zestawy nie są eksportowane.
 *
 * <p>Import jest pobłażliwy: akceptuje też końce linii LF, brak nagłówka i stary format bez kolumn
 * {@code imageData} i {@code imagePath}. Gdy {@code imageData} jest puste, {@code imagePath} trafia do fiszki
 * bez zmian (jak w starym formacie). Gdy nie jest puste, obraz jest zapisywany przez {@link ImageManager}
 * pod nową nazwą (UUID), także przy pustym katalogu obrazów. Puste linie są pomijane bez komunikatu.
 * Błędny wiersz (zła liczba pól, pusty awers lub rewers, niezamknięty lub źle postawiony cudzysłów,
 * niepoprawne Base64, nieobsługiwany format obrazu) jest pomijany, a import trwa dalej. Opis każdego
 * pominiętego wiersza (z numerem linii w pliku) trafia do {@code ImportResult.skippedRows()} i do logu (WARN).
 *
 * <p>Serwis nie zapisuje niczego w bazie; zaimportowane fiszki zapisuje {@link FlashcardService} przez DAO.
 * Pliki obrazów zapisane przy imporcie są w {@code ImportResult.savedImages()}, żeby dało się je usunąć,
 * jeśli zapis w bazie się nie uda.
 */
public class CsvImportExportService {
    private static final Logger log = LoggerFactory.getLogger(CsvImportExportService.class);
    private static final String HEADER = "front,back,imagePath,imageData";
    private static final String EOL = "\r\n";
    private static final char BOM = '﻿';

    /**
     * Wynik importu.
     * @param flashcards nowe fiszki (id 0, domyślny stan SM-2), jeszcze niezapisane w bazie
     * @param skippedRows opisy pominiętych wierszy, z numerem linii w pliku
     * @param savedImages nazwy plików obrazów zapisanych z kolumny imageData
     */
    public record ImportResult(List<Flashcard> flashcards, List<String> skippedRows, List<String> savedImages) {}

    /** Jeden rekord CSV; error != null oznacza wiersz, którego nie dało się poprawnie odczytać. */
    private record CsvRecord(int line, List<String> fields, String error) {}

    private final ImageManager imageManager;

    /**
     * Serwis zapisujący i odczytujący obrazy przez podany menedżer.
     * @param imageManager zapis obrazów przy imporcie i odczyt przy eksporcie
     */
    public CsvImportExportService(ImageManager imageManager) {
        this.imageManager = imageManager;
    }

    /**
     * Odczytuje fiszki z pliku CSV (format w opisie klasy). Błędne wiersze są pomijane.
     * Gdy import zostanie przerwany wyjątkiem, zapisane do tej pory obrazy są usuwane.
     * @param file plik CSV w UTF-8
     * @return fiszki do zapisania, pominięte wiersze i zapisane obrazy
     * @throws java.io.UncheckedIOException gdy nie udało się odczytać pliku lub zapisać obrazu
     */
    public ImportResult importFromCsv(File file) {
        String content;
        try {
            content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Nie udało się odczytać pliku CSV " + file, e);
        }
        if (!content.isEmpty() && content.charAt(0) == BOM) {
            content = content.substring(1);
        }

        List<Flashcard> flashcards = new ArrayList<>();
        List<String> skippedRows = new ArrayList<>();
        List<String> savedImages = new ArrayList<>();
        boolean firstRecord = true;
        try {
            for (CsvRecord record : parse(content)) {
                String error = record.error();
                if (error == null && firstRecord && isHeader(record.fields())) {
                    firstRecord = false;
                    continue;
                }
                firstRecord = false;

                List<String> fields = record.fields();
                if (error == null && (fields.size() < 2 || fields.size() > 4)) {
                    error = "oczekiwano od 2 do 4 pól, jest " + fields.size();
                }
                if (error == null && (fields.get(0).isBlank() || fields.get(1).isBlank())) {
                    error = "pusty awers lub rewers";
                }
                String imagePath = error == null && fields.size() >= 3 && !fields.get(2).isBlank() ? fields.get(2) : null;
                String imageData = error == null && fields.size() == 4 ? fields.get(3).strip() : "";
                byte[] imageBytes = null;
                if (error == null && !imageData.isEmpty()) {
                    if (imagePath == null) {
                        error = "dane obrazu bez nazwy pliku (nie wiadomo, jaki to format)";
                    } else {
                        try {
                            imageBytes = Base64.getDecoder().decode(imageData);
                        } catch (IllegalArgumentException e) {
                            error = "niepoprawne dane obrazu (Base64)";
                        }
                    }
                }
                if (imageBytes != null) {
                    // Zapis na końcu, więc pominięty wiersz nie zostawia pliku
                    try {
                        imagePath = imageManager.saveImage(imageBytes, imagePath);
                        savedImages.add(imagePath);
                    } catch (IllegalArgumentException e) {
                        error = e.getMessage();
                    }
                }
                if (error != null) {
                    String message = "Wiersz " + record.line() + ": " + error;
                    log.warn("Import CSV {}: pominięto. {}", file, message);
                    skippedRows.add(message);
                    continue;
                }

                flashcards.add(new Flashcard(0, fields.get(0), fields.get(1), imagePath));
            }
        } catch (RuntimeException e) {
            // Import przerwany (np. brak miejsca na dysku): usuwamy obrazy zapisane do tej pory
            savedImages.forEach(this::deleteImageQuietly);
            throw e;
        }
        return new ImportResult(flashcards, skippedRows, savedImages);
    }

    /**
     * Zapisuje fiszki do pliku CSV (format w opisie klasy). Brak pliku obrazu nie przerywa eksportu.
     * @param flashcards fiszki do eksportu
     * @param file plik docelowy (nadpisywany)
     * @throws java.io.UncheckedIOException gdy nie udało się zapisać pliku
     */
    public void exportToCsv(List<Flashcard> flashcards, File file) {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(BOM);
            writer.write(HEADER);
            writer.write(EOL);
            for (Flashcard flashcard : flashcards) {
                writer.write(quote(flashcard.getFrontText()));
                writer.write(',');
                writer.write(quote(flashcard.getBackText()));
                writer.write(',');
                writer.write(quote(flashcard.getImagePath()));
                writer.write(',');
                writer.write(encodeImage(flashcard.getImagePath(), file));
                writer.write(EOL);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Nie udało się zapisać pliku CSV " + file, e);
        }
    }

    /**
     * Base64 nie zawiera przecinków, cudzysłowów ani nowych linii, więc nie wymaga cytowania.
     * Brak pliku obrazu nie przerywa eksportu: fiszka trafia do pliku bez obrazu.
     */
    private String encodeImage(String imagePath, File file) {
        if (imagePath == null || imagePath.isBlank()) {
            return "";
        }
        Path image = imageManager.resolvePath(imagePath);
        try {
            return Base64.getEncoder().encodeToString(Files.readAllBytes(image));
        } catch (IOException e) {
            log.warn("Eksport CSV {}: nie udało się odczytać obrazu {}, fiszka bez danych obrazu ({})", file, image, e.toString());
            return "";
        }
    }

    private void deleteImageQuietly(String path) {
        try {
            imageManager.deleteImage(path);
        } catch (UncheckedIOException e) {
            log.warn("Nie udało się usunąć pliku obrazu {}", path, e);
        }
    }

    private static String quote(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static boolean isHeader(List<String> fields) {
        return fields.size() >= 2
                && fields.get(0).trim().equalsIgnoreCase("front")
                && fields.get(1).trim().equalsIgnoreCase("back");
    }

    /** Prosty parser RFC 4180: pola w cudzysłowach mogą zawierać przecinki, "" i nowe linie. */
    private static List<CsvRecord> parse(String text) {
        List<CsvRecord> records = new ArrayList<>();
        int n = text.length();
        int i = 0;
        int line = 1;

        while (i < n) {
            int recordLine = line;
            List<String> fields = new ArrayList<>();
            StringBuilder field = new StringBuilder();
            String error = null;
            boolean blank = true;

            while (true) {
                if (i < n && text.charAt(i) == '"') {
                    blank = false;
                    i++;
                    boolean closed = false;
                    while (i < n) {
                        char c = text.charAt(i);
                        if (c == '"') {
                            if (i + 1 < n && text.charAt(i + 1) == '"') {
                                field.append('"');
                                i += 2;
                            } else {
                                i++;
                                closed = true;
                                break;
                            }
                        } else {
                            if (c == '\n') line++;
                            field.append(c);
                            i++;
                        }
                    }
                    if (!closed) {
                        // Do końca pliku nie było zamykającego cudzysłowu: reszta pliku to ten jeden wiersz
                        error = "niezamknięty cudzysłów";
                        break;
                    }
                    if (i < n && !isSeparator(text.charAt(i))) {
                        error = "nieoczekiwany znak po zamykającym cudzysłowie";
                        while (i < n && text.charAt(i) != '\n') i++;
                    }
                } else {
                    while (i < n && !isSeparator(text.charAt(i))) {
                        field.append(text.charAt(i));
                        i++;
                    }
                    if (field.length() > 0) blank = false;
                }

                fields.add(field.toString());
                field.setLength(0);
                if (i < n && text.charAt(i) == ',') {
                    blank = false;
                    i++;
                    continue;
                }
                // Koniec rekordu: CRLF, LF, samo CR albo koniec pliku
                if (i < n && text.charAt(i) == '\r') i++;
                if (i < n && text.charAt(i) == '\n') i++;
                line++;
                break;
            }

            if (!blank || error != null) {
                records.add(new CsvRecord(recordLine, fields, error));
            }
        }
        return records;
    }

    private static boolean isSeparator(char c) {
        return c == ',' || c == '\r' || c == '\n';
    }
}
