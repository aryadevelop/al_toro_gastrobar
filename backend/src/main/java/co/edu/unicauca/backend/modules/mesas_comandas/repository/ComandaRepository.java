package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
     * Obtiene todos los ítems activos de una visita, incluyendo borradores y producción.
     * Estados: BORRADOR, PENDIENTE, EN_PREPARACION, LISTO, COMPLETADO.
     *
     * @param visitaId ID de la visita
     * @return lista de ComandaItem con producto y comanda cargados, ordenados por categoría y nombre
     */
    @Query("""
        SELECT ci FROM ComandaItem ci
        JOIN FETCH ci.comanda c
        JOIN FETCH ci.producto p
        WHERE c.visita.visitaId = :visitaId
        AND c.comandaEstado IN ('BORRADOR', 'PENDIENTE', 'EN_PREPARACION', 'LISTO', 'COMPLETADO')
        ORDER BY p.productoCategoria, p.productoNombre
        """)
    List<ComandaItem> findAllItemsActivosByVisita(@Param("visitaId") Long visitaId);

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

    /**
     * Localiza la comanda BORRADOR de una visita para una estación específica.
     *
     * <p>Puede haber a lo sumo una BORRADOR por estación.
     *
     * @param visitaId identificador de la visita
     * @param estado estado a buscar (típicamente {@code BORRADOR})
     * @param estacion estación destino (COCINA o BARRA)
     * @return Optional con la comanda; vacío si no existe
     */
    Optional<Comanda> findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion(
            Long visitaId, EstadoComanda estado, EstacionComanda estacion);

    /**
     * Devuelve todas las comandas de una visita en un estado específico.
     * Hasta dos por visita en BORRADOR (una por estación).
     */
    List<Comanda> findByVisita_VisitaIdAndComandaEstado(Long visitaId, EstadoComanda estado);

    long countByComandaEstadoAndVisita_VisitaFechaHoraFinIsNull(EstadoComanda estado);

    @Query("""
        SELECT c FROM Comanda c
        JOIN FETCH c.visita v
        WHERE c.comandaEstado = :estado
        AND v.visitaFechaHoraFin IS NULL
        ORDER BY c.comandaFechaHoraListo ASC
        """)
    List<Comanda> findTop5ByComandaEstadoAndVisita_VisitaFechaHoraFinIsNull(@Param("estado") EstadoComanda estado,
                                                                          Pageable pageable);

    /**
     * Recupera las comandas de una estación cuyo estado pertenece al conjunto
     * indicado.
     *
     * <p>El ordenamiento se delega al servicio, que aplica comparadores
     * diferenciados por estado con respaldo en {@code createdAt} cuando los
     * timestamps de transición no están presentes.
     *
     * @param estacion estación destino ({@code COCINA} o {@code BARRA})
     * @param estados  estados de comanda a incluir
     * @return comandas que satisfacen el filtro con la visita hidratada;
     *         lista vacía cuando no existen coincidencias
     */
    @Query("""
        SELECT c FROM Comanda c
        JOIN FETCH c.visita v
        WHERE c.comandaEstacion = :estacion
        AND c.comandaEstado IN :estados
        """)
    List<Comanda> findByEstacionAndEstadoIn(@Param("estacion") EstacionComanda estacion,
                                            @Param("estados") Collection<EstadoComanda> estados);

    /**
     * Recupera las comandas de una estación cuyo estado pertenece al conjunto
     * indicado y que NO tienen ninguna notificación de tipo {@code CAMBIO} en
     * estado {@code ACTIVA}.
     *
     * <p>Las comandas con notificación {@code CAMBIO ACTIVA} ya fueron retiradas
     * del tablero vía WebSocket; esta consulta evita que reaparezcan al reconectar
     * la pantalla de producción.
     *
     * @param estacion estación destino ({@code COCINA} o {@code BARRA})
     * @param estados  estados de comanda a incluir
     * @return comandas que satisfacen el filtro con la visita hidratada
     */
    @Query("""
        SELECT c FROM Comanda c
        JOIN FETCH c.visita
        WHERE c.comandaEstacion = :estacion
        AND c.comandaEstado IN :estados
        AND NOT EXISTS (
            SELECT n FROM Notificacion n
            WHERE n.comanda = c
            AND n.notificacionTipo = co.edu.unicauca.backend.shared.enums.TipoNotificacion.CAMBIO
            AND n.notificacionEstado = co.edu.unicauca.backend.shared.enums.EstadoNotificacion.ACTIVA
        )
        """)
    List<Comanda> findByEstacionAndEstadoInSinCambioActivo(
            @Param("estacion") EstacionComanda estacion,
            @Param("estados") Collection<EstadoComanda> estados);

    /**
     * Localiza la comanda en estado {@code BORRADOR} asociada a una visita y a
     * una estación, adquiriendo un bloqueo pesimista de escritura sobre la
     * fila resultante.
     *
     * <p>El bloqueo evita condiciones de carrera cuando dos transacciones
     * intentan fusionar simultáneamente contenido sobre el mismo borrador.
     *
     * @param visitaId identificador de la visita
     * @param estacion estación destino
     * @return {@link Optional} con la comanda en {@code BORRADOR} bloqueada;
     *         vacío cuando no existe ningún borrador para esa combinación
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c FROM Comanda c
        WHERE c.visita.visitaId = :visitaId
        AND c.comandaEstacion = :estacion
        AND c.comandaEstado = co.edu.unicauca.backend.shared.enums.EstadoComanda.BORRADOR
        """)
    Optional<Comanda> findBorradorActivoByVisitaYEstacion(@Param("visitaId") Long visitaId,
                                                          @Param("estacion") EstacionComanda estacion);

    /**
     * Devuelve las comandas en estado {@code PENDIENTE} que contienen al menos un
     * ítem del producto indicado. Identifica comandas afectadas por un egreso
     * manual de un producto de venta directa.
     *
     * @param productoId identificador del producto
     * @return comandas afectadas con la visita hidratada
     */
    @Query("""
        SELECT DISTINCT c FROM Comanda c
        JOIN FETCH c.visita
        WHERE c.comandaEstado = co.edu.unicauca.backend.shared.enums.EstadoComanda.PENDIENTE
        AND c.comandaId IN (
            SELECT ci.comanda.comandaId FROM ComandaItem ci
            WHERE ci.producto.productoId = :productoId
            AND ci.comandaItemMenuGrupo IS NULL
        )
        ORDER BY c.createdAt ASC
        """)
    List<Comanda> findPendientesByProductoId(@Param("productoId") Long productoId);

    /**
     * Devuelve las comandas en estado {@code PENDIENTE} que contienen al menos un
     * ítem de cualquiera de los productos indicados. Identifica comandas afectadas
     * por un egreso manual de un insumo (cuyos productos de preparación se
     * determinaron previamente por receta).
     *
     * @param productoIds identificadores de producto
     * @return comandas afectadas con la visita hidratada, ordenadas por createdAt ASC (FIFO)
     */
    @Query("""
        SELECT DISTINCT c FROM Comanda c
        JOIN FETCH c.visita
        WHERE c.comandaEstado = co.edu.unicauca.backend.shared.enums.EstadoComanda.PENDIENTE
        AND c.comandaId IN (
            SELECT ci.comanda.comandaId FROM ComandaItem ci
            WHERE ci.producto.productoId IN :productoIds
            AND ci.comandaItemMenuGrupo IS NULL
        )
        ORDER BY c.createdAt ASC
        """)
    List<Comanda> findPendientesByProductoIds(@Param("productoIds") List<Long> productoIds);

    /**
     * Adquiere un bloqueo de escritura pesimista sobre la fila de la comanda indicada.
     * Usar antes de chequeos de estado o de creación de notificaciones para serializar
     * acceso concurrente a la misma comanda.
     *
     * @param id identificador de la comanda
     * @return comanda bloqueada, o {@link Optional#empty()} si no existe
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Comanda c WHERE c.comandaId = :id")
    Optional<Comanda> findByIdForUpdate(@Param("id") Long id);
}
