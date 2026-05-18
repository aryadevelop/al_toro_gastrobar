package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MesaValidador")
class MesaValidadorTest {

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private ZonaRepository zonaRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private MesaValidador mesaValidador;

    @Nested
    @DisplayName("validarIdentificadorNoOcupado")
    class ValidarIdentificadorNoOcupado {

        @Test
        @DisplayName("Debe permitir identificador único")
        void debePermitirIdentificadorUnico() {
            // Arrange
            String identificador = "M01";
            when(mesaRepository.existeMesaActivaConIdentificadorEnDia(
                eq(identificador),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
            )).thenReturn(false);

            // Act & Assert
            assertThatCode(() -> mesaValidador.validarIdentificadorNoOcupado(identificador))
                .doesNotThrowAnyException();

            verify(mesaRepository).existeMesaActivaConIdentificadorEnDia(
                eq(identificador),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
            );
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando identificador está duplicado")
        void debeLanzarExcepcionCuandoIdentificadorDuplicado() {
            // Arrange
            String identificador = "M01";
            when(mesaRepository.existeMesaActivaConIdentificadorEnDia(
                eq(identificador),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
            )).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> mesaValidador.validarIdentificadorNoOcupado(identificador))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "ENT-002")
                .hasMessageContaining("El identificador ingresado ya está ocupado");

