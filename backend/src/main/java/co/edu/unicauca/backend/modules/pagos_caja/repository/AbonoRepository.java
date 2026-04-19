package co.edu.unicauca.backend.modules.pagos_caja.repository;

import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link Abono}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y la consulta
 * para recuperar todos los abonos y devoluciones asociados a una reserva.
 *
 * @see Abono
 */
public interface AbonoRepository extends JpaRepository<Abono, Long> {

    /**
     * Devuelve todos los abonos de una reserva, ordenados del más antiguo al más reciente.
     *
     * <p>Incluye tanto anticipos como devoluciones, en orden cronológico ascendente.
     *
     * @param reservaId identificador de la reserva
     * @return lista de abonos de la reserva; vacía si no se registró ningún abono
     */
    List<Abono> findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(Long reservaId);

    /**
     * Calcula la suma de todos los abonos de una reserva directamente en la base de datos.
     *
     * <p>Usar esta query es más eficiente que cargar la lista completa de abonos en memoria
     * solo para sumarlos. {@code COALESCE} garantiza retornar {@code 0} si no hay abonos.</p>
     *
     * @param reservaId identificador de la reserva
     * @return suma total de {@code abonoMonto}; {@code 0} si no hay ningún abono registrado
     */
    @Query("SELECT COALESCE(SUM(a.abonoMonto), 0) FROM Abono a WHERE a.reserva.reservaId = :reservaId")
    BigDecimal sumAbonosByReservaId(@Param("reservaId") Long reservaId);
}
