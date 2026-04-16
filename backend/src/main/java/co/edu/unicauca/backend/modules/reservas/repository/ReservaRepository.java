package co.edu.unicauca.backend.modules.reservas.repository;

import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

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
}
