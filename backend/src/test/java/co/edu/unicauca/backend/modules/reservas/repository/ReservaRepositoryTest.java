package co.edu.unicauca.backend.modules.reservas.repository;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:reservadb;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS restaurante",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class ReservaRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired ReservaRepository reservaRepo;

    private Cliente cliente;
    private Zona zona;
    private final LocalDateTime BASE = LocalDateTime.now().plusDays(5).withHour(12).withMinute(0).withSecond(0).withNano(0);

    @BeforeEach
    void setUp() {
        Usuario usuario = em.persistAndFlush(Usuario.builder()
                .usuarioEmail("test" + System.nanoTime() + "@altoro.com")
                .usuarioPassword("pass")
                .build());

        cliente = em.persistAndFlush(Cliente.builder()
                .usuario(usuario)
                .clienteNombre("Test Cliente")
                .clienteTelefono("3001234567")
                .clienteAceptaTerminos(true)
                .clienteFechaAceptacion(LocalDateTime.now())
                .build());

        zona = em.persistAndFlush(Zona.builder()
                .zonaNombre("Zona Test")
                .zonaCapacidadPersonas(20)
                .build());
    }

    private Reserva reserva(EstadoReserva estado, int personas, LocalDateTime fecha) {
        return em.persistAndFlush(Reserva.builder()
                .cliente(cliente)
                .zona(zona)
                .reservaFechaHoraLlegada(fecha)
                .reservaNumeroPersonas(personas)
                .reservaEstado(estado)
                .reservaTipo(TipoReserva.BASICA)
                .build());
    }

    @Nested
    @DisplayName("findByCliente_UsuarioIdOrderByReservaFechaHoraLlegadaDesc")
    class FindByCliente {

        @Test
        @DisplayName("Retorna reservas del cliente ordenadas descendente")
        void retornaReservasDelCliente() {
            reserva(EstadoReserva.CONFIRMADA, 2, BASE);
            reserva(EstadoReserva.PENDIENTE, 3, BASE.plusDays(1));

            List<Reserva> result = reservaRepo
                    .findByCliente_UsuarioIdOrderByReservaFechaHoraLlegadaDesc(cliente.getUsuarioId());

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getReservaFechaHoraLlegada())
                    .isAfterOrEqualTo(result.get(1).getReservaFechaHoraLlegada());
        }

        @Test
        @DisplayName("No retorna reservas de otro cliente")
        void noRetornaReservasDeOtroCliente() {
            reserva(EstadoReserva.CONFIRMADA, 2, BASE);

            List<Reserva> result = reservaRepo
                    .findByCliente_UsuarioIdOrderByReservaFechaHoraLlegadaDesc(9999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("sumPersonasByZonaEnDia")
    class SumPersonas {

        private final List<EstadoReserva> ACTIVOS = List.of(EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE);

        @Test
        @DisplayName("Suma personas en rango")
        void sumaPersonasEnRango() {
            reserva(EstadoReserva.CONFIRMADA, 4, BASE);
            reserva(EstadoReserva.PENDIENTE, 3, BASE.plusHours(1));

            int sum = reservaRepo.sumPersonasByZonaEnDia(
                    zona.getZonaId(), BASE.minusHours(1), BASE.plusHours(2), ACTIVOS);

            assertThat(sum).isEqualTo(7);
        }

        @Test
        @DisplayName("Excluye estados CANCELADA")
        void excluyeEstadoCancelada() {
            reserva(EstadoReserva.CONFIRMADA, 5, BASE);
            reserva(EstadoReserva.CANCELADA, 10, BASE.plusMinutes(30));

            int sum = reservaRepo.sumPersonasByZonaEnDia(
                    zona.getZonaId(), BASE.minusHours(1), BASE.plusHours(1), ACTIVOS);

            assertThat(sum).isEqualTo(5);
        }

        @Test
        @DisplayName("Retorna 0 cuando zona vacía")
        void retornaCeroSinReservas() {
            int sum = reservaRepo.sumPersonasByZonaEnDia(
                    zona.getZonaId(), BASE.minusHours(1), BASE.plusHours(1), ACTIVOS);

            assertThat(sum).isZero();
        }
    }

    @Nested
    @DisplayName("sumPersonasByZonaEnDiaExcluyendo")
    class SumPersonasExcluyendo {

        private final List<EstadoReserva> ACTIVOS = List.of(EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE);

        @Test
        @DisplayName("No cuenta la reserva excluida")
        void noContaLaReservaExcluida() {
            Reserva r1 = reserva(EstadoReserva.CONFIRMADA, 5, BASE);
            Reserva r2 = reserva(EstadoReserva.CONFIRMADA, 3, BASE.plusHours(1));

            int sum = reservaRepo.sumPersonasByZonaEnDiaExcluyendo(
                    zona.getZonaId(), BASE.minusHours(1), BASE.plusHours(2), ACTIVOS, r1.getReservaId());

            assertThat(sum).isEqualTo(3);
        }
    }
}
