package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link Mesa}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y la consulta
 * puntual para recuperar la mesa asignada a una visita específica.
 *
 * @see Mesa
 */
public interface MesaRepository extends JpaRepository<Mesa, Long> {

    /**
     * Busca la mesa asignada a una visita concreta.
     *
     * <p>Cada visita tiene como máximo una mesa asignada ({@code Mesa} usa la misma PK
     * que {@code Visita} mediante {@code @MapsId}); por eso el resultado es {@link Optional}.
     *
     * @param visitaId identificador de la visita
     * @return la mesa asignada a la visita, o {@link Optional#empty()} si no hay mesa asignada
     */
    Optional<Mesa> findByVisita_VisitaId(Long visitaId);
}
