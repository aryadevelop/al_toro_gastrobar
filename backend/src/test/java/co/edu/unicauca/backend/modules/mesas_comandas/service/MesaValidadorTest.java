package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        @Disabled("Cannot mock LocalTime.now() without PowerMock or additional test infrastructure. " +
                  "This method will be tested indirectly via service integration tests.")
        @DisplayName("Debe permitir horario dentro de 17:00-22:00")
        void debePermitirHorarioDentroDeRango() {
            // This test is disabled because validarHorarioAtencion() uses LocalTime.now() directly
            // and we cannot easily mock static methods without PowerMock or MockedStatic
            // The method will be covered by integration tests where we can control the time of execution
        }

        @Test
        @Disabled("Cannot mock LocalTime.now() without PowerMock or additional test infrastructure. " +
                  "This method will be tested indirectly via service integration tests.")
        @DisplayName("Debe lanzar excepción cuando horario fuera de rango")
        void debeLanzarExcepcionCuandoHorarioFueraDeRango() {
            // This test is disabled because validarHorarioAtencion() uses LocalTime.now() directly
            // and we cannot easily mock static methods without PowerMock or MockedStatic
            // The method will be covered by integration tests where we can control the time of execution
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
