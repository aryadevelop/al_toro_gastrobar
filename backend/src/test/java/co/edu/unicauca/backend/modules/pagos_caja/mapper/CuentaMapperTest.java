package co.edu.unicauca.backend.modules.pagos_caja.mapper;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.pagos_caja.dto.response.CuentaPreliminarResponse;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CuentaMapper")
class CuentaMapperTest {

    private final CuentaMapper mapper = new CuentaMapper();

    private ComandaItem item(Long id, String nombre, CategoriaProducto cat, BigDecimal precio,
                             int cant, String desc, String grupo, boolean menuEsp) {
        Producto p = Producto.builder().productoId(id).productoNombre(nombre)
                .productoCategoria(cat).productoPrecio(precio).menuEspecial(menuEsp).build();
        return ComandaItem.builder().comandaItemId(id).producto(p)
                .comandaItemPrecio(precio).comandaItemCantidad(cant)
                .comandaItemDescripcion(desc).comandaItemMenuGrupo(grupo).build();
    }

    @Test
    @DisplayName("visita con cliente, decoración con costo y abonos → totales y saldo correctos")
    void cuentaCompleta() {
        Usuario u = Usuario.builder().usuarioEmail("ana@mail.com").build();
        Cliente cliente = Cliente.builder().usuarioId(7L).usuario(u).clienteNombre("Ana")
                .clientePuntos(3).clientePuntosAcumulados(20).build();
        Decoracion deco = Decoracion.builder().decoracionNombre("Globos")
                .decoracionCostoAdicional(new BigDecimal("15.00")).build();
        Reserva reserva = Reserva.builder().reservaId(2L).decoracion(deco).build();
        Visita visita = Visita.builder().visitaId(5L).cliente(cliente).reserva(reserva)
                .visitaFechaHoraInicio(LocalDateTime.of(2026, 5, 24, 19, 0)).build();
        List<ComandaItem> items = List.of(
                item(1L, "Paella", CategoriaProducto.PLATO, new BigDecimal("50.00"), 1, null, "g1", true),
                item(2L, "Limonada", CategoriaProducto.BEBIDA, BigDecimal.ZERO, 1, null, "g1", false));
        List<Abono> abonos = List.of(
                Abono.builder().abonoId(1L).abonoMonto(new BigDecimal("30.00")).abonoTipo(TipoAbono.ANTICIPO)
                        .abonoMetodo(MetodoPago.EFECTIVO).abonoFechaHora(LocalDateTime.of(2026, 5, 20, 10, 0)).build(),
                Abono.builder().abonoId(2L).abonoMonto(new BigDecimal("5.00")).abonoTipo(TipoAbono.DEVOLUCION)
                        .abonoMetodo(MetodoPago.EFECTIVO).abonoFechaHora(LocalDateTime.of(2026, 5, 21, 10, 0)).build());
        Mesa mesa = Mesa.builder().visitaId(5L).mesaIdentificador("M-01")
                .mesero(Empleado.builder().empleadoNombre("Luis").build()).build();

        CuentaPreliminarResponse r = mapper.toCuenta(visita, items, abonos, Optional.of(mesa));

        assertThat(r.getClienteId()).isEqualTo(7L);
        assertThat(r.getClienteNombre()).isEqualTo("Ana");
        assertThat(r.getClienteEmail()).isEqualTo("ana@mail.com");
        assertThat(r.getPuntosCanjeables()).isEqualTo(3);
        assertThat(r.getPuntosAcumulados()).isEqualTo(20);
        assertThat(r.getMeseroNombre()).isEqualTo("Luis");
        assertThat(r.getMesaIdentificador()).isEqualTo("M-01");
        assertThat(r.getDecoracionNombre()).isEqualTo("Globos");
        assertThat(r.getValorDecoracion()).isEqualByComparingTo("15.00");
        assertThat(r.getTotalPreorden()).isEqualByComparingTo("50.00");
        assertThat(r.getTotalAPagar()).isEqualByComparingTo("65.00");
        assertThat(r.getMontoAbonado()).isEqualByComparingTo("25.00");
        assertThat(r.getSaldoPendiente()).isEqualByComparingTo("40.00");
        assertThat(r.getItems()).hasSize(2);
        assertThat(r.getItems().get(0).getCategoriaProducto()).isEqualTo("PLATO");
        assertThat(r.getItems().get(0).isEsMenuEspecial()).isTrue();
        assertThat(r.getItems().get(0).getMenuGrupo()).isEqualTo("g1");
        assertThat(r.getItems().get(1).getSubtotal()).isEqualByComparingTo("0.00");
        assertThat(r.getAnticipos()).hasSize(2);
    }

