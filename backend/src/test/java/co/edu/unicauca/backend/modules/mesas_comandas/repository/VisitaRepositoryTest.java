package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:visitadb;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS restaurante",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class VisitaRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired VisitaRepository visitaRepo;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        Usuario usuario = em.persistAndFlush(Usuario.builder()
                .usuarioEmail("visita" + System.nanoTime() + "@altoro.com")
                .usuarioPassword("pass")
                .build());

        cliente = em.persistAndFlush(Cliente.builder()
                .usuario(usuario)
                .clienteNombre("Cliente Visita")
                .clienteTelefono("3001112233")
                .clienteAceptaTerminos(true)
                .clienteFechaAceptacion(LocalDateTime.now())
                .build());
    }

    private Visita visita(LocalDateTime inicio) {
        return em.persistAndFlush(Visita.builder()
                .cliente(cliente)
                .visitaFechaHoraInicio(inicio)
                .build());
    }

    @Nested
    @DisplayName("findByCliente_UsuarioIdOrderByVisitaFechaHoraInicioDesc")
    class FindByCliente {

        @Test
        @DisplayName("Retorna visitas del cliente ordenadas descendente")
        void retornaOrdenadoDescendente() {
            LocalDateTime t1 = LocalDateTime.now().minusDays(2);
            LocalDateTime t2 = LocalDateTime.now().minusDays(1);
            visita(t1);
            visita(t2);

            List<Visita> result = visitaRepo
                    .findByCliente_UsuarioIdOrderByVisitaFechaHoraInicioDesc(cliente.getUsuarioId());

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getVisitaFechaHoraInicio())
                    .isAfterOrEqualTo(result.get(1).getVisitaFechaHoraInicio());
        }

        @Test
        @DisplayName("Sin visitas retorna lista vacía")
        void sinVisitas_retornaVacia() {
            List<Visita> result = visitaRepo
                    .findByCliente_UsuarioIdOrderByVisitaFechaHoraInicioDesc(cliente.getUsuarioId());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByReserva_ReservaId")
    class FindByReserva {

        @Test
        @DisplayName("Visita sin reserva retorna vacío")
        void visitaSinReserva_retornaVacio() {
            visita(LocalDateTime.now());

            Optional<Visita> result = visitaRepo.findByReserva_ReservaId(9999L);

            assertThat(result).isEmpty();
        }
    }
}
