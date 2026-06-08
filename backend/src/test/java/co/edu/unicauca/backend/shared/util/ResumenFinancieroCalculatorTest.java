package co.edu.unicauca.backend.shared.util;

import co.edu.unicauca.backend.shared.dto.ResumenFinanciero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResumenFinancieroCalculator")
class ResumenFinancieroCalculatorTest {

    @Test
    @DisplayName("caso completo: preorden + decoración − abonos netos")
    void casoCompleto() {
        ResumenFinanciero r = ResumenFinancieroCalculator.calcular(
                new BigDecimal("50.00"), new BigDecimal("15.00"),
                new BigDecimal("30.00"), new BigDecimal("5.00"));

        assertThat(r.totalPreorden()).isEqualByComparingTo("50.00");
        assertThat(r.valorDecoracion()).isEqualByComparingTo("15.00");
        assertThat(r.totalAPagar()).isEqualByComparingTo("65.00");
        assertThat(r.montoAbonado()).isEqualByComparingTo("25.00");
        assertThat(r.saldoPendiente()).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("decoración null → totalAPagar = preorden y valorDecoracion preservado null")
    void decoracionNull() {
        ResumenFinanciero r = ResumenFinancieroCalculator.calcular(
                new BigDecimal("20.00"), null, null, null);

        assertThat(r.valorDecoracion()).isNull();
        assertThat(r.totalAPagar()).isEqualByComparingTo("20.00");
        assertThat(r.montoAbonado()).isEqualByComparingTo("0");
        assertThat(r.saldoPendiente()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("todos null → todo cero")
    void todoNull() {
        ResumenFinanciero r = ResumenFinancieroCalculator.calcular(null, null, null, null);

        assertThat(r.totalPreorden()).isEqualByComparingTo("0");
        assertThat(r.totalAPagar()).isEqualByComparingTo("0");
        assertThat(r.montoAbonado()).isEqualByComparingTo("0");
        assertThat(r.saldoPendiente()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("solo anticipos (sin devoluciones) → montoAbonado = anticipos")
    void soloAnticipos() {
        ResumenFinanciero r = ResumenFinancieroCalculator.calcular(
                new BigDecimal("100.00"), null, new BigDecimal("40.00"), null);

        assertThat(r.montoAbonado()).isEqualByComparingTo("40.00");
        assertThat(r.saldoPendiente()).isEqualByComparingTo("60.00");
    }
}
