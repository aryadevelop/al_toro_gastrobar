package co.edu.unicauca.backend.modules.notificaciones.repository;

import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para {@link Notificacion}.
 */
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    /**
     * Devuelve la primera notificación activa de un tipo concreto para la mesa de una visita.
     * Usado para verificar si ya existe una solicitud de asistencia sin atender.
     *
     * @param visitaId identificador de la visita (= PK de Mesa)
     * @param tipo     tipo de notificación a filtrar
     * @param estado   estado a filtrar
     * @return primera notificación que coincida, o empty
     */
    Optional<Notificacion> findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
            Long visitaId, TipoNotificacion tipo, EstadoNotificacion estado);

    /**
     * Obtiene todas las notificaciones activas de una mesa.
     *
     * @param mesaId ID de la mesa (visita_id)
     * @return lista de notificaciones activas ordenadas por fecha DESC
     */
    @Query("""
        SELECT n FROM Notificacion n
        WHERE n.mesa.visitaId = :mesaId
        AND n.notificacionEstado = 'ACTIVA'
        ORDER BY n.notificacionFechaHora DESC
        """)
    List<Notificacion> findNotificacionesActivasByMesa(@Param("mesaId") Long mesaId);

    /**
     * Verifica si existe al menos una notificación de un tipo y estado dados para la mesa.
     *
     * <p>El evaluador de estado de mesa la utiliza para decidir si la mesa puede
     * transicionar automáticamente a {@code ATENDIDA}.
     *
     * @param visitaId identificador de la visita (PK de Mesa)
     * @param tipo     tipo de notificación a verificar
     * @param estado   estado de la notificación a verificar
     * @return {@code true} si existe alguna notificación que cumple las tres condiciones
     */
    boolean existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
            Long visitaId, TipoNotificacion tipo, EstadoNotificacion estado);

    /**
     * Devuelve la primera notificación de un tipo y estado dados asociada a
     * una comanda específica.
     *
     * <p>Usado para evitar duplicar notificaciones de cambio activas sobre la
     * misma comanda en producción.
     *
     * @param comandaId identificador de la comanda
     * @param tipo      tipo de notificación a filtrar
     * @param estado    estado a filtrar
     * @return primera notificación que coincida, o empty
     */
    Optional<Notificacion> findFirstByComanda_ComandaIdAndNotificacionTipoAndNotificacionEstado(
            Long comandaId, TipoNotificacion tipo, EstadoNotificacion estado);
}
