package co.edu.unicauca.backend.modules.auth.service;

import co.edu.unicauca.backend.modules.auth.dto.request.LoginRequest;
import co.edu.unicauca.backend.modules.auth.dto.request.RefreshTokenRequest;
import co.edu.unicauca.backend.modules.auth.dto.response.AuthResponse;
import co.edu.unicauca.backend.modules.auth.entity.Sesion;
import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRepository;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRolRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.entity.UsuarioRol;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.entity.AuditableEntity;
import co.edu.unicauca.backend.shared.enums.RolEstado;
import co.edu.unicauca.backend.shared.enums.RolNombre;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    JwtTokenProvider jwtTokenProvider;

    @Mock
    UsuarioRepository usuarioRepository;

    @Mock
    UsuarioRolRepository usuarioRolRepository;

    @Mock
    SesionRepository sesionRepository;

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    EmpleadoRepository empleadoRepository;

    @Mock
    UserDetailsService userDetailsService;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AuthService authService;

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("Administrador con credenciales válidas y forceSessionOverride=true → login exitoso")
        void administradorCredencialesValidas_loginExitoso() throws Exception {
            Usuario admin = usuario(1L, "admin@altoro.com", "$2b$hash");
            Empleado empleado = Empleado.builder()
                    .usuarioId(1L)
                    .usuario(admin)
                    .empleadoNombre("Admin Principal")
                    .empleadoTelefono("3101234567")
                    .empleadoFechaIngreso(java.time.LocalDate.now())
                    .build();

            when(usuarioRepository.findByUsuarioEmail("admin@altoro.com")).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches("Al.Toro2026!", "$2b$hash")).thenReturn(true);
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(1L, RolEstado.ACTIVO))
                    .thenReturn(List.of(usuarioRol(1L, RolNombre.ADM, RolEstado.ACTIVO)));
            when(sesionRepository.findByUsuarioUsuarioIdAndSesionActivaTrue(1L))
                    .thenReturn(List.of(Sesion.builder().sesionId(99L).usuario(admin).sesionActiva(true).sesionToken("old").build()));

            UserDetails userDetails = User.withUsername("admin@altoro.com").password("ignored").roles("ADMIN").build();
            when(userDetailsService.loadUserByUsername("admin@altoro.com")).thenReturn(userDetails);
            when(jwtTokenProvider.generateToken(userDetails)).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(userDetails)).thenReturn("refresh-token");
            when(empleadoRepository.findByUsuario_UsuarioEmail("admin@altoro.com")).thenReturn(Optional.of(empleado));

            LoginRequest request = LoginRequest.builder()
                    .email("admin@altoro.com")
                    .password("Al.Toro2026!")
                    .forceSessionOverride(true)
                    .build();

            AuthResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getUser().getRole()).isEqualTo("ADMIN");
            assertThat(response.getUser().getFullName()).isEqualTo("Admin Principal");

            verify(sesionRepository).saveAll(anyList());
            verify(sesionRepository).save(any(Sesion.class));
        }

        @Test
        @DisplayName("Credenciales inválidas → 401 con mensaje genérico")
        void credencialesInvalidas_retorna401() throws Exception {
            Usuario admin = usuario(1L, "admin@altoro.com", "$2b$hash");
            when(usuarioRepository.findByUsuarioEmail("admin@altoro.com")).thenReturn(Optional.of(admin));
            when(passwordEncoder.matches("incorrecta", "$2b$hash")).thenReturn(false);

            LoginRequest request = LoginRequest.builder()
                    .email("admin@altoro.com")
                    .password("incorrecta")
                    .build();

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Credenciales incorrectas por favor veirfique su correo y/o contraseña");
        }

        @Test
        @DisplayName("Cuenta con roles inactivos → cuenta suspendida")
        void cuentaSuspendida_retorna403() throws Exception {
            Usuario usuario = usuario(11L, "cliente@altoro.com", "$2b$hash");

            when(usuarioRepository.findByUsuarioEmail("cliente@altoro.com")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("Al.Toro2026!", "$2b$hash")).thenReturn(true);
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(11L, RolEstado.ACTIVO)).thenReturn(List.of());

            LoginRequest request = LoginRequest.builder()
                    .email("cliente@altoro.com")
                    .password("Al.Toro2026!")
                    .build();

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Tu cuenta se encuentra suspendida. Por favor contacta al administrador");
        }

        @Test
        @DisplayName("Sesión activa y sin override → conflicto")
        void sesionActivaSinOverride_retorna409() throws Exception {
            Usuario usuario = usuario(4L, "mesero@altoro.com", "$2b$hash");

            when(usuarioRepository.findByUsuarioEmail("mesero@altoro.com")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("Al.Toro2026!", "$2b$hash")).thenReturn(true);
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(4L, RolEstado.ACTIVO))
                    .thenReturn(List.of(usuarioRol(4L, RolNombre.MESERO, RolEstado.ACTIVO)));
            when(sesionRepository.findByUsuarioUsuarioIdAndSesionActivaTrue(4L))
                    .thenReturn(List.of(Sesion.builder().sesionId(1L).usuario(usuario).sesionActiva(true).sesionToken("t").build()));

            LoginRequest request = LoginRequest.builder()
                    .email("mesero@altoro.com")
                    .password("Al.Toro2026!")
                    .forceSessionOverride(false)
                    .build();

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Ya tienes una sesión activa. ¿Deseas cerrar la otra sesión y continuar?");
        }
    }

    @Nested
    @DisplayName("refresh")
    class Refresh {

        @Test
        @DisplayName("Refresh válido con sesión activa → nuevo access token")
        void refreshValido_retornaNuevoToken() throws Exception {
            Usuario usuario = usuario(2L, "cajero@altoro.com", "$2b$hash");
            Empleado empleado = Empleado.builder()
                    .usuarioId(2L)
                    .usuario(usuario)
                    .empleadoNombre("Cajero Uno")
                    .empleadoTelefono("3109999999")
                    .empleadoFechaIngreso(java.time.LocalDate.now())
                    .build();

            when(jwtTokenProvider.extractUsername("refresh-ok")).thenReturn("cajero@altoro.com");
            when(usuarioRepository.findByUsuarioEmail("cajero@altoro.com")).thenReturn(Optional.of(usuario));
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(2L, RolEstado.ACTIVO))
                    .thenReturn(List.of(usuarioRol(2L, RolNombre.CAJERO, RolEstado.ACTIVO)));

            UserDetails userDetails = User.withUsername("cajero@altoro.com").password("x").roles("CAJERO").build();
            when(userDetailsService.loadUserByUsername("cajero@altoro.com")).thenReturn(userDetails);
            when(jwtTokenProvider.isTokenValid("refresh-ok", userDetails)).thenReturn(true);

            Sesion sesionActiva = Sesion.builder().sesionId(20L).usuario(usuario).sesionToken("old").sesionActiva(true).build();
            when(sesionRepository.findByUsuarioUsuarioIdAndSesionActivaTrue(2L)).thenReturn(List.of(sesionActiva));

            when(jwtTokenProvider.generateToken(userDetails)).thenReturn("new-access");
            when(jwtTokenProvider.generateRefreshToken(userDetails)).thenReturn("new-refresh");
            when(empleadoRepository.findByUsuario_UsuarioEmail("cajero@altoro.com")).thenReturn(Optional.of(empleado));

            AuthResponse response = authService.refresh(RefreshTokenRequest.builder().refreshToken("refresh-ok").build());

            assertThat(response.getAccessToken()).isEqualTo("new-access");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
            assertThat(response.getUser().getRole()).isEqualTo("CAJERO");

            ArgumentCaptor<List<Sesion>> sessionsCaptor = ArgumentCaptor.forClass(List.class);
            verify(sesionRepository).saveAll(sessionsCaptor.capture());
            assertThat(sessionsCaptor.getValue().get(0).getSesionToken()).isEqualTo("new-access");
        }
    }

    private Usuario usuario(Long id, String email, String passwordHash) throws Exception {
        Usuario usuario = Usuario.builder()
                .usuarioId(id)
                .usuarioEmail(email)
                .usuarioPassword(passwordHash)
                .build();
        setCreatedAt(usuario, LocalDateTime.of(2026, 1, 1, 10, 0));
        return usuario;
    }

    private UsuarioRol usuarioRol(Long usuarioId, RolNombre nombre, RolEstado estado) {
        return UsuarioRol.builder()
                .usuarioId(usuarioId)
                .rolNombre(nombre)
                .rolEstado(estado)
                .build();
    }

    private void setCreatedAt(AuditableEntity entity, LocalDateTime createdAt) throws Exception {
        Field field = AuditableEntity.class.getDeclaredField("createdAt");
        field.setAccessible(true);
        field.set(entity, createdAt);
    }
}
