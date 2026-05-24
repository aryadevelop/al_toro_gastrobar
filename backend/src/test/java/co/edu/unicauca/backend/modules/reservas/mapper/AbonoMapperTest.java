package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.reservas.dto.request.RegistrarAbonoRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.RegistrarAbonoResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ResumenPagoResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Tests unitarios de {@link AbonoMapper}.
 *
 * <p>Verifica los tres métodos públicos del mapper:
 * <ul>
 *   <li>{@link AbonoMapper#toEntity}: construcción de {@link Abono} a partir del request.</li>
 *   <li>{@link AbonoMapper#toResumenPago}: cálculo de neto, pendiente de abonar y pendiente de devolver.</li>
 *   <li>{@link AbonoMapper#toRegistrarResponse}: composición de la respuesta de registro.</li>
 * </ul>
 */
class AbonoMapperTest {

    private final AbonoMapper mapper = new AbonoMapper();

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Reserva reservaBase() {
        Cliente clienteMock = mock(Cliente.class, withSettings().strictness(Strictness.LENIENT));
        when(clienteMock.getClienteNombre()).thenReturn("María López");

        return Reserva.builder()
                .reservaId(10L)
                .cliente(clienteMock)
                .reservaFechaHoraLlegada(LocalDateTime.of(2026, 6, 15, 19, 0))
                .reservaNumeroPersonas(3)
                .reservaEstado(EstadoReserva.CONFIRMADA)
                .reservaTipo(TipoReserva.ESPECIAL)
                .build();
    }

    // ── toEntity ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toEntity → mapea todos los campos del request, reserva y cajero")
    void toEntity_mapeaCamposYUsaFechaHoraDelRequest() {
        // Given
        LocalDateTime fechaHora = LocalDateTime.of(2026, 6, 10, 14, 30);
        RegistrarAbonoRequest request = RegistrarAbonoRequest.builder()
                .tipo(TipoAbono.ANTICIPO)
                .monto(new BigDecimal("50000.00"))
                .metodo(MetodoPago.EFECTIVO)
                .fechaHora(fechaHora)
                .build();
        Reserva reserva = reservaBase();
        Empleado cajero = Empleado.builder().usuarioId(5L).build();

        // When
        Abono abono = mapper.toEntity(request, reserva, cajero);

        // Then
        assertThat(abono.getReserva()).isSameAs(reserva);
        assertThat(abono.getCajero()).isSameAs(cajero);
        assertThat(abono.getAbonoMonto()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(abono.getAbonoMetodo()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(abono.getAbonoTipo()).isEqualTo(TipoAbono.ANTICIPO);
        assertThat(abono.getAbonoFechaHora()).isEqualTo(fechaHora);
        // ID no asignado antes de persistir
        assertThat(abono.getAbonoId()).isNull();
    }

    // ── toResumenPago ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("toResumenPago CONFIRMADA → pendientePorAbonar presente, pendientePorDevolver null")
    void toResumenPago_calculaNetoYPendientes() {
        // Given — reserva CONFIRMADA (reservaBase ya la devuelve así)
        Reserva reserva = reservaBase();
        BigDecimal totalReserva   = new BigDecimal("200000.00");
        BigDecimal totalAnticipado = new BigDecimal("80000.00");
        BigDecimal totalDevuelto   = new BigDecimal("20000.00");

        // When
        ResumenPagoResponse resp = mapper.toResumenPago(reserva, totalReserva, totalAnticipado, totalDevuelto);

        // Then — identidad de la reserva
        assertThat(resp.getReservaId()).isEqualTo(10L);
        assertThat(resp.getClienteNombre()).isEqualTo("María López");
        assertThat(resp.getFechaHoraLlegada()).isEqualTo("2026-06-15T19:00:00");
        assertThat(resp.getNumeroPersonas()).isEqualTo(3);
        assertThat(resp.getEstado()).isEqualTo("CONFIRMADA");
        assertThat(resp.getTipo()).isEqualTo("ESPECIAL");
        // Importes calculados
        assertThat(resp.getTotalReserva()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(resp.getTotalAnticipado()).isEqualByComparingTo(new BigDecimal("80000.00"));
        assertThat(resp.getTotalDevuelto()).isEqualByComparingTo(new BigDecimal("20000.00"));
        // neto = 80000 - 20000 = 60000
        assertThat(resp.getNetoAbonado()).isEqualByComparingTo(new BigDecimal("60000.00"));
        // CONFIRMADA: pendienteAbonar = 200000 - 60000 = 140000; pendienteDevolver omitido
        assertThat(resp.getPendientePorAbonar()).isEqualByComparingTo(new BigDecimal("140000.00"));
        assertThat(resp.getPendientePorDevolver()).isNull();
    }

    @Test
    @DisplayName("toResumenPago CONFIRMADA → cuando neto mayor que total, pendienteAbonar es cero")
    void toResumenPago_netoMayorQueTotal_pendienteAbonarEsCero() {
        // Given: neto = 120000 > total = 100000 (caso borde por redondeos); reserva CONFIRMADA
        Reserva reserva = reservaBase();
        BigDecimal totalReserva    = new BigDecimal("100000.00");
        BigDecimal totalAnticipado = new BigDecimal("120000.00");
        BigDecimal totalDevuelto   = BigDecimal.ZERO;

        // When
        ResumenPagoResponse resp = mapper.toResumenPago(reserva, totalReserva, totalAnticipado, totalDevuelto);

        // Then — pendiente nunca es negativo; pendienteDevolver omitido
        assertThat(resp.getNetoAbonado()).isEqualByComparingTo(new BigDecimal("120000.00"));
        assertThat(resp.getPendientePorAbonar()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.getPendientePorDevolver()).isNull();
    }

    @Test
    @DisplayName("toResumenPago CANCELADA → pendientePorDevolver == neto, pendientePorAbonar null")
    void toResumenPago_reservaCancelada_pendienteDevolver() {
        // Given — reserva en estado CANCELADA con neto de 50000
        Cliente clienteMock = mock(Cliente.class, withSettings().strictness(Strictness.LENIENT));
        when(clienteMock.getClienteNombre()).thenReturn("María López");
        Reserva reserva = Reserva.builder()
                .reservaId(10L)
                .cliente(clienteMock)
                .reservaFechaHoraLlegada(LocalDateTime.of(2026, 6, 15, 19, 0))
                .reservaNumeroPersonas(3)
                .reservaEstado(EstadoReserva.CANCELADA)
                .reservaTipo(TipoReserva.ESPECIAL)
                .build();
        BigDecimal totalReserva    = new BigDecimal("100000.00");
        BigDecimal totalAnticipado = new BigDecimal("60000.00");
        BigDecimal totalDevuelto   = new BigDecimal("10000.00");

        // When
        ResumenPagoResponse resp = mapper.toResumenPago(reserva, totalReserva, totalAnticipado, totalDevuelto);

        // Then — neto = 50000; CANCELADA: pendienteDevolver == neto, pendienteAbonar omitido
        assertThat(resp.getNetoAbonado()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(resp.getPendientePorDevolver()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(resp.getPendientePorAbonar()).isNull();
    }

    @Test
    @DisplayName("toResumenPago DEVUELTA → pendientePorDevolver == 0 (neto), pendientePorAbonar null")
    void toResumenPago_reservaDevuelta_pendienteDevolverEsCero() {
        // Given — reserva DEVUELTA: anticipo = devolución → neto = 0
        Cliente clienteMock = mock(Cliente.class, withSettings().strictness(Strictness.LENIENT));
        when(clienteMock.getClienteNombre()).thenReturn("María López");
        Reserva reserva = Reserva.builder()
                .reservaId(10L)
                .cliente(clienteMock)
                .reservaFechaHoraLlegada(LocalDateTime.of(2026, 6, 15, 19, 0))
                .reservaNumeroPersonas(3)
                .reservaEstado(EstadoReserva.DEVUELTA)
                .reservaTipo(TipoReserva.ESPECIAL)
                .build();
        BigDecimal totalReserva    = new BigDecimal("100000.00");
        BigDecimal totalAnticipado = new BigDecimal("50000.00");
        BigDecimal totalDevuelto   = new BigDecimal("50000.00");

        // When
        ResumenPagoResponse resp = mapper.toResumenPago(reserva, totalReserva, totalAnticipado, totalDevuelto);

        // Then — neto = 0; DEVUELTA: pendienteDevolver == 0, pendienteAbonar omitido
        assertThat(resp.getNetoAbonado()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.getPendientePorDevolver()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.getPendientePorAbonar()).isNull();
    }

    @Test
    @DisplayName("toResumenPago PENDIENTE → ambos campos pendientes son null")
    void toResumenPago_reservaPendiente_ambosPendientesNull() {
        // Given — reserva en estado PENDIENTE (no aplica ningún campo pendiente)
        Cliente clienteMock = mock(Cliente.class, withSettings().strictness(Strictness.LENIENT));
        when(clienteMock.getClienteNombre()).thenReturn("María López");
        Reserva reserva = Reserva.builder()
                .reservaId(10L)
                .cliente(clienteMock)
                .reservaFechaHoraLlegada(LocalDateTime.of(2026, 6, 15, 19, 0))
                .reservaNumeroPersonas(3)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.ESPECIAL)
                .build();

        // When
        ResumenPagoResponse resp = mapper.toResumenPago(reserva, new BigDecimal("100000.00"),
                BigDecimal.ZERO, BigDecimal.ZERO);

        // Then — estado no aplicable: ambos campos pendientes omitidos
        assertThat(resp.getPendientePorAbonar()).isNull();
        assertThat(resp.getPendientePorDevolver()).isNull();
    }

    // ── toRegistrarResponse ───────────────────────────────────────────────────

    @Test
    @DisplayName("toRegistrarResponse → incluye abonoId, tipo, estado y resumen")
    void toRegistrarResponse_incluyeAbonoIdTipoEstadoYResumen() {
        // Given
        Abono abono = Abono.builder()
                .abonoId(99L)
                .abonoTipo(TipoAbono.ANTICIPO)
                .build();
        ResumenPagoResponse resumen = ResumenPagoResponse.builder()
                .reservaId(10L)
                .estado("CONFIRMADA")
                .netoAbonado(new BigDecimal("50000.00"))
                .build();

        // When
        RegistrarAbonoResponse resp = mapper.toRegistrarResponse(abono, resumen);

        // Then
        assertThat(resp.getAbonoId()).isEqualTo(99L);
        assertThat(resp.getTipo()).isEqualTo("ANTICIPO");
        assertThat(resp.getEstado()).isEqualTo("CONFIRMADA");
        assertThat(resp.getResumen()).isSameAs(resumen);
    }
}
