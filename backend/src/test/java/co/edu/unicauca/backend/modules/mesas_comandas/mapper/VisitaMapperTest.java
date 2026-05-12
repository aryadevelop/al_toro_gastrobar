package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemComandaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

class VisitaMapperTest {

    private VisitaMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new VisitaMapper();
    }

    private Visita visitaBase() {
        return Visita.builder()
                .visitaId(1L)
                .visitaFechaHoraInicio(LocalDateTime.of(2026, 4, 1, 19, 0))
                .build();
    }

    private Producto producto() {
        return Producto.builder()
                .productoNombre("Bandeja Paisa")
                .productoCategoria(CategoriaProducto.PLATO)
                .build();
    }

    private Comanda comanda() {
        return Comanda.builder()
                .comandaId(1L)
                .build();
    }

    @Test
    @DisplayName("toResumen → sin mesa ni venta → estado ATENDIDA, montoTotal null")
    void toResumen_sinMesaNiVenta() {
        VisitaResumenResponse resp = mapper.toResumen(visitaBase(), Optional.empty(), Optional.empty());

        assertThat(resp.getVisitaId()).isEqualTo(1L);
        assertThat(resp.getEstadoVisita()).isEqualTo("ATENDIDA");
        assertThat(resp.getMontoTotal()).isNull();
        assertThat(resp.getMesaIdentificador()).isNull();
    }

    @Test
    @DisplayName("toResumen → con venta → estado CERRADA y montoTotal presente")
    void toResumen_conVenta_estadoCerrada() {
        Venta venta = Venta.builder()
                .ventaTotal(BigDecimal.valueOf(75000))
                .ventaMetodo(MetodoPago.EFECTIVO)
                .build();

        VisitaResumenResponse resp = mapper.toResumen(visitaBase(), Optional.empty(), Optional.of(venta));

        assertThat(resp.getEstadoVisita()).isEqualTo("CERRADA");
        assertThat(resp.getMontoTotal()).isEqualByComparingTo(BigDecimal.valueOf(75000));
    }

    @Test
    @DisplayName("toResumen → con mesa → mapea identificador y numeroPersonas")
    void toResumen_conMesa_mapeaIdentificador() {
        Zona zona = Zona.builder().zonaId(1L).zonaNombre("Terraza").build();
        Mesa mesa = Mesa.builder()
                .mesaIdentificador("T3")
                .mesaNumeroPersonas(6)
                .zona(zona)
                .build();

        VisitaResumenResponse resp = mapper.toResumen(visitaBase(), Optional.of(mesa), Optional.empty());

        assertThat(resp.getMesaIdentificador()).isEqualTo("T3");
        assertThat(resp.getNumeroPersonas()).isEqualTo(6);
        assertThat(resp.getZonaNombre()).isEqualTo("Terraza");
    }

    @Test
    @DisplayName("toDetalle → sin items, sin abonos → itemsComanda null, abonos null")
    void toDetalle_sinItemsNiAbonos() {
        VisitaDetalleResponse resp = mapper.toDetalle(
                visitaBase(), List.of(), Optional.empty(), Optional.empty(), Optional.empty());

        assertThat(resp.getVisitaId()).isEqualTo(1L);
        assertThat(resp.getItemsComanda()).isNull();
        assertThat(resp.getAbonos()).isNull();
    }

    @Test
    @DisplayName("toDetalle → con venta → totalCuenta presente")
    void toDetalle_conVenta_totalCuentaPresente() {
        Venta venta = Venta.builder()
                .ventaSubtotal(BigDecimal.valueOf(80000))
                .ventaDescuento(BigDecimal.valueOf(5000))
                .ventaTotal(BigDecimal.valueOf(75000))
                .ventaMetodo(MetodoPago.TARJETA)
                .build();

        VisitaDetalleResponse resp = mapper.toDetalle(
                visitaBase(), List.of(), Optional.empty(), Optional.of(venta), Optional.empty());

        assertThat(resp.getTotalCuenta()).isEqualByComparingTo(BigDecimal.valueOf(75000));
        assertThat(resp.getSubtotalCuenta()).isEqualByComparingTo(BigDecimal.valueOf(80000));
    }

    @Test
    @DisplayName("Agrupa items duplicados por nombre y descripcion en detalle de visita")
    void agrupaItemsDuplicadosEnDetalle() {
        // Arrange: 2 items del mismo producto con misma descripcion
        Producto producto = producto();
        Comanda comanda = comanda();

        ComandaItem item1 = ComandaItem.builder()
                .comandaItemId(1L)
                .comanda(comanda)
                .producto(producto)
                .comandaItemCantidad(2)
                .comandaItemPrecio(new BigDecimal("15000"))
                .comandaItemDescripcion("Sin cebolla")
                .build();

        ComandaItem item2 = ComandaItem.builder()
                .comandaItemId(2L)
                .comanda(comanda)
                .producto(producto)
                .comandaItemCantidad(3)
                .comandaItemPrecio(new BigDecimal("15000"))
                .comandaItemDescripcion("Sin cebolla")
                .build();

        List<ComandaItem> items = List.of(item1, item2);

        // Act
        VisitaDetalleResponse response = mapper.toDetalle(
                visitaBase(), items, Optional.empty(), Optional.empty(), Optional.empty());

        // Assert
        assertThat(response.getItemsComanda()).hasSize(1);
        ItemComandaResponse itemAgrupado = response.getItemsComanda().get(0);
        assertThat(itemAgrupado.getNombreProducto()).isEqualTo("Bandeja Paisa");
        assertThat(itemAgrupado.getDescripcion()).isEqualTo("Sin cebolla");
        assertThat(itemAgrupado.getCantidad()).isEqualTo(5);  // 2 + 3
        assertThat(itemAgrupado.getSubtotal()).isEqualByComparingTo(new BigDecimal("75000")); // 15000 * 5
    }

    @Test
    @DisplayName("No agrupa items con diferente descripcion en detalle de visita")
    void noAgrupaItemsConDiferenteDescripcion() {
        // Arrange: 2 items del mismo producto con DIFERENTE descripcion
        Producto producto = producto();
        Comanda comanda = comanda();

        ComandaItem item1 = ComandaItem.builder()
                .comandaItemId(1L)
                .comanda(comanda)
                .producto(producto)
                .comandaItemCantidad(2)
                .comandaItemPrecio(new BigDecimal("15000"))
                .comandaItemDescripcion("Sin cebolla")
                .build();

        ComandaItem item2 = ComandaItem.builder()
                .comandaItemId(2L)
                .comanda(comanda)
                .producto(producto)
                .comandaItemCantidad(3)
                .comandaItemPrecio(new BigDecimal("15000"))
                .comandaItemDescripcion("Extra picante")
                .build();

        List<ComandaItem> items = List.of(item1, item2);

        // Act
        VisitaDetalleResponse response = mapper.toDetalle(
                visitaBase(), items, Optional.empty(), Optional.empty(), Optional.empty());

        // Assert: NO se agrupan, quedan 2 items separados
        assertThat(response.getItemsComanda()).hasSize(2);
    }
}
