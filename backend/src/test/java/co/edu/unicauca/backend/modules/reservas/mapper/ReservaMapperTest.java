package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.reservas.dto.response.AbonoItemResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenItemResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.CancelarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DecoracionDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ZonaDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.DecoracionZona;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
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
import static org.mockito.Mockito.withSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

@ExtendWith(MockitoExtension.class)
class ReservaMapperTest {

    @Mock
    PreOrdenMapper preOrdenMapper;

    @InjectMocks
    ReservaMapper mapper;

    private Reserva reservaBase() {
        Cliente clienteMock = mock(Cliente.class, withSettings().strictness(Strictness.LENIENT));
        when(clienteMock.getClienteNombre()).thenReturn("Juan Pérez");

        return Reserva.builder()
                .reservaId(1L)
                .cliente(clienteMock)
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

        ZonaDisponibleResponse resp = mapper.toZonaDto(zona, 5);

        assertThat(resp.getZonaId()).isEqualTo(1L);
        assertThat(resp.getNombre()).isEqualTo("Terraza");
        assertThat(resp.getCapacidad()).isEqualTo(15);
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
    @DisplayName("toDecoracionDto → un solo link → puedeSeleccionarZona=false")
    void toDecoracionDto_unSoloLink_noPuedeSeleccionarZona() {
        Decoracion dec = Decoracion.builder()
                .decoracionId(3L)
                .decoracionNombre("Velas")
                .decoracionEstado(EstadoGenerico.ACTIVO)
                .build();
        Zona zonaUnica = Zona.builder().zonaId(1L).zonaNombre("Terraza").build();
        DecoracionZona dzUnico = DecoracionZona.builder()
                .decoracionId(3L).zonaId(1L).zona(zonaUnica).build();

        DecoracionDisponibleResponse resp = mapper.toDecoracionDto(dec, List.of(dzUnico), Set.of(1L));

        assertThat(resp.getPuedeSeleccionarZona()).isFalse();
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
        assertThat(resp.getPreOrdenItems()).isNull();
        assertThat(resp.getAbonos()).isNull();
    }

    @Test
    @DisplayName("toDetalleResponse → incluye clienteId desde el usuarioId del cliente")
    void toDetalleResponse_incluyeClienteId() {
        Cliente clienteMock = mock(Cliente.class, withSettings().strictness(Strictness.LENIENT));
        when(clienteMock.getClienteNombre()).thenReturn("Juan Pérez");
        when(clienteMock.getUsuarioId()).thenReturn(7L);
        Reserva reserva = Reserva.builder()
                .reservaId(1L)
                .cliente(clienteMock)
                .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(5))
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();

        ReservaDetalleResponse resp = mapper.toDetalleResponse(reserva, List.of(), List.of());

        assertThat(resp.getClienteId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("toDetalleResponse → con zona asignada mapea zonaId y zonaNombre")
    void toDetalleResponse_conZona_mapeaZonaCampos() {
        Reserva reserva = reservaBase();
        reserva.setZona(Zona.builder().zonaId(7L).zonaNombre("TERRAZA").build());

        ReservaDetalleResponse resp = mapper.toDetalleResponse(reserva, List.of(), List.of());

        assertThat(resp.getZonaId()).isEqualTo(7L);
        assertThat(resp.getZonaNombre()).isEqualTo("TERRAZA");
    }

    @Test
    @DisplayName("toDetalleResponse → estado terminal CANCELADA → modificable=false")
    void toDetalleResponse_estadoTerminal_modificableFalse() {
        Reserva reserva = reservaBase();
        reserva.setReservaEstado(EstadoReserva.CANCELADA);

        ReservaDetalleResponse resp = mapper.toDetalleResponse(reserva, List.of(), List.of());

        assertThat(resp.isModificable()).isFalse();
    }

    // ── toConfirmarResponse ───────────────────────────────────────────────────

    @Nested
    @DisplayName("toConfirmarResponse — conversión a DTO de confirmación")
    class ToConfirmarResponse {

        @Test
        @DisplayName("Reserva especial confirmada → mapea todos los campos")
        void mapeaTodosLosCampos() {
            Reserva reserva = reservaBase();
            reserva.setReservaEstado(EstadoReserva.CONFIRMADA);
            reserva.setReservaTipo(TipoReserva.ESPECIAL);
            reserva.setZona(Zona.builder().zonaId(2L).zonaNombre("Terraza").build());

            co.edu.unicauca.backend.modules.reservas.dto.response.ConfirmarReservaResponse resp =
                    mapper.toConfirmarResponse(reserva);

            assertThat(resp.getReservaId()).isEqualTo(1L);
            assertThat(resp.getEstado()).isEqualTo("CONFIRMADA");
            assertThat(resp.getTipo()).isEqualTo("ESPECIAL");
            assertThat(resp.getNumeroPersonas()).isEqualTo(4);
            assertThat(resp.getClienteNombre()).isEqualTo("Juan Pérez");
            assertThat(resp.getZonaNombre()).isEqualTo("Terraza");
            assertThat(resp.getFechaHoraLlegada()).isNotNull();
        }

        @Test
        @DisplayName("Reserva sin zona → zonaNombre es null")
        void zonaNull_dejaZonaNombreNull() {
            Reserva reserva = reservaBase();
            reserva.setReservaEstado(EstadoReserva.CONFIRMADA);
            reserva.setReservaTipo(TipoReserva.ESPECIAL);

            co.edu.unicauca.backend.modules.reservas.dto.response.ConfirmarReservaResponse resp =
                    mapper.toConfirmarResponse(reserva);

            assertThat(resp.getZonaNombre()).isNull();
        }
    }

    // ── toResponse ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("toResponse — conversión a DTO de creación")
    class ToResponse {

        @Test
        @DisplayName("Sin WhatsApp → requiereWhatsApp es null, mensajeWhatsApp es null")
        void sinWhatsApp_camposNulos() {
            Reserva reserva = reservaBase();
            Zona zona = Zona.builder().zonaId(1L).zonaNombre("Salón Principal").build();
            reserva.setZona(zona);

            co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse resp =
                mapper.toResponse(reserva, false, null);

            assertThat(resp.getReservaId()).isEqualTo(1L);
            assertThat(resp.getNumeroPersonas()).isEqualTo(4);
            assertThat(resp.getEstado()).isEqualTo("PENDIENTE");
            assertThat(resp.getTipo()).isEqualTo("BASICA");
            assertThat(resp.getZonaNombre()).isEqualTo("Salón Principal");
            assertThat(resp.getRequiereWhatsApp()).isNull();
            assertThat(resp.getMensajeWhatsApp()).isNull();
        }

        @Test
        @DisplayName("Con WhatsApp → requiereWhatsApp es true, mensajeWhatsApp presente")
        void conWhatsApp_camposPresentes() {
            Reserva reserva = reservaBase();
            String mensaje = "Tu reserva ha sido confirmada";

            co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse resp =
                mapper.toResponse(reserva, true, mensaje);

            assertThat(resp.getRequiereWhatsApp()).isTrue();
            assertThat(resp.getMensajeWhatsApp()).isEqualTo(mensaje);
        }

        @Test
        @DisplayName("Con decoración → decoracionNombre presente")
        void conDecoracion_nombrePresente() {
            Reserva reserva = reservaBase();
            Decoracion decoracion = Decoracion.builder()
                .decoracionId(1L)
                .decoracionNombre("Flores")
                .build();
            reserva.setDecoracion(decoracion);

            co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse resp =
                mapper.toResponse(reserva, false, null);

            assertThat(resp.getDecoracionNombre()).isEqualTo("Flores");
        }

        @Test
        @DisplayName("Sin zona ni decoración → campos opcionales son null")
        void sinZonaNiDecoracion_camposNull() {
            Reserva reserva = reservaBase();

            co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse resp =
                mapper.toResponse(reserva, false, null);

            assertThat(resp.getZonaNombre()).isNull();
            assertThat(resp.getDecoracionNombre()).isNull();
        }
    }

    // ── toModificarResponse ───────────────────────────────────────────────────

    @Nested
    @DisplayName("toModificarResponse — conversión a DTO de modificación")
    class ToModificarResponse {

        @Test
        @DisplayName("Modificación sin WhatsApp → requiereWhatsApp es false")
        void sinWhatsApp_requiereFalse() {
            Reserva reserva = reservaBase();

            co.edu.unicauca.backend.modules.reservas.dto.response.ModificarReservaResponse resp =
                mapper.toModificarResponse(reserva, false, null);

            assertThat(resp.getReservaId()).isEqualTo(1L);
            assertThat(resp.getEstado()).isEqualTo("PENDIENTE");
            assertThat(resp.isRequiereWhatsApp()).isFalse();
            assertThat(resp.getMensajeWhatsApp()).isNull();
        }

        @Test
        @DisplayName("Modificación con WhatsApp → requiereWhatsApp es true, mensaje presente")
        void conWhatsApp_camposPresentes() {
            Reserva reserva = reservaBase();
            String mensaje = "La modificación requiere contacto para ajustar el abono";

            co.edu.unicauca.backend.modules.reservas.dto.response.ModificarReservaResponse resp =
                mapper.toModificarResponse(reserva, true, mensaje);

            assertThat(resp.isRequiereWhatsApp()).isTrue();
            assertThat(resp.getMensajeWhatsApp()).isEqualTo(mensaje);
        }

        @Test
        @DisplayName("Con zona y decoración → nombres mapeados correctamente")
        void conZonaYDecoracion_nombresMapeados() {
            Reserva reserva = reservaBase();
            Zona zona = Zona.builder().zonaId(2L).zonaNombre("Terraza").build();
            Decoracion decoracion = Decoracion.builder()
                .decoracionId(3L)
                .decoracionNombre("Globos")
                .build();
            reserva.setZona(zona);
            reserva.setDecoracion(decoracion);

            co.edu.unicauca.backend.modules.reservas.dto.response.ModificarReservaResponse resp =
                mapper.toModificarResponse(reserva, false, null);

            assertThat(resp.getZonaNombre()).isEqualTo("Terraza");
            assertThat(resp.getDecoracionNombre()).isEqualTo("Globos");
        }
    }

    // ── toResumen — casos adicionales ─────────────────────────────────────────

    @Test
    @DisplayName("toResumen → con zona y decoración")
    void toResumen_conZonaYDecoracion() {
        Reserva reserva = reservaBase();
        Zona zona = Zona.builder().zonaId(1L).zonaNombre("VIP").build();
        Decoracion decoracion = Decoracion.builder()
            .decoracionId(2L)
            .decoracionNombre("Velas")
            .build();
        reserva.setZona(zona);
        reserva.setDecoracion(decoracion);

        ReservaDetalleResponse resp = mapper.toResumen(reserva);

        assertThat(resp.getZonaId()).isEqualTo(1L);
        assertThat(resp.getZonaNombre()).isEqualTo("VIP");
        assertThat(resp.getDecoracionId()).isEqualTo(2L);
        assertThat(resp.getDecoracionNombre()).isEqualTo("Velas");
    }

    @Test
    @DisplayName("toResumen → sin zona ni decoración")
    void toResumen_sinZonaNiDecoracion() {
        Reserva reserva = reservaBase();

        ReservaDetalleResponse resp = mapper.toResumen(reserva);

        assertThat(resp.getZonaId()).isNull();
        assertThat(resp.getZonaNombre()).isNull();
        assertThat(resp.getDecoracionId()).isNull();
        assertThat(resp.getDecoracionNombre()).isNull();
    }

    // Nota: esModificable() es un método privado, se testea indirectamente via toResumen()

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

    // ── toDetalleResponse — casos con pre-orden y abonos ─────────────────────

    @Test
    @DisplayName("toDetalleResponse → con items y abonos calcula totales y delega items al PreOrdenMapper")
    void toDetalleResponse_conItemsYAbonos_calculaTotales() {
        // Items: 2 x 10000 + 1 x 5000 = 25000
        ComandaItem item1 = mock(ComandaItem.class, withSettings().strictness(Strictness.LENIENT));
        when(item1.getComandaItemPrecio()).thenReturn(new BigDecimal("10000"));
        when(item1.getComandaItemCantidad()).thenReturn(2);
        ComandaItem item2 = mock(ComandaItem.class, withSettings().strictness(Strictness.LENIENT));
        when(item2.getComandaItemPrecio()).thenReturn(new BigDecimal("5000"));
        when(item2.getComandaItemCantidad()).thenReturn(1);

        // Abonos: 30000 + 20000 = 50000
        Abono abono1 = Abono.builder()
                .abonoId(11L)
                .abonoMonto(new BigDecimal("30000"))
                .abonoFechaHora(LocalDateTime.of(2026, 1, 1, 10, 0))
                .abonoMetodo(MetodoPago.EFECTIVO)
                .abonoTipo(TipoAbono.ANTICIPO)
                .build();
        Abono abono2 = Abono.builder()
                .abonoId(12L)
                .abonoMonto(new BigDecimal("20000"))
                .abonoFechaHora(LocalDateTime.of(2026, 1, 2, 11, 30))
                .abonoMetodo(MetodoPago.TARJETA)
                .abonoTipo(TipoAbono.DEVOLUCION)
                .build();

        // Mock del preOrdenMapper para verificar delegación
        PreOrdenItemResponse mappedItem = PreOrdenItemResponse.builder()
                .comandaItemId(1L)
                .productoNombre("X")
                .cantidad(2)
                .precioUnitario(new BigDecimal("10000"))
                .build();
        when(preOrdenMapper.toDetalleResponse(List.of(item1, item2))).thenReturn(List.of(mappedItem));

        Reserva reserva = reservaBase();
        Decoracion deco = Decoracion.builder().decoracionId(7L).decoracionNombre("Velas").build();
        reserva.setDecoracion(deco);

        ReservaDetalleResponse resp = mapper.toDetalleResponse(reserva,
                List.of(item1, item2), List.of(abono1, abono2));

        // Totales calculados
        assertThat(resp.getPreOrdenTotal()).isEqualByComparingTo(new BigDecimal("25000"));
        assertThat(resp.getTotalAbonado()).isEqualByComparingTo(new BigDecimal("50000"));

        // Items delegados al preOrdenMapper
        assertThat(resp.getPreOrdenItems()).hasSize(1);

        // Abonos construidos manualmente
        assertThat(resp.getAbonos()).hasSize(2);
        AbonoItemResponse a1 = resp.getAbonos().get(0);
        assertThat(a1.getAbonoId()).isEqualTo(11L);
        assertThat(a1.getMonto()).isEqualByComparingTo(new BigDecimal("30000"));
        assertThat(a1.getFechaHora()).isEqualTo("2026-01-01T10:00:00");
        assertThat(a1.getMetodo()).isEqualTo("EFECTIVO");
        assertThat(a1.getTipo()).isEqualTo("ANTICIPO");
        AbonoItemResponse a2 = resp.getAbonos().get(1);
        assertThat(a2.getMetodo()).isEqualTo("TARJETA");
        assertThat(a2.getTipo()).isEqualTo("DEVOLUCION");

        // Decoración presente
        assertThat(resp.getDecoracionNombre()).isEqualTo("Velas");
    }

    @Test
    @DisplayName("toResumen → reserva en estado terminal no es modificable")
    void toResumen_estadoTerminal_noEsModificable() {
        Reserva reserva = reservaBase();
        reserva.setReservaEstado(EstadoReserva.CANCELADA);

        ReservaDetalleResponse resp = mapper.toResumen(reserva);

        assertThat(resp.isModificable()).isFalse();
    }
}
