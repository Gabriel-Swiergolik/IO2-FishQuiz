package pl.pcz.io2.fishquizfrontend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Bez toolkitu JavaFX nie tworzymy obiektów Image; sprawdzamy pliki i ścieżki
class ImageManagerTest {
    private static final byte[] CONTENT = {1, 2, 3, 4};

    @TempDir
    Path tempDir;

    private Path imagesDir;
    private ImageManager manager;

    @BeforeEach
    void setUp() {
        imagesDir = tempDir.resolve("images");
        manager = new ImageManager(imagesDir);
    }

    private File sourceFile(String name) throws IOException {
        Path file = tempDir.resolve(name);
        Files.write(file, CONTENT);
        return file.toFile();
    }

    @Test
    void saveCopiesFileUnderUniqueRelativeName() throws IOException {
        File source = sourceFile("kot.PNG");

        String first = manager.saveImage(source);
        String second = manager.saveImage(source);

        assertNotEquals(first, second);
        assertTrue(first.endsWith(".png"));
        assertFalse(Path.of(first).isAbsolute());
        assertEquals(first, Path.of(first).getFileName().toString());
        assertArrayEquals(CONTENT, Files.readAllBytes(imagesDir.resolve(first)));
        // Oryginał zostaje na miejscu
        assertTrue(source.exists());
    }

    @Test
    void saveCreatesImagesDirOnDemand() throws IOException {
        assertFalse(Files.exists(imagesDir));

        manager.saveImage(sourceFile("kot.jpg"));

        assertTrue(Files.isDirectory(imagesDir));
    }

    @Test
    void saveRejectsNonImageExtensions() throws IOException {
        File text = sourceFile("notatki.txt");
        File noExtension = sourceFile("plik");

        assertThrows(IllegalArgumentException.class, () -> manager.saveImage(text));
        assertThrows(IllegalArgumentException.class, () -> manager.saveImage(noExtension));
        assertFalse(Files.exists(imagesDir));
    }

    @Test
    void relativePathResolvesAgainstImagesDir() {
        assertEquals(imagesDir.resolve("abc.png"), manager.resolvePath("abc.png"));
    }

    @Test
    void absolutePathIsKept() {
        Path absolute = tempDir.resolve("gdzies/indziej.png").toAbsolutePath();

        assertEquals(absolute, manager.resolvePath(absolute.toString()));
    }

    @Test
    void loadReturnsNullForMissingOrEmptyPath() {
        assertNull(manager.loadImage(null));
        assertNull(manager.loadImage(" "));
        assertNull(manager.loadImage("nie-ma-takiego.png"));
    }

    @Test
    void deleteRemovesSavedFile() throws IOException {
        String saved = manager.saveImage(sourceFile("kot.gif"));

        manager.deleteImage(saved);

        assertFalse(Files.exists(imagesDir.resolve(saved)));
    }

    @Test
    void deleteOfMissingFileIsNotAnError() {
        assertDoesNotThrow(() -> manager.deleteImage("nie-ma-takiego.png"));
        assertDoesNotThrow(() -> manager.deleteImage(null));
    }
}
