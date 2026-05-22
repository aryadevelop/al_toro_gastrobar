package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaConsultaResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para {@link ReservaConsultaMapper}.
 */
class ReservaConsultaMapperTest {

    private ReservaConsultaMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ReservaConsultaMapper();
    }

    @Test
    void toConsultaResponse_conZonaYDecoracion_retornaDTOCompleto() {
        // Given
        Usuario usuario = Usuario.builder()
                .usuarioId(1L)
                .usuarioEmail("cliente@altoro.com")
                .build();

        Cliente cliente = Cliente.builder()
                .usuarioId(1L)
                .usuario(usuario)
                .clienteNombre("Juan Pérez")
                .build();

        Zona zona = Zona.builder()
                .zonaId(10L)
                .zonaNombre("Zona VIP")
                .build();

        Decoracion decoracion = Decoracion.builder()
                .decoracionId(5L)
                .decoracionNombre("Romántica")
                .build();

        Reserva reserva = Reserva.builder()
                .reservaId(100L)
                .cliente(cliente)
                .zona(zona)
                .decoracion(decoracion)
                .reservaFechaHoraLlegada(LocalDateTime.of(2026, 4, 25, 19, 30))
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.CONFIRMADA)
                .reservaTipo(TipoReserva.ESPECIAL)
                .build();

        // When
        ReservaConsultaResponse response = mapper.toConsultaResponse(reserva);

        // Then
        assertThat(response.getReservaId()).isEqualTo(100L);
        assertThat(response.getClienteNombre()).isEqualTo("Juan Pérez");
        assertThat(response.getZonaId()).isEqualTo(10L);
        assertThat(response.getZonaNombre()).isEqualTo("Zona VIP");
        assertThat(response.getDecoracionNombre()).isEqualTo("Romántica");
        assertThat(response.getHoraLlegada()).isEqualTo("19:30");
        assertThat(response.getNumeroPersonas()).isEqualTo(4);
        assertThat(response.getEstado()).isEqualTo("CONFIRMADA");
    }

    @Test
    void toConsultaResponse_sinZonaNiDecoracion_retornaNull() {
        // Given
        Usuario usuario = Usuario.builder()
                .usuarioId(2L)
                .usuarioEmail("cliente2@altoro.com")
                .build();

        Cliente cliente = Cliente.builder()
                .usuarioId(2L)
                .usuario(usuario)
                .clienteNombre("María López")
                .build();

        Reserva reserva = Reserva.builder()
                .reservaId(101L)
                .cliente(cliente)
                .zona(null)
                .decoracion(null)
                .reservaFechaHoraLlegada(LocalDateTime.of(2026, 4, 25, 20, 0))
                .reservaNumeroPersonas(2)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();

        // When
        ReservaConsultaResponse response = mapper.toConsultaResponse(reserva);

        // Then
        assertThat(response.getReservaId()).isEqualTo(101L);
        assertThat(response.getClienteNombre()).isEqualTo("María López");
        assertThat(response.getZonaId()).isNull();
        assertThat(response.getZonaNombre()).isNull();
        assertThat(response.getDecoracionNombre()).isNull();
        assertThat(response.getHoraLlegada()).isEqualTo("20:00");
        assertThat(response.getNumeroPersonas()).isEqualTo(2);
        assertThat(response.getEstado()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("toConsultaResponse (mesero) → no setea tipo ni flags de cajero")
    void toConsultaResponse_noSeteaFlagsCajero() {
        ReservaConsultaResponse response = mapper.toConsultaResponse(
                reservaCajero(EstadoReserva.CONFIRMADA, TipoReserva.ESPECIAL));

        assertThat(response.getTipo()).isNull();
        assertThat(response.getMostrarConfirmar()).isNull();
        assertThat(response.getMostrarAgregarAbono()).isNull();
        assertThat(response.getMostrarConfirmarDevolucion()).isNull();
        assertThat(response.getMostrarCancelar()).isNull();
    }

    // -----------------------------------------------------------------------
    // Tests para la vista de cajero (toCajeroConsultaResponse)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("toCajeroConsultaResponse — tipo y botones del cajero")
    class VistaCajero {

        @Test
        @DisplayName("ESPECIAL PENDIENTE → mostrarConfirmar y mostrarCancelar; expone tipo")
        void especialPendiente_mostrarConfirmar() {
            ReservaConsultaResponse r = mapper.toCajeroConsultaResponse(
                    reservaCajero(EstadoReserva.PENDIENTE, TipoReserva.ESPECIAL), false);

            assertThat(r.getTipo()).isEqualTo("ESPECIAL");
            assertThat(r.getMostrarConfirmar()).isTrue();
            assertThat(r.getMostrarCancelar()).isTrue();
            assertThat(r.getMostrarAgregarAbono()).isFalse();
            assertThat(r.getMostrarConfirmarDevolucion()).isFalse();
        }

        @Test
        @DisplayName("CONFIRMADA → mostrarAgregarAbono y mostrarCancelar")
        void confirmada_mostrarAgregarAbono() {
            ReservaConsultaResponse r = mapper.toCajeroConsultaResponse(
                    reservaCajero(EstadoReserva.CONFIRMADA, TipoReserva.ESPECIAL), false);

            assertThat(r.getMostrarAgregarAbono()).isTrue();
            assertThat(r.getMostrarCancelar()).isTrue();
            assertThat(r.getMostrarConfirmar()).isFalse();
        }

        @Test
        @DisplayName("CANCELADA con abono → mostrarConfirmarDevolucion")
        void canceladaConAbono_mostrarDevolucion() {
            ReservaConsultaResponse r = mapper.toCajeroConsultaResponse(
                    reservaCajero(EstadoReserva.CANCELADA, TipoReserva.ESPECIAL), true);

            assertThat(r.getMostrarConfirmarDevolucion()).isTrue();
            assertThat(r.getMostrarCancelar()).isFalse();
        }

        @Test
        @DisplayName("CANCELADA sin abono → no mostrarConfirmarDevolucion")
        void canceladaSinAbono_noMostrarDevolucion() {
            ReservaConsultaResponse r = mapper.toCajeroConsultaResponse(
                    reservaCajero(EstadoReserva.CANCELADA, TipoReserva.ESPECIAL), false);

            assertThat(r.getMostrarConfirmarDevolucion()).isFalse();
        }

        @Test
        @DisplayName("Vista de cajero → no setea mostrarBotonInasistencia")
        void noSeteaMostrarBotonInasistencia() {
            ReservaConsultaResponse r = mapper.toCajeroConsultaResponse(
                    reservaCajero(EstadoReserva.CONFIRMADA, TipoReserva.ESPECIAL), false);

            assertThat(r.getMostrarBotonInasistencia()).isNull();
        }

        @Test
        @DisplayName("BASICA CONFIRMADA → mostrarConfirmar=false, tipo=BASICA")
        void basicaConfirmada_noMostrarConfirmar() {
            ReservaConsultaResponse r = mapper.toCajeroConsultaResponse(
                    reservaCajero(EstadoReserva.CONFIRMADA, TipoReserva.BASICA), false);

            assertThat(r.getMostrarConfirmar()).isFalse();
            assertThat(r.getMostrarAgregarAbono()).isTrue();
            assertThat(r.getTipo()).isEqualTo("BASICA");
        }

        @Test
        @DisplayName("Cajero con zona y decoración → mapea zonaId, zonaNombre y decoracionNombre")
        void conZonaYDecoracion_mapeaCampos() {
            Cliente cliente = Cliente.builder().usuarioId(1L).clienteNombre("Juan Pérez").build();
            Zona zona = Zona.builder().zonaId(5L).zonaNombre("VIP").build();
            Decoracion deco = Decoracion.builder()
                    .decoracionId(3L).decoracionNombre("Romántica").build();
            Reserva reserva = Reserva.builder()
                    .reservaId(200L)
                    .cliente(cliente)
                    .zona(zona)
                    .decoracion(deco)
                    .reservaFechaHoraLlegada(LocalDateTime.of(2026, 4, 25, 19, 30))
                    .reservaNumeroPersonas(4)
                    .reservaEstado(EstadoReserva.CONFIRMADA)
                    .reservaTipo(TipoReserva.ESPECIAL)
                    .build();

            ReservaConsultaResponse r = mapper.toCajeroConsultaResponse(reserva, false);

            assertThat(r.getZonaId()).isEqualTo(5L);
            assertThat(r.getZonaNombre()).isEqualTo("VIP");
            assertThat(r.getDecoracionNombre()).isEqualTo("Romántica");
        }
    }

    private Reserva reservaCajero(EstadoReserva estado, TipoReserva tipo) {
        Cliente cliente = Cliente.builder()
                .usuarioId(1L)
                .clienteNombre("Juan Pérez")
                .build();
        return Reserva.builder()
                .reservaId(100L)
                .cliente(cliente)
                .reservaFechaHoraLlegada(LocalDateTime.of(2026, 4, 25, 19, 30))
                .reservaNumeroPersonas(4)
                .reservaEstado(estado)
                .reservaTipo(tipo)
                .build();
    }

    // -----------------------------------------------------------------------
    // Tests para mostrarBotonInasistencia
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Campo mostrarBotonInasistencia")
    class MostrarBotonInasistencia {

        private Usuario usuario;
        private Cliente cliente;

        @BeforeEach
        void setUpFixtures() {
            usuario = Usuario.builder()
                    .usuarioId(1L)
                    .usuarioEmail("cliente@altoro.com")
                    .build();

            cliente = Cliente.builder()
                    .usuarioId(1L)
                    .usuario(usuario)
                    .clienteNombre("Juan Pérez")
                    .clienteTelefono("3001234567")
                    .build();
        }

        @Test
        @DisplayName("CONFIRMADA con +30 minutos → mostrarBotonInasistencia = true")
        void confirmadaMas30Min_retornaTrue() {
            // Given: Reserva CONFIRMADA hace 40 minutos
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(40);
            Reserva reserva = Reserva.builder()
                    .reservaId(100L)
                    .cliente(cliente)
                    .reservaFechaHoraLlegada(horaLlegada)
                    .reservaNumeroPersonas(4)
                    .reservaEstado(EstadoReserva.CONFIRMADA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();

            // When
            ReservaConsultaResponse response = mapper.toConsultaResponse(reserva);

            // Then
            assertThat(response.getMostrarBotonInasistencia()).isTrue();
        }

        @Test
        @DisplayName("CONFIRMADA con exactamente 30 minutos → mostrarBotonInasistencia = true")
        void confirmadaExactamente30Min_retornaTrue() {
            // Given
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(30).minusSeconds(1);
            Reserva reserva = Reserva.builder()
                    .reservaId(101L)
                    .cliente(cliente)
                    .reservaFechaHoraLlegada(horaLlegada)
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.CONFIRMADA)
                    .reservaTipo(TipoReserva.ESPECIAL)
                    .build();

            // When
            ReservaConsultaResponse response = mapper.toConsultaResponse(reserva);

            // Then
            assertThat(response.getMostrarBotonInasistencia()).isTrue();
        }

        @Test
        @DisplayName("CONFIRMADA con menos de 30 minutos → mostrarBotonInasistencia = false")
        void confirmadaMenosDe30Min_retornaFalse() {
            // Given: Reserva CONFIRMADA hace solo 15 minutos
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(15);
            Reserva reserva = Reserva.builder()
                    .reservaId(102L)
                    .cliente(cliente)
                    .reservaFechaHoraLlegada(horaLlegada)
                    .reservaNumeroPersonas(3)
                    .reservaEstado(EstadoReserva.CONFIRMADA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();

            // When
            ReservaConsultaResponse response = mapper.toConsultaResponse(reserva);

            // Then
            assertThat(response.getMostrarBotonInasistencia()).isFalse();
        }

        @Test
        @DisplayName("PENDIENTE con +30 minutos → mostrarBotonInasistencia = false")
        void pendienteMas30Min_retornaFalse() {
            // Given: PENDIENTE no muestra botón sin importar el tiempo
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(60);
            Reserva reserva = Reserva.builder()
                    .reservaId(103L)
                    .cliente(cliente)
                    .reservaFechaHoraLlegada(horaLlegada)
                    .reservaNumeroPersonas(5)
                    .reservaEstado(EstadoReserva.PENDIENTE)
                    .reservaTipo(TipoReserva.ESPECIAL)
                    .build();

            // When
            ReservaConsultaResponse response = mapper.toConsultaResponse(reserva);

            // Then
            assertThat(response.getMostrarBotonInasistencia()).isFalse();
        }

        @Test
        @DisplayName("CONFIRMADA futura → mostrarBotonInasistencia = false")
        void confirmadaFutura_retornaFalse() {
            // Given: Reserva CONFIRMADA para dentro de 2 horas
            LocalDateTime horaLlegada = LocalDateTime.now().plusHours(2);
            Reserva reserva = Reserva.builder()
                    .reservaId(104L)
                    .cliente(cliente)
                    .reservaFechaHoraLlegada(horaLlegada)
                    .reservaNumeroPersonas(2)
                    .reservaEstado(EstadoReserva.CONFIRMADA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();

            // When
            ReservaConsultaResponse response = mapper.toConsultaResponse(reserva);

            // Then
            assertThat(response.getMostrarBotonInasistencia()).isFalse();
        }

        @Test
        @DisplayName("ATENDIDA → mostrarBotonInasistencia = false")
        void atendida_retornaFalse() {
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(40);
            Reserva reserva = Reserva.builder()
                    .reservaId(105L)
                    .cliente(cliente)
                    .reservaFechaHoraLlegada(horaLlegada)
                    .reservaNumeroPersonas(4)
                    .reservaEstado(EstadoReserva.ATENDIDA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();

            ReservaConsultaResponse response = mapper.toConsultaResponse(reserva);
            assertThat(response.getMostrarBotonInasistencia()).isFalse();
        }

        @Test
        @DisplayName("INASISTENCIA → mostrarBotonInasistencia = false")
        void inasistencia_retornaFalse() {
            LocalDateTime horaLlegada = LocalDateTime.now().minusHours(2);
            Reserva reserva = Reserva.builder()
                    .reservaId(106L)
                    .cliente(cliente)
                    .reservaFechaHoraLlegada(horaLlegada)
                    .reservaNumeroPersonas(3)
                    .reservaEstado(EstadoReserva.INASISTENCIA)
                    .reservaTipo(TipoReserva.BASICA)
                    .build();

            ReservaConsultaResponse response = mapper.toConsultaResponse(reserva);
            assertThat(response.getMostrarBotonInasistencia()).isFalse();
        }
    }
}
