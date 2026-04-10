package co.edu.unicauca.backend.modules.auth.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios de {@link JwtTokenProvider}.
 *
 * <p>No necesitan contexto de Spring: se instancia el proveedor directamente
 * con la misma firma de constructor que usa {@code @Value} en producción.
 *
 * <p>La clave de test cumple el mínimo de 32 bytes requerido por HMAC-SHA256.
 *
 * @see JwtTokenProvider
 */
class JwtTokenProviderTest {

    /** Clave HMAC-SHA256 usada en los tests; cumple el mínimo de 32 bytes. */
    private static final String SECRET = "clave-secreta-de-prueba-debe-tener-al-menos-32-caracteres";

    /** Tiempo de expiración estándar (24 h) expresado en milisegundos. */
    private static final long EXPIRACION_24H = 86_400_000L;

    /**
     * Instancia real de {@link JwtTokenProvider} configurada con {@link #SECRET}
     * y {@link #EXPIRACION_24H}; se recrea en cada test vía {@code @BeforeEach}.
     */
    private JwtTokenProvider jwtTokenProvider;

    /**
     * {@link UserDetails} de prueba con username {@code testuser} y rol {@code USER};
     * reutilizado como sujeto de los tokens en la mayoría de los escenarios.
     */
    private UserDetails usuario;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, EXPIRACION_24H);
        usuario = User.withUsername("testuser")
                .password("password")
                .roles("USER")
                .build();
    }

    /**
     * Verifica la generación de tokens JWT por {@link JwtTokenProvider#generateToken}.
     *
     * <p>Escenarios cubiertos:
     * <ul>
     *   <li>El token generado no es nulo ni vacío.</li>
     *   <li>El token tiene formato JWT válido: tres segmentos separados por {@code .}.</li>
     *   <li>Dos llamadas consecutivas producen tokens distintos (diferente {@code issuedAt}).</li>
     * </ul>
     */
    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("Genera un token no nulo y no vacío")
        void generaToken_noEsNuloNiVacio() {
            String token = jwtTokenProvider.generateToken(usuario);
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("Token tiene formato JWT: tres partes separadas por '.'")
        void generaToken_tieneFormatoJwt() {
            String token = jwtTokenProvider.generateToken(usuario);
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("Dos llamadas consecutivas generan tokens distintos (distinta issuedAt)")
        void dosTokens_sonDistintos() throws InterruptedException {
            String token1 = jwtTokenProvider.generateToken(usuario);
            Thread.sleep(1100);
            String token2 = jwtTokenProvider.generateToken(usuario);
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    /**
     * Verifica la extracción del subject del token por
     * {@link JwtTokenProvider#extractUsername}.
     *
     * <p>Escenarios cubiertos:
     * <ul>
     *   <li>Un token válido devuelve el username original del {@code subject}.</li>
     *   <li>Un token malformado lanza {@link io.jsonwebtoken.JwtException}.</li>
     * </ul>
     */
    @Nested
    @DisplayName("extractUsername")
    class ExtractUsername {

        @Test
        @DisplayName("Extrae el username del subject del token")
        void extrae_usernameCorrectoDelToken() {
            String token = jwtTokenProvider.generateToken(usuario);
            assertThat(jwtTokenProvider.extractUsername(token)).isEqualTo("testuser");
        }

        @Test
        @DisplayName("Token malformado lanza JwtException")
        void tokenMalformado_lanzaJwtException() {
            assertThatThrownBy(() -> jwtTokenProvider.extractUsername("esto.no.es.jwt"))
                    .isInstanceOf(JwtException.class);
        }
    }

    /**
     * Verifica la validación de tokens por {@link JwtTokenProvider#isTokenValid}.
     *
     * <p>Escenarios cubiertos:
     * <ul>
     *   <li>Token recién generado para el mismo usuario → {@code true}.</li>
     *   <li>Token generado para un usuario distinto → {@code false}.</li>
     *   <li>Token expirado (expiración configurada a {@code 0 ms}) → {@code false}.</li>
     *   <li>Token malformado → {@code false} (excepción capturada internamente).</li>
     *   <li>Token firmado con una clave diferente → {@code false}.</li>
     * </ul>
     */
    @Nested
    @DisplayName("isTokenValid")
    class IsTokenValid {

        @Test
        @DisplayName("Token recién generado para el mismo usuario → true")
        void tokenValido_retornaTrue() {
            String token = jwtTokenProvider.generateToken(usuario);
            assertThat(jwtTokenProvider.isTokenValid(token, usuario)).isTrue();
        }

        @Test
        @DisplayName("Token generado para usuario distinto → false")
        void tokenDeOtroUsuario_retornaFalse() {
            UserDetails otroUsuario = User.withUsername("otro")
                    .password("pass")
                    .roles("USER")
                    .build();
            String tokenDeOtro = jwtTokenProvider.generateToken(otroUsuario);
            assertThat(jwtTokenProvider.isTokenValid(tokenDeOtro, usuario)).isFalse();
        }

        @Test
        @DisplayName("Token expirado → false (expiracion = 0 ms)")
        void tokenExpirado_retornaFalse() {
            JwtTokenProvider proveedorConExpiracionCero =
                    new JwtTokenProvider(SECRET, 0L);
            String token = proveedorConExpiracionCero.generateToken(usuario);
            assertThat(proveedorConExpiracionCero.isTokenValid(token, usuario)).isFalse();
        }

        @Test
        @DisplayName("Token malformado → false (excepción capturada internamente)")
        void tokenMalformado_retornaFalse() {
            assertThat(jwtTokenProvider.isTokenValid("token.invalido.xyz", usuario)).isFalse();
        }

        @Test
        @DisplayName("Token firmado con clave diferente → false")
        void tokenFirmadoConOtraClave_retornaFalse() {
            JwtTokenProvider otroProveedor =
                    new JwtTokenProvider("clave-completamente-diferente-de-32-chars!!", EXPIRACION_24H);
            String tokenDeOtroProveedor = otroProveedor.generateToken(usuario);
            assertThat(jwtTokenProvider.isTokenValid(tokenDeOtroProveedor, usuario)).isFalse();
        }
    }
}
