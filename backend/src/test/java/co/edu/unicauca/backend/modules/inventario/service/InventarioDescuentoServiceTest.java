package co.edu.unicauca.backend.modules.inventario.service;

import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import co.edu.unicauca.backend.modules.inventario.entity.MovimientoInventario;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.entity.Receta;
import co.edu.unicauca.backend.modules.inventario.repository.InsumoRepository;
import co.edu.unicauca.backend.modules.inventario.repository.MovimientoInventarioRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.inventario.repository.RecetaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.TipoMovimiento;
import co.edu.unicauca.backend.shared.enums.TipoProducto;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InventarioDescuentoService")
class InventarioDescuentoServiceTest {

    @Mock ComandaItemRepository comandaItemRepository;
    @Mock RecetaRepository recetaRepository;
    @Mock InsumoRepository insumoRepository;
    @Mock ProductoRepository productoRepository;
    @Mock MovimientoInventarioRepository movimientoRepository;
    @InjectMocks InventarioDescuentoService service;

    private Empleado actor() { return Empleado.builder().build(); }
    private Comanda comanda() { return Comanda.builder().comandaId(1L).build(); }

    private Producto productoVentaDirecta(Long id, BigDecimal stock) {
        return Producto.builder()
                .productoId(id)
                .productoNombre("VD-" + id)
                .productoTipo(TipoProducto.VENTA_DIRECTA)
                .stockActual(stock)
                .build();
    }
    private Producto productoPreparacion(Long id) {
        return Producto.builder()
                .productoId(id)
                .productoNombre("Prep-" + id)
                .productoTipo(TipoProducto.PREPARACION)
                .build();
    }
    private ComandaItem item(Producto p, int cantidad, String menuGrupo) {
        return ComandaItem.builder()
                .comanda(comanda())
                .producto(p)
                .comandaItemCantidad(cantidad)
                .comandaItemMenuGrupo(menuGrupo)
                .build();
    }
    private Insumo insumo(Long id, String stock, String nombre) {
        return Insumo.builder()
                .insumoId(id)
                .insumoNombre(nombre)
                .insumoStockActual(new BigDecimal(stock))
                .build();
    }
    private Receta receta(Long productoId, Insumo i, String cantidad) {
        return Receta.builder()
                .productoId(productoId)
                .insumoId(i.getInsumoId())
                .insumo(i)
                .recetaCantidad(new BigDecimal(cantidad))
                .build();
    }

    @Nested @DisplayName("descontarPorComanda")
    class DescontarPorComanda {

        @Test @DisplayName("VENTA_DIRECTA suficiente → descuenta y registra movimiento de producto")
        void ventaDirectaSuficiente() {
            Producto p = productoVentaDirecta(10L, new BigDecimal("5"));
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(1L))
                    .thenReturn(List.of(item(p, 2, null)));
            when(productoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(p));

            service.descontarPorComanda(comanda(), actor());

            assertThat(p.getStockActual()).isEqualByComparingTo("3");
            verify(productoRepository).save(p);
            verify(movimientoRepository).save(argThat(m ->
                    m.getProducto() == p && m.getInsumo() == null
                            && m.getMovimientoTipo() == TipoMovimiento.EGRESO
                            && m.getMovimientoCantidad().compareTo(new BigDecimal("2")) == 0));
        }

        @Test @DisplayName("VENTA_DIRECTA con stockActual null → skip silencioso")
        void ventaDirectaSinStock() {
            Producto p = productoVentaDirecta(10L, null);
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(1L))
                    .thenReturn(List.of(item(p, 2, null)));
            when(productoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(p));

            service.descontarPorComanda(comanda(), actor());

            verify(productoRepository, never()).save(any());
            verify(movimientoRepository, never()).save(any());
        }

        @Test @DisplayName("VENTA_DIRECTA insuficiente → BusinessException con mensaje")
        void ventaDirectaInsuficiente() {
            Producto p = productoVentaDirecta(10L, new BigDecimal("1"));
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(1L))
                    .thenReturn(List.of(item(p, 5, null)));
            when(productoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> service.descontarPorComanda(comanda(), actor()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Stock insuficiente");
        }

        @Test @DisplayName("PREPARACION sin receta → no descuenta")
        void preparacionSinReceta() {
            Producto p = productoPreparacion(20L);
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(1L))
                    .thenReturn(List.of(item(p, 1, null)));
            when(recetaRepository.findByProductoIdFetchInsumo(20L)).thenReturn(List.of());

            service.descontarPorComanda(comanda(), actor());

            verify(insumoRepository, never()).save(any());
            verify(movimientoRepository, never()).save(any());
        }

        @Test @DisplayName("PREPARACION con 2 insumos suficientes → descuenta cada insumo y registra 2 movimientos")
        void preparacionVariosInsumos() {
            Producto p = productoPreparacion(20L);
            Insumo i1 = insumo(100L, "10", "Limón");
            Insumo i2 = insumo(101L, "5", "Sal");
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(1L))
                    .thenReturn(List.of(item(p, 2, null)));
            when(recetaRepository.findByProductoIdFetchInsumo(20L))
                    .thenReturn(List.of(receta(20L, i1, "0.5"), receta(20L, i2, "1.0")));
            when(insumoRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(i1));
            when(insumoRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(i2));

            service.descontarPorComanda(comanda(), actor());

            assertThat(i1.getInsumoStockActual()).isEqualByComparingTo("9.0");
            assertThat(i2.getInsumoStockActual()).isEqualByComparingTo("3.0");
            verify(insumoRepository).save(i1);
            verify(insumoRepository).save(i2);
            verify(movimientoRepository, times(2)).save(any(MovimientoInventario.class));
        }

        @Test @DisplayName("PREPARACION insumo insuficiente → BusinessException con nombre del insumo")
        void preparacionInsumoInsuficiente() {
            Producto p = productoPreparacion(20L);
            Insumo i = insumo(100L, "0.4", "Limón");
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(1L))
                    .thenReturn(List.of(item(p, 2, null)));
            when(recetaRepository.findByProductoIdFetchInsumo(20L))
                    .thenReturn(List.of(receta(20L, i, "0.5")));
            when(insumoRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(i));

            assertThatThrownBy(() -> service.descontarPorComanda(comanda(), actor()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Limón");
        }

        @Test @DisplayName("Item con comandaItemMenuGrupo → ignorado (no descuenta)")
        void menuEspecialIgnorado() {
            Producto p = productoVentaDirecta(10L, new BigDecimal("5"));
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(1L))
                    .thenReturn(List.of(item(p, 2, "grupo-uuid")));

            service.descontarPorComanda(comanda(), actor());

            assertThat(p.getStockActual()).isEqualByComparingTo("5");
            verifyNoInteractions(productoRepository, movimientoRepository);
        }
    }
}
