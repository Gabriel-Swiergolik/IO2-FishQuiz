package pl.pcz.io2.fishquizfrontend.dao;

import java.util.List;
import java.util.Optional;

/**
 * Podstawowe operacje CRUD na encjach jednego typu. Każda metoda przy błędzie bazy rzuca
 * {@link DataAccessException}.
 * @param <T> typ encji
 * @param <ID> typ identyfikatora
 */
public interface GenericDAO<T, ID> {
    /**
     * Zapisuje nową encję; wygenerowane id jest ustawiane w przekazanym obiekcie.
     * @param entity encja do zapisania
     * @throws DataAccessException gdy zapis się nie uda
     */
    void create(T entity);
    /**
     * Odczytuje encję o podanym id.
     * @param id identyfikator
     * @return encja albo pusty {@code Optional}, gdy jej nie ma
     * @throws DataAccessException gdy odczyt się nie uda
     */
    Optional<T> read(ID id);
    /**
     * Zapisuje zmiany istniejącej encji (wg jej id).
     * @param entity encja ze zmianami
     * @throws DataAccessException gdy zapis się nie uda
     */
    void update(T entity);
    /**
     * Usuwa encję o podanym id razem z jej powiązaniami.
     * @param id identyfikator
     * @throws DataAccessException gdy usunięcie się nie uda
     */
    void delete(ID id);
    /**
     * Odczytuje wszystkie encje.
     * @return wszystkie encje tego typu
     * @throws DataAccessException gdy odczyt się nie uda
     */
    List<T> findAll();
}