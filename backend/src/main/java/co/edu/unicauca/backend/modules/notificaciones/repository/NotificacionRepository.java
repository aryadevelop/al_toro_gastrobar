package co.edu.unicauca.backend.modules.notificaciones.repository;

import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
