package co.edu.unicauca.backend.modules.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Componente responsable de generar, firmar y validar tokens JWT.
 *
 * <p>La clave secreta y el tiempo de expiración se inyectan desde
 * {@code application.yml} o variables de entorno mediante las propiedades
 * {@code jwt.secret} y {@code jwt.expiration}.
 *
 * <p>Utiliza el algoritmo HMAC-SHA con una clave derivada del secreto configurado.
 * Los tokens generados incluyen el nombre de usuario como {@code subject}, la fecha
 * de emisión y la fecha de expiración.
 *
 * @see JwtAuthenticationFilter
 */
@Component
public class JwtTokenProvider {

    /** Clave HMAC derivada del secreto configurado; usada para firmar y verificar tokens. */
    private final SecretKey key;

    /** Tiempo de vida del token en milisegundos, inyectado desde {@code jwt.expiration}. */
    private final long expirationMs;

    /** Tiempo de vida del refresh token en milisegundos, inyectado desde {@code jwt.refresh-expiration}. */
    private final long refreshExpirationMs;

    /**
     * Construye el proveedor derivando la clave HMAC a partir del secreto
     * y almacenando el tiempo de expiración configurado.
     *
     * @param secret       cadena secreta usada para firmar los tokens; debe tener
     *                     al menos 32 caracteres para HMAC-SHA256
     * @param expirationMs tiempo de vida del token en milisegundos
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationMs,
            @Value("${jwt.refresh-expiration:604800000}") long refreshExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Genera un token JWT firmado para el usuario dado.
     *
     * <p>El token contiene el nombre de usuario como {@code subject}, la fecha
     * de emisión actual y una fecha de expiración calculada a partir de
     * {@code expirationMs}.
     *
     * @param userDetails detalles del usuario autenticado
     * @return token JWT compacto y firmado
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, expirationMs);
    }

    /**
     * Genera un refresh token JWT firmado para el usuario dado.
     *
     * @param userDetails detalles del usuario autenticado
     * @return refresh token JWT compacto y firmado
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(userDetails, refreshExpirationMs);
    }

    private String generateToken(UserDetails userDetails, long tokenExpirationMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + tokenExpirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * Extrae el nombre de usuario ({@code subject}) del token JWT.
     *
     * @param token token JWT del que se extrae el subject
     * @return nombre de usuario contenido en el token
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Verifica que el token sea válido para el usuario dado.
     *
     * <p>Un token es válido si su {@code subject} coincide con el nombre de usuario
     * y no ha expirado. Cualquier excepción de parseo ({@link JwtException},
     * {@link IllegalArgumentException}) se trata como token inválido.
     *
     * @param token       token JWT a validar
     * @param userDetails detalles del usuario contra los que se valida el token
     * @return {@code true} si el token es válido; {@code false} en caso contrario
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Comprueba si el token ha superado su fecha de expiración.
     *
     * @param token token JWT a comprobar
     * @return {@code true} si el token ha expirado; {@code false} si aún es vigente
     */
    private boolean isTokenExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    /**
     * Parsea y verifica la firma del token, devolviendo sus claims.
     *
     * @param token token JWT a parsear
     * @return claims contenidos en el payload del token
     * @throws JwtException si la firma es inválida o el token está malformado
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
