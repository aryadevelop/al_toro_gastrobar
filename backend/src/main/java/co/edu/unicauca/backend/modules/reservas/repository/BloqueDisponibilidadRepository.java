package co.edu.unicauca.backend.modules.reservas.repository;

import co.edu.unicauca.backend.modules.reservas.entity.BloqueDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link BloqueDisponibilidad}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y consultas
 * personalizadas para verificar si una fecha u hora está bloqueada antes de
 * permitir la creación de una reserva.
 *
 * @see BloqueDisponibilidad
 */
public interface BloqueDisponibilidadRepository extends JpaRepository<BloqueDisponibilidad, Long> {

    /**
     * Cuenta los bloqueos vigentes que cubren una fecha y hora concretas.
     *
     * <p>Un bloqueo aplica si se cumplen ambas condiciones:
     * <ul>
     *   <li>La {@code fecha} cae dentro del rango {@code [fechaInicio, fechaFin]} (ambos inclusive).</li>
     *   <li>No tiene franja horaria definida (día completo), o la {@code hora} cae dentro
     *       del rango {@code [horaInicio, horaFin)} (inicio inclusive, fin exclusivo).</li>
     * </ul>
     *
     * @param fecha fecha a verificar
     * @param hora  hora a verificar
     * @return número de bloqueos que cubren la fecha y hora indicadas; {@code 0} si no hay ninguno
     */
    @Query("SELECT COUNT(b) FROM BloqueDisponibilidad b " +
           "WHERE :fecha BETWEEN b.fechaInicio AND b.fechaFin " +
           "AND (b.horaInicio IS NULL OR (:hora >= b.horaInicio AND :hora < b.horaFin))")
    long countBloquesParaFechaHora(@Param("fecha") LocalDate fecha, @Param("hora") LocalTime hora);

    /**
     * Devuelve todos los bloqueos de disponibilidad ordenados por fecha de inicio ascendente.
     *
     * @return lista de bloqueos ordenada de más antiguo a más reciente; vacía si no hay ninguno
     */
    List<BloqueDisponibilidad> findAllByOrderByFechaInicioAsc();
}
