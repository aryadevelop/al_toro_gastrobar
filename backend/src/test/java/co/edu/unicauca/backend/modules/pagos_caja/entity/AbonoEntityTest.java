package co.edu.unicauca.backend.modules.pagos_caja.entity;

import co.edu.unicauca.backend.shared.enums.MetodoPago;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class AbonoEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        Abono a = Abono.builder()
                .abonoId(1L)
                .abonoMonto(BigDecimal.valueOf(50000))
                .abonoMetodo(MetodoPago.EFECTIVO)
                .abonoTipo(TipoAbono.ANTICIPO)
                .build();

        assertThat(a.getAbonoId()).isEqualTo(1L);
        assertThat(a.getAbonoMonto()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(a.getAbonoMetodo()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(a.getAbonoTipo()).isEqualTo(TipoAbono.ANTICIPO);
    }

    @Test
    void setter_actualizaMonto() {
        Abono a = new Abono();
        a.setAbonoMonto(BigDecimal.valueOf(20000));
        assertThat(a.getAbonoMonto()).isEqualByComparingTo(BigDecimal.valueOf(20000));
    }

    @Test
    void onCreate_setsFechasIfNull() {
        Abono a = new Abono();
        a.onCreate();
        assertThat(a.getAbonoFechaHora()).isNotNull();
        assertThat(a.getCreatedAt()).isNotNull();
    }
}
