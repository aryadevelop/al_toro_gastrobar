package co.edu.unicauca.backend.modules.reservas.repository;

import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByCliente_UsuarioIdOrderByReservaFechaHoraLlegadaDesc(Long clienteId);

    /**
     * Suma de personas por zona para todas las reservas activas en el mismo día.
     * Retorna filas [zonaId (Long), sumaPersonas (Long)].
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
     * Suma de personas ya reservadas en una zona específica durante el día.
     * Retorna 0 si no hay reservas.
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
     * IDs de decoraciones ya reservadas en el mismo día con estados activos.
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
