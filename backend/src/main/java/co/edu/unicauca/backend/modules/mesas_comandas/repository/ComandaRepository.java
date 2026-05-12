package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link Comanda}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y consultas
 * para recuperar comandas por visita o por reserva.
 *
 * @see Comanda
 */
public interface ComandaRepository extends JpaRepository<Comanda, Long> {

    /**
     * Devuelve todas las comandas (cocina y barra) asociadas a una visita.
     *
     * @param visitaId identificador de la visita
     * @return lista de comandas de la visita; vacía si no se generaron comandas
     */
    List<Comanda> findByVisita_VisitaId(Long visitaId);

    /**
     * Devuelve las comandas en el estado indicado asociadas a una reserva.
     *
     * <p>Retorna lista para soportar el split por estación (COCINA + BARRA), donde
     * una reserva con menú especial puede tener más de una comanda PRE_RESERVA.
     *
     * @param reservaId identificador de la reserva
     * @param estado    estado a filtrar (típicamente {@code PRE_RESERVA})
     * @return lista de comandas en ese estado asociadas a la reserva; vacía si no existe ninguna
     */
    List<Comanda> findByReserva_ReservaIdAndComandaEstado(Long reservaId, EstadoComanda estado);

    /**
     * Obtiene todos los items de comandas en producción de una visita.
     * Estados: PENDIENTE, EN_PREPARACION, LISTO, COMPLETADO.
     *
     * @param visitaId ID de la visita
     * @return lista de ComandaItem con producto y comanda cargados, ordenados por categoría y nombre
     */
    @Query("""
        SELECT ci FROM ComandaItem ci
        JOIN FETCH ci.comanda c
        JOIN FETCH ci.producto p
        WHERE c.visita.visitaId = :visitaId
        AND c.comandaEstado IN ('PENDIENTE', 'EN_PREPARACION', 'LISTO', 'COMPLETADO')
        ORDER BY p.productoCategoria, p.productoNombre
        """)
    List<ComandaItem> findItemsEnProduccionByVisita(@Param("visitaId") Long visitaId);

    /**
     * Verifica si existe al menos una comanda de la visita en alguno de los estados indicados.
     *
     * <p>Se utiliza para determinar si quedan comandas en producción
     * (PENDIENTE, EN_PREPARACION, LISTO) antes de transicionar la mesa a {@code ATENDIDA}.
     * Las comandas en {@code BORRADOR} o {@code PRE_RESERVA} no afectan la evaluación
     * porque aún no se enviaron a producción.
     *
     * @param visitaId identificador de la visita
     * @param estados  lista de estados de comanda a buscar
     * @return {@code true} si existe al menos una comanda en alguno de los estados dados
     */
    boolean existsByVisita_VisitaIdAndComandaEstadoIn(Long visitaId, List<EstadoComanda> estados);
}
