package co.edu.unicauca.backend.modules.reservas.entity;

import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class ReservaEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        LocalDateTime llegada = LocalDateTime.of(2026, 5, 10, 19, 0);
        Reserva r = Reserva.builder()
                .reservaId(1L)
                .reservaFechaHoraLlegada(llegada)
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();

        assertThat(r.getReservaId()).isEqualTo(1L);
        assertThat(r.getReservaFechaHoraLlegada()).isEqualTo(llegada);
        assertThat(r.getReservaNumeroPersonas()).isEqualTo(4);
        assertThat(r.getReservaEstado()).isEqualTo(EstadoReserva.PENDIENTE);
        assertThat(r.getReservaTipo()).isEqualTo(TipoReserva.BASICA);
    }

    @Test
    void setter_actualizaEstado() {
        Reserva r = new Reserva();
        r.setReservaEstado(EstadoReserva.CONFIRMADA);
        assertThat(r.getReservaEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    void onCreate_setsFechaCreacionSiEsNull() {
        Reserva r = new Reserva();
        r.onCreate();
        assertThat(r.getReservaFechaCreacion()).isNotNull();
    }

    @Test
    void onCreate_noSobreescribeFechaCreacionExistente() {
        LocalDateTime fijo = LocalDateTime.of(2026, 1, 1, 8, 0);
        Reserva r = new Reserva();
        r.setReservaFechaCreacion(fijo);
        r.onCreate();
        assertThat(r.getReservaFechaCreacion()).isEqualTo(fijo);
    }

    @Test
    void equals_mismoId_retornaTrue() {
        Reserva a = Reserva.builder().reservaId(1L).build();
        Reserva b = Reserva.builder().reservaId(1L).build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_diferenteId_retornaFalse() {
        Reserva a = Reserva.builder().reservaId(1L).build();
        Reserva b = Reserva.builder().reservaId(2L).build();
        assertThat(a).isNotEqualTo(b);
    }
}
