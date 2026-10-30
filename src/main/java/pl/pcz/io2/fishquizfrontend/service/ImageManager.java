package pl.pcz.io2.fishquizfrontend.service;

import javafx.scene.image.Image;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

// Obrazy fiszek kopiujemy do własnego katalogu, żeby nie zależeć od miejsca oryginału.
// W bazie zapisujemy ścieżkę względną (samą nazwę pliku w katalogu obrazów).
public class ImageManager {
    // Formaty, które JavaFX potrafi wyświetlić
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "bmp");

    private final Path imagesDir;

    // Domyślnie images/ w katalogu roboczym, czyli obok database.db
    public ImageManager() {
        this(Path.of("images"));
    }

    public ImageManager(Path imagesDir) {
        this.imagesDir = imagesDir;
    }

    // Kopiuje plik pod unikalną nazwą (UUID + rozszerzenie) i zwraca ścieżkę względną
    public String saveImage(File sourceFile) {
        String extension = extensionOf(sourceFile.getName());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Nieobsługiwany format obrazu: " + sourceFile.getName()
                    + " (dozwolone: png, jpg, jpeg, gif, bmp)");
        }
        String fileName = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(imagesDir);
            Files.copy(sourceFile.toPath(), imagesDir.resolve(fileName));
        } catch (IOException e) {
            throw new UncheckedIOException("Nie udało się skopiować obrazu " + sourceFile, e);
        }
        return fileName;
    }

    // Ścieżki względne liczymy od katalogu obrazów; bezwzględne (stare dane) zostają bez zmian
    public Path resolvePath(String path) {
        Path p = Path.of(path);
        return p.isAbsolute() ? p : imagesDir.resolve(p);
    }

    // null, jeśli fiszka nie ma obrazu, pliku już nie ma albo nie da się go odczytać jako obrazu
    public Image loadImage(String path) {
        if (path == null || path.isBlank()) return null;
        Path file = resolvePath(path);
        if (!Files.isRegularFile(file)) return null;
        // Wczytanie synchroniczne, więc błąd (np. uszkodzony plik) jest znany od razu
        Image image = new Image(file.toUri().toString());
        return image.isError() ? null : image;
    }

    // Brak pliku nie jest błędem
    public void deleteImage(String path) {
        if (path == null || path.isBlank()) return;
        try {
            Files.deleteIfExists(resolvePath(path));
        } catch (IOException e) {
            throw new UncheckedIOException("Nie udało się usunąć obrazu " + path, e);
        }
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
