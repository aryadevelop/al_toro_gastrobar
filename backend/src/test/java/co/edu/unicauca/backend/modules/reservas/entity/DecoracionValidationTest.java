package co.edu.unicauca.backend.modules.reservas.entity;

import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DecoracionValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Decoracion decoracionValida() {
        return Decoracion.builder()
                .decoracionNombre("Globos y flores")
                .decoracionEstado(EstadoGenerico.ACTIVO)
                .decoracionCostoAdicional(new BigDecimal("50.00"))
                .build();
    }

    @Nested
    @DisplayName("nombre")
    class NombreField {

        @Test
        @DisplayName("Nombre en blanco → violación @NotBlank")
        void nombreBlanco_violaNotBlank() {
            Decoracion d = decoracionValida();
            d.setDecoracionNombre("");

            Set<String> campos = validator.validate(d).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("decoracionNombre");
        }

        @Test
        @DisplayName("Nombre null → violación @NotBlank")
        void nombreNull_violaNotBlank() {
            Decoracion d = decoracionValida();
            d.setDecoracionNombre(null);

            Set<String> campos = validator.validate(d).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("decoracionNombre");
        }
    }

    @Nested
    @DisplayName("estado")
    class EstadoField {

        @Test
        @DisplayName("Estado null → violación @NotNull")
        void estadoNull_violaNotNull() {
            Decoracion d = decoracionValida();
            d.setDecoracionEstado(null);

            Set<String> campos = validator.validate(d).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("decoracionEstado");
        }
    }

    @Nested
    @DisplayName("costoAdicional")
    class CostoAdicionalField {

        @Test
        @DisplayName("Costo menor a 1.00 → violación @DecimalMin")
        void costoMenorAUno_violaDecimalMin() {
            Decoracion d = decoracionValida();
            d.setDecoracionCostoAdicional(new BigDecimal("0.50"));

            Set<String> campos = validator.validate(d).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("decoracionCostoAdicional");
        }

        @Test
        @DisplayName("Costo null → sin violación (campo nullable)")
        void costoNull_sinViolacion() {
            Decoracion d = decoracionValida();
            d.setDecoracionCostoAdicional(null);

            Set<ConstraintViolation<Decoracion>> violations = validator.validate(d);

            assertThat(violations).isEmpty();
        }
    }

    @Test
    @DisplayName("Decoración válida → sin violaciones")
    void decoracionValida_sinViolaciones() {
        Set<ConstraintViolation<Decoracion>> violations = validator.validate(decoracionValida());

        assertThat(violations).isEmpty();
    }
}