    @Test
    @DisplayName("invitado (cliente null) → campos de cliente null")
    void invitado_camposClienteNull() {
        Visita visita = Visita.builder().visitaId(5L)
                .visitaFechaHoraInicio(LocalDateTime.of(2026, 5, 24, 19, 0)).build();

        CuentaPreliminarResponse r = mapper.toCuenta(visita, List.of(), List.of(), Optional.empty());

        assertThat(r.getClienteId()).isNull();
        assertThat(r.getClienteNombre()).isNull();
        assertThat(r.getPuntosCanjeables()).isNull();
        assertThat(r.getMeseroNombre()).isNull();
        assertThat(r.getItems()).isEmpty();
    }

    @Test
    @DisplayName("sin reserva → sin decoración ni anticipos; saldo = total")
    void sinReserva_sinDecoracionNiAbonos() {
        Cliente cliente = Cliente.builder().usuarioId(7L)
                .usuario(Usuario.builder().usuarioEmail("a@b.com").build())
                .clienteNombre("Ana").clientePuntos(0).clientePuntosAcumulados(0).build();
        Visita visita = Visita.builder().visitaId(5L).cliente(cliente)
                .visitaFechaHoraInicio(LocalDateTime.of(2026, 5, 24, 19, 0)).build();
        List<ComandaItem> items = List.of(
                item(1L, "Taco", CategoriaProducto.PLATO, new BigDecimal("10.00"), 2, "sin cebolla", null, false));

        CuentaPreliminarResponse r = mapper.toCuenta(visita, items, List.of(), Optional.empty());

        assertThat(r.getValorDecoracion()).isNull();
        assertThat(r.getDecoracionNombre()).isNull();
        assertThat(r.getTotalAPagar()).isEqualByComparingTo("20.00");
        assertThat(r.getMontoAbonado()).isEqualByComparingTo("0.00");
        assertThat(r.getSaldoPendiente()).isEqualByComparingTo("20.00");
        assertThat(r.getAnticipos()).isNull();
        assertThat(r.getItems().get(0).isEsModificado()).isTrue();
    }

    @Test
    @DisplayName("reserva sin decoración + ítem con precio null → sin decoración, subtotal 0")
    void reservaSinDecoracion_itemPrecioNull() {
        Reserva reserva = Reserva.builder().reservaId(2L).build(); // decoración null
        Visita visita = Visita.builder().visitaId(5L).reserva(reserva)
                .visitaFechaHoraInicio(LocalDateTime.of(2026, 5, 24, 19, 0)).build();
        List<ComandaItem> items = List.of(
                item(1L, "Cortesia", CategoriaProducto.PLATO, null, 2, null, null, false));

        CuentaPreliminarResponse r = mapper.toCuenta(visita, items, List.of(), Optional.empty());

        assertThat(r.getValorDecoracion()).isNull();
        assertThat(r.getItems().get(0).getPrecioUnitario()).isEqualByComparingTo("0.00");
        assertThat(r.getItems().get(0).getSubtotal()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("reserva con decoración sin costo → valorDecoracion null")
    void reservaDecoracionSinCosto_valorNull() {
        Decoracion deco = Decoracion.builder().decoracionNombre("Velas").decoracionCostoAdicional(null).build();
        Reserva reserva = Reserva.builder().reservaId(2L).decoracion(deco).build();
        Visita visita = Visita.builder().visitaId(5L).reserva(reserva)
                .visitaFechaHoraInicio(LocalDateTime.of(2026, 5, 24, 19, 0)).build();

        CuentaPreliminarResponse r = mapper.toCuenta(visita, List.of(), List.of(), Optional.empty());

        assertThat(r.getValorDecoracion()).isNull();
        assertThat(r.getDecoracionNombre()).isNull();
    }
}
