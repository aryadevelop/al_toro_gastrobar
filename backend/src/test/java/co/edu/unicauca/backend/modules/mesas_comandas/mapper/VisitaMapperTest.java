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
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import co.edu.unicauca.backend.modules.reservas.dto.response.AbonoItemResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
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
    @DisplayName("toDetalle → con venta → totalCuenta presente y totalAPagar alineado con la Venta")
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
        // Visita cerrada: totalAPagar = ventaTotal (con descuento), no el cálculo en vivo (0 sin ítems)
        assertThat(resp.getTotalAPagar()).isEqualByComparingTo(BigDecimal.valueOf(75000));
        assertThat(resp.getSaldoPendiente()).isEqualByComparingTo(BigDecimal.valueOf(75000));
    }

    @Test
    @DisplayName("toDetalle → visita cerrada con abonos → saldoPendiente = ventaTotal − montoAbonado")
    void toDetalle_conVentaYAbonos_saldoNetoVenta() {
        Venta venta = Venta.builder()
                .ventaSubtotal(BigDecimal.valueOf(80000))
                .ventaDescuento(BigDecimal.valueOf(5000))
                .ventaTotal(BigDecimal.valueOf(75000))
                .ventaMetodo(MetodoPago.TARJETA)
                .build();
        Abono anticipo = Abono.builder().abonoId(1L).abonoMonto(BigDecimal.valueOf(20000))
                .abonoTipo(TipoAbono.ANTICIPO).abonoMetodo(MetodoPago.EFECTIVO)
                .abonoFechaHora(LocalDateTime.of(2026, 4, 1, 10, 0)).build();

        VisitaDetalleResponse resp = mapper.toDetalle(
                visitaBase(), List.of(), Optional.of(List.of(anticipo)), Optional.of(venta), Optional.empty());

        assertThat(resp.getTotalAPagar()).isEqualByComparingTo(BigDecimal.valueOf(75000));
        assertThat(resp.getMontoAbonado()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(resp.getSaldoPendiente()).isEqualByComparingTo(BigDecimal.valueOf(55000)); // 75000 − 20000
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

    @Test
    @DisplayName("toResumen → sin mesa pero con reserva con zona → toma zona de reserva")
    void toResumen_sinMesaConReservaConZona() {
        Zona zona = Zona.builder().zonaId(2L).zonaNombre("Salon Principal").build();
        Reserva reserva = Reserva.builder().reservaId(5L).zona(zona).build();
        Visita visita = Visita.builder()
                .visitaId(1L)
                .visitaFechaHoraInicio(LocalDateTime.of(2026, 4, 1, 19, 0))
                .reserva(reserva)
                .build();

        VisitaResumenResponse resp = mapper.toResumen(visita, Optional.empty(), Optional.empty());

        assertThat(resp.getZonaNombre()).isEqualTo("Salon Principal");
        assertThat(resp.getReservaId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("toResumen → sin mesa, reserva sin zona → zonaNombre null")
    void toResumen_sinMesaReservaSinZona() {
        Reserva reserva = Reserva.builder().reservaId(5L).zona(null).build();
        Visita visita = Visita.builder()
                .visitaId(1L)
                .visitaFechaHoraInicio(LocalDateTime.of(2026, 4, 1, 19, 0))
                .reserva(reserva)
                .build();

        VisitaResumenResponse resp = mapper.toResumen(visita, Optional.empty(), Optional.empty());

        assertThat(resp.getZonaNombre()).isNull();
    }

    @Test
    @DisplayName("toDetalle → con mesa con mesero → mapea meseroNombre")
    void toDetalle_conMesaConMesero() {
        Empleado mesero = Empleado.builder().empleadoNombre("Carlos Ruiz").build();
        Zona zona = Zona.builder().zonaNombre("Terraza").build();
        Mesa mesa = Mesa.builder()
                .mesaIdentificador("T1")
                .mesaNumeroPersonas(4)
                .mesero(mesero)
                .zona(zona)
                .build();

        VisitaDetalleResponse resp = mapper.toDetalle(
                visitaBase(), List.of(), Optional.empty(), Optional.empty(), Optional.of(mesa));

        assertThat(resp.getMeseroNombre()).isEqualTo("Carlos Ruiz");
        assertThat(resp.getZonaNombre()).isEqualTo("Terraza");
        assertThat(resp.getMesaIdentificador()).isEqualTo("T1");
    }

    @Test
    @DisplayName("toDetalle → sin mesa, reserva con zona y decoracion → mapea desde reserva")
    void toDetalle_sinMesaReservaConZonaYDecoracion() {
        Zona zona = Zona.builder().zonaNombre("VIP").build();
        Decoracion decoracion = Decoracion.builder().decoracionNombre("Aniversario").build();
        Reserva reserva = Reserva.builder()
                .reservaId(7L)
                .zona(zona)
                .decoracion(decoracion)
                .build();
        Visita visita = Visita.builder()
                .visitaId(1L)
                .visitaFechaHoraInicio(LocalDateTime.of(2026, 4, 1, 19, 0))
                .reserva(reserva)
                .build();

        VisitaDetalleResponse resp = mapper.toDetalle(
                visita, List.of(), Optional.empty(), Optional.empty(), Optional.empty());

        assertThat(resp.getZonaNombre()).isEqualTo("VIP");
        assertThat(resp.getDecoracionNombre()).isEqualTo("Aniversario");
        assertThat(resp.getReservaId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("toDetalle → reserva con decoración con costo y abonos → resumen financiero")
    void toDetalle_resumenFinanciero() {
        Decoracion deco = Decoracion.builder().decoracionNombre("Globos")
                .decoracionCostoAdicional(new BigDecimal("15000")).build();
        Reserva reserva = Reserva.builder().reservaId(7L).decoracion(deco).build();
        Visita visita = Visita.builder().visitaId(1L)
                .visitaFechaHoraInicio(LocalDateTime.of(2026, 4, 1, 19, 0))
                .reserva(reserva).build();
        Producto p = Producto.builder().productoNombre("Paella")
                .productoCategoria(CategoriaProducto.PLATO).build();
        ComandaItem item = ComandaItem.builder().comandaItemId(1L).producto(p)
                .comandaItemPrecio(new BigDecimal("50000")).comandaItemCantidad(1).build();
        Abono anticipo = Abono.builder().abonoId(1L).abonoMonto(new BigDecimal("30000"))
                .abonoTipo(TipoAbono.ANTICIPO).abonoMetodo(MetodoPago.EFECTIVO)
                .abonoFechaHora(LocalDateTime.of(2026, 4, 1, 10, 0)).build();
        Abono devol = Abono.builder().abonoId(2L).abonoMonto(new BigDecimal("5000"))
                .abonoTipo(TipoAbono.DEVOLUCION).abonoMetodo(MetodoPago.EFECTIVO)
                .abonoFechaHora(LocalDateTime.of(2026, 4, 1, 11, 0)).build();

        VisitaDetalleResponse resp = mapper.toDetalle(
                visita, List.of(item), Optional.of(List.of(anticipo, devol)), Optional.empty(), Optional.empty());

        assertThat(resp.getTotalPreorden()).isEqualByComparingTo("50000");
        assertThat(resp.getValorDecoracion()).isEqualByComparingTo("15000");
        assertThat(resp.getTotalAPagar()).isEqualByComparingTo("65000");
        assertThat(resp.getMontoAbonado()).isEqualByComparingTo("25000");
        assertThat(resp.getSaldoPendiente()).isEqualByComparingTo("40000");
    }

    @Test
    @DisplayName("toDetalle → reserva sin decoracion → decoracionNombre null")
    void toDetalle_reservaSinDecoracion() {
        Reserva reserva = Reserva.builder().reservaId(8L).decoracion(null).build();
        Visita visita = Visita.builder()
                .visitaId(1L)
                .visitaFechaHoraInicio(LocalDateTime.of(2026, 4, 1, 19, 0))
                .reserva(reserva)
                .build();

        VisitaDetalleResponse resp = mapper.toDetalle(
                visita, List.of(), Optional.empty(), Optional.empty(), Optional.empty());

        assertThat(resp.getDecoracionNombre()).isNull();
    }

    @Test
    @DisplayName("toDetalle → con cliente → mapea clienteId y clienteNombre")
    void toDetalle_conCliente() {
        Cliente cliente = Cliente.builder()
                .usuarioId(42L)
                .clienteNombre("Ana Gómez")
                .build();
        Visita visita = Visita.builder()
                .visitaId(1L)
                .visitaFechaHoraInicio(LocalDateTime.of(2026, 4, 1, 19, 0))
                .visitaFechaHoraFin(LocalDateTime.of(2026, 4, 1, 21, 30))
                .cliente(cliente)
                .build();

        VisitaDetalleResponse resp = mapper.toDetalle(
                visita, List.of(), Optional.empty(), Optional.empty(), Optional.empty());

        assertThat(resp.getClienteId()).isEqualTo(42L);
        assertThat(resp.getClienteNombre()).isEqualTo("Ana Gómez");
        assertThat(resp.getFechaHoraSalida()).isEqualTo("2026-04-01T21:30:00");
    }

    @Test
    @DisplayName("toDetalle → con abonos no vacíos → mapea lista de AbonoItemResponse")
    void toDetalle_conAbonos() {
        Abono abono = Abono.builder()
                .abonoId(100L)
                .abonoMonto(new BigDecimal("20000"))
                .abonoFechaHora(LocalDateTime.of(2026, 4, 1, 18, 0))
                .abonoMetodo(MetodoPago.EFECTIVO)
                .abonoTipo(TipoAbono.ANTICIPO)
                .build();

        VisitaDetalleResponse resp = mapper.toDetalle(
                visitaBase(), List.of(), Optional.of(List.of(abono)),
                Optional.empty(), Optional.empty());

        assertThat(resp.getAbonos()).hasSize(1);
        AbonoItemResponse dto = resp.getAbonos().get(0);
        assertThat(dto.getAbonoId()).isEqualTo(100L);
        assertThat(dto.getMonto()).isEqualByComparingTo(new BigDecimal("20000"));
        assertThat(dto.getMetodo()).isEqualTo("EFECTIVO");
        assertThat(dto.getTipo()).isEqualTo("ANTICIPO");
    }

    @Test
    @DisplayName("toDetalle → con abonos vacíos en Optional → abonos null")
    void toDetalle_conAbonosListaVacia() {
        VisitaDetalleResponse resp = mapper.toDetalle(
                visitaBase(), List.of(), Optional.of(List.of()),
                Optional.empty(), Optional.empty());

        assertThat(resp.getAbonos()).isNull();
    }
}
