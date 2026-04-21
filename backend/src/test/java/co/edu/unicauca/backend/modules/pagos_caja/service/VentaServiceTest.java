package co.edu.unicauca.backend.modules.pagos_caja.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.pagos_caja.dto.request.CerrarCuentaRequest;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import co.edu.unicauca.backend.modules.pagos_caja.repository.VentaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link VentaService}.
 *
 * <p>Cubre el método {@link VentaService#cerrarCuenta}:
 * <ul>
 *   <li>Validaciones de existencia de visita, venta previa y cajero.</li>
 *   <li>Persistencia correcta de la venta con todos sus campos.</li>
 *   <li>Regla de negocio: incremento de puntos del cliente al cerrar cuenta.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    VisitaRepository visitaRepository;

    @Mock
    VentaRepository ventaRepository;

    @Mock
    EmpleadoRepository empleadoRepository;

    @InjectMocks
    VentaService ventaService;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static final String EMAIL_CAJERO = "cajero@altoro.com";
    private static final Long VISITA_ID = 1L;

    private CerrarCuentaRequest requestBase() {
        return CerrarCuentaRequest.builder()
                .visitaId(VISITA_ID)
                .emailCajero(EMAIL_CAJERO)
                .subtotal(new BigDecimal("50.00"))
                .descuento(new BigDecimal("5.00"))
                .total(new BigDecimal("45.00"))
                .metodo(MetodoPago.EFECTIVO)
                .build();
    }

    private Empleado cajeroTest() {
        return Empleado.builder()
                .usuarioId(10L)
                .empleadoNombre("Cajero Test")
                .empleadoTelefono("3009876543")
                .empleadoFechaIngreso(LocalDate.now())
                .build();
    }

    private Cliente clienteConPuntos(int puntos, int acumulados) {
        return Cliente.builder()
                .usuarioId(20L)
                .clienteNombre("Cliente Test")
                .clienteTelefono("3001234567")
                .clientePuntos(puntos)
                .clientePuntosAcumulados(acumulados)
                .clienteAceptaTerminos(true)
                .clienteFechaAceptacion(LocalDateTime.now())
                .build();
    }

    // ── Validaciones de existencia ────────────────────────────────────────────

    @Nested
    @DisplayName("Validaciones de entidades requeridas")
    class ValidacionesExistencia {

        @Test
        @DisplayName("Visita no existe → ResourceNotFoundException")
        void visitaNoExiste_lanzaResourceNotFoundException() {
            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ventaService.cerrarCuenta(requestBase(), EMAIL_CAJERO))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Cuenta ya cerrada para esa visita → BusinessException 409 CONFLICT")
        void cuentaYaCerrada_lanzaBusinessException() {
            Visita visita = mock(Visita.class);
            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(ventaRepository.findByVisita_VisitaId(VISITA_ID))
                    .thenReturn(Optional.of(mock(Venta.class)));

            assertThatThrownBy(() -> ventaService.cerrarCuenta(requestBase(), EMAIL_CAJERO))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).getStatus())
                                    .isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("Cajero no existe → ResourceNotFoundException")
        void cajeroNoExiste_lanzaResourceNotFoundException() {
            Visita visita = mock(Visita.class);
            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(ventaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(empleadoRepository.findByUsuario_UsuarioEmail(EMAIL_CAJERO))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> ventaService.cerrarCuenta(requestBase(), EMAIL_CAJERO))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── Lógica de puntos ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Regla de negocio: puntos del cliente")
    class PuntosCliente {

        @Test
        @DisplayName("Cliente con 3 puntos actuales y 5 acumulados → pasa a 4 y 6")
        void cerrarCuentaConCliente_incrementaAmbosPuntos() {
            Cliente cliente = clienteConPuntos(3, 5);
            Visita visita = mock(Visita.class);
            when(visita.getCliente()).thenReturn(cliente);

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(ventaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(empleadoRepository.findByUsuario_UsuarioEmail(EMAIL_CAJERO))
                    .thenReturn(Optional.of(cajeroTest()));

            ventaService.cerrarCuenta(requestBase(), EMAIL_CAJERO);

            assertThat(cliente.getClientePuntos()).isEqualTo(4);
            assertThat(cliente.getClientePuntosAcumulados()).isEqualTo(6);
        }

        @Test
        @DisplayName("Visita sin cliente (walk-in anónimo) → no lanza NPE ni modifica puntos")
        void cerrarCuentaSinCliente_noCambiaPuntos() {
            Visita visita = mock(Visita.class);
            when(visita.getCliente()).thenReturn(null);

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(ventaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(empleadoRepository.findByUsuario_UsuarioEmail(EMAIL_CAJERO))
                    .thenReturn(Optional.of(cajeroTest()));

            // No debe lanzar ninguna excepción
            ventaService.cerrarCuenta(requestBase(), EMAIL_CAJERO);
        }

        @Test
        @DisplayName("Cliente con 0 puntos → después de cerrar cuenta tiene 1 punto")
        void puntosInicianEnCero_incrementaA1() {
            Cliente cliente = clienteConPuntos(0, 0);
            Visita visita = mock(Visita.class);
            when(visita.getCliente()).thenReturn(cliente);

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(ventaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(empleadoRepository.findByUsuario_UsuarioEmail(EMAIL_CAJERO))
                    .thenReturn(Optional.of(cajeroTest()));

            ventaService.cerrarCuenta(requestBase(), EMAIL_CAJERO);

            assertThat(cliente.getClientePuntos()).isEqualTo(1);
            assertThat(cliente.getClientePuntosAcumulados()).isEqualTo(1);
        }
    }

    // ── Persistencia de la venta ──────────────────────────────────────────────

    @Nested
    @DisplayName("Persistencia correcta de la entidad Venta")
    class PersistenciaVenta {

        @Test
        @DisplayName("Venta guardada contiene subtotal, total, metodo, cajero y visita correctos")
        void ventaPersistidaConCamposCorrectos() {
            Visita visita = mock(Visita.class);
            when(visita.getCliente()).thenReturn(null);
            Empleado cajero = cajeroTest();

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(ventaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(empleadoRepository.findByUsuario_UsuarioEmail(EMAIL_CAJERO))
                    .thenReturn(Optional.of(cajero));

            CerrarCuentaRequest request = requestBase();
            ventaService.cerrarCuenta(request, EMAIL_CAJERO);

            ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
            verify(ventaRepository).save(captor.capture());

            Venta ventaGuardada = captor.getValue();
            assertThat(ventaGuardada.getVentaSubtotal()).isEqualByComparingTo(new BigDecimal("50.00"));
            assertThat(ventaGuardada.getVentaTotal()).isEqualByComparingTo(new BigDecimal("45.00"));
            assertThat(ventaGuardada.getVentaMetodo()).isEqualTo(MetodoPago.EFECTIVO);
            assertThat(ventaGuardada.getCajero()).isEqualTo(cajero);
            assertThat(ventaGuardada.getVisita()).isEqualTo(visita);
        }

        @Test
        @DisplayName("Descuento null en request → venta guardada con ventaDescuento == 0")
        void descuentoNullSeAsume0() {
            Visita visita = mock(Visita.class);
            when(visita.getCliente()).thenReturn(null);

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(ventaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(empleadoRepository.findByUsuario_UsuarioEmail(EMAIL_CAJERO))
                    .thenReturn(Optional.of(cajeroTest()));

            CerrarCuentaRequest requestSinDescuento = CerrarCuentaRequest.builder()
                    .visitaId(VISITA_ID)
                    .emailCajero(EMAIL_CAJERO)
                    .subtotal(new BigDecimal("30.00"))
                    .descuento(null)
                    .total(new BigDecimal("30.00"))
                    .metodo(MetodoPago.TARJETA)
                    .build();

            ventaService.cerrarCuenta(requestSinDescuento, EMAIL_CAJERO);

            ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
            verify(ventaRepository).save(captor.capture());

            assertThat(captor.getValue().getVentaDescuento())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
