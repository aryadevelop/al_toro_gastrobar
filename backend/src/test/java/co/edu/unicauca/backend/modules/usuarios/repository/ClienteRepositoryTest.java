package co.edu.unicauca.backend.modules.usuarios.repository;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:clientedb;MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS restaurante",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class ClienteRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired ClienteRepository clienteRepo;

    private Cliente crearCliente(String email) {
        Usuario usuario = em.persistAndFlush(Usuario.builder()
                .usuarioEmail(email)
                .usuarioPassword("pass")
                .build());

        return em.persistAndFlush(Cliente.builder()
                .usuario(usuario)
                .clienteNombre("Cliente Test")
                .clienteTelefono("3001234567")
                .clienteAceptaTerminos(true)
                .clienteFechaAceptacion(LocalDateTime.now())
                .build());
    }

    @Nested
    @DisplayName("findByUsuario_UsuarioEmail")
    class FindByEmail {

        @Test
        @DisplayName("Email existente → retorna cliente")
        void emailExistente_retornaCliente() {
            crearCliente("cli@altoro.com");

            Optional<Cliente> result = clienteRepo.findByUsuario_UsuarioEmail("cli@altoro.com");

            assertThat(result).isPresent();
            assertThat(result.get().getClienteNombre()).isEqualTo("Cliente Test");
        }

        @Test
        @DisplayName("Email inexistente → vacío")
        void emailInexistente_retornaVacio() {
            Optional<Cliente> result = clienteRepo.findByUsuario_UsuarioEmail("noexiste@altoro.com");

            assertThat(result).isEmpty();
        }
    }
}
