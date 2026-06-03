package co.edu.unicauca.backend.modules.reportes.ventas_detalle.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.repository.ComandaItemDetalleAdminRepository;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.repository.MesaDetalleAdminRepository;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.repository.VentaDetalleAdminRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaDetalleAdminServiceTest {

    @Mock
    private VentaDetalleAdminRepository ventaRepository;

    @Mock
    private MesaDetalleAdminRepository mesaRepository;

    @Mock
    private ComandaItemDetalleAdminRepository itemRepository;

    @InjectMocks
    private VentaDetalleAdminService service;

    @Test
    void listarVentas_filtraPorRangoYMetodoPago() {
        Cliente cliente = Cliente.builder()
                .clienteNombre("Carlos Pérez")
                .build();
        Visita visita = Visita.builder()
                .visitaId(123L)
                .cliente(cliente)
                .build();
        Venta venta = Venta.builder()
                .visitaId(123L)
                .visita(visita)
                .ventaFechaHora(LocalDateTime.of(2026, 6, 1, 14, 30))
                .ventaSubtotal(BigDecimal.valueOf(100000))
                .ventaDescuento(BigDecimal.valueOf(5000))
                .ventaTotal(BigDecimal.valueOf(95000))
                .ventaMetodo(MetodoPago.EFECTIVO)
                .build();

        when(ventaRepository.buscarVentasPorFiltros(any(), any(), any(), any()))
                .thenReturn(List.of(venta));

        var resultado = service.listarVentas(null, "2026-06-01", "2026-06-01", "EFECTIVO");

        assertThat(resultado.getVentas()).hasSize(1);
        assertThat(resultado.getTotalPeriodo()).isEqualByComparingTo(BigDecimal.valueOf(95000));
        assertThat(resultado.getVentas().get(0).getClienteNombre()).isEqualTo("Carlos Pérez");
    }

    @Test
    void listarVentas_fechaFutura_lanzaBusinessException() {
        assertThatThrownBy(() -> service.listarVentas(null, null, LocalDate.now().plusDays(1).toString(), null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("No es posible consultar ventas en fechas futuras");
    }
}
