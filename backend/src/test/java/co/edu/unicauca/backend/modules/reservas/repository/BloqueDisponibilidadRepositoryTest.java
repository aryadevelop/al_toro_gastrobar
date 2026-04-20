package co.edu.unicauca.backend.modules.reservas.repository;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.reservas.entity.BloqueDisponibilidad;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
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
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:bloquedb;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS restaurante",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class BloqueDisponibilidadRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired BloqueDisponibilidadRepository bloqueRepo;

    private Empleado admin;

    @BeforeEach
    void setUp() {
        Usuario usuario = em.persistAndFlush(Usuario.builder()
                .usuarioEmail("admin" + System.nanoTime() + "@altoro.com")
                .usuarioPassword("pass")
                .build());

        admin = em.persistAndFlush(Empleado.builder()
                .usuario(usuario)
                .empleadoNombre("Admin Test")
                .empleadoTelefono("3009876543")
                .empleadoFechaIngreso(LocalDate.now())
                .build());
    }

    private void crearBloque(LocalDate inicio, LocalDate fin, LocalTime horaInicio, LocalTime horaFin) {
        em.persistAndFlush(BloqueDisponibilidad.builder()
                .fechaInicio(inicio)
                .fechaFin(fin)
                .horaInicio(horaInicio)
                .horaFin(horaFin)
                .motivo("Test")
                .creadoPor(admin)
                .build());
    }

    @Nested
    @DisplayName("countBloquesParaFechaHora")
    class CountBloques {

        private final LocalDate HOY = LocalDate.now().plusDays(10);

        @Test
        @DisplayName("Bloque de jornada completa (sin hora) aplica a cualquier hora del día")
        void bloqueJornadaCompleta_coincide() {
            crearBloque(HOY, HOY, null, null);

            long count = bloqueRepo.countBloquesParaFechaHora(HOY, LocalTime.of(14, 0));

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Bloque con rango horario coincide cuando la hora está dentro")
        void bloqueConHora_dentroDeRango() {
            crearBloque(HOY, HOY, LocalTime.of(12, 0), LocalTime.of(16, 0));

            long count = bloqueRepo.countBloquesParaFechaHora(HOY, LocalTime.of(14, 0));

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Bloque con rango horario no coincide cuando la hora está fuera")
        void bloqueConHora_fueraDeRango() {
            crearBloque(HOY, HOY, LocalTime.of(12, 0), LocalTime.of(16, 0));

            long count = bloqueRepo.countBloquesParaFechaHora(HOY, LocalTime.of(18, 0));

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Sin bloqueos retorna 0")
        void sinBloques_retornaCero() {
            long count = bloqueRepo.countBloquesParaFechaHora(HOY, LocalTime.of(10, 0));

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Bloque no aplica si fecha está fuera del rango")
        void fechaFueraDeRango_noCoincide() {
            crearBloque(HOY, HOY, null, null);

            long count = bloqueRepo.countBloquesParaFechaHora(HOY.plusDays(1), LocalTime.of(10, 0));

            assertThat(count).isZero();
        }
    }
}
