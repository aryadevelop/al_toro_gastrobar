package co.edu.unicauca.backend.modules.usuarios.service;

import co.edu.unicauca.backend.modules.usuarios.dto.response.ClientePuntosResponse;
import co.edu.unicauca.backend.modules.usuarios.entity.CanjePuntos;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.repository.CanjePuntosRepository;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link PuntosService}.
 *
 * <p>Cubre los tres métodos públicos:
 * <ul>
 *   <li>{@link PuntosService#obtenerPuntos}: consulta por email del cliente autenticado.</li>
 *   <li>{@link PuntosService#obtenerPuntosPorId}: consulta por ID (uso CAJERO/ADMIN).</li>
 *   <li>{@link PuntosService#canjearPuntos}: reset de saldo y auditoría en {@code canje_puntos}.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PuntosServiceTest {

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    EmpleadoRepository empleadoRepository;

    @Mock
    CanjePuntosRepository canjePuntosRepository;

    @InjectMocks
    PuntosService puntosService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Cliente clienteConPuntos(Long id, int puntos, int acumulados) {
        return Cliente.builder()
                .usuarioId(id)
                .clienteNombre("Cliente Test")
                .clienteTelefono("3001234567")
                .clientePuntos(puntos)
                .clientePuntosAcumulados(acumulados)
                .clienteAceptaTerminos(true)
                .clienteFechaAceptacion(LocalDateTime.now())
                .build();
    }

    private Empleado empleado(Long id) {
        return Empleado.builder()
                .usuarioId(id)
                .empleadoNombre("Cajero Test")
                .empleadoTelefono("3009876543")
                .empleadoFechaIngreso(LocalDate.now())
                .build();
    }

    // ── obtenerPuntos ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("obtenerPuntos — por email del cliente autenticado")
    class ObtenerPuntos {

        @Test
        @DisplayName("Cliente existente → retorna puntosActuales y puntosAcumulados correctos")
        void clienteExistente_retornaPuntos() {
            when(clienteRepository.findByUsuario_UsuarioEmail("cliente@altoro.com"))
                    .thenReturn(Optional.of(clienteConPuntos(1L, 5, 15)));

            ClientePuntosResponse response = puntosService.obtenerPuntos("cliente@altoro.com");

            assertThat(response.getPuntosActuales()).isEqualTo(5);
            assertThat(response.getPuntosAcumulados()).isEqualTo(15);
        }

        @Test
        @DisplayName("Cliente no encontrado → ResourceNotFoundException")
        void clienteNoEncontrado_lanzaResourceNotFound() {
            when(clienteRepository.findByUsuario_UsuarioEmail("nulo@altoro.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> puntosService.obtenerPuntos("nulo@altoro.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── obtenerPuntosPorId ────────────────────────────────────────────────────

    @Nested
    @DisplayName("obtenerPuntosPorId — por ID del cliente (CAJERO/ADMIN)")
    class ObtenerPuntosPorId {

        @Test
        @DisplayName("Cliente existente → retorna DTO correcto")
        void clienteExistente_retornaPuntos() {
            when(clienteRepository.findById(7L))
                    .thenReturn(Optional.of(clienteConPuntos(7L, 3, 8)));

            ClientePuntosResponse response = puntosService.obtenerPuntosPorId(7L);

            assertThat(response.getPuntosActuales()).isEqualTo(3);
            assertThat(response.getPuntosAcumulados()).isEqualTo(8);
        }

        @Test
        @DisplayName("Cliente no encontrado → ResourceNotFoundException")
        void clienteNoEncontrado_lanzaResourceNotFound() {
            when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> puntosService.obtenerPuntosPorId(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── canjearPuntos ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("canjearPuntos — reset de puntos y auditoría")
    class CanjearPuntos {

        @Test
        @DisplayName("Éxito: clientePuntos → 0; puntosAcumulados sin cambio; CanjePuntos guardado con el monto previo")
        void canjeoExitoso_reseteaPuntosYGuardaAuditoria() {
            Cliente cliente = clienteConPuntos(2L, 8, 20);
            when(clienteRepository.findById(2L)).thenReturn(Optional.of(cliente));
            when(empleadoRepository.findByUsuario_UsuarioEmail("cajero@altoro.com"))
                    .thenReturn(Optional.of(empleado(5L)));

            ClientePuntosResponse response = puntosService.canjearPuntos(2L, "cajero@altoro.com");

            assertThat(response.getPuntosActuales()).isEqualTo(0);
            assertThat(response.getPuntosAcumulados()).isEqualTo(20);

            // La auditoría debe registrar los puntos que tenía el cliente antes del reset
            ArgumentCaptor<CanjePuntos> captor = ArgumentCaptor.forClass(CanjePuntos.class);
            verify(canjePuntosRepository).save(captor.capture());
            assertThat(captor.getValue().getCanjePuntosCanjeados()).isEqualTo(8);
        }

        @Test
        @DisplayName("Cliente con 0 puntos → BusinessException 422 UNPROCESSABLE_ENTITY")
        void clienteSinPuntos_lanzaBusinessException422() {
            when(clienteRepository.findById(3L))
                    .thenReturn(Optional.of(clienteConPuntos(3L, 0, 5)));

            assertThatThrownBy(() -> puntosService.canjearPuntos(3L, "cajero@altoro.com"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).getStatus())
                                    .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        }

        @Test
        @DisplayName("Cliente no encontrado → ResourceNotFoundException")
        void clienteNoEncontrado_lanzaResourceNotFound() {
            when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> puntosService.canjearPuntos(99L, "cajero@altoro.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Empleado no encontrado → ResourceNotFoundException")
        void empleadoNoEncontrado_lanzaResourceNotFound() {
            when(clienteRepository.findById(4L))
                    .thenReturn(Optional.of(clienteConPuntos(4L, 3, 7)));
            when(empleadoRepository.findByUsuario_UsuarioEmail("fantasma@altoro.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> puntosService.canjearPuntos(4L, "fantasma@altoro.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
