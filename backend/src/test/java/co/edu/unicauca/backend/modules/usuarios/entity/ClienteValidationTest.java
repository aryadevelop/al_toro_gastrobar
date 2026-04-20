package co.edu.unicauca.backend.modules.usuarios.entity;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
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

class ClienteValidationTest {

    private Validator validator;
    private Usuario usuarioValido;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        usuarioValido = new Usuario();
    }

    private Cliente clienteValido() {
        return Cliente.builder()
                .usuario(usuarioValido)
                .clienteNombre("Juan Pérez")
                .clienteTelefono("3001234567")
                .clienteAceptaTerminos(true)
                .clienteFechaAceptacion(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("nombre")
    class NombreField {

        @Test
        @DisplayName("Nombre en blanco → violación @NotBlank")
        void nombreBlanco_violaNotBlank() {
            Cliente c = clienteValido();
            c.setClienteNombre("");

            Set<String> campos = validator.validate(c).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("clienteNombre");
        }

        @Test
        @DisplayName("Nombre null → violación @NotBlank")
        void nombreNull_violaNotBlank() {
            Cliente c = clienteValido();
            c.setClienteNombre(null);

            Set<String> campos = validator.validate(c).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("clienteNombre");
        }
    }

    @Nested
    @DisplayName("telefono")
    class TelefonoField {

        @Test
        @DisplayName("Teléfono con letras → violación @Pattern")
        void telefonoConLetras_violaPattern() {
            Cliente c = clienteValido();
            c.setClienteTelefono("300abc1234");

            Set<String> campos = validator.validate(c).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("clienteTelefono");
        }

        @Test
        @DisplayName("Teléfono con menos de 10 dígitos → violación @Pattern")
        void telefonoCorto_violaPattern() {
            Cliente c = clienteValido();
            c.setClienteTelefono("300123");

            Set<String> campos = validator.validate(c).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("clienteTelefono");
        }

        @Test
        @DisplayName("Teléfono con más de 10 dígitos → violación @Pattern")
        void telefonoLargo_violaPattern() {
            Cliente c = clienteValido();
            c.setClienteTelefono("30012345678");

            Set<String> campos = validator.validate(c).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("clienteTelefono");
        }
    }

    @Nested
    @DisplayName("puntos")
    class PuntosField {

        @Test
        @DisplayName("Puntos negativos → violación @Min(0)")
        void puntosNegativos_violaMin() {
            Cliente c = clienteValido();
            c.setClientePuntos(-1);

            Set<String> campos = validator.validate(c).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("clientePuntos");
        }

        @Test
        @DisplayName("Puntos acumulados negativos → violación @Min(0)")
        void puntosAcumuladosNegativos_violaMin() {
            Cliente c = clienteValido();
            c.setClientePuntosAcumulados(-5);

            Set<String> campos = validator.validate(c).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("clientePuntosAcumulados");
        }
    }

    @Nested
    @DisplayName("terminos")
    class TerminosField {

        @Test
        @DisplayName("Sin aceptación de términos → violación @NotNull")
        void sinAceptaTerminos_violaNotNull() {
            Cliente c = clienteValido();
            c.setClienteAceptaTerminos(null);

            Set<String> campos = validator.validate(c).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("clienteAceptaTerminos");
        }

        @Test
        @DisplayName("Sin fecha de aceptación → violación @NotNull")
        void sinFechaAceptacion_violaNotNull() {
            Cliente c = clienteValido();
            c.setClienteFechaAceptacion(null);

            Set<String> campos = validator.validate(c).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("clienteFechaAceptacion");
        }
    }

    @Test
    @DisplayName("Cliente válido → sin violaciones")
    void clienteValido_sinViolaciones() {
        Set<ConstraintViolation<Cliente>> violations = validator.validate(clienteValido());

        assertThat(violations).isEmpty();
    }
}
