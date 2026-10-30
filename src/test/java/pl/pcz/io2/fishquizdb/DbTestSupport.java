package pl.pcz.io2.fishquizdb;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

// Klasa bazowa dla testów bazy: każdy test dostaje własny, tymczasowy plik SQLite zamiast database.db
public abstract class DbTestSupport {
    @TempDir
    protected Path tempDir;

    @BeforeEach
    protected void initTestDatabase() {
        DatabaseManager.init("jdbc:sqlite:" + tempDir.resolve("test.db"));
    }

    @AfterEach
    protected void closeTestDatabase() {
        DatabaseManager.close();
    }
}
