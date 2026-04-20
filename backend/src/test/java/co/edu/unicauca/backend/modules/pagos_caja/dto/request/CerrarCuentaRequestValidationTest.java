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
                .subtotal(new BigDecimal("50.00"))
                .total(new BigDecimal("50.00"))
                .metodo(MetodoPago.EFECTIVO)
                .build();
    }

    @Nested
    @DisplayName("emailCajero")
    class EmailCajeroField {

        @Test
        @DisplayName("Email cajero null → violación @NotNull")
        void emailNull_violaNotNull() {
            CerrarCuentaRequest r = CerrarCuentaRequest.builder()
                    .emailCajero(null)
                    .visitaId(1L)
                    .subtotal(new BigDecimal("50.00"))
                    .total(new BigDecimal("50.00"))
                    .metodo(MetodoPago.EFECTIVO)
                    .build();

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("emailCajero");
        }

        @Test
        @DisplayName("Email cajero inválido → violación @Email")
        void emailInvalido_violaEmail() {
            CerrarCuentaRequest r = CerrarCuentaRequest.builder()
                    .emailCajero("no-es-email")
                    .visitaId(1L)
                    .subtotal(new BigDecimal("50.00"))
                    .total(new BigDecimal("50.00"))
                    .metodo(MetodoPago.EFECTIVO)
                    .build();

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("emailCajero");
        }
    }

    @Nested
    @DisplayName("visitaId")
    class VisitaIdField {

        @Test
        @DisplayName("VisitaId null → violación @NotNull")
        void visitaIdNull_violaNotNull() {
            CerrarCuentaRequest r = CerrarCuentaRequest.builder()
                    .emailCajero("cajero@altoro.com")
                    .visitaId(null)
                    .subtotal(new BigDecimal("50.00"))
                    .total(new BigDecimal("50.00"))
                    .metodo(MetodoPago.EFECTIVO)
                    .build();

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("visitaId");
        }
    }

    @Nested
    @DisplayName("subtotal")
    class SubtotalField {

        @Test
        @DisplayName("Subtotal null → violación @NotNull")
        void subtotalNull_violaNotNull() {
            CerrarCuentaRequest r = CerrarCuentaRequest.builder()
                    .emailCajero("cajero@altoro.com")
                    .visitaId(1L)
                    .subtotal(null)
                    .total(new BigDecimal("50.00"))
                    .metodo(MetodoPago.EFECTIVO)
                    .build();

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("subtotal");
        }

        @Test
        @DisplayName("Subtotal negativo → violación @DecimalMin")
        void subtotalNegativo_violaDecimalMin() {
            CerrarCuentaRequest r = CerrarCuentaRequest.builder()
                    .emailCajero("cajero@altoro.com")
                    .visitaId(1L)
                    .subtotal(new BigDecimal("-1.00"))
                    .total(new BigDecimal("50.00"))
                    .metodo(MetodoPago.EFECTIVO)
                    .build();

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("subtotal");
        }
    }

    @Nested
    @DisplayName("total")
    class TotalField {

        @Test
        @DisplayName("Total null → violación @NotNull")
        void totalNull_violaNotNull() {
            CerrarCuentaRequest r = CerrarCuentaRequest.builder()
                    .emailCajero("cajero@altoro.com")
                    .visitaId(1L)
                    .subtotal(new BigDecimal("50.00"))
                    .total(null)
                    .metodo(MetodoPago.EFECTIVO)
                    .build();

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("total");
        }
    }

    @Nested
    @DisplayName("metodo")
    class MetodoField {

        @Test
        @DisplayName("Método null → violación @NotNull")
        void metodoNull_violaNotNull() {
            CerrarCuentaRequest r = CerrarCuentaRequest.builder()
                    .emailCajero("cajero@altoro.com")
                    .visitaId(1L)
                    .subtotal(new BigDecimal("50.00"))
                    .total(new BigDecimal("50.00"))
                    .metodo(null)
                    .build();

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("metodo");
        }
    }

    @Test
    @DisplayName("Request válido → sin violaciones")
    void requestValido_sinViolaciones() {
        Set<ConstraintViolation<CerrarCuentaRequest>> violations = validator.validate(requestValido());

        assertThat(violations).isEmpty();
    }
}
