package co.edu.unicauca.backend.shared.exception;

import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración ligera del manejador global de excepciones ({@link GlobalExceptionHandler}).
 *
 * <p>Verifica que cada {@code @ExceptionHandler} produce el código HTTP, el {@link ErrorCode}
 * y el mensaje correctos en la respuesta {@link ApiResponse}.
 *
 * <p>Estrategia de prueba:
 * <ul>
 *   <li><b>{@code @WebMvcTest}:</b> carga únicamente la capa web — el {@link TestExceptionController}
 *       auxiliar y el {@link GlobalExceptionHandler} — sin base de datos ni contexto completo.</li>
 *   <li><b>{@link TestExceptionController}:</b> controlador auxiliar con un endpoint por cada
 *       tipo de excepción cubierta, lo que permite activar cada handler de forma aislada.</li>
 * </ul>
 *
 * <p>Aspectos cubiertos:
 * <ol>
 *   <li>{@link ResourceNotFoundException} → {@code 404 NOT_FOUND} con código {@code ENT-001}
 *       y mensaje dinámico (por id y por campo/valor).</li>
 *   <li>{@link BusinessException} → código HTTP del estado embebido en la excepción,
 *       mensaje dinámico o por defecto del {@link ErrorCode}, y código del frontend correcto.</li>
 *   <li>{@link org.springframework.web.bind.MethodArgumentNotValidException} →
 *       {@code 422 UNPROCESSABLE_ENTITY} con código {@code VAL-001} y mensajes de campo
 *       concatenados con {@code "; "}.</li>
 *   <li>{@link BadCredentialsException} → {@code 401 UNAUTHORIZED} con código {@code AUTH-001}
 *       y mensaje fijo (no el de la excepción original).</li>
 *   <li>{@link AccessDeniedException} → {@code 403 FORBIDDEN} con código {@code AUTH-002}
 *       y mensaje fijo.</li>
 *   <li>{@link Exception} genérica → {@code 500 INTERNAL_SERVER_ERROR} con código {@code SRV-001},
 *       mensaje fijo y sin exposición de detalles internos al cliente.</li>
 * </ol>
 *
 * @see GlobalExceptionHandler
 * @see TestExceptionController
 * @see ApiResponse
 * @see ErrorCode
 */
