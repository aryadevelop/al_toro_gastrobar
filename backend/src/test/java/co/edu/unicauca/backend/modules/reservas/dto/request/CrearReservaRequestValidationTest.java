package co.edu.unicauca.backend.modules.reservas.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CrearReservaRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private CrearReservaRequest requestValido() {
        CrearReservaRequest r = new CrearReservaRequest();
        r.setFechaHoraLlegada(LocalDateTime.now().plusDays(1));
        r.setNumeroPersonas(2);
        return r;
    }

    @Nested
    @DisplayName("emailCliente")
    class EmailClienteField {

        @Test
        @DisplayName("Email con formato inválido → violación @Email")
        void emailInvalido_violaEmail() {
            CrearReservaRequest r = requestValido();
            r.setEmailCliente("no-es-un-email");

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("emailCliente");
        }

        @Test
        @DisplayName("Email null → sin violación (campo opcional)")
        void emailNull_sinViolacion() {
            CrearReservaRequest r = requestValido();
            r.setEmailCliente(null);

            Set<ConstraintViolation<CrearReservaRequest>> violations = validator.validate(r);

            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("fechaHoraLlegada")
    class FechaHoraLlegadaField {

        @Test
        @DisplayName("Fecha null → violación @NotNull")
        void fechaNull_violaNotNull() {
            CrearReservaRequest r = requestValido();
            r.setFechaHoraLlegada(null);

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("fechaHoraLlegada");
        }

        @Test
        @DisplayName("Fecha en el pasado → violación @Future")
        void fechaPasada_violaFuture() {
            CrearReservaRequest r = requestValido();
            r.setFechaHoraLlegada(LocalDateTime.now().minusDays(1));

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("fechaHoraLlegada");
        }
    }

    @Nested
    @DisplayName("numeroPersonas")
    class NumeroPersonasField {

        @Test
        @DisplayName("Personas null → violación @NotNull")
        void personasNull_violaNotNull() {
            CrearReservaRequest r = requestValido();
            r.setNumeroPersonas(null);

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("numeroPersonas");
        }

        @Test
        @DisplayName("Cero personas → violación @Min(1)")
        void ceroPersonas_violaMin() {
            CrearReservaRequest r = requestValido();
            r.setNumeroPersonas(0);

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("numeroPersonas");
        }
    }

    @Test
    @DisplayName("Request válido → sin violaciones")
    void requestValido_sinViolaciones() {
        Set<ConstraintViolation<CrearReservaRequest>> violations = validator.validate(requestValido());

        assertThat(violations).isEmpty();
    }
}
