package co.edu.unicauca.backend.modules.mesas_comandas.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DescripcionNormalizer")
class DescripcionNormalizerTest {

    @Test
    @DisplayName("null se normaliza a cadena vacía")
    void nullDevuelveVacio() {
        assertThat(DescripcionNormalizer.normalizar(null)).isEmpty();
    }

    @Test
    @DisplayName("cadena vacía y solo espacios se normalizan a cadena vacía")
    void vaciaYSoloEspacios() {
        assertThat(DescripcionNormalizer.normalizar("")).isEmpty();
        assertThat(DescripcionNormalizer.normalizar("   ")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "'sin azucar', 'sin azucar'",
            "'SIN AZUCAR', 'sin azucar'",
            "'Sin Azucar', 'sin azucar'",
            "'  sin azucar  ', 'sin azucar'",
            "'sin   azucar', 'sin azucar'",
            "'  SIN   AZUCAR  ', 'sin azucar'"
    })
    @DisplayName("variantes de mayúsculas y espacios convergen al mismo valor")
    void variantesConvergen(String entrada, String esperado) {
        assertThat(DescripcionNormalizer.normalizar(entrada)).isEqualTo(esperado);
    }
}
