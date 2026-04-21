package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.reservas.dto.response.CancelarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DecoracionDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ZonaDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.DecoracionZona;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

@ExtendWith(MockitoExtension.class)
class ReservaMapperTest {

    @Mock
    PreOrdenMapper preOrdenMapper;

    @InjectMocks
    ReservaMapper mapper;

    private Reserva reservaBase() {
        return Reserva.builder()
                .reservaId(1L)
                .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(5))
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();
    }

    @Test
    @DisplayName("sinDisponibilidad → disponible=false, listas vacías")
    void sinDisponibilidad_retornaResponseVacio() {
        DisponibilidadResponse resp = mapper.sinDisponibilidad();

        assertThat(resp.getDisponible()).isFalse();
        assertThat(resp.getZonas()).isEmpty();
        assertThat(resp.getDecoraciones()).isEmpty();
    }

    @Test
    @DisplayName("toZonaDto → mapea nombre y capacidad")
    void toZonaDto_mapeaCampos() {
        Zona zona = Zona.builder()
                .zonaId(1L)
                .zonaNombre("Terraza")
                .zonaCapacidadPersonas(20)
                .build();

        ZonaDisponibleResponse resp = mapper.toZonaDto(zona);

        assertThat(resp.getZonaId()).isEqualTo(1L);
        assertThat(resp.getNombre()).isEqualTo("Terraza");
        assertThat(resp.getCapacidad()).isEqualTo(20);
    }

    @Test
    @DisplayName("toDecoracionDto → decoración con zonas libres muestra zonas compatibles")
    void toDecoracionDto_decoracionConZonasLibres_tieneZonasCompatibles() {
        Decoracion dec = Decoracion.builder()
                .decoracionId(1L)
                .decoracionNombre("Globos")
                .decoracionEstado(EstadoGenerico.ACTIVO)
                .decoracionCostoAdicional(BigDecimal.valueOf(20000))
                .build();
        Zona zona = Zona.builder().zonaId(1L).zonaNombre("Terraza").build();
        DecoracionZona dz = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(1L)
                .zona(zona)
                .build();

        // Two links → puedeSeleccionarZona = true; zona 1L is in idsZonasLibres
        DecoracionZona dz2 = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(2L)
                .build();

        DecoracionDisponibleResponse resp = mapper.toDecoracionDto(dec, List.of(dz, dz2), Set.of(1L));

        assertThat(resp.getDecoracionId()).isEqualTo(1L);
        assertThat(resp.getNombre()).isEqualTo("Globos");
        assertThat(resp.getPuedeSeleccionarZona()).isTrue();
        assertThat(resp.getZonaIdsCompatibles()).containsExactly(1L);
    }

    @Test
    @DisplayName("toDecoracionDto → decoración sin zonas libres no tiene zonas compatibles")
    void toDecoracionDto_sinZonasLibres_listaCompatiblesVacia() {
        Decoracion dec = Decoracion.builder()
                .decoracionId(2L)
                .decoracionNombre("Flores")
                .decoracionEstado(EstadoGenerico.ACTIVO)
                .build();
        DecoracionZona dz1 = DecoracionZona.builder().decoracionId(2L).zonaId(3L).build();
        DecoracionZona dz2 = DecoracionZona.builder().decoracionId(2L).zonaId(4L).build();

        DecoracionDisponibleResponse resp = mapper.toDecoracionDto(dec, List.of(dz1, dz2), Set.of());

        assertThat(resp.getZonaIdsCompatibles()).isEmpty();
    }

    @Test
    @DisplayName("toResumen → mapea campos básicos")
    void toResumen_mapeaCamposBasicos() {
        Reserva reserva = reservaBase();

        ReservaDetalleResponse resp = mapper.toResumen(reserva);

        assertThat(resp.getReservaId()).isEqualTo(1L);
        assertThat(resp.getNumeroPersonas()).isEqualTo(4);
    }

    @Test
    @DisplayName("toDetalleResponse → sin items ni abonos")
    void toDetalleResponse_sinItemsNiAbonos() {
        ReservaDetalleResponse resp = mapper.toDetalleResponse(reservaBase(), List.of(), List.of());

        assertThat(resp.getReservaId()).isEqualTo(1L);
    }

    // ── toCancelarResponse ────────────────────────────────────────────────────

    @Nested
    @DisplayName("toCancelarResponse — conversión a DTO de cancelación")
    class ToCancelarResponse {

        private Reserva reservaMock;

        @BeforeEach
        void setUp() {
            reservaMock = mock(Reserva.class);
            when(reservaMock.getReservaId()).thenReturn(1L);
            when(reservaMock.getReservaEstado()).thenReturn(EstadoReserva.CANCELADA);
            when(reservaMock.getReservaTipo()).thenReturn(TipoReserva.BASICA);
            when(reservaMock.getReservaFechaHoraLlegada())
                    .thenReturn(LocalDateTime.of(2027, 6, 15, 19, 0));
            when(reservaMock.getReservaNumeroPersonas()).thenReturn(3);
        }

        @Test
        @DisplayName("Sin WhatsApp → mensajeWhatsApp es null y requiereWhatsApp=false")
        void sinWhatsApp_camposNulos() {
            CancelarReservaResponse response = mapper.toCancelarResponse(reservaMock, false, null);

            assertThat(response.getReservaId()).isEqualTo(1L);
            assertThat(response.getEstado()).isEqualTo("CANCELADA");
            assertThat(response.getTipo()).isEqualTo("BASICA");
            assertThat(response.getFechaHoraLlegada()).isEqualTo("2027-06-15T19:00:00");
            assertThat(response.getNumeroPersonas()).isEqualTo(3);
            assertThat(response.isRequiereWhatsApp()).isFalse();
            assertThat(response.getMensajeWhatsApp()).isNull();
        }

        @Test
        @DisplayName("Con WhatsApp → mensajeWhatsApp y requiereWhatsApp=true presentes")
        void conWhatsApp_camposPresentes() {
            String mensaje = "Comunícate para gestionar el reembolso de tu abono.";

            CancelarReservaResponse response = mapper.toCancelarResponse(reservaMock, true, mensaje);

            assertThat(response.isRequiereWhatsApp()).isTrue();
            assertThat(response.getMensajeWhatsApp()).isEqualTo(mensaje);
        }

        @Test
        @DisplayName("Estado siempre es 'CANCELADA' independientemente del enum")
        void estadoEsCancelada() {
            CancelarReservaResponse response = mapper.toCancelarResponse(reservaMock, false, null);

            assertThat(response.getEstado()).isEqualTo("CANCELADA");
        }

        @Test
        @DisplayName("Tipo ESPECIAL se mapea correctamente")
        void tipoEspecial_seMapea() {
            when(reservaMock.getReservaTipo()).thenReturn(TipoReserva.ESPECIAL);

            CancelarReservaResponse response = mapper.toCancelarResponse(reservaMock, true, "msg");

            assertThat(response.getTipo()).isEqualTo("ESPECIAL");
        }
    }
}
