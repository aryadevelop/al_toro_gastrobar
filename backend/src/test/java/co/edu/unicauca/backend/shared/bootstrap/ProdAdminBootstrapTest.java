package co.edu.unicauca.backend.shared.bootstrap;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRepository;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRolRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.UsuarioRol;
import co.edu.unicauca.backend.shared.enums.RolEstado;
import co.edu.unicauca.backend.shared.enums.RolNombre;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdAdminBootstrapTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioRolRepository usuarioRolRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private ProdAdminBootstrap bootstrap;

    private void setCreds(String email, String password) {
        ReflectionTestUtils.setField(bootstrap, "adminEmail", email);
        ReflectionTestUtils.setField(bootstrap, "adminPassword", password);
    }

    @Test
    void sinCredenciales_noHaceNada() {
        setCreds("", "");
        bootstrap.run(null);
        verify(usuarioRepository, never()).save(any());
        verify(usuarioRolRepository, never()).save(any());
    }

    @Test
    void yaExisteAdminActivo_noCreaNada() {
        setCreds("admin@altoro.com", "secret");
        when(usuarioRolRepository.existsByRolNombreAndRolEstado(RolNombre.ADMIN, RolEstado.ACTIVO))
                .thenReturn(true);
        bootstrap.run(null);
        verify(usuarioRepository, never()).save(any());
        verify(usuarioRolRepository, never()).save(any());
    }

    @Test
    void sinAdmin_creaUsuarioYRolAdmin() {
        setCreds("Admin@Altoro.com", "secret");
        when(usuarioRolRepository.existsByRolNombreAndRolEstado(RolNombre.ADMIN, RolEstado.ACTIVO))
                .thenReturn(false);
        when(usuarioRepository.findByUsuarioEmail("admin@altoro.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(inv -> { Usuario u = inv.getArgument(0); u.setUsuarioId(1L); return u; });

        bootstrap.run(null);

        verify(usuarioRepository).save(any(Usuario.class));
        verify(usuarioRolRepository).save(any(UsuarioRol.class));
    }

    @Test
    void usuarioExisteSinRolAdmin_reutilizaUsuarioYAgregaRol() {
        setCreds("admin@altoro.com", "secret");
        Usuario existente = Usuario.builder().usuarioEmail("admin@altoro.com").build();
        existente.setUsuarioId(7L);
        when(usuarioRolRepository.existsByRolNombreAndRolEstado(RolNombre.ADMIN, RolEstado.ACTIVO))
                .thenReturn(false);
        when(usuarioRepository.findByUsuarioEmail("admin@altoro.com")).thenReturn(Optional.of(existente));

        bootstrap.run(null);

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(usuarioRolRepository).save(any(UsuarioRol.class));
    }
}
