package co.edu.unicauca.backend.modules.auth.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de {@link JwtAuthenticationFilter}.
 *
 * <p>Se prueba la lógica interna del filtro de forma aislada:
 * <ul>
 *   <li>Extracción del token del header {@code Authorization}.</li>
 *   <li>Delegación a {@link JwtTokenProvider} y {@link UserDetailsService}.</li>
 *   <li>Correcta escritura (o no escritura) en el {@link SecurityContextHolder}.</li>
 *   <li>En todos los casos el filtro continúa la cadena ({@code filterChain.doFilter}).</li>
 * </ul>
 *
 * <p>Se usan {@link MockHttpServletRequest}/{@link MockHttpServletResponse} de Spring
 * y {@link MockFilterChain} para invocar {@code doFilter} sin un servidor real.
 *
 * @see JwtAuthenticationFilter
 * @see JwtTokenProvider
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    /**
     * Mock de {@link JwtTokenProvider}: satisface la dependencia del filtro para
     * validar y extraer información de los tokens JWT.
     */
    @Mock
    JwtTokenProvider jwtTokenProvider;

    /**
     * Mock de {@link UserDetailsService}: satisface la dependencia del filtro para
     * cargar los detalles del usuario a partir del username extraído del token.
     */
    @Mock
    UserDetailsService userDetailsService;

    @InjectMocks
    JwtAuthenticationFilter filtro;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    /** Token ficticio con formato JWT (tres segmentos) usado en todos los escenarios. */
    private static final String TOKEN = "header.payload.firma";

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Verifica el comportamiento del filtro cuando la petición no incluye el header
     * {@code Authorization}.
     *
     * <p>Escenarios cubiertos:
     * <ul>
     *   <li>No se escribe ninguna autenticación en el {@link SecurityContextHolder}.</li>
     *   <li>El filtro continúa la cadena sin interrumpir el request.</li>
     *   <li>No se invoca ni {@link JwtTokenProvider} ni {@link UserDetailsService}.</li>
     * </ul>
     */
    @Nested
    @DisplayName("Sin header Authorization")
    class SinHeader {

        @Test
        @DisplayName("No establece autenticación en el SecurityContext")
        void sinHeader_noAutentica() throws Exception {
            filtro.doFilter(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Continúa la cadena de filtros (no interrumpe el request)")
        void sinHeader_continuaCadena() throws Exception {
            filtro.doFilter(request, response, filterChain);

            // MockFilterChain guarda el request si doFilter fue llamado
            assertThat(filterChain.getRequest()).isNotNull();
        }

        @Test
        @DisplayName("No interactúa con JwtTokenProvider ni UserDetailsService")
        void sinHeader_noUsaDependencias() throws Exception {
            filtro.doFilter(request, response, filterChain);

            verifyNoInteractions(jwtTokenProvider, userDetailsService);
        }
    }

    /**
     * Verifica que el filtro ignora headers {@code Authorization} que no tienen el
     * prefijo {@code Bearer }.
     *
     * <p>Si el valor del header no comienza por {@code Bearer }, el filtro no debe
     * extraer ningún token, no debe autenticar y debe continuar la cadena sin
     * interactuar con las dependencias.
     */
    @Nested
    @DisplayName("Header Authorization sin prefijo Bearer")
    class HeaderSinBearer {

        @Test
        @DisplayName("No extrae token → no autentica → continúa cadena")
        void headerSinBearer_noAutenticaYContinua() throws Exception {
            request.addHeader("Authorization", TOKEN); // sin "Bearer "

            filtro.doFilter(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            assertThat(filterChain.getRequest()).isNotNull();
            verifyNoInteractions(jwtTokenProvider, userDetailsService);
        }
    }

    /**
     * Verifica el flujo completo cuando el filtro recibe un token JWT válido.
     *
     * <p>El {@code setUp} configura los mocks para que {@link JwtTokenProvider#extractUsername}
     * devuelva un username, {@link UserDetailsService#loadUserByUsername} devuelva un
     * {@link UserDetails} y {@link JwtTokenProvider#isTokenValid} devuelva {@code true}.
     *
     * <p>Escenarios cubiertos:
     * <ul>
     *   <li>Se escribe la autenticación en el {@link SecurityContextHolder} con el username correcto.</li>
     *   <li>La autenticación resultante contiene los roles del usuario.</li>
     *   <li>El filtro continúa la cadena de filtros tras autenticar.</li>
     * </ul>
     */
    @Nested
    @DisplayName("Token JWT válido")
    class TokenValido {

        private UserDetails usuarioMock;

        @BeforeEach
        void setUp() {
            usuarioMock = User.withUsername("testuser")
                    .password("password")
                    .roles("USER")
                    .build();
            request.addHeader("Authorization", "Bearer " + TOKEN);

            when(jwtTokenProvider.extractUsername(TOKEN)).thenReturn("testuser");
            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(usuarioMock);
            when(jwtTokenProvider.isTokenValid(TOKEN, usuarioMock)).thenReturn(true);
        }

        @Test
        @DisplayName("Establece autenticación en el SecurityContext")
        void tokenValido_autenticaEnSecurityContext() throws Exception {
            filtro.doFilter(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                    .isEqualTo("testuser");
        }

        @Test
        @DisplayName("La autenticación contiene los roles del usuario")
        void tokenValido_autenticacionContieneRoles() throws Exception {
            filtro.doFilter(request, response, filterChain);

            var authorities = SecurityContextHolder.getContext()
                    .getAuthentication().getAuthorities();
            assertThat(authorities)
                    .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
        }

        @Test
        @DisplayName("Continúa la cadena de filtros tras autenticar")
        void tokenValido_continuaCadena() throws Exception {
            filtro.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isNotNull();
        }
    }

    /**
     * Verifica el comportamiento del filtro cuando {@link JwtTokenProvider#isTokenValid}
     * devuelve {@code false} (token expirado, firma incorrecta, etc.).
     *
     * <p>Aunque el username se extrae correctamente del token, la validación falla y
     * el filtro no debe escribir ninguna autenticación en el {@link SecurityContextHolder}.
     * La cadena de filtros debe continuar para que Spring Security rechace el request.
     */
    @Nested
    @DisplayName("Token JWT inválido")
    class TokenInvalido {

        @BeforeEach
        void setUp() {
            UserDetails usuarioMock = User.withUsername("testuser")
                    .password("password")
                    .roles("USER")
                    .build();
            request.addHeader("Authorization", "Bearer " + TOKEN);

            when(jwtTokenProvider.extractUsername(TOKEN)).thenReturn("testuser");
            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(usuarioMock);
            when(jwtTokenProvider.isTokenValid(TOKEN, usuarioMock)).thenReturn(false);
        }

        @Test
        @DisplayName("No establece autenticación en el SecurityContext")
        void tokenInvalido_noAutentica() throws Exception {
            filtro.doFilter(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }

        @Test
        @DisplayName("Continúa la cadena de filtros (no interrumpe el request)")
        void tokenInvalido_continuaCadena() throws Exception {
            filtro.doFilter(request, response, filterChain);

            assertThat(filterChain.getRequest()).isNotNull();
        }
    }
}
