package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class ComandaEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        Comanda c = Comanda.builder()
                .comandaId(1L)
                .comandaEstacion(EstacionComanda.COCINA)
                .comandaEstado(EstadoComanda.PENDIENTE)
                .build();

        assertThat(c.getComandaId()).isEqualTo(1L);
        assertThat(c.getComandaEstacion()).isEqualTo(EstacionComanda.COCINA);
        assertThat(c.getComandaEstado()).isEqualTo(EstadoComanda.PENDIENTE);
    }

    @Test
    void setter_actualizaEstado() {
        Comanda c = new Comanda();
        c.setComandaEstado(EstadoComanda.PENDIENTE);
        assertThat(c.getComandaEstado()).isEqualTo(EstadoComanda.PENDIENTE);
    }

    @Test
    void onCreate_setsFechaInicioSiEsNull() {
        Comanda c = new Comanda();
        c.onCreate();
        assertThat(c.getComandaFechaHoraInicio()).isNotNull();
    }

    @Test
    void onCreate_noSobreescribeFechaExistente() {
        LocalDateTime fijo = LocalDateTime.of(2026, 1, 1, 9, 0);
        Comanda c = new Comanda();
        c.setComandaFechaHoraInicio(fijo);
        c.onCreate();
        assertThat(c.getComandaFechaHoraInicio()).isEqualTo(fijo);
    }
}
