package co.edu.unicauca.backend.modules.auth.repository;

import co.edu.unicauca.backend.modules.auth.entity.Sesion;
import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:sesiondb;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS restaurante",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class SesionRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired SesionRepository sesionRepo;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = em.persistAndFlush(Usuario.builder()
                .usuarioEmail("sesion" + System.nanoTime() + "@altoro.com")
                .usuarioPassword("pass")
                .build());
    }

    private Sesion sesion(String token, String refreshToken, boolean activa) {
        return em.persistAndFlush(Sesion.builder()
                .usuario(usuario)
                .sesionToken(token)
                .sesionRefreshToken(refreshToken)
                .sesionActiva(activa)
                .build());
    }

    @Nested
    @DisplayName("findBySesionTokenAndSesionActivaTrue")
    class FindByToken {

        @Test
        @DisplayName("Token activo → retorna sesión")
        void tokenActivo_retornaSesion() {
            sesion("token-activo", "refresh-1", true);

            Optional<Sesion> result = sesionRepo.findBySesionTokenAndSesionActivaTrue("token-activo");

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("Token revocado → vacío")
        void tokenRevocado_retornaVacio() {
            sesion("token-revocado", "refresh-2", false);

            Optional<Sesion> result = sesionRepo.findBySesionTokenAndSesionActivaTrue("token-revocado");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findBySesionRefreshTokenAndSesionActivaTrue")
    class FindByRefreshToken {

        @Test
        @DisplayName("Refresh token activo → retorna sesión")
        void refreshTokenActivo_retornaSesion() {
            sesion("token-r1", "refresh-activo", true);

            Optional<Sesion> result = sesionRepo.findBySesionRefreshTokenAndSesionActivaTrue("refresh-activo");

            assertThat(result).isPresent();
        }

        @Test
        @DisplayName("Refresh token inactivo → vacío")
        void refreshTokenInactivo_retornaVacio() {
            sesion("token-r2", "refresh-inactivo", false);

            Optional<Sesion> result = sesionRepo.findBySesionRefreshTokenAndSesionActivaTrue("refresh-inactivo");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByUsuarioUsuarioIdAndSesionActivaTrue")
    class FindActiveByUsuario {

        @Test
        @DisplayName("Solo devuelve sesiones activas del usuario")
        void soloSesionesActivas() {
            sesion("token-a", "refresh-a", true);
            sesion("token-b", "refresh-b", false);

            List<Sesion> result = sesionRepo
                    .findByUsuarioUsuarioIdAndSesionActivaTrue(usuario.getUsuarioId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSesionToken()).isEqualTo("token-a");
        }
    }
}
