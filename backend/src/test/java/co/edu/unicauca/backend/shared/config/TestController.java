package co.edu.unicauca.backend.shared.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador auxiliar exclusivo para los tests de {@link SecurityConfig}.
 *
 * <p><b>No forma parte de la aplicación en producción.</b> Su único propósito
 * es exponer dos endpoints con paths deliberadamente elegidos para ejercitar
 * las reglas de autorización definidas en {@code SecurityConfig}.
 *
 * <p>Reglas que se verifican con este controlador:
 * <ul>
 *   <li><b>Endpoint público:</b> {@code GET /api/auth/test} cae bajo el patrón
 *       {@code /api/auth/**} incluido en {@code PUBLIC_ENDPOINTS}, por lo que
 *       Spring Security aplica {@code permitAll()} y no exige autenticación.</li>
 *   <li><b>Endpoint privado:</b> {@code GET /api/resources/test} no coincide con
 *       ningún patrón de {@code PUBLIC_ENDPOINTS}, por lo que queda cubierto por
 *       la regla {@code anyRequest().authenticated()} y exige un usuario autenticado.</li>
 * </ul>
 *
 * @see SecurityConfig
 * @see SecurityConfigTest
 */
@RestController
class TestController {

    /**
     * Endpoint público de prueba.
     *
     * <p>El path {@code /api/auth/test} coincide con el patrón {@code /api/auth/**}
     * declarado en {@code PUBLIC_ENDPOINTS}, por lo que es accesible sin autenticación.
     *
     * @return cadena literal {@code "Publico"} usada en los tests
     */
    @GetMapping("/api/auth/test")
    public String endpointPublico() {
        return "Publico";
    }

    /**
     * Endpoint privado de prueba — método GET.
     *
     * <p>El path {@code /api/resources/test} no está en {@code PUBLIC_ENDPOINTS},
     * por lo que la regla {@code anyRequest().authenticated()} obliga a presentar
     * credenciales válidas (token JWT o {@code @WithMockUser} en tests).
     *
     * @return cadena literal {@code "Privado"} usada en los tests
     */
    @GetMapping("/api/resources/test")
    public String endpointPrivado() {
        return "Privado";
    }

    /**
     * Endpoint privado de prueba — método POST.
     *
     * <p>Expone el mismo path que {@link #endpointPrivado()} con método {@code POST}
     * para permitir que el test de CSRF verifique que un {@code POST} autenticado
     * sin token CSRF retorna {@code 200} (no {@code 403}), confirmando que CSRF
     * está deshabilitado en {@link SecurityConfig}.
     *
     * @return cadena literal {@code "Privado POST"} usada en los tests
     */
    @PostMapping("/api/resources/test")
    public String endpointPrivadoPost() {
        return "Privado POST";
    }
}
