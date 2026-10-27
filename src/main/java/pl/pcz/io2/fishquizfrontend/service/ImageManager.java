package pl.pcz.io2.fishquizfrontend.service;

import javafx.scene.image.Image;
import java.io.File;

public class ImageManager {
    public String saveImage(File sourceFile) {
        return "todo";
    }

    public Image loadImage(String path) {
        if (path == null || path.isBlank()) return null;
        return new Image(new File(path).toURI().toString());
    }

    public void deleteImage(String path) {
        //todo
    }
}