package co.edu.unicauca.backend.modules.reservas.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalTime;
import static org.assertj.core.api.Assertions.assertThat;

class BloqueDisponibilidadEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        BloqueDisponibilidad b = BloqueDisponibilidad.builder()
                .bloqueId(1L)
                .fechaInicio(LocalDate.of(2026, 5, 1))
                .fechaFin(LocalDate.of(2026, 5, 3))
                .horaInicio(LocalTime.of(12, 0))
                .horaFin(LocalTime.of(16, 0))
                .motivo("Evento privado")
                .build();

        assertThat(b.getBloqueId()).isEqualTo(1L);
        assertThat(b.getFechaInicio()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(b.getMotivo()).isEqualTo("Evento privado");
        assertThat(b.getHoraInicio()).isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    void setter_actualizaMotivo() {
        BloqueDisponibilidad b = new BloqueDisponibilidad();
        b.setMotivo("Feriado");
        assertThat(b.getMotivo()).isEqualTo("Feriado");
    }

    @Test
    void equals_mismoId_retornaTrue() {
        BloqueDisponibilidad a = BloqueDisponibilidad.builder().bloqueId(1L).build();
        BloqueDisponibilidad b = BloqueDisponibilidad.builder().bloqueId(1L).build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_diferenteId_retornaFalse() {
        BloqueDisponibilidad a = BloqueDisponibilidad.builder().bloqueId(1L).build();
        BloqueDisponibilidad b = BloqueDisponibilidad.builder().bloqueId(2L).build();
        assertThat(a).isNotEqualTo(b);
    }
}
