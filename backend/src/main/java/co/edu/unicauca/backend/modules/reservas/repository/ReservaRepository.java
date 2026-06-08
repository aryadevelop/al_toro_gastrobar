package co.edu.unicauca.backend.modules.reservas.repository;

import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link Reserva}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y consultas
 * personalizadas para calcular la disponibilidad de zonas y decoraciones en un día
 * concreto, usadas por la capa de servicio al crear o consultar reservas.
 *
 * @see Reserva
 * @see co.edu.unicauca.backend.shared.enums.EstadoReserva
 */
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /**
     * Devuelve todas las reservas de un cliente ordenadas por fecha de llegada descendente.
     *
     * @param clienteId identificador del cliente
     * @return lista de reservas del cliente de más reciente a más antigua; vacía si no tiene ninguna
     */
    List<Reserva> findByCliente_UsuarioIdOrderByReservaFechaHoraLlegadaDesc(Long clienteId);

    List<Reserva> findByCliente_UsuarioIdInAndReservaFechaCreacionAfter(List<Long> clienteIds, LocalDateTime fechaCreacion);

    /**
     * Devuelve la suma de comensales agrupada por zona para un rango de fecha/hora y estados dados.
     *
     * <p>Cada elemento del resultado es un array {@code Object[]} con:
     * <ul>
     *   <li>{@code [0]} — {@code Long} con el identificador de la zona</li>
     *   <li>{@code [1]} — {@code Long} con la suma de personas reservadas en esa zona</li>
     * </ul>
     * Solo se incluyen reservas que tengan zona asignada.
     *
     * @param inicio   inicio del rango de fecha/hora (inclusive)
     * @param fin      fin del rango de fecha/hora (inclusive)
     * @param estados  estados de reserva a considerar (p. ej. {@code PENDIENTE}, {@code CONFIRMADA})
     * @return lista de pares {@code [zonaId, sumaPersonas]}; vacía si no hay reservas en el rango
     */
    @Query("SELECT r.zona.zonaId, SUM(r.reservaNumeroPersonas) FROM Reserva r " +
           "WHERE r.reservaFechaHoraLlegada BETWEEN :inicio AND :fin " +
           "AND r.reservaEstado IN :estados " +
           "AND r.zona IS NOT NULL " +
           "GROUP BY r.zona.zonaId")
    List<Object[]> findPersonasPorZonaEnDia(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estados") List<EstadoReserva> estados);

    /**
     * Suma los comensales ya reservados en una zona específica durante un rango de fecha/hora.
     *
     * <p>Utilizado para calcular el cupo restante de una zona antes de confirmar una nueva reserva.
     *
     * @param zonaId   identificador de la zona a consultar
     * @param inicio   inicio del rango de fecha/hora (inclusive)
     * @param fin      fin del rango de fecha/hora (inclusive)
     * @param estados  estados de reserva a considerar
     * @return suma de personas reservadas en la zona; {@code 0} si no hay reservas
     */
    @Query("SELECT COALESCE(SUM(r.reservaNumeroPersonas), 0) FROM Reserva r " +
           "WHERE r.zona.zonaId = :zonaId " +
           "AND r.reservaFechaHoraLlegada BETWEEN :inicio AND :fin " +
           "AND r.reservaEstado IN :estados")
    int sumPersonasByZonaEnDia(
            @Param("zonaId") Long zonaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estados") List<EstadoReserva> estados);

    /**
     * Devuelve los identificadores de las decoraciones ya ocupadas en un rango de fecha/hora.
     *
     * <p>Utilizado para excluir del listado de disponibilidad aquellas decoraciones que
     * ya están asignadas a otra reserva activa en el mismo día.
     *
     * @param inicio  inicio del rango de fecha/hora (inclusive)
     * @param fin     fin del rango de fecha/hora (inclusive)
     * @param estados estados de reserva a considerar como ocupados
     * @return lista de {@code decoracionId} ocupados; vacía si no hay ninguno
     */
    @Query("SELECT r.decoracion.decoracionId FROM Reserva r " +
           "WHERE r.reservaFechaHoraLlegada BETWEEN :inicio AND :fin " +
           "AND r.reservaEstado IN :estados " +
           "AND r.decoracion IS NOT NULL")
    List<Long> findDecoracionesOcupadasEnDia(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estados") List<EstadoReserva> estados);

    /**
     * Suma los comensales reservados en una zona excluyendo una reserva específica.
     *
     * <p>Usado al modificar una reserva para no contar la reserva actual
     * en el cálculo de capacidad disponible.
     *
     * @param zonaId           identificador de la zona
     * @param inicio           inicio del rango de fecha/hora (inclusive)
     * @param fin              fin del rango de fecha/hora (inclusive)
     * @param estados          estados a considerar como activos
     * @param excludeReservaId reserva a excluir del cómputo
     * @return suma de personas; {@code 0} si no hay reservas
     */
    @Query("SELECT COALESCE(SUM(r.reservaNumeroPersonas), 0) FROM Reserva r " +
           "WHERE r.zona.zonaId = :zonaId " +
           "AND r.reservaFechaHoraLlegada BETWEEN :inicio AND :fin " +
           "AND r.reservaEstado IN :estados " +
           "AND r.reservaId <> :excludeReservaId")
    int sumPersonasByZonaEnDiaExcluyendo(
            @Param("zonaId") Long zonaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estados") List<EstadoReserva> estados,
            @Param("excludeReservaId") Long excludeReservaId);

    /**
     * Devuelve los IDs de decoraciones ocupadas excluyendo una reserva específica.
     *
     * <p>Usado al modificar una reserva para no bloquear la decoración que ya tiene asignada.
     *
     * @param inicio           inicio del rango (inclusive)
     * @param fin              fin del rango (inclusive)
     * @param estados          estados a considerar como activos
     * @param excludeReservaId reserva a excluir
     * @return lista de {@code decoracionId} ocupados
     */
    @Query("SELECT r.decoracion.decoracionId FROM Reserva r " +
           "WHERE r.reservaFechaHoraLlegada BETWEEN :inicio AND :fin " +
           "AND r.reservaEstado IN :estados " +
           "AND r.decoracion IS NOT NULL " +
           "AND r.reservaId <> :excludeReservaId")
    List<Long> findDecoracionesOcupadasEnDiaExcluyendo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estados") List<EstadoReserva> estados,
            @Param("excludeReservaId") Long excludeReservaId);

    /**
     * Suma comensales por zona excluyendo una reserva específica.
     *
     * <p>Usado al modificar una reserva para no contar la reserva actual en los
     * cálculos de ocupación de zona.
     *
     * @param inicio           inicio del rango (inclusive)
     * @param fin              fin del rango (inclusive)
     * @param estados          estados a considerar como activos
     * @param excludeReservaId reserva a excluir
     * @return lista de pares {@code [zonaId, sumaPersonas]}
     */
    @Query("SELECT r.zona.zonaId, SUM(r.reservaNumeroPersonas) FROM Reserva r " +
           "WHERE r.reservaFechaHoraLlegada BETWEEN :inicio AND :fin " +
           "AND r.reservaEstado IN :estados " +
           "AND r.zona IS NOT NULL " +
           "AND r.reservaId <> :excludeReservaId " +
           "GROUP BY r.zona.zonaId")
    List<Object[]> findPersonasPorZonaEnDiaExcluyendo(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estados") List<EstadoReserva> estados,
            @Param("excludeReservaId") Long excludeReservaId);

    /**
     * Devuelve todas las reservas activas (CONFIRMADA/PENDIENTE) de un día específico,
     * ordenadas por hora de llegada ascendente.
     *
     * <p>Usado por meseros para consultar el listado del día.
     *
     * @param inicio inicio del día (00:00:00)
     * @param fin fin del día (23:59:59)
     * @param estados    estados a considerar como activos
     * @return lista de reservas activas del día; vacía si no hay ninguna
     */
    @Query("SELECT r FROM Reserva r " +
           "LEFT JOIN FETCH r.cliente c " +
           "LEFT JOIN FETCH c.usuario u " +
           "LEFT JOIN FETCH r.zona z " +
           "LEFT JOIN FETCH r.decoracion d " +
           "WHERE r.reservaFechaHoraLlegada BETWEEN :inicio AND :fin " +
           "AND r.reservaEstado IN :estados " +
           "ORDER BY r.reservaFechaHoraLlegada ASC")
    List<Reserva> findReservasActivasDelDia(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estados") List<EstadoReserva> estados);

    /**
     * Adquiere un bloqueo de escritura pesimista sobre la fila de la reserva indicada.
     * Usar dentro de una transacción {@code @Transactional} para serializar
     * concurrencia en operaciones de registro de anticipos y devoluciones.
     *
     * @param id identificador de la reserva
     * @return reserva bloqueada, o {@link Optional#empty()} si no existe
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reserva r WHERE r.reservaId = :id")
    Optional<Reserva> findByIdForUpdate(@Param("id") Long id);

    /**
     * Devuelve todas las reservas activas que coincidan con un identificador específico,
     * sin importar la fecha.
     *
     * <p>Usado por meseros para buscar reservas por ID.
     *
     * @param reservaId  identificador a buscar
     * @param estados    estados a considerar como activos
     * @return lista de reservas que coincidan; vacía si no se encuentra
     */
    @Query("SELECT r FROM Reserva r " +
           "LEFT JOIN FETCH r.cliente c " +
           "LEFT JOIN FETCH c.usuario u " +
           "LEFT JOIN FETCH r.zona z " +
           "LEFT JOIN FETCH r.decoracion d " +
           "WHERE r.reservaId = :reservaId " +
           "AND r.reservaEstado IN :estados " +
           "ORDER BY r.reservaFechaHoraLlegada ASC")
    List<Reserva> findReservasActivasPorIdentificador(
            @Param("reservaId") Long reservaId,
            @Param("estados") List<EstadoReserva> estados);

    /**
     * Obtiene datos de una reserva confirmada para asignación de mesa.
     *
     * @param reservaId ID de la reserva
     * @param estado estado esperado (CONFIRMADA)
     * @return Reserva con cliente, zona, decoración cargados; empty si no existe
     */
    @Query("""
        SELECT r FROM Reserva r
        LEFT JOIN FETCH r.cliente c
        LEFT JOIN FETCH c.usuario u
        LEFT JOIN FETCH r.zona z
        LEFT JOIN FETCH r.decoracion d
        WHERE r.reservaId = :reservaId
        AND r.reservaEstado = :estado
        """)
    Optional<Reserva> findByIdAndEstadoForAsignacion(
            @Param("reservaId") Long reservaId,
            @Param("estado") EstadoReserva estado);
}
