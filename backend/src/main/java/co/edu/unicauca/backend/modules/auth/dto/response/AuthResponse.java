package co.edu.unicauca.backend.modules.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Respuesta devuelta por los endpoints {@code /login} y {@code /refresh}.
 *
 * <p>Contiene el par de tokens JWT emitidos y el perfil del usuario autenticado.
 * El access token se incluye en el header {@code Authorization: Bearer <token>}
 * en cada petición subsiguiente; el refresh token se usa exclusivamente en
 * {@code POST /api/auth/refresh} para rotar ambos tokens.
 *
 * @see AuthUserResponse
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** Token de acceso JWT de corta duración; se incluye en cada petición autenticada. */
    private String accessToken;

    /** Token de refresco JWT de larga duración; solo para rotar el par de tokens. */
    private String refreshToken;

    /** Tipo de token; siempre {@code "Bearer"} según el estándar OAuth 2.0. */
    private String tokenType;

    /** Segundos hasta la expiración del access token desde el momento de la emisión. */
    private Long expiresIn;

    /** Perfil del usuario autenticado para que el frontend configure la sesión. */
    private AuthUserResponse user;
}