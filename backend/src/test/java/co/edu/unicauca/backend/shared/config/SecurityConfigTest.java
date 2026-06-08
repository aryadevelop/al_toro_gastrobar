package co.edu.unicauca.backend.shared.config;

import co.edu.unicauca.backend.modules.auth.entity.Sesion;
import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de la configuración central de seguridad ({@link SecurityConfig}).
 *
 * <p>Verifica que las reglas de autorización, el filtro JWT, CSRF, CORS y la
 * política de sesión se comportan exactamente como se configuraron en producción.
 *
 * <p>Estrategia de prueba:
 * <ul>
 *   <li><b>{@code @WebMvcTest}:</b> carga únicamente la capa web — controladores,
 *       filtros y configuración de seguridad — sin levantar base de datos ni
 *       contexto completo de Spring.</li>
 *   <li><b>{@link TestController}:</b> controlador auxiliar cuyos paths coinciden
 *       con las reglas reales de {@code PUBLIC_ENDPOINTS}:
 *       {@code /api/auth/test} es público y {@code /api/resources/test} es privado.</li>
 *   <li><b>Mocks de infraestructura:</b> {@link JwtTokenProvider} y
 *       {@link UserDetailsService} se mockean con {@code @MockitoBean} para que
 *       el {@link co.edu.unicauca.backend.modules.auth.security.JwtAuthenticationFilter}
 *       real pueda iniciarse sin conexión a base de datos.</li>
 * </ul>
 *
 * <p>Aspectos cubiertos:
 * <ol>
 *   <li>Acceso a endpoints públicos ({@code /api/auth/**}) sin autenticación.</li>
 *   <li>Bloqueo de endpoints privados sin autenticación.</li>
 *   <li>Acceso a endpoints privados con {@code @WithMockUser}.</li>
 *   <li>Autenticación real mediante token JWT en el header {@code Authorization}.</li>
 *   <li>CSRF deshabilitado: los {@code POST} sin token no dan 403.</li>
 *   <li>Sesión {@code STATELESS}: ninguna respuesta genera cookie {@code JSESSIONID}.</li>
 *   <li>CORS: preflight con origen permitido y con origen no permitido.</li>
 * </ol>
 *
 * @see SecurityConfig
 * @see TestController
 * @see co.edu.unicauca.backend.modules.auth.security.JwtAuthenticationFilter
 */
@WebMvcTest(TestController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "cors.allowed-origins=http://localhost:4200,http://localhost:80",
        "jwt.secret=clave-secreta-de-prueba-debe-tener-al-menos-32-caracteres",
        "jwt.expiration=86400000"
})
class SecurityConfigTest {

    @Autowired
    MockMvc mockMvc;

    /**
     * Mock de {@link JwtTokenProvider}: satisface la dependencia del
     * {@code JwtAuthenticationFilter} real
     */
    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    /**
     * Mock de {@link UserDetailsService}: requerido tanto por {@code SecurityConfig}
     * (para construir el {@code AuthenticationProvider}) como por el
     * {@code JwtAuthenticationFilter} al cargar los detalles del usuario del token.
     */
    @MockitoBean
    UserDetailsService userDetailsService;

    /**
     * Mock de {@link SesionRepository}: requerido por {@code JwtAuthenticationFilter}
     * para verificar que el access token tiene sesión activa.
     */
    @MockitoBean
    SesionRepository sesionRepository;

    /**
    * Verifica que {@code /api/auth/login} es accesible sin autenticación.
     *
     * <p>El path referencia {@code /api/auth/**}, incluido en {@code PUBLIC_ENDPOINTS}
     * con {@code permitAll()}, por lo que Spring Security no debe bloquearlo
     * independientemente de si hay o no usuario autenticado.
     */
    @Nested
    @DisplayName("Endpoint público /api/auth/login")
    class EndpointPublico {

        @Test
        @DisplayName("Sin autenticación → 200 (Security hace permitAll para /api/auth/login)")
        void sinAuth_retorna200() throws Exception {
            mockMvc.perform(get("/api/auth/login"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Publico"));
        }

        @Test
        @WithMockUser
        @DisplayName("Con autenticación → 200 (Security hace permitAll para /api/auth/login)")
        void conAuth_retorna200() throws Exception {
            mockMvc.perform(get("/api/auth/login"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Publico"));
        }
    }

    /**
     * Verifica que {@code /api/resources/test} exige autenticación.
     *
     * <p>El path no está en {@code PUBLIC_ENDPOINTS}, por lo que queda cubierto
     * por la regla {@code anyRequest().authenticated()} de {@code SecurityConfig}.
     * Sin credenciales válidas la respuesta debe ser {@code 403 Forbidden} y
     * con un usuario autenticado (real o simulado) debe ser {@code 200 OK}.
     */
    @Nested
    @DisplayName("Endpoint privado /api/resources/test")
    class EndpointPrivado {

        @Test
        @DisplayName("Sin autenticación → 401")
        void sinAuth_retorna403() throws Exception {
            mockMvc.perform(get("/api/resources/test"))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        @DisplayName("Con @WithMockUser → 200")
        void conWithMockUser_retorna200() throws Exception {
            mockMvc.perform(get("/api/resources/test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Privado"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Con rol ADMIN → 200 (no hay @PreAuthorize aún, solo autenticación)")
        void conRolAdmin_retorna200() throws Exception {
            mockMvc.perform(get("/api/resources/test"))
                    .andExpect(status().isOk());
        }
    }

    /**
     * Verifica el flujo completo de autenticación JWT a través del
     * {@code JwtAuthenticationFilter}.
     * 
     * <p>Escenarios cubiertos:
     * <ul>
     *   <li>Token válido → el filtro autentica → endpoint privado retorna {@code 200}.</li>
     *   <li>Token expirado ({@code isTokenValid} devuelve {@code false}) → {@code 403}.</li>
     *   <li>Header sin prefijo {@code Bearer} → el filtro lo ignora → {@code 403}.</li>
     *   <li>Token válido sobre endpoint público → no altera el {@code 200}.</li>
     * </ul>
     */
    @Nested
    @DisplayName("Autenticación con token JWT")
    class ConTokenJwt {

        /** Token ficticio con formato JWT (tres segmentos) usado en todos los escenarios. */
        private static final String TOKEN = "header.payload.firma";

        /**
         * Construye un {@link UserDetails} de prueba con username {@code testuser}
         * y rol {@code USER}, reutilizado en los mocks de cada test.
         *
         * @return instancia inmutable de {@link UserDetails}
         */
        private UserDetails usuarioMock() {
            return User.withUsername("testuser")
                    .password("password")
                    .roles("USER")
                    .build();
        }

        @Test
        @DisplayName("Bearer válido en /api/resources/test → JwtFilter autentica → 200")
        void jwtValido_endpointPrivado_retorna200() throws Exception {
            UserDetails usuario = usuarioMock();
            when(jwtTokenProvider.isAccessToken(TOKEN)).thenReturn(true);
            when(jwtTokenProvider.extractUsername(TOKEN)).thenReturn("testuser");
            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(usuario);
            when(jwtTokenProvider.isTokenValid(TOKEN, usuario)).thenReturn(true);
            when(sesionRepository.findBySesionTokenAndSesionActivaTrue(TOKEN))
                    .thenReturn(Optional.of(Sesion.builder().build()));

            mockMvc.perform(get("/api/resources/test")
                            .header("Authorization", "Bearer " + TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Privado"));
        }

        @Test
        @DisplayName("Bearer expirado en /api/resources/test → isTokenValid false → 401")
        void jwtExpirado_endpointPrivado_retorna403() throws Exception {
            UserDetails usuario = usuarioMock();
            when(jwtTokenProvider.extractUsername(TOKEN)).thenReturn("testuser");
            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(usuario);
            when(jwtTokenProvider.isTokenValid(TOKEN, usuario)).thenReturn(false);

            mockMvc.perform(get("/api/resources/test")
                            .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Header Authorization sin prefijo Bearer → se ignora → 401")
        void headerSinBearer_endpointPrivado_retorna403() throws Exception {
            mockMvc.perform(get("/api/resources/test")
                            .header("Authorization", TOKEN))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("JWT válido no bloquea endpoint público → sigue siendo 200")
        void jwtValido_endpointPublico_sigueRetornando200() throws Exception {
            UserDetails usuario = usuarioMock();
            when(jwtTokenProvider.extractUsername(TOKEN)).thenReturn("testuser");
            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(usuario);
            when(jwtTokenProvider.isTokenValid(TOKEN, usuario)).thenReturn(true);

                mockMvc.perform(get("/api/auth/login")
                            .header("Authorization", "Bearer " + TOKEN))
                    .andExpect(status().isOk());
        }
    }

    /**
     * Verifica que la protección CSRF está deshabilitada en la API.
     *
     * <p>{@code SecurityConfig} deshabilita CSRF con {@code AbstractHttpConfigurer::disable}
     * porque la API REST usa JWT y no cookies de sesión, por lo que los ataques CSRF no aplican..
     */
    @Nested
    @DisplayName("CSRF deshabilitado")
    class Csrf {

        @Test
        @WithMockUser
        @DisplayName("POST sin token CSRF con auth → 200 (CSRF deshabilitado, no bloquea)")
        void post_sinCsrfToken_noRetorna403PorCsrf() throws Exception {
            // Si CSRF estuviera habilitado, este POST sin token devolvería 403.
            // Con CSRF deshabilitado, el request pasa y el handler responde 200.
            mockMvc.perform(post("/api/resources/test"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Privado POST"));
        }
    }

    /**
     * Verifica que la API no crea sesiones HTTP.
     *
     * <p>{@code SecurityConfig} configura {@code SessionCreationPolicy.STATELESS},
     * lo que impide que Spring Security genere o use la cookie {@code JSESSIONID}.
     * Cada request debe ser autónomo y transportar su propio token JWT.
     * Se verifica tanto en respuestas autenticadas como en respuestas públicas.
     */
    @Nested
    @DisplayName("Sesión stateless")
    class Stateless {

        @Test
        @WithMockUser
        @DisplayName("Respuesta de endpoint privado no genera cookie JSESSIONID")
        void respuestaAutenticada_noGeneraCookieSesion() throws Exception {
            mockMvc.perform(get("/api/resources/test"))
                    .andExpect(status().isOk())
                    .andExpect(cookie().doesNotExist("JSESSIONID"));
        }

        @Test
        @DisplayName("Respuesta de endpoint público tampoco genera cookie JSESSIONID")
        void respuestaPublica_noGeneraCookieSesion() throws Exception {
                mockMvc.perform(get("/api/auth/login"))
                    .andExpect(status().isOk())
                    .andExpect(cookie().doesNotExist("JSESSIONID"));
        }
    }
    
    /**
     * Verifica la política CORS configurada en {@code SecurityConfig}.
     *
     * <p>{@code SecurityConfig} registra una {@code CorsConfigurationSource} que
     * permite los orígenes definidos en la propiedad {@code cors.allowed-origins}
     * (en estos tests: {@code localhost:4200} y {@code localhost:80}).
     *
     * <p>Se usan peticiones preflight ({@code OPTIONS} con
     * {@code Access-Control-Request-Method}) porque Spring Security las procesa
     * antes de la capa de autenticación, lo que permite verificar CORS de forma
     * aislada sin necesidad de un usuario autenticado.
     *
     * <p>Comportamiento esperado:
     * <ul>
     *   <li>Origen permitido → {@code 200} con header
     *       {@code Access-Control-Allow-Origin}.</li>
     *   <li>Origen no permitido → {@code 403} sin header
     *       {@code Access-Control-Allow-Origin}.</li>
     *   <li>Request normal con origen permitido → header CORS presente en la respuesta.</li>
     * </ul>
     */
    @Nested
    @DisplayName("CORS - preflight OPTIONS")
    class Cors {

        @Test
        @DisplayName("Preflight desde origen permitido → 200 + Access-Control-Allow-Origin")
        void preflight_origenPermitido_retornaCorsHeaders() throws Exception {
            mockMvc.perform(options("/api/resources/test")
                            .header("Origin", "http://localhost:4200")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        }

        @Test
        @DisplayName("Preflight desde origen NO permitido → 403, sin Access-Control-Allow-Origin")
        void preflight_origenNoPermitido_retornaForbidden() throws Exception {
            mockMvc.perform(options("/api/resources/test")
                            .header("Origin", "http://sitio-malicioso.com")
                            .header("Access-Control-Request-Method", "GET"))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }

        @Test
        @WithMockUser
        @DisplayName("Request normal con origen permitido → respuesta incluye Access-Control-Allow-Origin")
        void request_origenPermitido_incluyeCorsHeader() throws Exception {
            mockMvc.perform(get("/api/resources/test")
                            .header("Origin", "http://localhost:4200"))
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
        }
    }
}
