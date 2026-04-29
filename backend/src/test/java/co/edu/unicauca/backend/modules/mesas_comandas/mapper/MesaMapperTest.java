package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.NotificacionActivaResponse;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class MesaMapperTest {

    private final MesaMapper mapper = new MesaMapper();

    @Test
    void toNotificacionActivaResponse_DeberiaMapearCorrectamente() {
        // Arrange
        LocalDateTime fechaHora = LocalDateTime.of(2026, 4, 29, 19, 30);
        Notificacion notificacion = Notificacion.builder()
                .notificacionId(1L)
                .notificacionTipo(TipoNotificacion.ATENCION)
                .notificacionFechaHora(fechaHora)
                .build();

        // Act
        NotificacionActivaResponse response = mapper.toNotificacionActivaResponse(notificacion);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getNotificacionId()).isEqualTo(1L);
        assertThat(response.getTipo()).isEqualTo("ATENCION");  // Enum → String
        assertThat(response.getFechaHora()).isEqualTo(fechaHora);
    }
}
