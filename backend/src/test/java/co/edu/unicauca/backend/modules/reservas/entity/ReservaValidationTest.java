package co.edu.unicauca.backend.modules.reservas.entity;

import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
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

class ReservaValidationTest {

    private Validator validator;
    private Cliente clienteValido;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        clienteValido = new Cliente();
    }

    private Reserva reservaValida() {
        return Reserva.builder()
                .cliente(clienteValido)
                .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(1))
                .reservaNumeroPersonas(2)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();
    }

    @Nested
    @DisplayName("cliente")
    class ClienteField {

        @Test
        @DisplayName("Reserva sin cliente → violación @NotNull")
        void sinCliente_violaNotNull() {
            Reserva r = reservaValida();
            r.setCliente(null);

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("cliente");
        }
    }

    @Nested
    @DisplayName("fechaHoraLlegada")
    class FechaHoraLlegadaField {

        @Test
        @DisplayName("Reserva sin fecha → violación @NotNull")
        void sinFecha_violaNotNull() {
            Reserva r = reservaValida();
            r.setReservaFechaHoraLlegada(null);

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("reservaFechaHoraLlegada");
        }
    }

    @Nested
    @DisplayName("numeroPersonas")
    class NumeroPersonasField {

        @Test
        @DisplayName("Reserva sin personas → violación @NotNull")
        void sinPersonas_violaNotNull() {
            Reserva r = reservaValida();
            r.setReservaNumeroPersonas(null);

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("reservaNumeroPersonas");
        }

        @Test
        @DisplayName("Reserva con 0 personas → violación @Min(1)")
        void ceroPersonas_violaMin() {
            Reserva r = reservaValida();
            r.setReservaNumeroPersonas(0);

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("reservaNumeroPersonas");
        }
    }

    @Nested
    @DisplayName("estado y tipo")
    class EstadoTipoField {

        @Test
        @DisplayName("Reserva sin estado → violación @NotNull")
        void sinEstado_violaNotNull() {
            Reserva r = reservaValida();
            r.setReservaEstado(null);

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("reservaEstado");
        }

        @Test
        @DisplayName("Reserva sin tipo → violación @NotNull")
        void sinTipo_violaNotNull() {
            Reserva r = reservaValida();
            r.setReservaTipo(null);

            Set<String> campos = validator.validate(r).stream()
                    .map(v -> v.getPropertyPath().toString())
                    .collect(Collectors.toSet());

            assertThat(campos).contains("reservaTipo");
        }
    }

    @Test
    @DisplayName("Reserva válida → sin violaciones")
    void reservaValida_sinViolaciones() {
        Reserva r = reservaValida();

        Set<ConstraintViolation<Reserva>> violations = validator.validate(r);

        assertThat(violations).isEmpty();
    }
}
