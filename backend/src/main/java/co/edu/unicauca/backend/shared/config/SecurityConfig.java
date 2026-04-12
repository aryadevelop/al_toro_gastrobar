package co.edu.unicauca.backend.shared.config;

import co.edu.unicauca.backend.modules.auth.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración central de Spring Security para la API REST.
 *
 * <p>Establece una política de seguridad sin estado (stateless) basada en JWT,
 * sin sesiones HTTP ni protección CSRF, ya que la API no usa formularios de navegador.
 *
 * <p>Comportamiento general:
 * <ul>
 *   <li><b>Stateless:</b> no se crea ni persiste ninguna sesión HTTP; cada request
 *       debe incluir su propio token JWT.</li>
 *   <li><b>CSRF deshabilitado:</b> innecesario en APIs REST autenticadas con JWT.</li>
 *   <li><b>CORS:</b> permite el frontend Angular en {@code localhost:4200} (desarrollo)
 *       y {@code localhost:80} (producción con Docker).</li>
 *   <li><b>Endpoints públicos:</b> autenticación, Swagger, Actuator health y WebSocket.</li>
 * </ul>
 *
 * @see JwtAuthenticationFilter
 * @see EnableMethodSecurity
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * Rutas que no requieren autenticación.
     *
     * <p>Incluye los endpoints de autenticación, la documentación Swagger,
     * el health check de Actuator y el handshake del WebSocket.
     */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/**",
            "/api/reservas/disponibilidad",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health",
            "/ws/**"
    };

    /**
     * Orígenes permitidos en CORS, inyectados desde {@code cors.allowed-origins}.
     *
     * <p>En dev tiene valor por defecto ({@code localhost:4200} y {@code localhost:80});
     * en prod es obligatorio definir la variable de entorno {@code CORS_ALLOWED_ORIGINS}.
     */
    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    /** Filtro que extrae y valida el token JWT antes del filtro de autenticación estándar. */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** Servicio que carga los detalles del usuario desde la fuente de datos. */
    private final UserDetailsService userDetailsService;

    /**
     * Define la cadena de filtros de seguridad principal de la aplicación.
     *
     * <p>Configura en orden:
     * <ol>
     *   <li>CORS con los orígenes permitidos.</li>
     *   <li>CSRF deshabilitado.</li>
     *   <li>Política de sesión {@code STATELESS}.</li>
     *   <li>Reglas de autorización: {@code PUBLIC_ENDPOINTS} libres, el resto autenticado.</li>
     *   <li>Proveedor de autenticación DAO con BCrypt.</li>
     *   <li>{@link JwtAuthenticationFilter} antes del filtro estándar de usuario/contraseña.</li>
     * </ol>
     *
     * @param http objeto de configuración de seguridad HTTP
     * @return cadena de filtros construida
     * @throws Exception si ocurre un error durante la configuración
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // DEV: bypass temporal mientras el módulo de auth no está implementado
                        .requestMatchers(HttpMethod.POST, "/api/reservas").permitAll()
                        .requestMatchers(HttpMethod.GET,  "/api/reservas/mis-reservas").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                "{\"success\":false,\"code\":\"UNAUTHORIZED\",\"message\":\"Autenticaci\u00f3n requerida\"}");
                        }))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Configura el proveedor de autenticación basado en base de datos.
     *
     * <p>Usa {@link DaoAuthenticationProvider} con el {@link UserDetailsService} inyectado
     * y {@link BCryptPasswordEncoder} para verificar las contraseñas almacenadas.
     *
     * @return proveedor de autenticación DAO configurado
     */
    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Expone el {@link AuthenticationManager} como bean de Spring.
     *
     * <p>Necesario para que el servicio de autenticación pueda invocar el proceso
     * de validación de credenciales de forma programática durante el login.
     *
     * @param config configuración de autenticación de Spring Security
     * @return gestor de autenticación activo
     * @throws Exception si no es posible obtener el gestor de autenticación
     */
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Registra el codificador de contraseñas BCrypt.
     *
     * <p>BCrypt aplica un factor de trabajo adaptativo que dificulta los ataques
     * de fuerza bruta incluso si la base de datos es comprometida.
     *
     * @return instancia de {@link BCryptPasswordEncoder}
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Define la política CORS aplicada a todos los endpoints ({@code /**}).
     *
     * <p>Permite los orígenes del frontend Angular en desarrollo ({@code localhost:4200})
     * y en producción con Docker ({@code localhost:80}), todos los métodos HTTP estándar
     * y cualquier header. Las credenciales (cookies, headers de autorización) están habilitadas.
     *
     * @return fuente de configuración CORS registrada para todos los paths
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
