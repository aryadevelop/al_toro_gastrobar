package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class VisitaEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        LocalDateTime inicio = LocalDateTime.of(2026, 4, 1, 12, 0);
        Visita v = Visita.builder()
                .visitaId(1L)
                .visitaFechaHoraInicio(inicio)
                .build();

        assertThat(v.getVisitaId()).isEqualTo(1L);
        assertThat(v.getVisitaFechaHoraInicio()).isEqualTo(inicio);
    }

    @Test
    void setter_actualizaFechaFin() {
        Visita v = new Visita();
        LocalDateTime fin = LocalDateTime.of(2026, 4, 1, 14, 0);
        v.setVisitaFechaHoraFin(fin);
        assertThat(v.getVisitaFechaHoraFin()).isEqualTo(fin);
    }

    @Test
    void onCreate_setsFechaInicioSiEsNull() {
        Visita v = new Visita();
        v.onCreate();
        assertThat(v.getVisitaFechaHoraInicio()).isNotNull();
    }

    @Test
    void onCreate_noSobreescribeFechaExistente() {
        LocalDateTime fijo = LocalDateTime.of(2026, 1, 1, 10, 0);
        Visita v = new Visita();
        v.setVisitaFechaHoraInicio(fijo);
        v.onCreate();
        assertThat(v.getVisitaFechaHoraInicio()).isEqualTo(fijo);
    }

    @Test
    void equals_mismoId_retornaTrue() {
        Visita a = Visita.builder().visitaId(1L).build();
        Visita b = Visita.builder().visitaId(1L).build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_diferenteId_retornaFalse() {
        Visita a = Visita.builder().visitaId(1L).build();
        Visita b = Visita.builder().visitaId(2L).build();
        assertThat(a).isNotEqualTo(b);
    }
}
