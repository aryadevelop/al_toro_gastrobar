package co.edu.unicauca.backend.entity;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.inventario.entity.CategoriaCarta;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoProducto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class OtrasEntidadesValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // -------------------------------------------------------------------------
    // Venta
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Venta")
    class VentaValidation {

        private Venta ventaValida() {
            return Venta.builder()
                    .visita(new Visita())
                    .cajero(new Empleado())
                    .ventaSubtotal(new BigDecimal("100.00"))
                    .ventaTotal(new BigDecimal("100.00"))
                    .ventaMetodo(MetodoPago.EFECTIVO)
                    .build();
        }

        @Test
        @DisplayName("Venta válida → sin violaciones")
        void ventaValida_sinViolaciones() {
            assertThat(validator.validate(ventaValida())).isEmpty();
        }

        @Test
        @DisplayName("Sin visita → violación @NotNull")
        void sinVisita_violaNotNull() {
            Venta v = ventaValida();
            v.setVisita(null);
            Set<String> campos = validator.validate(v).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("visita");
        }

        @Test
        @DisplayName("Sin cajero → violación @NotNull")
        void sinCajero_violaNotNull() {
            Venta v = ventaValida();
            v.setCajero(null);
            Set<String> campos = validator.validate(v).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("cajero");
        }

        @Test
        @DisplayName("Subtotal null → violación @NotNull")
        void subtotalNull_violaNotNull() {
            Venta v = ventaValida();
            v.setVentaSubtotal(null);
            Set<String> campos = validator.validate(v).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("ventaSubtotal");
        }

        @Test
        @DisplayName("Total null → violación @NotNull")
        void totalNull_violaNotNull() {
            Venta v = ventaValida();
            v.setVentaTotal(null);
            Set<String> campos = validator.validate(v).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("ventaTotal");
        }

        @Test
        @DisplayName("Método null → violación @NotNull")
        void metodoNull_violaNotNull() {
            Venta v = ventaValida();
            v.setVentaMetodo(null);
            Set<String> campos = validator.validate(v).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("ventaMetodo");
        }
    }

    // -------------------------------------------------------------------------
    // Abono
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Abono")
    class AbonoValidation {

        private Abono abonoValido() {
            return Abono.builder()
                    .cajero(new Empleado())
                    .reserva(new Reserva())
                    .abonoMonto(new BigDecimal("50.00"))
                    .abonoMetodo(MetodoPago.EFECTIVO)
                    .abonoTipo(TipoAbono.ANTICIPO)
                    .build();
        }

        @Test
        @DisplayName("Abono válido → sin violaciones")
        void abonoValido_sinViolaciones() {
            assertThat(validator.validate(abonoValido())).isEmpty();
        }

        @Test
        @DisplayName("Sin cajero → violación @NotNull")
        void sinCajero_violaNotNull() {
            Abono a = abonoValido();
            a.setCajero(null);
            Set<String> campos = validator.validate(a).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("cajero");
        }

        @Test
        @DisplayName("Sin reserva → violación @NotNull")
        void sinReserva_violaNotNull() {
            Abono a = abonoValido();
            a.setReserva(null);
            Set<String> campos = validator.validate(a).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("reserva");
        }

        @Test
        @DisplayName("Monto cero → violación @DecimalMin(0.01)")
        void montoCero_violaDecimalMin() {
            Abono a = abonoValido();
            a.setAbonoMonto(BigDecimal.ZERO);
            Set<String> campos = validator.validate(a).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("abonoMonto");
        }
    }

    // -------------------------------------------------------------------------
    // Producto
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Producto")
    class ProductoValidation {

        private Producto productoValido() {
            return Producto.builder()
                    .categoriaCarta(new CategoriaCarta())
                    .productoNombre("Hamburguesa")
                    .productoEstado(EstadoGenerico.ACTIVO)
                    .productoPrecio(new BigDecimal("25.00"))
                    .productoTipo(TipoProducto.PREPARACION)
                    .productoCategoria(CategoriaProducto.PLATO)
                    .build();
        }

        @Test
        @DisplayName("Producto válido → sin violaciones")
        void productoValido_sinViolaciones() {
            assertThat(validator.validate(productoValido())).isEmpty();
        }

        @Test
        @DisplayName("Sin nombre → violación @NotBlank")
        void sinNombre_violaNotBlank() {
            Producto p = productoValido();
            p.setProductoNombre("");
            Set<String> campos = validator.validate(p).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("productoNombre");
        }

        @Test
        @DisplayName("Sin estado → violación @NotNull")
        void sinEstado_violaNotNull() {
            Producto p = productoValido();
            p.setProductoEstado(null);
            Set<String> campos = validator.validate(p).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("productoEstado");
        }

        @Test
        @DisplayName("Sin precio → violación @NotNull")
        void sinPrecio_violaNotNull() {
            Producto p = productoValido();
            p.setProductoPrecio(null);
            Set<String> campos = validator.validate(p).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("productoPrecio");
        }
    }

    // -------------------------------------------------------------------------
    // CategoriaCarta
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("CategoriaCarta")
    class CategoriaCartaValidation {

        @Test
        @DisplayName("Sin nombre → violación @NotBlank")
        void sinNombre_violaNotBlank() {
            CategoriaCarta cc = CategoriaCarta.builder()
                    .categoriaNombre("")
                    .build();
            Set<String> campos = validator.validate(cc).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("categoriaNombre");
        }

        @Test
        @DisplayName("CategoriaCarta válida → sin violaciones")
        void categoriaValida_sinViolaciones() {
            CategoriaCarta cc = CategoriaCarta.builder()
                    .categoriaNombre("Entradas")
                    .build();
            assertThat(validator.validate(cc)).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Zona
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Zona")
    class ZonaValidation {

        private Zona zonaValida() {
            return Zona.builder()
                    .zonaNombre("Terraza")
                    .zonaCapacidadPersonas(20)
                    .build();
        }

        @Test
        @DisplayName("Zona válida → sin violaciones")
        void zonaValida_sinViolaciones() {
            assertThat(validator.validate(zonaValida())).isEmpty();
        }

        @Test
        @DisplayName("Sin nombre → violación @NotBlank")
        void sinNombre_violaNotBlank() {
            Zona z = zonaValida();
            z.setZonaNombre("");
            Set<String> campos = validator.validate(z).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("zonaNombre");
        }

        @Test
        @DisplayName("Capacidad cero → violación @Min(1)")
        void capacidadCero_violaMin() {
            Zona z = zonaValida();
            z.setZonaCapacidadPersonas(0);
            Set<String> campos = validator.validate(z).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("zonaCapacidadPersonas");
        }
    }

    // -------------------------------------------------------------------------
    // Mesa
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Mesa")
    class MesaValidation {

        private Mesa mesaValida() {
            return Mesa.builder()
                    .visita(new Visita())
                    .zona(new Zona())
                    .mesero(new Empleado())
                    .mesaIdentificador("M-01")
                    .mesaNumeroPersonas(4)
                    .mesaEstado(EstadoMesa.ESPERA)
                    .build();
        }

        @Test
        @DisplayName("Mesa válida → sin violaciones")
        void mesaValida_sinViolaciones() {
            assertThat(validator.validate(mesaValida())).isEmpty();
        }

        @Test
        @DisplayName("Sin identificador → violación @NotBlank")
        void sinIdentificador_violaNotBlank() {
            Mesa m = mesaValida();
            m.setMesaIdentificador("");
            Set<String> campos = validator.validate(m).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("mesaIdentificador");
        }

        @Test
        @DisplayName("Personas cero → violación @Min(1)")
        void personasCero_violaMin() {
            Mesa m = mesaValida();
            m.setMesaNumeroPersonas(0);
            Set<String> campos = validator.validate(m).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("mesaNumeroPersonas");
        }

        @Test
        @DisplayName("Sin estado → violación @NotNull")
        void sinEstado_violaNotNull() {
            Mesa m = mesaValida();
            m.setMesaEstado(null);
            Set<String> campos = validator.validate(m).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("mesaEstado");
        }
    }

    // -------------------------------------------------------------------------
    // Comanda
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Comanda")
    class ComandaValidation {

        @Test
        @DisplayName("Sin estado → violación @NotNull")
        void sinEstado_violaNotNull() {
            Comanda c = Comanda.builder()
                    .comandaEstado(null)
                    .build();
            Set<String> campos = validator.validate(c).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("comandaEstado");
        }

        @Test
        @DisplayName("Comanda válida → sin violaciones")
        void comandaValida_sinViolaciones() {
            Comanda c = Comanda.builder()
                    .comandaEstado(EstadoComanda.PENDIENTE)
                    .build();
            assertThat(validator.validate(c)).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // ComandaItem
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("ComandaItem")
    class ComandaItemValidation {

        private ComandaItem itemValido() {
            return ComandaItem.builder()
                    .comanda(new Comanda())
                    .producto(new Producto())
                    .comandaItemCantidad(2)
                    .comandaItemPrecio(new BigDecimal("15.00"))
                    .build();
        }

        @Test
        @DisplayName("Item válido → sin violaciones")
        void itemValido_sinViolaciones() {
            assertThat(validator.validate(itemValido())).isEmpty();
        }

        @Test
        @DisplayName("Cantidad cero → violación @Min(1)")
        void cantidadCero_violaMin() {
            ComandaItem ci = itemValido();
            ci.setComandaItemCantidad(0);
            Set<String> campos = validator.validate(ci).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("comandaItemCantidad");
        }

        @Test
        @DisplayName("Precio null → violación @NotNull")
        void precioNull_violaNotNull() {
            ComandaItem ci = itemValido();
            ci.setComandaItemPrecio(null);
            Set<String> campos = validator.validate(ci).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("comandaItemPrecio");
        }
    }

    // -------------------------------------------------------------------------
    // Empleado
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Empleado")
    class EmpleadoValidation {

        private Empleado empleadoValido() {
            return Empleado.builder()
                    .usuario(new Usuario())
                    .empleadoNombre("Carlos López")
                    .empleadoTelefono("3109876543")
                    .empleadoFechaIngreso(LocalDate.now())
                    .build();
        }

        @Test
        @DisplayName("Empleado válido → sin violaciones")
        void empleadoValido_sinViolaciones() {
            assertThat(validator.validate(empleadoValido())).isEmpty();
        }

        @Test
        @DisplayName("Sin nombre → violación @NotBlank")
        void sinNombre_violaNotBlank() {
            Empleado e = empleadoValido();
            e.setEmpleadoNombre("");
            Set<String> campos = validator.validate(e).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("empleadoNombre");
        }

        @Test
        @DisplayName("Teléfono con formato inválido → violación @Pattern")
        void telefonoInvalido_violaPattern() {
            Empleado e = empleadoValido();
            e.setEmpleadoTelefono("31098");
            Set<String> campos = validator.validate(e).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("empleadoTelefono");
        }

        @Test
        @DisplayName("Sin fecha de ingreso → violación @NotNull")
        void sinFechaIngreso_violaNotNull() {
            Empleado e = empleadoValido();
            e.setEmpleadoFechaIngreso(null);
            Set<String> campos = validator.validate(e).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("empleadoFechaIngreso");
        }
    }

    // -------------------------------------------------------------------------
    // Notificacion
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Notificacion")
    class NotificacionValidation {

        private Notificacion notificacionValida() {
            return Notificacion.builder()
                    .mesa(new Mesa())
                    .empleado(new Empleado())
                    .notificacionEstado(EstadoNotificacion.ACTIVA)
                    .notificacionTipo(TipoNotificacion.ATENCION)
                    .build();
        }

        @Test
        @DisplayName("Notificación válida → sin violaciones")
        void notificacionValida_sinViolaciones() {
            assertThat(validator.validate(notificacionValida())).isEmpty();
        }

        @Test
        @DisplayName("Sin estado → violación @NotNull")
        void sinEstado_violaNotNull() {
            Notificacion n = notificacionValida();
            n.setNotificacionEstado(null);
            Set<String> campos = validator.validate(n).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("notificacionEstado");
        }

        @Test
        @DisplayName("Sin tipo → violación @NotNull")
        void sinTipo_violaNotNull() {
            Notificacion n = notificacionValida();
            n.setNotificacionTipo(null);
            Set<String> campos = validator.validate(n).stream()
                    .map(cv -> cv.getPropertyPath().toString()).collect(Collectors.toSet());
            assertThat(campos).contains("notificacionTipo");
        }
    }
}
