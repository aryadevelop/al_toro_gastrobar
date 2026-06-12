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
                    .thenReturn(List.of(usuarioRol(1L, RolNombre.ADMIN, RolEstado.ACTIVO)));
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
            assertThat(response.getUser().getNombre()).isEqualTo("Admin Principal");

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
                    .hasMessage("Credenciales incorrectas por favor verifique su correo y/o contraseña");
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
        @DisplayName("Email inexistente → BadCredentialsException con mensaje genérico")
        void emailInexistente_retorna401() {
            when(usuarioRepository.findByUsuarioEmail("nadie@altoro.com")).thenReturn(Optional.empty());

            LoginRequest request = LoginRequest.builder()
                    .email("nadie@altoro.com")
                    .password("Al.Toro2026!")
                    .build();

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Credenciales incorrectas");
        }

        @Test
        @DisplayName("Empleado sin fila en tabla empleado → fallbackUser usa email como nombre")
        void empleadoSinPerfil_usaEmailComoNombre() throws Exception {
            Usuario usuario = usuario(50L, "huerfano@altoro.com", "$2b$hash");

            when(usuarioRepository.findByUsuarioEmail("huerfano@altoro.com")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("Al.Toro2026!", "$2b$hash")).thenReturn(true);
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(50L, RolEstado.ACTIVO))
                    .thenReturn(List.of(usuarioRol(50L, RolNombre.MESERO, RolEstado.ACTIVO)));
            when(sesionRepository.findByUsuarioUsuarioIdAndSesionActivaTrue(50L)).thenReturn(List.of());

            UserDetails userDetails = User.withUsername("huerfano@altoro.com").password("x").roles("MESERO").build();
            when(userDetailsService.loadUserByUsername("huerfano@altoro.com")).thenReturn(userDetails);
            when(jwtTokenProvider.generateToken(userDetails)).thenReturn("a");
            when(jwtTokenProvider.generateRefreshToken(userDetails)).thenReturn("r");
            when(empleadoRepository.findByUsuario_UsuarioEmail("huerfano@altoro.com")).thenReturn(Optional.empty());

            AuthResponse response = authService.login(LoginRequest.builder()
                    .email("huerfano@altoro.com").password("Al.Toro2026!").build());

            assertThat(response.getUser().getNombre()).isEqualTo("huerfano@altoro.com");
        }

        @Test
        @DisplayName("Cliente sin fila en tabla cliente → fallbackUser usa email como nombre")
        void clienteSinPerfil_usaEmailComoNombre() throws Exception {
            Usuario usuario = usuario(60L, "cliente.huerfano@altoro.com", "$2b$hash");

            when(usuarioRepository.findByUsuarioEmail("cliente.huerfano@altoro.com")).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("Al.Toro2026!", "$2b$hash")).thenReturn(true);
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(60L, RolEstado.ACTIVO))
                    .thenReturn(List.of(usuarioRol(60L, RolNombre.CLIENTE, RolEstado.ACTIVO)));
            when(sesionRepository.findByUsuarioUsuarioIdAndSesionActivaTrue(60L)).thenReturn(List.of());

            UserDetails userDetails = User.withUsername("cliente.huerfano@altoro.com").password("x").roles("CLIENTE").build();
            when(userDetailsService.loadUserByUsername("cliente.huerfano@altoro.com")).thenReturn(userDetails);
            when(jwtTokenProvider.generateToken(userDetails)).thenReturn("a");
            when(jwtTokenProvider.generateRefreshToken(userDetails)).thenReturn("r");
            when(clienteRepository.findByUsuario_UsuarioEmail("cliente.huerfano@altoro.com")).thenReturn(Optional.empty());

            AuthResponse response = authService.login(LoginRequest.builder()
                    .email("cliente.huerfano@altoro.com").password("Al.Toro2026!").build());

            assertThat(response.getUser().getNombre()).isEqualTo("cliente.huerfano@altoro.com");
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

            when(jwtTokenProvider.isRefreshToken("refresh-ok")).thenReturn(true);
            when(jwtTokenProvider.extractUsername("refresh-ok")).thenReturn("cajero@altoro.com");
            when(usuarioRepository.findByUsuarioEmail("cajero@altoro.com")).thenReturn(Optional.of(usuario));
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(2L, RolEstado.ACTIVO))
                    .thenReturn(List.of(usuarioRol(2L, RolNombre.CAJERO, RolEstado.ACTIVO)));

            UserDetails userDetails = User.withUsername("cajero@altoro.com").password("x").roles("CAJERO").build();
            when(userDetailsService.loadUserByUsername("cajero@altoro.com")).thenReturn(userDetails);
            when(jwtTokenProvider.isTokenValid("refresh-ok", userDetails)).thenReturn(true);

            Sesion sesionActiva = Sesion.builder()
                    .sesionId(20L).usuario(usuario)
                    .sesionToken("old").sesionActiva(true).build();
            when(sesionRepository.findBySesionRefreshTokenAndSesionActivaTrue("refresh-ok"))
                    .thenReturn(Optional.of(sesionActiva));

            when(jwtTokenProvider.generateToken(userDetails)).thenReturn("new-access");
            when(jwtTokenProvider.generateRefreshToken(userDetails)).thenReturn("new-refresh");
            when(empleadoRepository.findByUsuario_UsuarioEmail("cajero@altoro.com")).thenReturn(Optional.of(empleado));

            AuthResponse response = authService.refresh(
                    RefreshTokenRequest.builder().refreshToken("refresh-ok").build());

            assertThat(response.getAccessToken()).isEqualTo("new-access");
            assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
            assertThat(response.getUser().getRole()).isEqualTo("CAJERO");

            // El refresh actualiza la sesión existente in-place (save), no crea una nueva (saveAll)
            verify(sesionRepository).save(sesionActiva);
            assertThat(sesionActiva.getSesionToken()).isEqualTo("new-access");
            assertThat(sesionActiva.getSesionRefreshToken()).isEqualTo("new-refresh");
        }

        @Test
        @DisplayName("Token NO es refresh token → BadCredentialsException")
        void tokenNoEsRefresh_retorna401() {
            when(jwtTokenProvider.isRefreshToken("access-token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refresh(
                    RefreshTokenRequest.builder().refreshToken("access-token").build()))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Usuario del token ya no existe → BadCredentialsException")
        void usuarioInexistente_retorna401() {
            when(jwtTokenProvider.isRefreshToken("r")).thenReturn(true);
            when(jwtTokenProvider.extractUsername("r")).thenReturn("borrado@altoro.com");
            when(usuarioRepository.findByUsuarioEmail("borrado@altoro.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(
                    RefreshTokenRequest.builder().refreshToken("r").build()))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Usuario sin roles activos → 403 cuenta suspendida")
        void sinRolesActivos_retorna403() throws Exception {
            Usuario usuario = usuario(7L, "suspendido@altoro.com", "$2b$hash");
            when(jwtTokenProvider.isRefreshToken("r")).thenReturn(true);
            when(jwtTokenProvider.extractUsername("r")).thenReturn("suspendido@altoro.com");
            when(usuarioRepository.findByUsuarioEmail("suspendido@altoro.com")).thenReturn(Optional.of(usuario));
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(7L, RolEstado.ACTIVO)).thenReturn(List.of());

            assertThatThrownBy(() -> authService.refresh(
                    RefreshTokenRequest.builder().refreshToken("r").build()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("suspendida");
        }

        @Test
        @DisplayName("isTokenValid retorna false → BadCredentialsException")
        void tokenInvalido_retorna401() throws Exception {
            Usuario usuario = usuario(8L, "u@altoro.com", "$2b$hash");
            when(jwtTokenProvider.isRefreshToken("r")).thenReturn(true);
            when(jwtTokenProvider.extractUsername("r")).thenReturn("u@altoro.com");
            when(usuarioRepository.findByUsuarioEmail("u@altoro.com")).thenReturn(Optional.of(usuario));
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(8L, RolEstado.ACTIVO))
                    .thenReturn(List.of(usuarioRol(8L, RolNombre.MESERO, RolEstado.ACTIVO)));
            UserDetails ud = User.withUsername("u@altoro.com").password("x").roles("MESERO").build();
            when(userDetailsService.loadUserByUsername("u@altoro.com")).thenReturn(ud);
            when(jwtTokenProvider.isTokenValid("r", ud)).thenReturn(false);

            assertThatThrownBy(() -> authService.refresh(
                    RefreshTokenRequest.builder().refreshToken("r").build()))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("Sesión no encontrada en BD → BadCredentialsException")
        void sesionInexistente_retorna401() throws Exception {
            Usuario usuario = usuario(9L, "u2@altoro.com", "$2b$hash");
            when(jwtTokenProvider.isRefreshToken("r")).thenReturn(true);
            when(jwtTokenProvider.extractUsername("r")).thenReturn("u2@altoro.com");
            when(usuarioRepository.findByUsuarioEmail("u2@altoro.com")).thenReturn(Optional.of(usuario));
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(9L, RolEstado.ACTIVO))
                    .thenReturn(List.of(usuarioRol(9L, RolNombre.MESERO, RolEstado.ACTIVO)));
            UserDetails ud = User.withUsername("u2@altoro.com").password("x").roles("MESERO").build();
            when(userDetailsService.loadUserByUsername("u2@altoro.com")).thenReturn(ud);
            when(jwtTokenProvider.isTokenValid("r", ud)).thenReturn(true);
            when(sesionRepository.findBySesionRefreshTokenAndSesionActivaTrue("r")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.refresh(
                    RefreshTokenRequest.builder().refreshToken("r").build()))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("Registro válido con todos los campos → cliente creado exitosamente")
        void registroValido_clienteCreadoExitosamente() {
            co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest request =
                    co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest.builder()
                            .email("NUEVO@ALTORO.COM")
                            .nombre("Juan Pérez")
                            .telefono("3101234567")
                            .password("SecurePass123!")
                            .passwordConfirmation("SecurePass123!")
                            .aceptaTerminos(true)
                            .fechaNacimiento("1995-05-15")
                            .build();

            Usuario usuarioGuardado = Usuario.builder()
                    .usuarioId(99L)
                    .usuarioEmail("nuevo@altoro.com")
                    .usuarioPassword("$2b$hashed")
                    .build();

            when(usuarioRepository.findByUsuarioEmail("nuevo@altoro.com")).thenReturn(Optional.empty());
            when(clienteRepository.existsByClienteTelefono("3101234567")).thenReturn(false);
            when(passwordEncoder.encode("SecurePass123!")).thenReturn("$2b$hashed");
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);
            when(clienteRepository.save(any(co.edu.unicauca.backend.modules.usuarios.entity.Cliente.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));  // Retorna el cliente tal como se pasó
            when(usuarioRolRepository.save(any(UsuarioRol.class))).thenReturn(UsuarioRol.builder().build());

            co.edu.unicauca.backend.modules.auth.dto.response.RegisterResponse response = authService.register(request);

            assertThat(response.getSuccess()).isTrue();
            assertThat(response.getMessage()).contains("Tu cuenta ha sido creada");
            assertThat(response.getUser().getEmail()).isEqualTo("nuevo@altoro.com");
            assertThat(response.getUser().getNombre()).isEqualTo("Juan Pérez");
            assertThat(response.getUser().getRole()).isEqualTo("CLIENTE");

            verify(usuarioRepository).save(any(Usuario.class));
            verify(clienteRepository).save(any(co.edu.unicauca.backend.modules.usuarios.entity.Cliente.class));
            verify(usuarioRolRepository).save(any(UsuarioRol.class));
        }

        @Test
        @DisplayName("Contraseñas no coinciden → 400 BAD_REQUEST")
        void passwordsMismatch_retorna400() {
            co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest request =
                    co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest.builder()
                            .email("test@altoro.com")
                            .nombre("Test User")
                            .telefono("3109999999")
                            .password("Password123!")
                            .passwordConfirmation("DifferentPassword!")
                            .aceptaTerminos(true)
                            .build();

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Las contraseñas no coinciden");
        }

        @Test
        @DisplayName("Email duplicado → 409 CONFLICT")
        void emailDuplicado_retorna409() {
            co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest request =
                    co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest.builder()
                            .email("EXISTENTE@altoro.com")
                            .nombre("Test User")
                            .telefono("3109999999")
                            .password("Password123!")
                            .passwordConfirmation("Password123!")
                            .aceptaTerminos(true)
                            .build();

            when(usuarioRepository.findByUsuarioEmail("existente@altoro.com"))
                    .thenReturn(Optional.of(Usuario.builder().usuarioId(1L).build()));

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Este correo electrónico ya está registrado");
        }

        @Test
        @DisplayName("Teléfono duplicado → 409 CONFLICT")
        void telefonoDuplicado_retorna409() {
            co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest request =
                    co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest.builder()
                            .email("nuevo@altoro.com")
                            .nombre("Test User")
                            .telefono("3101234567")
                            .password("Password123!")
                            .passwordConfirmation("Password123!")
                            .aceptaTerminos(true)
                            .build();

            when(usuarioRepository.findByUsuarioEmail("nuevo@altoro.com")).thenReturn(Optional.empty());
            when(clienteRepository.existsByClienteTelefono("3101234567")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Este número de teléfono ya está registrado");
        }

        @Test
        @DisplayName("Fecha nacimiento inválida → 400 BAD_REQUEST")
        void fechaNacimientoInvalida_retorna400() {
            co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest request =
                    co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest.builder()
                            .email("nuevo@altoro.com")
                            .nombre("Test User")
                            .telefono("3109999999")
                            .password("Password123!")
                            .passwordConfirmation("Password123!")
                            .aceptaTerminos(true)
                            .fechaNacimiento("15/05/1995")  // Formato incorrecto
                            .build();

            when(usuarioRepository.findByUsuarioEmail("nuevo@altoro.com")).thenReturn(Optional.empty());
            when(clienteRepository.existsByClienteTelefono("3109999999")).thenReturn(false);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("El formato de la fecha debe ser YYYY-MM-DD");
        }

        @Test
        @DisplayName("Fecha nacimiento más de 100 años atrás → 400 BAD_REQUEST")
        void fechaNacimientoMuyVieja_retorna400() {
            String fechaVieja = java.time.LocalDate.now().minusYears(101).toString();
            co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest request =
                    co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest.builder()
                            .email("nuevo@altoro.com")
                            .nombre("Test User")
                            .telefono("3109999999")
                            .password("Password123!")
                            .passwordConfirmation("Password123!")
                            .aceptaTerminos(true)
                            .fechaNacimiento(fechaVieja)
                            .build();

            when(usuarioRepository.findByUsuarioEmail("nuevo@altoro.com")).thenReturn(Optional.empty());
            when(clienteRepository.existsByClienteTelefono("3109999999")).thenReturn(false);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("100 años");
        }

        @Test
        @DisplayName("Fecha nacimiento en el futuro → 400 BAD_REQUEST")
        void fechaNacimientoFutura_retorna400() {
            String fechaFutura = java.time.LocalDate.now().plusDays(1).toString();
            co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest request =
                    co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest.builder()
                            .email("nuevo@altoro.com")
                            .nombre("Test User")
                            .telefono("3109999999")
                            .password("Password123!")
                            .passwordConfirmation("Password123!")
                            .aceptaTerminos(true)
                            .fechaNacimiento(fechaFutura)
                            .build();

            when(usuarioRepository.findByUsuarioEmail("nuevo@altoro.com")).thenReturn(Optional.empty());
            when(clienteRepository.existsByClienteTelefono("3109999999")).thenReturn(false);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("100 años");
        }

        @Test
        @DisplayName("fechaNacimiento blank → registro exitoso ignorando el campo")
        void fechaNacimientoBlank_registroExitoso() {
            co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest request =
                    co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest.builder()
                            .email("blank@altoro.com")
                            .nombre("Test")
                            .telefono("3105555555")
                            .password("Password123!")
                            .passwordConfirmation("Password123!")
                            .aceptaTerminos(true)
                            .fechaNacimiento("   ")
                            .build();

            when(usuarioRepository.findByUsuarioEmail("blank@altoro.com")).thenReturn(Optional.empty());
            when(clienteRepository.existsByClienteTelefono("3105555555")).thenReturn(false);
            when(passwordEncoder.encode("Password123!")).thenReturn("$2b$h");
            when(usuarioRepository.save(any(Usuario.class)))
                    .thenReturn(Usuario.builder().usuarioId(77L).usuarioEmail("blank@altoro.com").build());
            when(clienteRepository.save(any(co.edu.unicauca.backend.modules.usuarios.entity.Cliente.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(usuarioRolRepository.save(any(UsuarioRol.class))).thenReturn(UsuarioRol.builder().build());

            assertThat(authService.register(request).getSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("me")
    class Me {

        @Test
        @DisplayName("Usuario válido con roles activos → retorna perfil completo")
        void usuarioValido_retornaPerfil() throws Exception {
            Usuario cliente = usuario(11L, "cliente1@altoro.com", "$2b$hash");
            co.edu.unicauca.backend.modules.usuarios.entity.Cliente clienteEntity =
                    co.edu.unicauca.backend.modules.usuarios.entity.Cliente.builder()
                            .usuarioId(11L)
                            .usuario(cliente)
                            .clienteNombre("Cliente Uno")
                            .clienteTelefono("3101111111")
                            .clientePuntos(10)
                            .build();

            when(usuarioRepository.findByUsuarioEmail("cliente1@altoro.com")).thenReturn(Optional.of(cliente));
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(11L, RolEstado.ACTIVO))
                    .thenReturn(List.of(usuarioRol(11L, RolNombre.CLIENTE, RolEstado.ACTIVO)));
            when(clienteRepository.findByUsuario_UsuarioEmail("cliente1@altoro.com"))
                    .thenReturn(Optional.of(clienteEntity));

            co.edu.unicauca.backend.modules.auth.dto.response.AuthUserResponse response =
                    authService.me("cliente1@altoro.com");

            assertThat(response.getEmail()).isEqualTo("cliente1@altoro.com");
            assertThat(response.getNombre()).isEqualTo("Cliente Uno");
            assertThat(response.getRole()).isEqualTo("CLIENTE");
            assertThat(response.getEstaciones()).isNull();
        }

        @Test
        @DisplayName("Usuario COCINERO + BARTENDER → estaciones=[BARRA, COCINA]")
        void usuarioConRolesDeProduccion_devuelveEstaciones() throws Exception {
            Usuario user = usuario(33L, "produccion@altoro.com", "$2b$hash");
            Empleado empleado = Empleado.builder()
                    .usuarioId(33L)
                    .usuario(user)
                    .empleadoNombre("Mixto")
                    .empleadoTelefono("3100000000")
                    .empleadoFechaIngreso(java.time.LocalDate.now())
                    .build();

            when(usuarioRepository.findByUsuarioEmail("produccion@altoro.com")).thenReturn(Optional.of(user));
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(33L, RolEstado.ACTIVO))
                    .thenReturn(List.of(
                            usuarioRol(33L, RolNombre.COCINERO, RolEstado.ACTIVO),
                            usuarioRol(33L, RolNombre.BARTENDER, RolEstado.ACTIVO)));
            when(empleadoRepository.findByUsuario_UsuarioEmail("produccion@altoro.com"))
                    .thenReturn(Optional.of(empleado));

            co.edu.unicauca.backend.modules.auth.dto.response.AuthUserResponse response =
                    authService.me("produccion@altoro.com");

            assertThat(response.getEstaciones()).containsExactly("BARRA", "COCINA");
        }

        @Test
        @DisplayName("Usuario no existe → BadCredentialsException")
        void usuarioNoExiste_lanzaExcepcion() {
            when(usuarioRepository.findByUsuarioEmail("noexiste@altoro.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.me("noexiste@altoro.com"))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Credenciales incorrectas");
        }

        @Test
        @DisplayName("Usuario sin roles activos → 403 FORBIDDEN")
        void usuarioSinRolesActivos_retorna403() throws Exception {
            Usuario usuario = usuario(15L, "suspendido@altoro.com", "$2b$hash");

            when(usuarioRepository.findByUsuarioEmail("suspendido@altoro.com")).thenReturn(Optional.of(usuario));
            when(usuarioRolRepository.findByUsuarioIdAndRolEstado(15L, RolEstado.ACTIVO))
                    .thenReturn(List.of());  // Sin roles activos

            assertThatThrownBy(() -> authService.me("suspendido@altoro.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Tu cuenta se encuentra suspendida");
        }
    }

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("Logout exitoso → invalida todas las sesiones activas del usuario")
        void logoutExitoso_invalidaSesiones() throws Exception {
            Usuario usuario = usuario(5L, "mesero1@altoro.com", "$2b$hash");

            Sesion sesion1 = Sesion.builder().sesionId(10L).usuario(usuario).sesionActiva(true).build();
            Sesion sesion2 = Sesion.builder().sesionId(11L).usuario(usuario).sesionActiva(true).build();

            when(usuarioRepository.findByUsuarioEmail("mesero1@altoro.com")).thenReturn(Optional.of(usuario));
            when(sesionRepository.findByUsuarioUsuarioIdAndSesionActivaTrue(5L))
                    .thenReturn(List.of(sesion1, sesion2));

            authService.logout("mesero1@altoro.com");

            assertThat(sesion1.getSesionActiva()).isFalse();
            assertThat(sesion2.getSesionActiva()).isFalse();
            verify(sesionRepository).saveAll(List.of(sesion1, sesion2));
        }

        @Test
        @DisplayName("Usuario no existe → BadCredentialsException")
        void usuarioNoExiste_lanzaExcepcion() {
            when(usuarioRepository.findByUsuarioEmail("noexiste@altoro.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.logout("noexiste@altoro.com"))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Credenciales incorrectas");
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
