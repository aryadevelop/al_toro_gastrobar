package co.edu.unicauca.backend.modules.auth.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de autenticación basado en tokens JWT.
 *
 * <p>Se ejecuta una única vez por request HTTP, antes del filtro
 * {@code UsernamePasswordAuthenticationFilter} en la cadena de Spring Security.
 * Extrae el token del header {@code Authorization}, lo valida y, si es correcto,
 * establece la autenticación en el {@link SecurityContextHolder}.
 *
 * <p>Si el token está ausente o es inválido, el filtro no interrumpe el flujo;
 * delega la decisión de acceso al {@code AuthorizationFilter} al final de la cadena.
 *
 * @see JwtTokenProvider
 * @see OncePerRequestFilter
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Nombre del header HTTP que transporta el token JWT. */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** Prefijo esperado en el valor del header; se elimina para obtener el token puro. */
    private static final String BEARER_PREFIX = "Bearer ";

    /** Proveedor usado para validar y extraer información del token JWT. */
    private final JwtTokenProvider jwtTokenProvider;

    /** Servicio que carga los detalles del usuario desde la fuente de datos. */
    private final UserDetailsService userDetailsService;

    /**
     * Procesa el token JWT del request y establece la autenticación en el contexto
     * de seguridad si el token es válido.
     *
     * @param request     request HTTP entrante
     * @param response    response HTTP saliente
     * @param filterChain cadena de filtros de Spring Security
     * @throws ServletException si ocurre un error en el procesamiento del servlet
     * @throws IOException      si ocurre un error de entrada/salida
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = extractToken(request);

            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null
                    && jwtTokenProvider.isAccessToken(token)) {
                String username = jwtTokenProvider.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtTokenProvider.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException ex) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token JWT del header {@code Authorization: Bearer <token>}.
     *
     * @param request request HTTP del que se extrae el header
     * @return el token sin el prefijo {@code Bearer }, o {@code null} si el header
     *         está ausente o no tiene el formato esperado
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
