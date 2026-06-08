package co.edu.unicauca.backend.modules.reservas.repository;

import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link Decoracion}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y consultas
 * para filtrar decoraciones por estado, usado principalmente al listar las
 * opciones disponibles para una nueva reserva.
 *
 * @see Decoracion
 * @see co.edu.unicauca.backend.shared.enums.EstadoGenerico
 */
public interface DecoracionRepository extends JpaRepository<Decoracion, Long> {

    /**
     * Devuelve todas las decoraciones que tienen el estado indicado.
     *
     * <p>Se utiliza principalmente con {@code EstadoGenerico.ACTIVO} para obtener
     * las decoraciones que pueden ser seleccionadas al crear una reserva.
     *
     * @param estado estado por el que filtrar ({@code ACTIVO} o {@code INACTIVO})
     * @return lista de decoraciones con el estado indicado; vacía si no hay ninguna
     */
    List<Decoracion> findByDecoracionEstado(EstadoGenerico estado);
}
