package co.edu.unicauca.backend.modules.auth.service;

import co.edu.unicauca.backend.modules.auth.dto.request.LoginRequest;
import co.edu.unicauca.backend.modules.auth.dto.request.RefreshTokenRequest;
import co.edu.unicauca.backend.modules.auth.dto.response.AuthResponse;
import co.edu.unicauca.backend.modules.auth.dto.response.AuthUserResponse;
import co.edu.unicauca.backend.modules.auth.entity.Sesion;
import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRepository;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRolRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.entity.UsuarioRol;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.enums.RolEstado;
import co.edu.unicauca.backend.shared.enums.RolNombre;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.security.RoleMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Servicio de negocio para los flujos de autenticación JWT.
 *
 * <p>Centraliza la lógica de login, rotación de tokens, consulta de perfil
 * y cierre de sesión. Trabaja con {@link JwtTokenProvider} para emitir y
 * validar tokens, y con {@link SesionRepository} para controlar la unicidad
 * de sesiones activas por usuario.
 *
 * <p>Responsabilidades principales:
 * <ul>
 *   <li><b>Login:</b> verifica credenciales, controla sesiones concurrentes y
 *       emite un par de tokens (access + refresh) registrando la sesión.</li>
 *   <li><b>Refresh:</b> valida el refresh token contra la sesión activa y rota
 *       ambos tokens sin requerir contraseña.</li>
 *   <li><b>Me:</b> resuelve el perfil del usuario (Cliente o Empleado) a partir
 *       del email extraído del token de acceso.</li>
 *   <li><b>Logout:</b> marca como inactivas todas las sesiones del usuario.</li>
 * </ul>
 *
 * @see JwtTokenProvider
 * @see co.edu.unicauca.backend.modules.auth.security.JwtAuthenticationFilter
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    /** Mensaje genérico para no revelar si el email existe en el sistema. */
    private static final String LOGIN_CREDENTIALS_MESSAGE = "Credenciales incorrectas por favor verifique su correo y/o contraseña";

    /** Mensaje cuando todos los roles activos del usuario están suspendidos. */
    private static final String ACCOUNT_SUSPENDED_MESSAGE = "Tu cuenta se encuentra suspendida. Por favor contacta al administrador";

    /** Mensaje cuando el usuario ya tiene una sesión abierta en otro dispositivo. */
    private static final String ACTIVE_SESSION_MESSAGE = "Ya tienes una sesión activa. ¿Deseas cerrar la otra sesión y continuar?";

    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final SesionRepository sesionRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Autentica al usuario y devuelve un par de tokens JWT con su perfil.
     *
     * <p>Flujo de ejecución:
     * <ol>
     *   <li>Normaliza el email a minúsculas y busca el usuario en base de datos.</li>
     *   <li>Verifica la contraseña contra el hash almacenado.</li>
     *   <li>Comprueba que el usuario tenga al menos un rol activo.</li>
     *   <li>Detecta sesiones activas previas; si las hay y no se fuerza override,
     *       lanza {@code 409 Conflict}.</li>
     *   <li>Invalida sesiones previas si {@code forceSessionOverride = true}.</li>
     *   <li>Genera access token y refresh token, persiste la sesión y construye la respuesta.</li>
     * </ol>
     *
     * @param request credenciales de acceso (email, password, flag de override)
     * @return {@link AuthResponse} con access token, refresh token y perfil del usuario
     * @throws BadCredentialsException si email o contraseña son incorrectos
     * @throws BusinessException       si la cuenta está suspendida ({@code 403}) o hay
     *                                 sesión activa sin override ({@code 409})
     */
    public AuthResponse login(LoginRequest request) {
        // Normaliza a minúsculas para evitar duplicados por capitalización
        String email = normalizeEmail(request.getEmail());

        // Busca el usuario; usa BadCredentials para no revelar si el email existe
        Usuario usuario = usuarioRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new BadCredentialsException(LOGIN_CREDENTIALS_MESSAGE));

        // Verifica la contraseña contra el hash BCrypt almacenado
        if (!passwordEncoder.matches(request.getPassword(), usuario.getUsuarioPassword())) {
            throw new BadCredentialsException(LOGIN_CREDENTIALS_MESSAGE);
        }

        // El usuario debe tener al menos un rol activo para poder ingresar
        List<UsuarioRol> activeRoles = usuarioRolRepository.findByUsuarioIdAndRolEstado(usuario.getUsuarioId(), RolEstado.ACTIVO);
        if (activeRoles.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, ACCOUNT_SUSPENDED_MESSAGE, HttpStatus.FORBIDDEN);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        RolNombre primaryRol = resolvePrimaryRole(activeRoles);

        // Controla sesiones concurrentes: invalida previas o lanza conflicto según flag
        gestionarSesion(usuario, Boolean.TRUE.equals(request.getForceSessionOverride()));

        // Genera los tokens y persiste la nueva sesión
        String accessToken = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        crearNuevaSesion(usuario, accessToken, refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationSeconds())
                .user(buildUserResponse(usuario, primaryRol, activeRoles))
                .build();
    }

    /**
     * Rota el par de tokens usando un refresh token válido y activo.
     *
     * <p>La rotación invalida el refresh token anterior y emite uno nuevo,
     * evitando la reutilización del token comprometido (token rotation pattern).
     *
     * @param request cuerpo con el refresh token a rotar
     * @return {@link AuthResponse} con el nuevo par de tokens y el perfil actualizado
     * @throws BadCredentialsException si el token no es un refresh token, la firma es
     *                                 inválida, expiró o la sesión ya no está activa
     * @throws BusinessException       si la cuenta del usuario fue suspendida ({@code 403})
     */
    public AuthResponse refresh(RefreshTokenRequest request) {
        // Rechaza tokens de acceso enviados por error en lugar del refresh token
        if (!jwtTokenProvider.isRefreshToken(request.getRefreshToken())) {
            throw new BadCredentialsException(LOGIN_CREDENTIALS_MESSAGE);
        }

        // Extrae el usuario del token y verifica que siga existiendo
        String username = jwtTokenProvider.extractUsername(request.getRefreshToken());
        Usuario usuario = usuarioRepository.findByUsuarioEmail(username)
                .orElseThrow(() -> new BadCredentialsException(LOGIN_CREDENTIALS_MESSAGE));

        // Verifica que el usuario siga con roles activos
        List<UsuarioRol> activeRoles = usuarioRolRepository.findByUsuarioIdAndRolEstado(usuario.getUsuarioId(), RolEstado.ACTIVO);
        if (activeRoles.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, ACCOUNT_SUSPENDED_MESSAGE, HttpStatus.FORBIDDEN);
        }

        // Valida firma y expiración del token contra los detalles del usuario
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtTokenProvider.isTokenValid(request.getRefreshToken(), userDetails)) {
            throw new BadCredentialsException(LOGIN_CREDENTIALS_MESSAGE);
        }

        // Busca la sesión activa asociada al refresh token para actualizarla in-place
        Sesion sesion = sesionRepository.findBySesionRefreshTokenAndSesionActivaTrue(request.getRefreshToken())
                .orElseThrow(() -> new BadCredentialsException(LOGIN_CREDENTIALS_MESSAGE));

        // Genera los nuevos tokens y actualiza la sesión existente (no crea una nueva)
        String accessToken = jwtTokenProvider.generateToken(userDetails);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        sesion.setSesionToken(accessToken);
        sesion.setSesionRefreshToken(newRefreshToken);
        sesion.setSesionFechaCreacion(LocalDateTime.now());
        sesionRepository.save(sesion);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationSeconds())
                .user(buildUserResponse(usuario, resolvePrimaryRole(activeRoles), activeRoles))
                .build();
    }

    /**
     * Devuelve el perfil del usuario identificado por su email.
     *
     * @param email correo del usuario extraído del access token por el filtro JWT
     * @return {@link AuthUserResponse} con el perfil actualizado del usuario
     * @throws BadCredentialsException si el usuario no existe
     * @throws BusinessException       si la cuenta está suspendida ({@code 403})
     */
    public AuthUserResponse me(String email) {
        Usuario usuario = usuarioRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new BadCredentialsException(LOGIN_CREDENTIALS_MESSAGE));
        List<UsuarioRol> activeRoles = usuarioRolRepository.findByUsuarioIdAndRolEstado(usuario.getUsuarioId(), RolEstado.ACTIVO);
        if (activeRoles.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, ACCOUNT_SUSPENDED_MESSAGE, HttpStatus.FORBIDDEN);
        }
        return buildUserResponse(usuario, resolvePrimaryRole(activeRoles), activeRoles);
    }

    /**
     * Invalida todas las sesiones activas del usuario identificado por su email.
     *
     * <p>Los tokens emitidos para esas sesiones dejan de ser aceptados en el siguiente
     * request, ya que el filtro JWT verifica la existencia de una sesión activa.
     *
     * @param email correo del usuario extraído del access token por el filtro JWT
     * @throws BadCredentialsException si el usuario no existe
     */
    public void logout(String email) {
        Usuario usuario = usuarioRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new BadCredentialsException(LOGIN_CREDENTIALS_MESSAGE));
        // Invalida todas las sesiones activas del usuario de forma masiva
        List<Sesion> activeSessions = sesionRepository.findByUsuarioUsuarioIdAndSesionActivaTrue(usuario.getUsuarioId());
        activeSessions.forEach(session -> session.setSesionActiva(false));
        sesionRepository.saveAll(activeSessions);
    }

    // -----------------------------------------------------------------------
    // Helpers privados
    // -----------------------------------------------------------------------

    /**
     * Controla las sesiones concurrentes del usuario durante el inicio de sesión.
     *
     * <p>Si el usuario tiene sesiones activas y {@code forceOverride} es {@code false},
     * lanza excepción de conflicto. Si es {@code true}, invalida todas las sesiones previas.</p>
     *
     * @param usuario       usuario que inicia sesión
     * @param forceOverride si {@code true}, las sesiones activas existentes se invalidan
     * @throws BusinessException (HTTP 409) si hay sesión activa y no se fuerza el reemplazo
     */
    private void gestionarSesion(Usuario usuario, boolean forceOverride) {
        // Busca sesiones activas del usuario por su ID
        List<Sesion> activeSessions = sesionRepository.findByUsuarioUsuarioIdAndSesionActivaTrue(usuario.getUsuarioId());
        if (!activeSessions.isEmpty()) {
            // Si no se pidió override, rechazar con conflicto para que el cliente confirme
            if (!forceOverride) {
                throw new BusinessException(ErrorCode.INVALID_STATE, ACTIVE_SESSION_MESSAGE, HttpStatus.CONFLICT);
            }
            // Si se fuerza el reemplazo, cierra las sesiones anteriores
            activeSessions.forEach(session -> session.setSesionActiva(false));
            sesionRepository.saveAll(activeSessions);
        }
    }

    /**
     * Crea y persiste una nueva sesión activa para el usuario con los tokens generados.
     *
     * @param usuario      usuario propietario de la nueva sesión
     * @param accessToken  token de acceso generado para esta sesión
     * @param refreshToken token de refresco generado para esta sesión
     */
    private void crearNuevaSesion(Usuario usuario, String accessToken, String refreshToken) {
        // Construye la sesión con ambos tokens y la marca como activa
        sesionRepository.save(Sesion.builder()
                .usuario(usuario)
                .sesionToken(accessToken)
                .sesionRefreshToken(refreshToken)
                .sesionFechaCreacion(LocalDateTime.now())
                .sesionActiva(true)
                .build());
    }

    /**
     * Construye el DTO de perfil consultando la tabla de Cliente o Empleado
     * según el rol primario resuelto. Si el perfil extendido no existe, usa
     * el email como nombre de fallback.
     *
     * @param usuario     entidad de autenticación base
     * @param primaryRole rol de mayor prioridad del usuario
     * @param activeRoles todos los roles activos (para la lista {@code roles})
     * @return {@link AuthUserResponse} con nombre, email, roles y estado del usuario
     */
    private AuthUserResponse buildUserResponse(Usuario usuario, RolNombre primaryRole, List<UsuarioRol> activeRoles) {
        String role = RoleMapper.toFrontendRole(primaryRole);
        // Convierte todos los roles a nombres de frontend, elimina duplicados y ordena
        List<String> roles = activeRoles.stream()
                .map(UsuarioRol::getRolNombre)
                .map(RoleMapper::toFrontendRole)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());

        // Los clientes tienen su nombre en la tabla cliente, no en la de usuario
        if (primaryRole == RolNombre.CLIENTE) {
            Optional<Cliente> cliente = clienteRepository.findByUsuario_UsuarioEmail(usuario.getUsuarioEmail());
            return cliente.map(value -> AuthUserResponse.builder()
                    .id(String.valueOf(usuario.getUsuarioId()))
                    .nombre(value.getClienteNombre())
                    .email(usuario.getUsuarioEmail())
                    .roles(roles)
                    .status("ACTIVE")
                    .createdAt(usuario.getCreatedAt())
                    .build()).orElseGet(() -> fallbackUser(usuario, role, roles));
        }

        // Los empleados (CAJERO, MESERO, ADMIN) tienen su nombre en la tabla empleado
        Optional<Empleado> empleado = empleadoRepository.findByUsuario_UsuarioEmail(usuario.getUsuarioEmail());
        return empleado.map(value -> AuthUserResponse.builder()
                .id(String.valueOf(usuario.getUsuarioId()))
                .nombre(value.getEmpleadoNombre())
                .email(usuario.getUsuarioEmail())
                .role(role)
                .roles(roles)
                .status("ACTIVE")
                .createdAt(usuario.getCreatedAt())
                .build()).orElseGet(() -> fallbackUser(usuario, role, roles));
    }

    /**
     * Construye un perfil mínimo cuando no existe fila en la tabla de perfil
     * extendido (cliente o empleado). Usa el email como nombre de presentación.
     *
     * @param usuario entidad de autenticación base
     * @param role    rol principal en formato frontend
     * @param roles   todos los roles en formato frontend
     * @return {@link AuthUserResponse} con datos mínimos del usuario
     */
    private AuthUserResponse fallbackUser(Usuario usuario, String role, List<String> roles) {
        return AuthUserResponse.builder()
                .id(String.valueOf(usuario.getUsuarioId()))
                .nombre(usuario.getUsuarioEmail())
                .email(usuario.getUsuarioEmail())
                .role(role)
                .roles(roles)
                .status("ACTIVE")
                .createdAt(usuario.getCreatedAt())
                .build();
    }

    /**
     * Resuelve el rol de mayor prioridad entre los roles activos del usuario.
     *
     * <p>El orden de prioridad lo define {@link RoleMapper#priorityComparator()}.
     * Típicamente: {@code ADMIN > CAJERO > MESERO > CLIENTE}.
     *
     * @param activeRoles roles activos del usuario (no vacío)
     * @return el {@link RolNombre} de mayor prioridad
     * @throws BusinessException si la lista está vacía (no debería ocurrir en condiciones normales)
     */
    private RolNombre resolvePrimaryRole(List<UsuarioRol> activeRoles) {
        return activeRoles.stream()
                .map(UsuarioRol::getRolNombre)
                .sorted(RoleMapper.priorityComparator())
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE, ACCOUNT_SUSPENDED_MESSAGE, HttpStatus.FORBIDDEN));
    }

    /**
     * Normaliza el email a minúsculas y elimina espacios extremos para evitar
     * duplicados por diferencias de capitalización.
     *
     * @param email email tal como lo envía el cliente; puede ser {@code null}
     * @return email normalizado, o {@code null} si la entrada era {@code null}
     */
    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}