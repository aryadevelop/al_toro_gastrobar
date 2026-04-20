package co.edu.unicauca.backend.modules.notificaciones.entity;

import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class NotificacionEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        Notificacion n = Notificacion.builder()
                .notificacionId(1L)
                .notificacionEstado(EstadoNotificacion.ACTIVA)
                .notificacionTipo(TipoNotificacion.ATENCION)
                .build();

        assertThat(n.getNotificacionId()).isEqualTo(1L);
        assertThat(n.getNotificacionEstado()).isEqualTo(EstadoNotificacion.ACTIVA);
        assertThat(n.getNotificacionTipo()).isEqualTo(TipoNotificacion.ATENCION);
    }

    @Test
    void setter_actualizaEstado() {
        Notificacion n = new Notificacion();
        n.setNotificacionEstado(EstadoNotificacion.ATENDIDA);
        assertThat(n.getNotificacionEstado()).isEqualTo(EstadoNotificacion.ATENDIDA);
    }

    @Test
    void onCreate_setsFechasIfNull() {
        Notificacion n = new Notificacion();
        n.onCreate();
        assertThat(n.getNotificacionFechaHora()).isNotNull();
        assertThat(n.getCreatedAt()).isNotNull();
    }
}
