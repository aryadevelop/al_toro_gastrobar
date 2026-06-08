package co.edu.unicauca.backend.modules.mesas_comandas.repository;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:mesadb;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS restaurante",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
@DisplayName("MesaRepository")
class MesaRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired MesaRepository repo;

    private Zona zona;
    private Empleado mesero;

    @BeforeEach
    void setUp() {
        Usuario usuarioMesero = em.persistAndFlush(Usuario.builder()
                .usuarioEmail("mr" + System.nanoTime() + "@altoro.com")
                .usuarioPassword("pass")
                .build());

        mesero = em.persistAndFlush(Empleado.builder()
                .usuario(usuarioMesero)
                .empleadoNombre("Mesero Test")
                .empleadoTelefono("3001112233")
                .empleadoFechaIngreso(LocalDate.now())
                .build());

        zona = em.persistAndFlush(Zona.builder()
                .zonaNombre("Terraza" + System.nanoTime())
                .zonaCapacidadPersonas(20)
                .build());
    }

    private Mesa persistMesa(String identificador) {
        Visita visita = em.persistAndFlush(Visita.builder()
                .visitaFechaHoraInicio(LocalDateTime.now())
                .build());
        return em.persistAndFlush(Mesa.builder()
                .visita(visita)
                .zona(zona)
                .mesero(mesero)
                .mesaIdentificador(identificador)
                .mesaNumeroPersonas(2)
                .mesaEstado(EstadoMesa.ATENDIDA)
                .build());
    }

    @Nested
    @DisplayName("findByVisita_VisitaIdIn")
    class FindByVisitaIdIn {

        @Test
        @DisplayName("devuelve las mesas pedidas con mesero+usuario hidratados")
        void devuelveMesasConMeseroHidratado() {
            Mesa m1 = persistMesa("T-1");
            Mesa m2 = persistMesa("T-2");
            persistMesa("T-3"); // no se pide
            em.clear();

            List<Mesa> resultado = repo.findByVisita_VisitaIdIn(
                    Set.of(m1.getVisitaId(), m2.getVisitaId()));

            assertThat(resultado)
                    .hasSize(2)
                    .extracting(Mesa::getMesaIdentificador)
                    .containsExactlyInAnyOrder("T-1", "T-2");
            // mesero y usuario deben estar hidratados sin disparar lazy loading
            resultado.forEach(m -> {
                assertThat(m.getMesero().getEmpleadoNombre()).isEqualTo("Mesero Test");
                assertThat(m.getMesero().getUsuario().getUsuarioEmail()).isNotBlank();
            });
        }

        @Test
        @DisplayName("colección vacía devuelve lista vacía sin error")
        void coleccionVacia_listaVacia() {
            persistMesa("T-A");

            List<Mesa> resultado = repo.findByVisita_VisitaIdIn(Collections.emptySet());

            assertThat(resultado).isEmpty();
        }
    }
}
