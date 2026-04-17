package co.edu.unicauca.backend.modules.auth.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Nested
    @DisplayName("validaciones de email")
    class EmailValidation {

        @Test
        @DisplayName("Email inválido → mensaje de formato")
        void emailInvalido_muestraMensajeFormato() {
            LoginRequest request = LoginRequest.builder()
                    .email("correo.com")
                    .password("Al.Toro2026!")
                    .build();

            Set<String> mensajes = validator.validate(request).stream()
                    .map(v -> v.getMessage())
                    .collect(java.util.stream.Collectors.toSet());

            assertThat(mensajes)
                    .contains("Por favor ingresa un correo electrónico válido. Ej: juan@gmail.com");
        }

        @Test
        @DisplayName("Email vacío → mensaje requerido")
        void emailVacio_muestraMensajeRequerido() {
            LoginRequest request = LoginRequest.builder()
                    .email("")
                    .password("Al.Toro2026!")
                    .build();

            Set<String> mensajes = validator.validate(request).stream()
                    .map(v -> v.getMessage())
                    .collect(java.util.stream.Collectors.toSet());

            assertThat(mensajes)
                    .contains("Por favor ingresa tu correo electronico");
        }
    }

    @Nested
    @DisplayName("validaciones de password")
    class PasswordValidation {

        @Test
        @DisplayName("Password vacía → mensaje requerido")
        void passwordVacia_muestraMensajeRequerido() {
            LoginRequest request = LoginRequest.builder()
                    .email("admin@altoro.com")
                    .password("")
                    .build();

            Set<String> mensajes = validator.validate(request).stream()
                    .map(v -> v.getMessage())
                    .collect(java.util.stream.Collectors.toSet());

            assertThat(mensajes)
                    .contains("Por favor, ingresa tu contraseña");
        }
    }
}
