package co.edu.unicauca.backend.modules.reservas.entity;

import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BloqueDisponibilidadValidationTest {

    private Validator validator;
    private Empleado adminValido;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        adminValido = new Empleado();
    }

    private BloqueDisponibilidad bloqueValido() {
        return BloqueDisponibilidad.builder()
                .fechaInicio(LocalDate.now())
                .fechaFin(LocalDate.now().plusDays(1))
                .creadoPor(adminValido)
                .build();
    }

    @Nested
    @DisplayName("fechaInicio")
    class FechaInicioField {

        @Test
        @DisplayName("Sin fecha de inicio → violación @NotNull")
        void sinFechaInicio_violaNotNull() {
            BloqueDisponibilidad b = bloqueValido();
            b.setFechaInicio(null);

            Set<String> campos = validator.validate(b).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("fechaInicio");
        }
    }

    @Nested
    @DisplayName("fechaFin")
    class FechaFinField {

        @Test
        @DisplayName("Sin fecha de fin → violación @NotNull")
        void sinFechaFin_violaNotNull() {
            BloqueDisponibilidad b = bloqueValido();
            b.setFechaFin(null);

            Set<String> campos = validator.validate(b).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("fechaFin");
        }
    }

    @Nested
    @DisplayName("creadoPor")
    class CreadoPorField {

        @Test
        @DisplayName("Sin administrador → violación @NotNull")
        void sinAdmin_violaNotNull() {
            BloqueDisponibilidad b = bloqueValido();
            b.setCreadoPor(null);

            Set<String> campos = validator.validate(b).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("creadoPor");
        }
    }

    @Test
    @DisplayName("Bloque válido → sin violaciones")
    void bloqueValido_sinViolaciones() {
        Set<ConstraintViolation<BloqueDisponibilidad>> violations = validator.validate(bloqueValido());

        assertThat(violations).isEmpty();
    }
}
