package co.edu.unicauca.backend.modules.pagos_caja.repository;

import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link Venta}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y la consulta
 * puntual para recuperar la venta cerrada de una visita.
 *
 * @see Venta
 */
public interface VentaRepository extends JpaRepository<Venta, Long> {

    /**
     * Busca la venta registrada para una visita concreta.
     *
     * <p>{@code Venta} usa la misma PK que {@code Visita} mediante {@code @MapsId};
     * cada visita tiene como máximo una venta, por eso el resultado es {@link Optional}.
     *
     * @param visitaId identificador de la visita
     * @return la venta de esa visita, o {@link Optional#empty()} si la cuenta aún no fue cerrada
     */
    Optional<Venta> findByVisita_VisitaId(Long visitaId);
}
