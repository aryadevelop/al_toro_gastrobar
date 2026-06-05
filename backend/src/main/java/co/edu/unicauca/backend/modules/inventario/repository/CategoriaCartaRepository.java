package co.edu.unicauca.backend.modules.inventario.repository;

import co.edu.unicauca.backend.modules.inventario.entity.CategoriaCarta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad {@link CategoriaCarta}.
 */
public interface CategoriaCartaRepository extends JpaRepository<CategoriaCarta, Integer> {

    /**
     * Busca una categoría de carta por su nombre, ignorando mayúsculas/minúsculas.
     *
     * @param categoriaNombre nombre de la categoría
     * @return categoría encontrada o vacía si no existe
     */
    Optional<CategoriaCarta> findByCategoriaNombreIgnoreCase(String categoriaNombre);
}
