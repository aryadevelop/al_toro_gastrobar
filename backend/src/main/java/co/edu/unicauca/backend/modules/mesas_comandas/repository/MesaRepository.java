package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * Obtiene todas las mesas activas (con visita que no ha finalizado).
     * NOTA: Una mesa está activa si visita.visitaFechaHoraFin IS NULL.
     *
     * @return lista de mesas activas ordenadas por zona y identificador
     */
    @Query("""
        SELECT m FROM Mesa m
        JOIN FETCH m.visita v
        JOIN FETCH m.zona z
        JOIN FETCH m.mesero me
        JOIN FETCH me.usuario u
        WHERE v.visitaFechaHoraFin IS NULL
        ORDER BY z.zonaNombre, m.mesaIdentificador
        """)
    List<Mesa> findAllMesasActivas();

    /**
     * Obtiene todas las mesas activas de una zona específica.
     *
     * @param zonaId ID de la zona
     * @return lista de mesas activas en la zona
     */
    @Query("""
        SELECT m FROM Mesa m
        JOIN FETCH m.visita v
        JOIN FETCH m.zona z
        JOIN FETCH m.mesero me
        JOIN FETCH me.usuario u
        WHERE v.visitaFechaHoraFin IS NULL
        AND z.zonaId = :zonaId
        ORDER BY m.mesaIdentificador
        """)
    List<Mesa> findMesasActivasByZona(@Param("zonaId") Long zonaId);

    /**
     * Verifica si una mesa tiene al menos una comanda en estado BORRADOR.
     *
     * @param visitaId ID de la visita (PK de Mesa)
     * @return true si existe al menos una comanda en BORRADOR
     */
    @Query("""
        SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
        FROM Comanda c
        WHERE c.visita.visitaId = :visitaId
        AND c.comandaEstado = 'BORRADOR'
        """)
    boolean existeComandaBorradorEnMesa(@Param("visitaId") Long visitaId);

    /**
     * Verifica si existe una mesa activa con el identificador dado en el día especificado.
     *
     * @param mesaIdentificador identificador de mesa a buscar
     * @param inicioDelDia inicio del día (00:00:00)
     * @param finDelDia fin del día (23:59:59)
     * @return true si existe mesa activa con ese identificador en el rango
     */
    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
        FROM Mesa m
        WHERE m.mesaIdentificador = :mesaIdentificador
        AND m.visita.visitaFechaHoraInicio BETWEEN :inicioDelDia AND :finDelDia
        AND m.visita.visitaFechaHoraFin IS NULL
        """)
    boolean existeMesaActivaConIdentificadorEnDia(
            @Param("mesaIdentificador") String mesaIdentificador,
            @Param("inicioDelDia") LocalDateTime inicioDelDia,
            @Param("finDelDia") LocalDateTime finDelDia);

    /**
     * Calcula el total de personas en visitas activas agrupadas por zona.
     *
     * @return lista de pares [zonaId, totalPersonas]
     */
    @Query("""
        SELECT m.zona.zonaId, COALESCE(SUM(m.mesaNumeroPersonas), 0)
        FROM Mesa m
        WHERE m.visita.visitaFechaHoraFin IS NULL
        GROUP BY m.zona.zonaId
        """)
    List<Object[]> sumPersonasPorZonaActiva();
}
