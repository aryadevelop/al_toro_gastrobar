package co.edu.unicauca.backend.modules.pagos_caja.entity;

import co.edu.unicauca.backend.shared.enums.MetodoPago;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
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
        v.onCreate();
        assertThat(v.getVentaFechaHora()).isNotNull();
        assertThat(v.getCreatedAt()).isNotNull();
    }
}
