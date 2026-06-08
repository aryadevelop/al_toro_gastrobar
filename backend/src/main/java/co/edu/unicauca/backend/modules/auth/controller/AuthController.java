package co.edu.unicauca.backend.modules.auth.controller;

import co.edu.unicauca.backend.modules.auth.dto.request.LoginRequest;
import co.edu.unicauca.backend.modules.auth.dto.request.RefreshTokenRequest;
import co.edu.unicauca.backend.modules.auth.dto.request.RegisterRequest;
import co.edu.unicauca.backend.modules.auth.dto.response.AuthResponse;
import co.edu.unicauca.backend.modules.auth.dto.response.AuthUserResponse;
import co.edu.unicauca.backend.modules.auth.dto.response.RegisterResponse;
import co.edu.unicauca.backend.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para los flujos de autenticación JWT.
 *
 * <p>Expone los endpoints bajo {@code /api/auth} y delega la lógica
 * de negocio en {@link AuthService}.
 *
 * <p>Comportamiento general:
 * <ul>
 *   <li><b>Register:</b> registra una nueva cuenta de cliente con datos personales
 *       y asigna el rol CLIENTE.</li>
 *   <li><b>Login:</b> valida credenciales, gestiona sesiones activas y devuelve
 *       un par de tokens (access + refresh).</li>
 *   <li><b>Refresh:</b> rota el par de tokens sin requerir contraseña, siempre
 *       que el refresh token sea válido y la sesión siga activa.</li>
 *   <li><b>Me:</b> devuelve el perfil del usuario cuya sesión está activa,
 *       identificado por el token de acceso en el header {@code Authorization}.</li>
 *   <li><b>Logout:</b> invalida todas las sesiones activas del usuario autenticado.</li>
 * </ul>
 *
 * <p>Los endpoints {@code /register}, {@code /login} y {@code /refresh} son públicos; 
 * los demás requieren un access token válido en el header {@code Authorization: Bearer <token>}.
 *
 * @see AuthService
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Login, refresh de token, perfil y logout")
public class AuthController {

    private final AuthService authService;

    /**
     * Registra una nueva cuenta de cliente en el sistema.
     *
     * <p>Valida los datos de entrada, verifica que el correo y teléfono no estén
     * duplicados, crea las entidades de usuario y cliente, y asigna el rol CLIENTE.
     *
     * @param request datos de registro (nombre, correo, teléfono, contraseña, términos)
     * @return {@link RegisterResponse} con datos mínimos del usuario creado
     */
    @PostMapping("/register")
    @Operation(summary = "Registrar una nueva cuenta de cliente")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Autentica al usuario con email y contraseña y devuelve un par de tokens JWT.
     *
     * <p>Si el usuario ya tiene una sesión activa, el servidor responde
     * {@code 409 Conflict} con un mensaje informativo. Para forzar el cierre
     * de la sesión anterior, el cliente debe reenviar la petición con
     * {@code forceSessionOverride: true}.
     *
     * @param request credenciales del usuario (email, password y flag opcional de override)
     * @return {@link AuthResponse} con access token, refresh token y datos del usuario
     */
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión con email y contraseña")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Rota el par de tokens usando un refresh token válido.
     *
     * <p>El refresh token debe corresponder a una sesión activa; de lo contrario
     * se rechaza con {@code 401 Unauthorized}. Al completarse la rotación, el
     * token anterior queda inválido y la sesión se actualiza con los nuevos tokens.
     *
     * @param request cuerpo con el refresh token a rotar
     * @return {@link AuthResponse} con el nuevo par de tokens y los datos del usuario
     */
    @PostMapping("/refresh")
    @Operation(summary = "Rotar el par de tokens con un refresh token válido")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * Devuelve el perfil del usuario cuya sesión está activa.
     *
     * <p>El usuario se identifica a partir del access token enviado en el header
     * {@code Authorization: Bearer <token>}. Requiere sesión activa.
     *
     * @param authentication contexto de seguridad del que se extrae el email del usuario
     * @return {@link AuthUserResponse} con el perfil del usuario autenticado
     */
    @GetMapping("/me")
    @Operation(summary = "Obtener el perfil del usuario autenticado")
    public ResponseEntity<AuthUserResponse> me(Authentication authentication) {
        return ResponseEntity.ok(authService.me(authentication.getName()));
    }

    /**
     * Cierra todas las sesiones activas del usuario autenticado.
     *
     * <p>Marca como inactivas todas las sesiones del usuario en base de datos.
     * Los tokens emitidos anteriormente dejan de ser aceptados por el filtro JWT.
     *
     * @param authentication contexto de seguridad del que se extrae el email del usuario
     * @return {@code 204 No Content} si el logout fue exitoso
     */
    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión e invalidar todos los tokens activos")
    public ResponseEntity<Void> logout(Authentication authentication) {
        authService.logout(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}