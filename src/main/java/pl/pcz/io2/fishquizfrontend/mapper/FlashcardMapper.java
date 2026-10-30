package pl.pcz.io2.fishquizfrontend.mapper;

import pl.pcz.io2.fishquizfrontend.dto.FlashcardDTO;
import pl.pcz.io2.fishquizfrontend.model.Flashcard;

// Przepisywanie danych między Flashcard (baza) a FlashcardDTO (widok).
// Obrazu tu nie wczytujemy: widok robi to sam przez ImageManager.loadImage(dto.getImagePath()).
public final class FlashcardMapper {
    private FlashcardMapper() {}

    public static FlashcardDTO toDto(Flashcard flashcard) {
        FlashcardDTO dto = new FlashcardDTO();
        dto.setId(flashcard.getId());
        dto.setFrontText(flashcard.getFrontText());
        dto.setBackText(flashcard.getBackText());
        dto.setImagePath(flashcard.getImagePath());
        return dto;
    }

    // Nowa fiszka z domyślnym stanem SM-2; do edycji istniejącej użyj updateModel
    public static Flashcard toModel(FlashcardDTO dto) {
        return new Flashcard(dto.getId(), dto.getFrontText(), dto.getBackText(), dto.getImagePath());
    }

    // Przepisuje tylko pola edytowalne w widoku; id i stan SM-2 zostają bez zmian
    public static void updateModel(FlashcardDTO dto, Flashcard target) {
        target.setFrontText(dto.getFrontText());
        target.setBackText(dto.getBackText());
        target.setImagePath(dto.getImagePath());
    }
}
