package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link Visita}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y consultas
 * para recuperar visitas por reserva o por cliente (incluyendo walk-ins).
 *
 * @see Visita
 */
public interface VisitaRepository extends JpaRepository<Visita, Long> {

    /**
     * Busca la visita asociada a una reserva concreta.
     *
     * <p>Una reserva tiene a lo sumo una visita (el cliente se presenta una sola vez);
     * por eso el resultado es {@link Optional}.
     *
     * @param reservaId identificador de la reserva
     * @return la visita vinculada a la reserva, o {@link Optional#empty()} si no existe
     */
    Optional<Visita> findByReserva_ReservaId(Long reservaId);

    /**
     * Devuelve todas las visitas de un cliente,
     * ordenadas de la más reciente a la más antigua.
     *
     * @param clienteId identificador del cliente
     * @return lista de todas las visitas del cliente
     */
    List<Visita> findByCliente_UsuarioIdOrderByVisitaFechaHoraInicioDesc(Long clienteId);

    /**
     * Devuelve las visitas activas del cliente (sin fecha de fin registrada),
     * ordenadas de la más reciente a la más antigua. Una visita es activa mientras
     * {@code visitaFechaHoraFin} sea {@code null}.
     *
     * <p>Normalmente un cliente tiene 0 o 1 visita activa, pero el sistema no impide
     * que existan varias (p.ej. una visita previa sin cerrar). Por eso se devuelve una
     * lista ordenada: el llamador toma la primera (la más reciente = la visita en curso)
     * en lugar de fallar con {@code NonUniqueResultException}.
     *
     * @param email correo del cliente autenticado
     * @return lista de visitas activas (más reciente primero); vacía si no tiene ninguna
     */
    @Query("SELECT v FROM Visita v WHERE v.cliente.usuario.usuarioEmail = :email "
            + "AND v.visitaFechaHoraFin IS NULL ORDER BY v.visitaId DESC")
    List<Visita> findActiveByClienteEmail(@Param("email") String email);

    /**
     * Adquiere un bloqueo de escritura pesimista sobre la fila de la visita indicada.
     * Sirve como mutex del cierre de venta: serializa asignación de cliente,
     * ajuste de ítems y registro de pago de una misma visita.
     *
     * @param id identificador de la visita
     * @return la visita bloqueada, o {@link Optional#empty()} si no existe
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Visita v WHERE v.visitaId = :id")
    Optional<Visita> findByIdForUpdate(@Param("id") Long id);

    long countByVisitaFechaHoraFinIsNull();

}