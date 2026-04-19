package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link MensajeWhatsAppBuilder}.
 *
 * <p>El componente es stateless y no depende de repositorios, por lo que se
 * instancia directamente con {@code new} y se ejercita con entidades construidas
 * mediante el builder de Lombok.</p>
 */
@ExtendWith(MockitoExtension.class)
class MensajeWhatsAppBuilderTest {

    private MensajeWhatsAppBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new MensajeWhatsAppBuilder();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Reserva reservaBase() {
        Cliente cliente = Cliente.builder()
                .clienteNombre("María López")
                .build();
        return Reserva.builder()
                .reservaId(42L)
                .cliente(cliente)
                .reservaFechaHoraLlegada(LocalDateTime.of(2026, 5, 10, 19, 30))
                .reservaNumeroPersonas(4)
                .build();
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Mensaje con decoración y zona → incluye ambos campos en el texto")
    void conDecoracionYZona_incluyeAmbosEnMensaje() {
        Decoracion decoracion = mock(Decoracion.class);
        when(decoracion.getDecoracionNombre()).thenReturn("Globos Rojos");

        Zona zona = mock(Zona.class);
        when(zona.getZonaNombre()).thenReturn("Terraza");

        Reserva reserva = reservaBase();
        reserva.setDecoracion(decoracion);
        reserva.setZona(zona);

        String mensaje = builder.construirMensaje(reserva, MensajeWhatsAppBuilder.MSG_WA_ANTICIPO);

        assertThat(mensaje).contains("Globos Rojos");
        assertThat(mensaje).contains("Terraza");
    }

    @Test
    @DisplayName("Mensaje sin decoración (null) → no contiene línea '- Decoración:'")
    void sinDecoracion_noIncluyeLineaDecoracion() {
        Reserva reserva = reservaBase();
        reserva.setDecoracion(null);
        reserva.setZona(null);

        String mensaje = builder.construirMensaje(reserva, MensajeWhatsAppBuilder.MSG_WA_ANTICIPO);

        assertThat(mensaje).doesNotContain("- Decoración:");
    }

    @Test
    @DisplayName("Mensaje sin zona (null) → no contiene línea '- Zona:'")
    void sinZona_noIncluyeLineaZona() {
        Reserva reserva = reservaBase();
        reserva.setDecoracion(null);
        reserva.setZona(null);

        String mensaje = builder.construirMensaje(reserva, MensajeWhatsAppBuilder.MSG_WA_ABONO_AJUSTE);

        assertThat(mensaje).doesNotContain("- Zona:");
    }

    @Test
    @DisplayName("Nota MSG_WA_ANTICIPO se adjunta al final del mensaje")
    void notaAnticipo_seAdjuntaEnMensaje() {
        Reserva reserva = reservaBase();
        reserva.setDecoracion(null);
        reserva.setZona(null);

        String mensaje = builder.construirMensaje(reserva, MensajeWhatsAppBuilder.MSG_WA_ANTICIPO);

        assertThat(mensaje).contains(MensajeWhatsAppBuilder.MSG_WA_ANTICIPO);
    }

    @Test
    @DisplayName("Fecha formateada como dd/MM/yyyy HH:mm → aparece en el mensaje")
    void fechaFormateada_apareceEnMensaje() {
        Reserva reserva = reservaBase(); // 2026-05-10 19:30
        reserva.setDecoracion(null);
        reserva.setZona(null);

        String mensaje = builder.construirMensaje(reserva, MensajeWhatsAppBuilder.MSG_WA_CAMBIO_ESPECIAL);

        assertThat(mensaje).contains("10/05/2026 19:30");
    }
}
