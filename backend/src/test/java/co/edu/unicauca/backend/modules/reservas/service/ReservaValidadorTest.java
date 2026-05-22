package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.DecoracionZona;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.repository.BloqueDisponibilidadRepository;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionZonaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link ReservaValidador}.
 *
 * <p>Cubre los cinco métodos públicos del componente:
 * <ul>
 *   <li>{@link ReservaValidador#validarElegibilidadModificacion}: ownership + estado.</li>
 *   <li>{@link ReservaValidador#esHorarioValido}: rango de atención.</li>
 *   <li>{@link ReservaValidador#estaBloqueda}: bloqueos administrativos.</li>
 *   <li>{@link ReservaValidador#validarCompatibilidadDecoracionZona}: compatibilidad decoración-zona.</li>
 *   <li>{@link ReservaValidador#tieneDecoracionConCosto}: clasificación ESPECIAL por decoración.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ReservaValidadorTest {

    @Mock
    BloqueDisponibilidadRepository bloqueRepository;

    @Mock
    DecoracionZonaRepository decoracionZonaRepository;

    @InjectMocks
    ReservaValidador validador;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Reserva reservaCon(String emailPropietario, EstadoReserva estado) {
        Usuario usuario = Usuario.builder().usuarioEmail(emailPropietario).build();
        Cliente cliente = Cliente.builder().usuario(usuario).build();
        return Reserva.builder().cliente(cliente).reservaEstado(estado).build();
    }

    // ── validarElegibilidadModificacion ───────────────────────────────────────

    @Nested
    @DisplayName("validarElegibilidadModificacion — ownership + estado")
    class ValidarElegibilidadModificacion {

        @Test
        @DisplayName("Propietario con estado PENDIENTE → no lanza excepción")
        void propietarioPendiente_noLanza() {
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.PENDIENTE);

            assertThatCode(() -> validador.validarElegibilidadModificacion(reserva, "cliente@altoro.com"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Propietario con estado CONFIRMADA → no lanza excepción")
        void propietarioConfirmada_noLanza() {
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CONFIRMADA);

            assertThatCode(() -> validador.validarElegibilidadModificacion(reserva, "cliente@altoro.com"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Email no coincide con propietario → BusinessException 403 FORBIDDEN")
        void emailDistinto_lanzaForbidden() {
            Reserva reserva = reservaCon("otro@altoro.com", EstadoReserva.PENDIENTE);

            assertThatThrownBy(() -> validador.validarElegibilidadModificacion(reserva, "cliente@altoro.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("Estado CANCELADA → BusinessException 422 UNPROCESSABLE_ENTITY")
        void estadoCancelada_lanzaUnprocessable() {
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CANCELADA);

            assertThatThrownBy(() -> validador.validarElegibilidadModificacion(reserva, "cliente@altoro.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("emailCliente=null (ADMIN) → omite ownership y no lanza excepción")
        void adminSinEmail_noLanza() {
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CONFIRMADA);

            assertThatCode(() -> validador.validarElegibilidadModificacion(reserva, null))
                    .doesNotThrowAnyException();
        }
    }

    // ── validarElegibilidadConfirmacion ───────────────────────────────────────

    @Nested
    @DisplayName("validarElegibilidadConfirmacion — solo ESPECIAL + PENDIENTE")
    class ValidarElegibilidadConfirmacion {

        private Reserva reservaCon(EstadoReserva estado, TipoReserva tipo) {
            return Reserva.builder().reservaEstado(estado).reservaTipo(tipo).build();
        }

        @Test
        @DisplayName("ESPECIAL en estado PENDIENTE → no lanza excepción")
        void especialPendiente_noLanza() {
            Reserva reserva = reservaCon(EstadoReserva.PENDIENTE, TipoReserva.ESPECIAL);

            assertThatCode(() -> validador.validarElegibilidadConfirmacion(reserva))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ESPECIAL ya CONFIRMADA → BusinessException 422 UNPROCESSABLE_ENTITY")
        void especialConfirmada_lanzaUnprocessable() {
            Reserva reserva = reservaCon(EstadoReserva.CONFIRMADA, TipoReserva.ESPECIAL);

            assertThatThrownBy(() -> validador.validarElegibilidadConfirmacion(reserva))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("ESPECIAL CANCELADA → BusinessException 422 UNPROCESSABLE_ENTITY")
        void especialCancelada_lanzaUnprocessable() {
            Reserva reserva = reservaCon(EstadoReserva.CANCELADA, TipoReserva.ESPECIAL);

            assertThatThrownBy(() -> validador.validarElegibilidadConfirmacion(reserva))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("BASICA en estado PENDIENTE → BusinessException 422 UNPROCESSABLE_ENTITY")
        void basicaPendiente_lanzaUnprocessable() {
            Reserva reserva = reservaCon(EstadoReserva.PENDIENTE, TipoReserva.BASICA);

            assertThatThrownBy(() -> validador.validarElegibilidadConfirmacion(reserva))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("ESPECIAL en estado terminal ATENDIDA → BusinessException 422 UNPROCESSABLE_ENTITY")
        void especialAtendida_lanzaUnprocessable() {
            Reserva reserva = reservaCon(EstadoReserva.ATENDIDA, TipoReserva.ESPECIAL);

            assertThatThrownBy(() -> validador.validarElegibilidadConfirmacion(reserva))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }
    }

    // ── esHorarioValido ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("esHorarioValido — rango [apertura, cierre)")
    class EsHorarioValido {

        private final LocalDate fecha = LocalDate.of(2026, 6, 1);
        private final LocalTime apertura = LocalTime.of(12, 0);
        private final LocalTime cierre   = LocalTime.of(22, 0);

        @Test
        @DisplayName("Hora dentro del rango → true")
        void horaDentroDeRango_retornaTrue() {
            assertThat(validador.esHorarioValido(
                    fecha.atTime(19, 0), apertura, cierre)).isTrue();
        }

        @Test
        @DisplayName("Hora exactamente en la apertura → true (inclusive)")
        void horaEnApertura_retornaTrue() {
            assertThat(validador.esHorarioValido(
                    fecha.atTime(12, 0), apertura, cierre)).isTrue();
        }

        @Test
        @DisplayName("Hora antes de la apertura → false")
        void horaAntesDeApertura_retornaFalse() {
            assertThat(validador.esHorarioValido(
                    fecha.atTime(11, 59), apertura, cierre)).isFalse();
        }

        @Test
        @DisplayName("Hora igual a cierre → false (exclusivo)")
        void horaEnCierre_retornaFalse() {
            assertThat(validador.esHorarioValido(
                    fecha.atTime(22, 0), apertura, cierre)).isFalse();
        }
    }

    // ── estaBloqueda ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("estaBloqueda — bloqueos administrativos (typo intencional)")
    class EstaBloqueda {

        private final LocalDateTime fechaHora = LocalDateTime.of(2026, 6, 1, 19, 0);

        @Test
        @DisplayName("Existe al menos un bloqueo → true")
        void conBloqueo_retornaTrue() {
            when(bloqueRepository.countBloquesParaFechaHora(
                    fechaHora.toLocalDate(), fechaHora.toLocalTime())).thenReturn(2L);

            assertThat(validador.estaBloqueda(fechaHora)).isTrue();
        }

        @Test
        @DisplayName("Sin bloqueos → false")
        void sinBloqueo_retornaFalse() {
            when(bloqueRepository.countBloquesParaFechaHora(
                    fechaHora.toLocalDate(), fechaHora.toLocalTime())).thenReturn(0L);

            assertThat(validador.estaBloqueda(fechaHora)).isFalse();
        }
    }

    // ── validarCompatibilidadDecoracionZona ───────────────────────────────────

    @Nested
    @DisplayName("validarCompatibilidadDecoracionZona — decoración vs zona")
    class ValidarCompatibilidadDecoracionZona {

        @Test
        @DisplayName("Sin enlaces → compatible con cualquier zona (no lanza)")
        void sinEnlaces_noLanza() {
            Decoracion dec = mock(Decoracion.class);
            when(dec.getDecoracionId()).thenReturn(1L);
            when(decoracionZonaRepository.findByDecoracionId(1L)).thenReturn(List.of());

            assertThatCode(() -> validador.validarCompatibilidadDecoracionZona(dec, 5L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("1 enlace, zona coincide → no lanza")
        void unEnlaceZonaCorrecta_noLanza() {
            Decoracion dec = mock(Decoracion.class);
            DecoracionZona link = mock(DecoracionZona.class);
            when(dec.getDecoracionId()).thenReturn(2L);
            when(link.getZonaId()).thenReturn(5L);
            when(decoracionZonaRepository.findByDecoracionId(2L)).thenReturn(List.of(link));

            assertThatCode(() -> validador.validarCompatibilidadDecoracionZona(dec, 5L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("1 enlace, zona no coincide → BusinessException")
        void unEnlaceZonaIncorrecta_lanzaBusinessException() {
            Decoracion dec = mock(Decoracion.class);
            DecoracionZona link = mock(DecoracionZona.class);
            when(dec.getDecoracionId()).thenReturn(3L);
            when(link.getZonaId()).thenReturn(7L);
            when(decoracionZonaRepository.findByDecoracionId(3L)).thenReturn(List.of(link));

            assertThatThrownBy(() -> validador.validarCompatibilidadDecoracionZona(dec, 5L))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Múltiples enlaces, zona incluida → no lanza")
        void variosEnlacesZonaIncluida_noLanza() {
            Decoracion dec = mock(Decoracion.class);
            DecoracionZona link1 = mock(DecoracionZona.class);
            DecoracionZona link2 = mock(DecoracionZona.class);
            when(dec.getDecoracionId()).thenReturn(4L);
            when(link1.getZonaId()).thenReturn(3L);
            when(link2.getZonaId()).thenReturn(5L);
            when(decoracionZonaRepository.findByDecoracionId(4L)).thenReturn(List.of(link1, link2));

            assertThatCode(() -> validador.validarCompatibilidadDecoracionZona(dec, 5L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Múltiples enlaces, zona no incluida → BusinessException")
        void variosEnlacesZonaNoIncluida_lanzaBusinessException() {
            Decoracion dec = mock(Decoracion.class);
            DecoracionZona link1 = mock(DecoracionZona.class);
            DecoracionZona link2 = mock(DecoracionZona.class);
            when(dec.getDecoracionId()).thenReturn(4L);
            when(link1.getZonaId()).thenReturn(3L);
            when(link2.getZonaId()).thenReturn(6L);
            when(decoracionZonaRepository.findByDecoracionId(4L)).thenReturn(List.of(link1, link2));

            assertThatThrownBy(() -> validador.validarCompatibilidadDecoracionZona(dec, 5L))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ── tieneDecoracionConCosto ───────────────────────────────────────────────

    @Nested
    @DisplayName("tieneDecoracionConCosto — clasificación ESPECIAL")
    class TieneDecoracionConCosto {

        @Test
        @DisplayName("Decoración null → false")
        void decoracionNull_retornaFalse() {
            assertThat(validador.tieneDecoracionConCosto(null)).isFalse();
        }

        @Test
        @DisplayName("Costo adicional null → false")
        void costoNull_retornaFalse() {
            Decoracion dec = mock(Decoracion.class);
            when(dec.getDecoracionCostoAdicional()).thenReturn(null);

            assertThat(validador.tieneDecoracionConCosto(dec)).isFalse();
        }

        @Test
        @DisplayName("Costo adicional = 0 → false")
        void costoCero_retornaFalse() {
            Decoracion dec = mock(Decoracion.class);
            when(dec.getDecoracionCostoAdicional()).thenReturn(BigDecimal.ZERO);

            assertThat(validador.tieneDecoracionConCosto(dec)).isFalse();
        }

        @Test
        @DisplayName("Costo adicional > 0 → true (reserva ESPECIAL)")
        void costoPositivo_retornaTrue() {
            Decoracion dec = mock(Decoracion.class);
            when(dec.getDecoracionCostoAdicional()).thenReturn(new BigDecimal("25.00"));

            assertThat(validador.tieneDecoracionConCosto(dec)).isTrue();
        }
    }

    // ── validarElegibilidadCancelacion ────────────────────────────────────────

    @Nested
    @DisplayName("validarElegibilidadCancelacion — ownership + estado")
    class ValidarElegibilidadCancelacion {

        @Test
        @DisplayName("Propietario con estado PENDIENTE → no lanza excepción")
        void propietarioPendiente_noLanza() {
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.PENDIENTE);

            assertThatCode(() -> validador.validarElegibilidadCancelacion(reserva, "cliente@altoro.com"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Propietario con estado CONFIRMADA → no lanza excepción")
        void propietarioConfirmada_noLanza() {
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CONFIRMADA);

            assertThatCode(() -> validador.validarElegibilidadCancelacion(reserva, "cliente@altoro.com"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Email no coincide con propietario → BusinessException 403 FORBIDDEN")
        void emailDistinto_lanzaForbidden() {
            Reserva reserva = reservaCon("otro@altoro.com", EstadoReserva.PENDIENTE);

            assertThatThrownBy(() -> validador.validarElegibilidadCancelacion(reserva, "cliente@altoro.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("Estado CANCELADA → BusinessException 422 UNPROCESSABLE_ENTITY")
        void estadoCancelada_lanzaUnprocessable() {
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CANCELADA);

            assertThatThrownBy(() -> validador.validarElegibilidadCancelacion(reserva, "cliente@altoro.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("Estado DEVUELTA → BusinessException 422 UNPROCESSABLE_ENTITY")
        void estadoDevuelta_lanzaUnprocessable() {
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.DEVUELTA);

            assertThatThrownBy(() -> validador.validarElegibilidadCancelacion(reserva, "cliente@altoro.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("Email coincide en mayúsculas → no lanza (comparación case-insensitive)")
        void emailMayusculas_noLanza() {
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CONFIRMADA);

            assertThatCode(() -> validador.validarElegibilidadCancelacion(reserva, "CLIENTE@ALTORO.COM"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("emailCliente=null (ADMIN) → omite ownership y no lanza excepción")
        void adminSinEmail_noLanza() {
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CONFIRMADA);

            assertThatCode(() -> validador.validarElegibilidadCancelacion(reserva, null))
                    .doesNotThrowAnyException();
        }
    }

    // -----------------------------------------------------------------------
    // validarElegibilidadInasistencia
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("validarElegibilidadInasistencia")
    class ValidarElegibilidadInasistencia {

        @Test
        @DisplayName("CONFIRMADA con +30 minutos → no lanza excepción")
        void confirmadaMas30Min_noLanza() {
            // Given: Reserva CONFIRMADA hace 40 minutos
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(40);
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CONFIRMADA);
            reserva.setReservaFechaHoraLlegada(horaLlegada);

            // When/Then
            assertThatCode(() -> validador.validarElegibilidadInasistencia(reserva, false))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("CONFIRMADA con exactamente 30 minutos → no lanza excepción")
        void confirmadaExactamente30Min_noLanza() {
            // Given: Reserva CONFIRMADA hace exactamente 30 minutos
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(30);
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CONFIRMADA);
            reserva.setReservaFechaHoraLlegada(horaLlegada);

            // When/Then
            assertThatCode(() -> validador.validarElegibilidadInasistencia(reserva, false))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("CONFIRMADA con menos de 30 minutos → BusinessException INVALID_STATE")
        void confirmadaMenosDe30Min_lanzaInvalidState() {
            // Given: Reserva CONFIRMADA hace solo 15 minutos
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(15);
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CONFIRMADA);
            reserva.setReservaFechaHoraLlegada(horaLlegada);

            // When/Then
            assertThatThrownBy(() -> validador.validarElegibilidadInasistencia(reserva, false))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                        assertThat(bex.getCode()).isEqualTo("NEG-002");
                        assertThat(bex.getMessage()).contains("Debe esperar");
                        assertThat(bex.getMessage()).contains("minuto(s) más");
                    });
        }

        @Test
        @DisplayName("PENDIENTE con +30 minutos → BusinessException INVALID_STATE")
        void pendienteMas30Min_lanzaInvalidState() {
            // Given: Reserva PENDIENTE hace 40 minutos
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(40);
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.PENDIENTE);
            reserva.setReservaFechaHoraLlegada(horaLlegada);

            // When/Then
            assertThatThrownBy(() -> validador.validarElegibilidadInasistencia(reserva, false))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                        assertThat(bex.getCode()).isEqualTo("NEG-002");
                        assertThat(bex.getMessage()).contains("confirmadas");
                    });
        }

        @Test
        @DisplayName("ATENDIDA → BusinessException INVALID_STATE")
        void atendida_lanzaInvalidState() {
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(40);
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.ATENDIDA);
            reserva.setReservaFechaHoraLlegada(horaLlegada);

            assertThatThrownBy(() -> validador.validarElegibilidadInasistencia(reserva, false))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("CANCELADA → BusinessException INVALID_STATE")
        void cancelada_lanzaInvalidState() {
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(40);
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CANCELADA);
            reserva.setReservaFechaHoraLlegada(horaLlegada);

            assertThatThrownBy(() -> validador.validarElegibilidadInasistencia(reserva, false))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("INASISTENCIA → BusinessException INVALID_STATE")
        void inasistencia_lanzaInvalidState() {
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(40);
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.INASISTENCIA);
            reserva.setReservaFechaHoraLlegada(horaLlegada);

            assertThatThrownBy(() -> validador.validarElegibilidadInasistencia(reserva, false))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("Mensaje incluye minutos restantes cuando falta tiempo")
        void mensajeIncluyeMinutosRestantes() {
            // Given: Reserva CONFIRMADA hace 10 minutos (faltan 20)
            LocalDateTime horaLlegada = LocalDateTime.now().minusMinutes(10);
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CONFIRMADA);
            reserva.setReservaFechaHoraLlegada(horaLlegada);

            // When/Then: Mensaje debe incluir minutos restantes (~19-20)
            assertThatThrownBy(() -> validador.validarElegibilidadInasistencia(reserva, false))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        String mensaje = ex.getMessage();
                        assertThat(mensaje).matches("Debe esperar \\d+ minuto\\(s\\) más antes de marcar inasistencia");
                    });
        }


        @Test
        @DisplayName("MESERO con reserva del día actual → no lanza excepción")
        void meseroReservaHoy_noLanza() {
            LocalDateTime cuarentaAtras = LocalDateTime.now().minusMinutes(40);
            // Si retroceder 40 min cruza medianoche, el escenario es físicamente imposible hoy
            Assumptions.assumeTrue(
                cuarentaAtras.toLocalDate().isEqual(LocalDate.now()),
                "Omitido: primeros 40 minutos del día — 'hace 40 min' cae en el día anterior"
            );

            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CONFIRMADA);
            reserva.setReservaFechaHoraLlegada(cuarentaAtras);

            assertThatCode(() -> validador.validarElegibilidadInasistencia(reserva, true))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("MESERO con reserva de día pasado → BusinessException INVALID_STATE")
        void meseroReservaPasada_lanzaInvalidState() {
            // Given: Reserva CONFIRMADA de ayer (hace 1 día + 40 minutos)
            LocalDateTime horaLlegada = LocalDateTime.now().minusDays(1).minusMinutes(40);
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CONFIRMADA);
            reserva.setReservaFechaHoraLlegada(horaLlegada);

            // When/Then: MESERO no puede marcar inasistencia de reserva pasada
            assertThatThrownBy(() -> validador.validarElegibilidadInasistencia(reserva, true))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                        assertThat(bex.getCode()).isEqualTo("NEG-002");
                        assertThat(bex.getMessage()).contains("día actual");
                    });
        }

        @Test
        @DisplayName("MESERO con reserva de día futuro → BusinessException INVALID_STATE")
        void meseroReservaFutura_lanzaInvalidState() {
            // Given: Reserva CONFIRMADA de mañana (dentro de 1 día)
            LocalDateTime horaLlegada = LocalDateTime.now().plusDays(1);
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CONFIRMADA);
            reserva.setReservaFechaHoraLlegada(horaLlegada);

            // When/Then: MESERO no puede marcar inasistencia de reserva futura
            assertThatThrownBy(() -> validador.validarElegibilidadInasistencia(reserva, true))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException bex = (BusinessException) ex;
                        assertThat(bex.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                        assertThat(bex.getCode()).isEqualTo("NEG-002");
                        assertThat(bex.getMessage()).contains("día actual");
                    });
        }

        @Test
        @DisplayName("ADMIN con reserva de día pasado → no lanza excepción")
        void adminReservaPasada_noLanza() {
            // Given: Reserva CONFIRMADA de ayer (hace 1 día + 40 minutos)
            LocalDateTime horaLlegada = LocalDateTime.now().minusDays(1).minusMinutes(40);
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CONFIRMADA);
            reserva.setReservaFechaHoraLlegada(horaLlegada);

            // When/Then: ADMIN puede marcar inasistencia de cualquier fecha
            assertThatCode(() -> validador.validarElegibilidadInasistencia(reserva, false))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("ADMIN con reserva de día futuro → no lanza excepción")
        void adminReservaFutura_noLanza() {
            // Given: Reserva CONFIRMADA de hace 2 días (simulando que pasó hace 2 días + 40 min)
            LocalDateTime horaLlegada = LocalDateTime.now().minusDays(2).minusMinutes(40);
            Reserva reserva = reservaCon("cliente@altoro.com", EstadoReserva.CONFIRMADA);
            reserva.setReservaFechaHoraLlegada(horaLlegada);

            // When/Then: ADMIN puede marcar inasistencia de cualquier fecha
            assertThatCode(() -> validador.validarElegibilidadInasistencia(reserva, false))
                    .doesNotThrowAnyException();
        }
    }
}