            verify(mesaRepository).existeMesaActivaConIdentificadorEnDia(
                eq(identificador),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
            );
        }
    }

    @Nested
    @DisplayName("validarZonaExiste")
    class ValidarZonaExiste {

        @Test
        @DisplayName("Debe permitir zona existente")
        void debePermitirZonaExistente() {
            // Arrange
            Long zonaId = 1L;
            when(zonaRepository.existsById(zonaId)).thenReturn(true);

            // Act & Assert
            assertThatCode(() -> mesaValidador.validarZonaExiste(zonaId))
                .doesNotThrowAnyException();

            verify(zonaRepository).existsById(zonaId);
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando zona no existe")
        void debeLanzarExcepcionCuandoZonaNoExiste() {
            // Arrange
            Long zonaId = 999L;
            when(zonaRepository.existsById(zonaId)).thenReturn(false);

            // Act & Assert
            assertThatThrownBy(() -> mesaValidador.validarZonaExiste(zonaId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "ENT-001")
                .hasMessageContaining("La zona especificada no existe");

            verify(zonaRepository).existsById(zonaId);
        }
    }

    @Nested
    @DisplayName("validarHorarioAtencion")
    class ValidarHorarioAtencion {

        @Test
        @DisplayName("Debe permitir hora exactamente en apertura (17:00)")
        void debePermitirHoraApertura() {
            LocalTime fixed = LocalTime.of(17, 0);
            try (MockedStatic<LocalTime> mocked = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
                mocked.when(LocalTime::now).thenReturn(fixed);
                assertThatCode(() -> mesaValidador.validarHorarioAtencion())
                        .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("Debe permitir hora dentro del rango (19:30)")
        void debePermitirHoraDentroDeRango() {
            LocalTime fixed = LocalTime.of(19, 30);
            try (MockedStatic<LocalTime> mocked = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
                mocked.when(LocalTime::now).thenReturn(fixed);
                assertThatCode(() -> mesaValidador.validarHorarioAtencion())
                        .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción antes de la apertura (16:59)")
        void debeLanzarExcepcionAntesDeApertura() {
            LocalTime fixed = LocalTime.of(16, 59);
            try (MockedStatic<LocalTime> mocked = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
                mocked.when(LocalTime::now).thenReturn(fixed);
                assertThatThrownBy(() -> mesaValidador.validarHorarioAtencion())
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("code", "NEG-001")
                        .hasMessageContaining("horario de atención");
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción exactamente en la hora de cierre (22:00 exclusivo)")
        void debeLanzarExcepcionEnCierre() {
            LocalTime fixed = LocalTime.of(22, 0);
            try (MockedStatic<LocalTime> mocked = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
                mocked.when(LocalTime::now).thenReturn(fixed);
                assertThatThrownBy(() -> mesaValidador.validarHorarioAtencion())
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("code", "NEG-001");
            }
        }

        @Test
        @DisplayName("Debe lanzar excepción después del cierre (23:00)")
        void debeLanzarExcepcionDespuesDelCierre() {
            LocalTime fixed = LocalTime.of(23, 0);
            try (MockedStatic<LocalTime> mocked = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
                mocked.when(LocalTime::now).thenReturn(fixed);
                assertThatThrownBy(() -> mesaValidador.validarHorarioAtencion())
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("code", "NEG-001");
            }
        }
    }

    @Nested
    @DisplayName("validarOwnership")
    class ValidarOwnership {

        private Mesa crearMesa(String emailMesero, LocalDateTime fechaFin) {
            Usuario usuario = Usuario.builder()
                    .usuarioId(1L)
                    .usuarioEmail(emailMesero)
                    .usuarioPassword("pwd")
                    .build();
            Empleado empleado = Empleado.builder()
                    .usuarioId(1L)
                    .usuario(usuario)
                    .empleadoNombre("Mesero")
                    .build();
            Visita visita = Visita.builder()
                    .visitaId(10L)
                    .visitaFechaHoraInicio(LocalDateTime.now())
                    .visitaFechaHoraFin(fechaFin)
                    .build();
            Mesa mesa = Mesa.builder()
                    .visitaId(10L)
                    .mesaIdentificador("M01")
                    .visita(visita)
                    .mesero(empleado)
                    .build();
            return mesa;
        }

        private Authentication auth(String email, String... roles) {
            List<SimpleGrantedAuthority> auths = java.util.Arrays.stream(roles)
                    .map(SimpleGrantedAuthority::new).toList();
            return new UsernamePasswordAuthenticationToken(email, "N/A", auths);
        }

        @Test
        @DisplayName("Debe retornar mesa cuando el mesero asignado coincide con principal")
        void debeRetornarMesaCuandoMeseroCoincide() {
            Mesa mesa = crearMesa("mesero1@altoro.com", null);
            when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));

            Mesa resultado = mesaValidador.validarOwnership(10L, auth("mesero1@altoro.com", "ROLE_MESERO"));

            assertThat(resultado).isSameAs(mesa);
            verify(mesaRepository).findById(10L);
        }

        @Test
        @DisplayName("Debe retornar mesa cuando el usuario es ADMIN aunque no sea el mesero asignado")
        void debeRetornarMesaCuandoEsAdmin() {
            Mesa mesa = crearMesa("mesero1@altoro.com", null);
            when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));

            Mesa resultado = mesaValidador.validarOwnership(10L, auth("admin@altoro.com", "ROLE_ADMIN"));

            assertThat(resultado).isSameAs(mesa);
        }

        @Test
        @DisplayName("Debe lanzar ENT-001 cuando la mesa no existe")
        void debeLanzarCuandoMesaNoExiste() {
            when(mesaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mesaValidador.validarOwnership(99L, auth("x@x.com", "ROLE_MESERO")))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "ENT-001")
                    .hasMessageContaining("Mesa no encontrada");
        }

        @Test
        @DisplayName("Debe lanzar ENT-001 cuando la visita ya está cerrada")
        void debeLanzarCuandoVisitaCerrada() {
            Mesa mesa = crearMesa("mesero1@altoro.com", LocalDateTime.now().minusHours(1));
            when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));

            assertThatThrownBy(() -> mesaValidador.validarOwnership(10L, auth("mesero1@altoro.com", "ROLE_MESERO")))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "ENT-001")
                    .hasMessageContaining("visita ya está cerrada");
        }

        @Test
        @DisplayName("Debe lanzar AUTH-002 cuando el mesero no es el asignado y no es ADMIN")
        void debeLanzarCuandoNoEsMeseroNiAdmin() {
            Mesa mesa = crearMesa("mesero1@altoro.com", null);
            when(mesaRepository.findById(10L)).thenReturn(Optional.of(mesa));

            assertThatThrownBy(() ->
                    mesaValidador.validarOwnership(10L, auth("otro@altoro.com", "ROLE_MESERO")))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "AUTH-002")
                    .hasMessageContaining("Solo el mesero asignado");
        }
    }

    @Nested
    @DisplayName("validarReservaParaAsignacion")
    class ValidarReservaParaAsignacion {

        @Test
        @DisplayName("Debe retornar reserva cuando está CONFIRMADA y es de hoy")
        void debeRetornarReservaValida() {
            // Arrange
            Long reservaId = 1L;
            LocalDate hoy = LocalDate.now();
            LocalDateTime fechaHoraLlegada = hoy.atTime(19, 0);

            Usuario usuario = Usuario.builder()
                .usuarioId(1L)
                .usuarioEmail("juan@example.com")
                .usuarioPassword("hashedPassword")
                .build();

            Cliente cliente = Cliente.builder()
                .usuarioId(1L)
                .usuario(usuario)
                .clienteNombre("Juan Pérez")
                .clienteTelefono("3001234567")
                .clienteAceptaTerminos(true)
                .clienteFechaAceptacion(LocalDateTime.now())
                .build();

            Reserva reserva = Reserva.builder()
                .reservaId(reservaId)
                .cliente(cliente)
                .reservaFechaHoraLlegada(fechaHoraLlegada)
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.CONFIRMADA)
                .build();

            when(reservaRepository.findByIdAndEstadoForAsignacion(
                eq(reservaId),
                eq(EstadoReserva.CONFIRMADA)
            )).thenReturn(Optional.of(reserva));

            // Act
            Reserva resultado = mesaValidador.validarReservaParaAsignacion(reservaId);

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getReservaId()).isEqualTo(reservaId);
            assertThat(resultado.getReservaEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
            assertThat(resultado.getReservaFechaHoraLlegada().toLocalDate()).isEqualTo(hoy);

            verify(reservaRepository).findByIdAndEstadoForAsignacion(
                eq(reservaId),
                eq(EstadoReserva.CONFIRMADA)
            );
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando reserva no existe o no está CONFIRMADA")
        void debeLanzarExcepcionCuandoReservaNoExisteONoConfirmada() {
            // Arrange
            Long reservaId = 999L;
            when(reservaRepository.findByIdAndEstadoForAsignacion(
                eq(reservaId),
                eq(EstadoReserva.CONFIRMADA)
            )).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> mesaValidador.validarReservaParaAsignacion(reservaId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "ENT-001")
                .hasMessageContaining("La reserva no existe o no está en estado CONFIRMADA");

            verify(reservaRepository).findByIdAndEstadoForAsignacion(
                eq(reservaId),
                eq(EstadoReserva.CONFIRMADA)
            );
        }

        @Test
        @DisplayName("Debe lanzar excepción cuando reserva es de mañana")
        void debeLanzarExcepcionCuandoReservaEsDeMañana() {
            // Arrange
            Long reservaId = 1L;
            LocalDate mañana = LocalDate.now().plusDays(1);
            LocalDateTime fechaHoraLlegada = mañana.atTime(19, 0);

            Usuario usuario = Usuario.builder()
                .usuarioId(1L)
                .usuarioEmail("juan@example.com")
                .usuarioPassword("hashedPassword")
                .build();

            Cliente cliente = Cliente.builder()
                .usuarioId(1L)
                .usuario(usuario)
                .clienteNombre("Juan Pérez")
                .clienteTelefono("3001234567")
                .clienteAceptaTerminos(true)
                .clienteFechaAceptacion(LocalDateTime.now())
                .build();

            Reserva reservaMañana = Reserva.builder()
                .reservaId(reservaId)
                .cliente(cliente)
                .reservaFechaHoraLlegada(fechaHoraLlegada)
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.CONFIRMADA)
                .build();

            // Repository finds the reserva but validator checks date
            when(reservaRepository.findByIdAndEstadoForAsignacion(
                eq(reservaId),
                eq(EstadoReserva.CONFIRMADA)
            )).thenReturn(Optional.of(reservaMañana));

            // Act & Assert
            assertThatThrownBy(() -> mesaValidador.validarReservaParaAsignacion(reservaId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "NEG-001")
                .hasMessageContaining("La reserva no es para el día de hoy");

            verify(reservaRepository).findByIdAndEstadoForAsignacion(
                eq(reservaId),
                eq(EstadoReserva.CONFIRMADA)
            );
        }
    }
}
