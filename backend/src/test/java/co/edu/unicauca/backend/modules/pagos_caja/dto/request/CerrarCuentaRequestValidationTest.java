package co.edu.unicauca.backend.modules.pagos_caja.dto.request;

import co.edu.unicauca.backend.shared.enums.MetodoPago;
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

class CerrarCuentaRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private CerrarCuentaRequest requestValido() {
        return CerrarCuentaRequest.builder()
                .emailCajero("cajero@altoro.com")
                .visitaId(1L)
                .descuento(new BigDecimal("5.00"))
                .metodo(MetodoPago.EFECTIVO)
                .build();
    }

    private Set<String> camposConViolacion(CerrarCuentaRequest r) {
        return validator.validate(r).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Nested
    @DisplayName("emailCajero")
    class EmailCajeroField {

        @Test
        @DisplayName("Email cajero null → violación @NotNull")
        void emailNull_violaNotNull() {
            CerrarCuentaRequest r = CerrarCuentaRequest.builder()
                    .emailCajero(null).visitaId(1L).metodo(MetodoPago.EFECTIVO).build();
            assertThat(camposConViolacion(r)).contains("emailCajero");
        }

        @Test
        @DisplayName("Email cajero inválido → violación @Email")
        void emailInvalido_violaEmail() {
            CerrarCuentaRequest r = CerrarCuentaRequest.builder()
                    .emailCajero("no-es-email").visitaId(1L).metodo(MetodoPago.EFECTIVO).build();
            assertThat(camposConViolacion(r)).contains("emailCajero");
        }
    }

    @Nested
    @DisplayName("visitaId")
    class VisitaIdField {

        @Test
        @DisplayName("VisitaId null → violación @NotNull")
        void visitaIdNull_violaNotNull() {
            CerrarCuentaRequest r = CerrarCuentaRequest.builder()
                    .emailCajero("cajero@altoro.com").visitaId(null).metodo(MetodoPago.EFECTIVO).build();
            assertThat(camposConViolacion(r)).contains("visitaId");
        }
    }

    @Nested
    @DisplayName("descuento")
    class DescuentoField {

        @Test
        @DisplayName("Descuento negativo → violación @DecimalMin")
        void descuentoNegativo_violaDecimalMin() {
            CerrarCuentaRequest r = CerrarCuentaRequest.builder()
                    .emailCajero("cajero@altoro.com").visitaId(1L)
                    .descuento(new BigDecimal("-1.00")).metodo(MetodoPago.EFECTIVO).build();
            assertThat(camposConViolacion(r)).contains("descuento");
        }

        @Test
        @DisplayName("Descuento null → válido (se asume 0)")
        void descuentoNull_esValido() {
            CerrarCuentaRequest r = CerrarCuentaRequest.builder()
                    .emailCajero("cajero@altoro.com").visitaId(1L)
                    .descuento(null).metodo(MetodoPago.EFECTIVO).build();
            assertThat(camposConViolacion(r)).doesNotContain("descuento");
        }
    }

    @Nested
    @DisplayName("metodo")
    class MetodoField {

        @Test
        @DisplayName("Método null → violación @NotNull")
        void metodoNull_violaNotNull() {
            CerrarCuentaRequest r = CerrarCuentaRequest.builder()
                    .emailCajero("cajero@altoro.com").visitaId(1L).metodo(null).build();
            assertThat(camposConViolacion(r)).contains("metodo");
        }
    }

    @Test
    @DisplayName("Request válido → sin violaciones")
    void requestValido_sinViolaciones() {
        Set<ConstraintViolation<CerrarCuentaRequest>> violations = validator.validate(requestValido());
        assertThat(violations).isEmpty();
    }
}
