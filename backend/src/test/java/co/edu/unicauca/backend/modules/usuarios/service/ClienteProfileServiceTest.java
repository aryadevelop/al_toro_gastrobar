package co.edu.unicauca.backend.modules.usuarios.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRepository;
import co.edu.unicauca.backend.modules.usuarios.dto.request.ChangePasswordRequest;
import co.edu.unicauca.backend.modules.usuarios.dto.request.UpdateClienteRequest;
import co.edu.unicauca.backend.modules.usuarios.dto.response.ChangePasswordResponse;
import co.edu.unicauca.backend.modules.usuarios.dto.response.UpdateClienteResponse;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link ClienteProfileService}.
 *
 * <p>Cubre los cuatro métodos públicos del servicio:
 * <ul>
 *   <li>{@code getClienteIdByEmail}: éxito y cliente no encontrado.</li>
 *   <li>{@code obtenerMiPerfil}: éxito y cliente no encontrado.</li>
 *   <li>{@code actualizarPerfil}: éxito, cliente no encontrado, email duplicado,
 *       email del mismo usuario (válido), teléfono duplicado y teléfono igual al actual.</li>
 *   <li>{@code cambiarContraseña}: éxito, cliente no encontrado, contraseña actual
 *       incorrecta, confirmación no coincide y contraseña débil.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class ClienteProfileServiceTest {

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    UsuarioRepository usuarioRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    ClienteProfileService service;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Usuario usuario(Long id, String email) {
        Usuario u = new Usuario();
        u.setUsuarioId(id);
        u.setUsuarioEmail(email);
        u.setUsuarioPassword("hashOriginal");
        return u;
    }

    private Cliente cliente(Long id, Usuario u) {
        return Cliente.builder()
                .usuarioId(id)
                .usuario(u)
                .clienteNombre("Juan Perez")
                .clienteTelefono("3001234567")
                .clienteDireccion("Cra 1")
                .clientePuntos(5)
                .clientePuntosAcumulados(20)
                .clienteAceptaTerminos(true)
                .build();
    }

    private UpdateClienteRequest updateRequest(String email, String telefono) {
        return UpdateClienteRequest.builder()
                .nombre("Maria Lopez")
                .email(email)
                .telefono(telefono)
                .direccion("Cll 2")
                .aceptaTerminos(true)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("getClienteIdByEmail")
    class GetClienteIdByEmail {

        @Test
        @DisplayName("email existente → retorna usuarioId")
        void emailExistente_retornaId() {
            Usuario u = usuario(10L, "a@b.co");
            Cliente c = cliente(10L, u);
            when(clienteRepository.findByUsuario_UsuarioEmail("a@b.co"))
                    .thenReturn(Optional.of(c));

            Long id = service.getClienteIdByEmail("a@b.co");

            assertThat(id).isEqualTo(10L);
        }

        @Test
        @DisplayName("email no existe → ResourceNotFoundException")
        void emailNoExiste_lanzaNotFound() {
            when(clienteRepository.findByUsuario_UsuarioEmail("x@y.co"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getClienteIdByEmail("x@y.co"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("obtenerMiPerfil")
    class ObtenerMiPerfil {

        @Test
        @DisplayName("cliente existente → retorna ClienteData con todos los campos")
        void clienteExistente_retornaData() {
            Usuario u = usuario(7L, "p@p.co");
            Cliente c = cliente(7L, u);
            when(clienteRepository.findById(7L)).thenReturn(Optional.of(c));

            UpdateClienteResponse.ClienteData data = service.obtenerMiPerfil(7L);

            assertThat(data.getId()).isEqualTo(7L);
            assertThat(data.getNombre()).isEqualTo("Juan Perez");
            assertThat(data.getEmail()).isEqualTo("p@p.co");
            assertThat(data.getTelefono()).isEqualTo("3001234567");
            assertThat(data.getDireccion()).isEqualTo("Cra 1");
            assertThat(data.getPuntosActuales()).isEqualTo(5);
            assertThat(data.getPuntosAcumulados()).isEqualTo(20);
            assertThat(data.getAceptaTerminos()).isTrue();
        }

        @Test
        @DisplayName("cliente no existe → ResourceNotFoundException")
        void clienteNoExiste_lanzaNotFound() {
            when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerMiPerfil(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("actualizarPerfil")
    class ActualizarPerfil {

        @Test
        @DisplayName("datos válidos → actualiza y retorna respuesta success=true")
        void datosValidos_actualiza() {
            Usuario u = usuario(1L, "viejo@a.co");
            Cliente c = cliente(1L, u);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
            when(usuarioRepository.findByUsuarioEmail("nuevo@a.co"))
                    .thenReturn(Optional.empty());
            when(clienteRepository.existsByClienteTelefono("3009998877")).thenReturn(false);
            when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateClienteResponse resp = service.actualizarPerfil(
                    1L, updateRequest("NUEVO@A.CO", "3009998877"));

            assertThat(resp.getSuccess()).isTrue();
            assertThat(resp.getMessage()).contains("actualizados");
            assertThat(resp.getCliente().getEmail()).isEqualTo("nuevo@a.co");
            assertThat(resp.getCliente().getNombre()).isEqualTo("Maria Lopez");
            assertThat(resp.getCliente().getTelefono()).isEqualTo("3009998877");
            // Verifica que el email fue normalizado a lowercase en el Usuario
            assertThat(u.getUsuarioEmail()).isEqualTo("nuevo@a.co");
            verify(usuarioRepository).save(u);
        }

        @Test
        @DisplayName("teléfono igual al actual → no consulta unicidad y actualiza")
        void telefonoIgualAlActual_omiteValidacion() {
            Usuario u = usuario(1L, "viejo@a.co");
            Cliente c = cliente(1L, u);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
            when(usuarioRepository.findByUsuarioEmail("nuevo@a.co"))
                    .thenReturn(Optional.empty());
            when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateClienteResponse resp = service.actualizarPerfil(
                    1L, updateRequest("nuevo@a.co", "3001234567")); // mismo tel

            assertThat(resp.getSuccess()).isTrue();
            verify(clienteRepository, never()).existsByClienteTelefono(anyString());
        }

        @Test
        @DisplayName("email pertenece al mismo usuario → no se considera duplicado")
        void emailDelMismoUsuario_permitido() {
            Usuario u = usuario(1L, "viejo@a.co");
            Cliente c = cliente(1L, u);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
            // findByUsuarioEmail devuelve al MISMO usuario
            when(usuarioRepository.findByUsuarioEmail("viejo@a.co"))
                    .thenReturn(Optional.of(u));
            when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateClienteResponse resp = service.actualizarPerfil(
                    1L, updateRequest("viejo@a.co", "3001234567"));

            assertThat(resp.getSuccess()).isTrue();
        }

        @Test
        @DisplayName("cliente no existe → ResourceNotFoundException")
        void clienteNoExiste_lanzaNotFound() {
            when(clienteRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.actualizarPerfil(2L,
                    updateRequest("a@a.co", "3000000000")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("email pertenece a OTRO usuario → BusinessException 409")
        void emailDuplicado_lanzaBusiness() {
            Usuario u = usuario(1L, "viejo@a.co");
            Cliente c = cliente(1L, u);
            Usuario otro = usuario(99L, "nuevo@a.co");
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
            when(usuarioRepository.findByUsuarioEmail("nuevo@a.co"))
                    .thenReturn(Optional.of(otro));

            assertThatThrownBy(() -> service.actualizarPerfil(1L,
                    updateRequest("NUEVO@A.CO", "3009998877")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("teléfono ya registrado por otro cliente → BusinessException 409")
        void telefonoDuplicado_lanzaBusiness() {
            Usuario u = usuario(1L, "viejo@a.co");
            Cliente c = cliente(1L, u);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
            when(usuarioRepository.findByUsuarioEmail("nuevo@a.co"))
                    .thenReturn(Optional.empty());
            when(clienteRepository.existsByClienteTelefono("3009998877")).thenReturn(true);

            assertThatThrownBy(() -> service.actualizarPerfil(1L,
                    updateRequest("nuevo@a.co", "3009998877")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.CONFLICT);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("cambiarContraseña")
    class CambiarContrasena {

        private ChangePasswordRequest req(String actual, String nueva, String confirm) {
            return ChangePasswordRequest.builder()
                    .contraseñaActual(actual)
                    .nuevaContraseña(nueva)
                    .confirmacion(confirm)
                    .build();
        }

        @Test
        @DisplayName("contraseña actual correcta y nueva válida → success=true y guarda hash")
        void cambioExitoso() {
            Usuario u = usuario(1L, "a@a.co");
            Cliente c = cliente(1L, u);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
            when(passwordEncoder.matches("Vieja1!aa", "hashOriginal")).thenReturn(true);
            when(passwordEncoder.encode("Nueva1!aa")).thenReturn("hashNuevo");

            ChangePasswordResponse resp = service.cambiarContraseña(1L,
                    req("Vieja1!aa", "Nueva1!aa", "Nueva1!aa"));

            assertThat(resp.getSuccess()).isTrue();
            assertThat(u.getUsuarioPassword()).isEqualTo("hashNuevo");
            verify(usuarioRepository).save(u);
        }

        @Test
        @DisplayName("cliente no existe → ResourceNotFoundException")
        void clienteNoExiste_lanzaNotFound() {
            when(clienteRepository.findById(7L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.cambiarContraseña(7L,
                    req("a", "Nueva1!aa", "Nueva1!aa")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("contraseña actual incorrecta → BusinessException 400")
        void actualIncorrecta_lanzaBusiness() {
            Usuario u = usuario(1L, "a@a.co");
            Cliente c = cliente(1L, u);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
            when(passwordEncoder.matches("mala", "hashOriginal")).thenReturn(false);

            assertThatThrownBy(() -> service.cambiarContraseña(1L,
                    req("mala", "Nueva1!aa", "Nueva1!aa")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getStatus())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("nueva y confirmación no coinciden → BusinessException 400")
        void noCoinciden_lanzaBusiness() {
            Usuario u = usuario(1L, "a@a.co");
            Cliente c = cliente(1L, u);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
            when(passwordEncoder.matches("Vieja1!aa", "hashOriginal")).thenReturn(true);

            assertThatThrownBy(() -> service.cambiarContraseña(1L,
                    req("Vieja1!aa", "Nueva1!aa", "Otro1!aaa")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no coinciden");
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("nueva contraseña débil → BusinessException 400")
        void contrasenaDebil_lanzaBusiness() {
            Usuario u = usuario(1L, "a@a.co");
            Cliente c = cliente(1L, u);
            when(clienteRepository.findById(1L)).thenReturn(Optional.of(c));
            when(passwordEncoder.matches("Vieja1!aa", "hashOriginal")).thenReturn(true);

            assertThatThrownBy(() -> service.cambiarContraseña(1L,
                    req("Vieja1!aa", "debil123", "debil123")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("mayuscula");
            verify(usuarioRepository, never()).save(any());
        }
    }
}
