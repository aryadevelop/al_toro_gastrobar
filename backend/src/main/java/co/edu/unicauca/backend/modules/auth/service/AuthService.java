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

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final String LOGIN_CREDENTIALS_MESSAGE = "Credenciales incorrectas por favor verifique su correo y/o contraseña";
    private static final String ACCOUNT_SUSPENDED_MESSAGE = "Tu cuenta se encuentra suspendida. Por favor contacta al administrador";
    private static final String ACTIVE_SESSION_MESSAGE = "Ya tienes una sesión activa. ¿Deseas cerrar la otra sesión y continuar?";

    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final SesionRepository sesionRepository;
    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        Usuario usuario = usuarioRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new BadCredentialsException(LOGIN_CREDENTIALS_MESSAGE));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getUsuarioPassword())) {
            throw new BadCredentialsException(LOGIN_CREDENTIALS_MESSAGE);
        }

        List<UsuarioRol> activeRoles = usuarioRolRepository.findByUsuarioIdAndRolEstado(usuario.getUsuarioId(), RolEstado.ACTIVO);
        if (activeRoles.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, ACCOUNT_SUSPENDED_MESSAGE, HttpStatus.FORBIDDEN);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        RolNombre primaryRol = resolvePrimaryRole(activeRoles);

        List<Sesion> activeSessions = sesionRepository.findByUsuarioUsuarioIdAndSesionActivaTrue(usuario.getUsuarioId());
        if (!activeSessions.isEmpty()) {
            if (!Boolean.TRUE.equals(request.getForceSessionOverride())) {
                throw new BusinessException(ErrorCode.INVALID_STATE, ACTIVE_SESSION_MESSAGE, HttpStatus.CONFLICT);
            }
            activeSessions.forEach(session -> session.setSesionActiva(false));
            sesionRepository.saveAll(activeSessions);
        }

        String accessToken = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        sesionRepository.save(Sesion.builder()
                .usuario(usuario)
                .sesionToken(accessToken)
                .sesionRefreshToken(refreshToken)
                .sesionFechaCreacion(LocalDateTime.now())
                .sesionActiva(true)
                .build());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationSeconds())
                .user(buildUserResponse(usuario, primaryRol, activeRoles))
                .build();
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        if (!jwtTokenProvider.isRefreshToken(request.getRefreshToken())) {
            throw new BadCredentialsException(LOGIN_CREDENTIALS_MESSAGE);
        }

        String username = jwtTokenProvider.extractUsername(request.getRefreshToken());
        Usuario usuario = usuarioRepository.findByUsuarioEmail(username)
                .orElseThrow(() -> new BadCredentialsException(LOGIN_CREDENTIALS_MESSAGE));

        List<UsuarioRol> activeRoles = usuarioRolRepository.findByUsuarioIdAndRolEstado(usuario.getUsuarioId(), RolEstado.ACTIVO);
        if (activeRoles.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, ACCOUNT_SUSPENDED_MESSAGE, HttpStatus.FORBIDDEN);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtTokenProvider.isTokenValid(request.getRefreshToken(), userDetails)) {
            throw new BadCredentialsException(LOGIN_CREDENTIALS_MESSAGE);
        }

        Sesion sesion = sesionRepository.findBySesionRefreshTokenAndSesionActivaTrue(request.getRefreshToken())
                .orElseThrow(() -> new BadCredentialsException(LOGIN_CREDENTIALS_MESSAGE));

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

    public AuthUserResponse me(String email) {
        Usuario usuario = usuarioRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new BadCredentialsException(LOGIN_CREDENTIALS_MESSAGE));
        List<UsuarioRol> activeRoles = usuarioRolRepository.findByUsuarioIdAndRolEstado(usuario.getUsuarioId(), RolEstado.ACTIVO);
        if (activeRoles.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_STATE, ACCOUNT_SUSPENDED_MESSAGE, HttpStatus.FORBIDDEN);
        }
        return buildUserResponse(usuario, resolvePrimaryRole(activeRoles), activeRoles);
    }

    public void logout(String email) {
        Usuario usuario = usuarioRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new BadCredentialsException(LOGIN_CREDENTIALS_MESSAGE));
        List<Sesion> activeSessions = sesionRepository.findByUsuarioUsuarioIdAndSesionActivaTrue(usuario.getUsuarioId());
        activeSessions.forEach(session -> session.setSesionActiva(false));
        sesionRepository.saveAll(activeSessions);
    }

    private AuthUserResponse buildUserResponse(Usuario usuario, RolNombre primaryRole, List<UsuarioRol> activeRoles) {
        String role = RoleMapper.toFrontendRole(primaryRole);
        List<String> roles = activeRoles.stream()
                .map(UsuarioRol::getRolNombre)
                .map(RoleMapper::toFrontendRole)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());

        if (primaryRole == RolNombre.CLIENTE) {
            Optional<Cliente> cliente = clienteRepository.findByUsuario_UsuarioEmail(usuario.getUsuarioEmail());
            return cliente.map(value -> AuthUserResponse.builder()
                    .id(String.valueOf(usuario.getUsuarioId()))
                    .fullName(value.getClienteNombre())
                    .email(usuario.getUsuarioEmail())
                    .phone(value.getClienteTelefono())
                    .role(role)
                    .roles(roles)
                    .status("ACTIVE")
                    .avatarUrl(null)
                    .createdAt(usuario.getCreatedAt())
                    .build()).orElseGet(() -> fallbackUser(usuario, role, roles));
        }

        Optional<Empleado> empleado = empleadoRepository.findByUsuario_UsuarioEmail(usuario.getUsuarioEmail());
        return empleado.map(value -> AuthUserResponse.builder()
                .id(String.valueOf(usuario.getUsuarioId()))
                .fullName(value.getEmpleadoNombre())
                .email(usuario.getUsuarioEmail())
                .phone(value.getEmpleadoTelefono())
                .role(role)
                .roles(roles)
                .status("ACTIVE")
                .avatarUrl(null)
                .createdAt(usuario.getCreatedAt())
                .build()).orElseGet(() -> fallbackUser(usuario, role, roles));
    }

    private AuthUserResponse fallbackUser(Usuario usuario, String role, List<String> roles) {
        return AuthUserResponse.builder()
                .id(String.valueOf(usuario.getUsuarioId()))
                .fullName(usuario.getUsuarioEmail())
                .email(usuario.getUsuarioEmail())
                .phone(null)
                .role(role)
                .roles(roles)
                .status("ACTIVE")
                .avatarUrl(null)
                .createdAt(usuario.getCreatedAt())
                .build();
    }

    private RolNombre resolvePrimaryRole(List<UsuarioRol> activeRoles) {
        return activeRoles.stream()
                .map(UsuarioRol::getRolNombre)
                .sorted(RoleMapper.priorityComparator())
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE, ACCOUNT_SUSPENDED_MESSAGE, HttpStatus.FORBIDDEN));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}