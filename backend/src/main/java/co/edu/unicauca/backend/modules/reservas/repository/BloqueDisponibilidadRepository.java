package co.edu.unicauca.backend.modules.reservas.repository;

import co.edu.unicauca.backend.modules.reservas.entity.BloqueDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface BloqueDisponibilidadRepository extends JpaRepository<BloqueDisponibilidad, Long> {

    /**
     * Cuenta los bloqueos activos para una fecha y hora específica.
     * Un bloqueo aplica si:
     *  - La fecha cae dentro del rango [fechaInicio, fechaFin], Y
     *  - No tiene franja horaria definida (día completo), O la hora cae dentro del rango [horaInicio, horaFin).
     */
    @Query("SELECT COUNT(b) FROM BloqueDisponibilidad b " +
           "WHERE :fecha BETWEEN b.fechaInicio AND b.fechaFin " +
           "AND (b.horaInicio IS NULL OR (:hora >= b.horaInicio AND :hora < b.horaFin))")
    long countBloquesParaFechaHora(@Param("fecha") LocalDate fecha, @Param("hora") LocalTime hora);

    /** Todos los bloqueos ordenados por fecha de inicio ascendente. */
    List<BloqueDisponibilidad> findAllByOrderByFechaInicioAsc();
}