@WebMvcTest(
        controllers = GlobalExceptionHandlerTest.TestExceptionController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    /**
     * Mock de {@link JwtTokenProvider}: satisface la dependencia del
     * {@code JwtAuthenticationFilter} que {@code @WebMvcTest} recoge como {@code Filter} bean.
     * Las peticiones de este test no envían cabecera {@code Authorization}, por lo que
     * el filtro se limita a llamar {@code chain.doFilter} sin interactuar con el mock.
     */
    @MockitoBean
    JwtTokenProvider jwtTokenProvider;

    /**
     * Mock de {@link UserDetailsService}: requerido por el mismo filtro JWT.
     * No se stubea porque el filtro nunca lo invoca sin un token válido en la cabecera.
     */
    @MockitoBean
    UserDetailsService userDetailsService;

    // ── Controlador auxiliar ──────────────────────────────────────────────────

    /**
     * Controlador de prueba con un endpoint por cada tipo de excepción cubierta por
     * {@link GlobalExceptionHandler}.
     *
     * <p>Los paths {@code /test/**} no tienen significado de dominio; se eligen únicamente
     * para evitar colisiones y dejar claras las intenciones de cada test.
     */
    @RestController
    static class TestExceptionController {

        // ── ResourceNotFoundException ─────────────────────────────────────────

        @GetMapping("/test/not-found-id")
        void notFoundPorId() {
            throw new ResourceNotFoundException("Mesa", 5L);
        }

        @GetMapping("/test/not-found-field")
        void notFoundPorCampo() {
            throw new ResourceNotFoundException("Mesa", "numero", 5);
        }

        // ── BusinessException ─────────────────────────────────────────────────

        @GetMapping("/test/business-default")
        void businessDefault() {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR);
        }

        @GetMapping("/test/business-message")
        void businessConMensaje() {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "Mesa ya está ocupada");
        }

        @GetMapping("/test/business-conflict")
        void businessConflict() {
            throw new BusinessException(
                    ErrorCode.ENTITY_ALREADY_EXISTS, "La mesa 5 ya existe", HttpStatus.CONFLICT);
        }

        @GetMapping("/test/business-capacity")
        void businessCapacity() {
            throw new BusinessException(ErrorCode.CAPACITY_EXCEEDED);
        }

        // ── MethodArgumentNotValidException ───────────────────────────────────

        @PostMapping("/test/validation")
        void validacion(@Valid @RequestBody TestDto dto) {}

        // ── BadCredentialsException ───────────────────────────────────────────

        @GetMapping("/test/bad-credentials")
        void badCredentials() {
            throw new BadCredentialsException("Detalles internos que no deben exponerse");
        }

        // ── AccessDeniedException ─────────────────────────────────────────────

        @GetMapping("/test/access-denied")
        void accessDenied() {
            throw new AccessDeniedException("Detalles internos que no deben exponerse");
        }

        // ── Exception genérica ────────────────────────────────────────────────

        @GetMapping("/test/generic-runtime")
        void genericRuntime() {
            throw new RuntimeException("Detalles internos que no deben exponerse");
        }

        @GetMapping("/test/generic-illegal-state")
        void genericIllegalState() {
            throw new IllegalStateException("Causa interna del IllegalState");
        }
    }

    /**
     * DTO auxiliar con dos campos validados, usado para generar
     * {@link org.springframework.web.bind.MethodArgumentNotValidException} en los tests.
     */
    static class TestDto {

        @NotBlank(message = "El nombre no puede estar vacío")
        private String nombre;

        @Positive(message = "La capacidad debe ser positiva")
        private Integer capacidad;

        public TestDto() {}
        public String getNombre()       { return nombre; }
        public void setNombre(String n) { this.nombre = n; }
        public Integer getCapacidad()         { return capacidad; }
        public void setCapacidad(Integer c)   { this.capacidad = c; }
    }

    /**
     * Verifica que {@link GlobalExceptionHandler#handleNotFound} produce
     * {@code 404 NOT_FOUND} con el código {@code ENT-001} y el mensaje generado
     * dinámicamente por {@link ResourceNotFoundException}.
     */
    @Nested
    @DisplayName("handleNotFound — ResourceNotFoundException → 404")
    class RecursoNoEncontrado {

        @Test
        @DisplayName("Por id → 404 con mensaje 'Mesa con id \\'5\\' no encontrado.'")
        void porId_retorna404ConMensajeDinamico() throws Exception {
            mockMvc.perform(get("/test/not-found-id"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.ENTITY_NOT_FOUND.getCode()))
                    .andExpect(jsonPath("$.message").value("Mesa con id '5' no encontrado."));
        }

        @Test
        @DisplayName("Por campo → 404 con mensaje 'Mesa con numero \\'5\\' no encontrado.'")
        void porCampo_retorna404ConMensajeDinamico() throws Exception {
            mockMvc.perform(get("/test/not-found-field"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ENTITY_NOT_FOUND.getCode()))
                    .andExpect(jsonPath("$.message").value("Mesa con numero '5' no encontrado."));
        }

        @Test
        @DisplayName("El campo 'data' no aparece en la respuesta de error")
        void porId_sinCampoDatabaseEnRespuesta() throws Exception {
            mockMvc.perform(get("/test/not-found-id"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    /**
     * Verifica que {@link GlobalExceptionHandler#handleBusiness} usa el {@link HttpStatus}
     * embebido en la excepción y propaga el {@link ErrorCode} y el mensaje correctos.
     *
     * <p>Cubre los tres constructores de {@link BusinessException}:
     * <ul>
     *   <li>Solo {@code ErrorCode} → {@code 400} + mensaje por defecto del código.</li>
     *   <li>{@code ErrorCode} + mensaje → {@code 400} + mensaje dinámico.</li>
     *   <li>{@code ErrorCode} + mensaje + {@code HttpStatus} → status personalizado.</li>
     * </ul>
     */
    @Nested
    @DisplayName("handleBusiness — BusinessException → HTTP variable")
    class ErrorDeNegocio {

        @Test
        @DisplayName("Constructor(ErrorCode) → 400 con mensaje por defecto del código")
        void conSoloErrorCode_retorna400ConMensajePorDefecto() throws Exception {
            mockMvc.perform(get("/test/business-default"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.BUSINESS_ERROR.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.BUSINESS_ERROR.getMessage()));
        }

        @Test
        @DisplayName("Constructor(ErrorCode, mensaje) → 400 con mensaje dinámico")
        void conMensajePersonalizado_retorna400ConMensajeDinamico() throws Exception {
            mockMvc.perform(get("/test/business-message"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.BUSINESS_ERROR.getCode()))
                    .andExpect(jsonPath("$.message").value("Mesa ya está ocupada"));
        }

        @Test
        @DisplayName("Constructor(ErrorCode, mensaje, HttpStatus.CONFLICT) → 409")
        void conStatusConflict_retorna409() throws Exception {
            mockMvc.perform(get("/test/business-conflict"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.ENTITY_ALREADY_EXISTS.getCode()))
                    .andExpect(jsonPath("$.message").value("La mesa 5 ya existe"));
        }

        @Test
        @DisplayName("Distintos ErrorCode son propagados correctamente (CAPACITY_EXCEEDED)")
        void otroErrorCode_propagaCodigoDelEnum() throws Exception {
            mockMvc.perform(get("/test/business-capacity"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.CAPACITY_EXCEEDED.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.CAPACITY_EXCEEDED.getMessage()));
        }

        @Test
        @DisplayName("El campo 'data' no aparece en la respuesta de error")
        void sinCampoData() throws Exception {
            mockMvc.perform(get("/test/business-default"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    /**
     * Verifica que {@link GlobalExceptionHandler#handleValidation} produce
     * {@code 422 UNPROCESSABLE_ENTITY} con código {@code VAL-001} y concatena
     * los mensajes de todos los campos inválidos separados por {@code "; "}.
     */
    @Nested
    @DisplayName("handleValidation — @Valid → 422")
    class ValidacionFallida {

        @Test
        @DisplayName("Un campo inválido → 422 con el mensaje de ese campo")
        void unCampoInvalido_retorna422ConMensajeDeCampo() throws Exception {
            mockMvc.perform(post("/test/validation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\": \"\", \"capacidad\": 1}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()))
                    .andExpect(jsonPath("$.message").value("El nombre no puede estar vacío"));
        }

        @Test
        @DisplayName("Dos campos inválidos → mensaje contiene ambos errores")
        void dosCamposInvalidos_mensajeContieneAmbosErrores() throws Exception {
            mockMvc.perform(post("/test/validation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\": \"\", \"capacidad\": -1}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.message", containsString("El nombre no puede estar vacío")))
                    .andExpect(jsonPath("$.message", containsString("La capacidad debe ser positiva")));
        }

        @Test
        @DisplayName("Dos campos inválidos → mensajes separados por '; '")
        void dosCamposInvalidos_separadorPuntoYComa() throws Exception {
            mockMvc.perform(post("/test/validation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\": \"\", \"capacidad\": -1}"))
                    .andExpect(jsonPath("$.message", containsString("; ")));
        }

        @Test
        @DisplayName("Código de error siempre es VAL-001 independientemente del campo")
        void codeEsValidationErrorSiempre() throws Exception {
            mockMvc.perform(post("/test/validation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\": \"\", \"capacidad\": -1}"))
                    .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.getCode()));
        }

        @Test
        @DisplayName("El campo 'data' no aparece en la respuesta de error")
        void sinCampoData() throws Exception {
            mockMvc.perform(post("/test/validation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nombre\": \"\", \"capacidad\": 1}"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    /**
     * Verifica que {@link GlobalExceptionHandler#handleBadCredentials} produce
     * {@code 401 UNAUTHORIZED} con el mensaje y código fijos de {@link ErrorCode#INVALID_CREDENTIALS},
     * sin exponer el mensaje interno de la excepción original.
     */
    @Nested
    @DisplayName("handleBadCredentials — BadCredentialsException → 401")
    class CredencialesInvalidas {

        @Test
        @DisplayName("Retorna 401 UNAUTHORIZED")
        void retorna401() throws Exception {
            mockMvc.perform(get("/test/bad-credentials"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Código es AUTH-001")
        void codeEsInvalidCredentials() throws Exception {
            mockMvc.perform(get("/test/bad-credentials"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_CREDENTIALS.getCode()));
        }

        @Test
        @DisplayName("Mensaje es el fijo del ErrorCode, no el de la excepción original")
        void mensajeEsFijoYNoExponeMensajeOriginal() throws Exception {
            mockMvc.perform(get("/test/bad-credentials"))
                    .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_CREDENTIALS.getMessage()))
                    .andExpect(jsonPath("$.message",
                            not(containsString("Detalles internos que no deben exponerse"))));
        }
    }

    /**
     * Verifica que {@link GlobalExceptionHandler#handleAccessDenied} produce
     * {@code 403 FORBIDDEN} con el mensaje y código fijos de {@link ErrorCode#ACCESS_DENIED},
     * sin exponer el mensaje interno de la excepción original.
     *
     * <p>La seguridad se excluye de la autoconfiguración de este test para que la excepción,
     * lanzada desde el controlador, llegue directamente al {@code @ExceptionHandler}
     * sin ser interceptada por el {@code ExceptionTranslationFilter} de Spring Security.
     */
    @Nested
    @DisplayName("handleAccessDenied — AccessDeniedException → 403")
    class AccesoDenegado {

        @Test
        @DisplayName("Retorna 403 FORBIDDEN")
        void retorna403() throws Exception {
            mockMvc.perform(get("/test/access-denied"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Código es AUTH-002")
        void codeEsAccessDenied() throws Exception {
            mockMvc.perform(get("/test/access-denied"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.getCode()));
        }

        @Test
        @DisplayName("Mensaje es el fijo del ErrorCode, no el de la excepción original")
        void mensajeEsFijoYNoExponeMensajeOriginal() throws Exception {
            mockMvc.perform(get("/test/access-denied"))
                    .andExpect(jsonPath("$.message").value(ErrorCode.ACCESS_DENIED.getMessage()))
                    .andExpect(jsonPath("$.message",
                            not(containsString("Detalles internos que no deben exponerse"))));
        }
    }
    
    /**
     * Verifica que {@link GlobalExceptionHandler#handleGeneric} actúa como red de seguridad
     * para cualquier excepción no contemplada, produciendo {@code 500 INTERNAL_SERVER_ERROR}
     * con código y mensaje fijos y <strong>sin exponer detalles internos</strong> al cliente.
     *
     * <p>La no-exposición de detalles internos es un requisito de seguridad: los mensajes de
     * excepción pueden contener rutas de fichero, nombres de tabla o trazas de pila que serían
     * útiles para un atacante.
     */
    @Nested
    @DisplayName("handleGeneric — Exception no contemplada → 500")
    class ErrorGenerico {

        @Test
        @DisplayName("RuntimeException → 500 INTERNAL_SERVER_ERROR")
        void runtimeException_retorna500() throws Exception {
            mockMvc.perform(get("/test/generic-runtime"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("IllegalStateException → también retorna 500 (catch-all)")
        void illegalStateException_retorna500() throws Exception {
            mockMvc.perform(get("/test/generic-illegal-state"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("Código es SRV-001")
        void codeEsInternalError() throws Exception {
            mockMvc.perform(get("/test/generic-runtime"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.getCode()));
        }

        @Test
        @DisplayName("Mensaje es el fijo del ErrorCode (no el mensaje interno de la excepción)")
        void mensajeEsFijoDelErrorCode() throws Exception {
            mockMvc.perform(get("/test/generic-runtime"))
                    .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_ERROR.getMessage()));
        }

        @Test
        @DisplayName("El mensaje de la excepción original NO se expone al cliente")
        void mensajeOriginalNoEsExpuestoAlCliente() throws Exception {
            mockMvc.perform(get("/test/generic-runtime"))
                    .andExpect(jsonPath("$.message",
                            not(containsString("Detalles internos que no deben exponerse"))));
        }

        @Test
        @DisplayName("El campo 'data' no aparece en la respuesta de error")
        void sinCampoData() throws Exception {
            mockMvc.perform(get("/test/generic-runtime"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }
}
