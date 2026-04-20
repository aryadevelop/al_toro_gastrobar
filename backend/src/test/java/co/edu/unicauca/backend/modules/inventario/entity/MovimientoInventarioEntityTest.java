package co.edu.unicauca.backend.modules.inventario.entity;

import co.edu.unicauca.backend.shared.enums.TipoMovimiento;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class MovimientoInventarioEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        MovimientoInventario m = MovimientoInventario.builder()
                .movimientoId(1L)
                .movimientoCantidad(BigDecimal.valueOf(5))
                .movimientoTipo(TipoMovimiento.INGRESO)
                .build();

        assertThat(m.getMovimientoId()).isEqualTo(1L);
        assertThat(m.getMovimientoCantidad()).isEqualByComparingTo(BigDecimal.valueOf(5));
        assertThat(m.getMovimientoTipo()).isEqualTo(TipoMovimiento.INGRESO);
    }

    @Test
    void setter_actualizaTipo() {
        MovimientoInventario m = new MovimientoInventario();
        m.setMovimientoTipo(TipoMovimiento.EGRESO);
        assertThat(m.getMovimientoTipo()).isEqualTo(TipoMovimiento.EGRESO);
    }
}
