package co.edu.unicauca.backend.modules.auth.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRepository;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRolRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.UsuarioRol;
import co.edu.unicauca.backend.shared.enums.RolEstado;
import co.edu.unicauca.backend.shared.enums.RolNombre;
import co.edu.unicauca.backend.shared.security.RoleMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioRolRepository usuarioRolRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    // -----------------------------------------------------------------------
    // Shared test fixtures
    // -----------------------------------------------------------------------

    private static final String EMAIL = "test@test.com";
    private static final Long USUARIO_ID = 1L;

    private Usuario buildUsuario() {
        return Usuario.builder()
                .usuarioId(USUARIO_ID)
                .usuarioEmail(EMAIL)
                .usuarioPassword("hash")
                .build();
    }

    private UsuarioRol buildRol(RolNombre nombre) {
        return UsuarioRol.builder()
                .usuarioId(USUARIO_ID)
                .rolNombre(nombre)
                .rolEstado(RolEstado.ACTIVO)
                .build();
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Cuando el usuario no existe")
    class UsuarioNoExiste {

        @Test
        @DisplayName("lanza UsernameNotFoundException")
        void usuarioNoExiste_lanzaUsernameNotFoundException() {
            when(usuarioRepository.findByUsuarioEmail("bad@test.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("bad@test.com"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Cuando el usuario existe pero no tiene roles activos")
    class UsuarioSinRolesActivos {

        @Test
        @DisplayName("lanza UsernameNotFoundException")
        void usuarioSinRolesActivos_lanzaUsernameNotFoundException() {
            Usuario usuario = buildUsuario();
            when(usuarioRepository.findByUsuarioEmail(EMAIL))
                    .thenReturn(Optional.of(usuario));
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(USUARIO_ID, RolEstado.ACTIVO))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(EMAIL))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Cuando el usuario tiene roles activos")
    class UsuarioConRolesActivos {

        @Test
        @DisplayName("retorna UserDetails con el email correcto")
        void usuarioConRolActivo_retornaUserDetailsConEmail() {
            Usuario usuario = buildUsuario();
            when(usuarioRepository.findByUsuarioEmail(EMAIL))
                    .thenReturn(Optional.of(usuario));
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(USUARIO_ID, RolEstado.ACTIVO))
                    .thenReturn(List.of(buildRol(RolNombre.CLIENTE)));

            UserDetails result = customUserDetailsService.loadUserByUsername(EMAIL);

            assertThat(result.getUsername()).isEqualTo(EMAIL);
        }

        @Test
        @DisplayName("retorna UserDetails con la authority correspondiente al rol")
        void usuarioConRolActivo_retornaUserDetailsConAuthority() {
            Usuario usuario = buildUsuario();
            when(usuarioRepository.findByUsuarioEmail(EMAIL))
                    .thenReturn(Optional.of(usuario));
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(USUARIO_ID, RolEstado.ACTIVO))
                    .thenReturn(List.of(buildRol(RolNombre.CLIENTE)));

            UserDetails result = customUserDetailsService.loadUserByUsername(EMAIL);

            String expectedAuthority = RoleMapper.toAuthority(RolNombre.CLIENTE);
            assertThat(result.getAuthorities())
                    .extracting("authority")
                    .contains(expectedAuthority);
        }

        @Test
        @DisplayName("con múltiples roles activos retorna todas las authorities")
        void usuarioConMultiplesRolesActivos_todasLasAuthorities() {
            Usuario usuario = buildUsuario();
            when(usuarioRepository.findByUsuarioEmail(EMAIL))
                    .thenReturn(Optional.of(usuario));
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(USUARIO_ID, RolEstado.ACTIVO))
                    .thenReturn(List.of(buildRol(RolNombre.CLIENTE), buildRol(RolNombre.CAJERO)));

            UserDetails result = customUserDetailsService.loadUserByUsername(EMAIL);

            assertThat(result.getAuthorities()).hasSize(2);
        }

        @Test
        @DisplayName("con roles duplicados no duplica la authority gracias a distinct()")
        void rolesDuplicados_authorityNoSeDuplica() {
            Usuario usuario = buildUsuario();
            when(usuarioRepository.findByUsuarioEmail(EMAIL))
                    .thenReturn(Optional.of(usuario));
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(USUARIO_ID, RolEstado.ACTIVO))
                    .thenReturn(List.of(buildRol(RolNombre.CLIENTE), buildRol(RolNombre.CLIENTE)));

            UserDetails result = customUserDetailsService.loadUserByUsername(EMAIL);

            assertThat(result.getAuthorities()).hasSize(1);
        }
    }
}
