package co.edu.unicauca.backend.modules.pagos_caja.entity;

import co.edu.unicauca.backend.shared.enums.MetodoPago;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class VentaEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        Venta v = Venta.builder()
                .visitaId(1L)
                .ventaSubtotal(BigDecimal.valueOf(80000))
                .ventaDescuento(BigDecimal.valueOf(5000))
                .ventaTotal(BigDecimal.valueOf(75000))
                .ventaMetodo(MetodoPago.TARJETA)
                .build();

        assertThat(v.getVisitaId()).isEqualTo(1L);
        assertThat(v.getVentaSubtotal()).isEqualByComparingTo(BigDecimal.valueOf(80000));
        assertThat(v.getVentaDescuento()).isEqualByComparingTo(BigDecimal.valueOf(5000));
        assertThat(v.getVentaTotal()).isEqualByComparingTo(BigDecimal.valueOf(75000));
        assertThat(v.getVentaMetodo()).isEqualTo(MetodoPago.TARJETA);
    }

    @Test
    void setter_actualizaMetodo() {
        Venta v = new Venta();
        v.setVentaMetodo(MetodoPago.TRANSFERENCIA);
        assertThat(v.getVentaMetodo()).isEqualTo(MetodoPago.TRANSFERENCIA);
    }

    @Test
    void onCreate_setsFechasIfNull() {
        Venta v = new Venta();
        v.setVentaFechaHora(null);
        v.setCreatedAt(null);
        v.onCreate();
        assertThat(v.getVentaFechaHora()).isNotNull();
        assertThat(v.getCreatedAt()).isNotNull();
    }

    @Test
    void onCreate_noSobrescribeFechasExistentes() {
        LocalDateTime fechaVenta = LocalDateTime.of(2026, 1, 15, 12, 30);
        LocalDateTime fechaCreated = LocalDateTime.of(2026, 1, 14, 10, 0);
        Venta v = new Venta();
        v.setVentaFechaHora(fechaVenta);
        v.setCreatedAt(fechaCreated);
        v.onCreate();
        assertThat(v.getVentaFechaHora()).isEqualTo(fechaVenta);
        assertThat(v.getCreatedAt()).isEqualTo(fechaCreated);
    }

    @Test
    void equals_mismoObjeto_devuelveTrue() {
        Venta v = Venta.builder().visitaId(1L).build();
        assertThat(v.equals(v)).isTrue();
    }

    @Test
    void equals_objetoNoVenta_devuelveFalse() {
        Venta v = Venta.builder().visitaId(1L).build();
        assertThat(v.equals("string")).isFalse();
    }

    @Test
    void equals_objetoNull_devuelveFalse() {
        Venta v = Venta.builder().visitaId(1L).build();
        assertThat(v.equals(null)).isFalse();
    }

    @Test
    void equals_mismoVisitaId_devuelveTrue() {
        Venta a = Venta.builder().visitaId(10L).build();
        Venta b = Venta.builder().visitaId(10L).ventaMetodo(MetodoPago.EFECTIVO).build();
        assertThat(a.equals(b)).isTrue();
    }

    @Test
    void equals_diferenteVisitaId_devuelveFalse() {
        Venta a = Venta.builder().visitaId(1L).build();
        Venta b = Venta.builder().visitaId(2L).build();
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void equals_visitaIdNull_devuelveFalse() {
        Venta a = new Venta();
        Venta b = Venta.builder().visitaId(1L).build();
        assertThat(a.equals(b)).isFalse();
    }

    @Test
    void hashCode_esConsistentePorClase() {
        Venta a = Venta.builder().visitaId(1L).build();
        Venta b = Venta.builder().visitaId(99L).build();
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.hashCode()).isEqualTo(Venta.class.hashCode());
    }
}
