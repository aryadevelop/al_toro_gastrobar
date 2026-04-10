package co.edu.unicauca.backend.shared.exception;

import co.edu.unicauca.backend.shared.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Manejo centralizado de excepciones para todos los controladores REST.
 *
 * <p>Intercepta las excepciones conocidas y las convierte en {@link ApiResponse}
 * con el código HTTP y el {@link ErrorCode} adecuados. Las excepciones no
 * contempladas se capturan como {@code 500 INTERNAL_SERVER_ERROR} y se registran en el log.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Recurso no encontrado → {@code 404 NOT_FOUND}.
     *
     * @param ex excepción lanzada por el servicio
     * @return respuesta con código {@link ErrorCode#ENTITY_NOT_FOUND} y mensaje de la excepción
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    /**
     * Violación de regla de negocio → código HTTP definido en la excepción.
     *
     * @param ex excepción con el {@link HttpStatus} y {@link ErrorCode} asociados
     * @return respuesta con el código y mensaje de la excepción
     */
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    /**
     * Error de validación ({@code @Valid}) → {@code 422 UNPROCESSABLE_ENTITY}.
     *
     * <p>Concatena todos los mensajes de los campos inválidos separados por {@code "; "}.
     *
     * @param ex excepción con los errores de binding
     * @return respuesta con código {@link ErrorCode#VALIDATION_ERROR} y mensajes concatenados
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR.getCode(), errors));
    }

    /**
     * Credenciales inválidas → {@code 401 UNAUTHORIZED}.
     *
     * @param ex excepción de autenticación fallida
     * @return respuesta con código {@link ErrorCode#INVALID_CREDENTIALS}
     */
    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(
                        ErrorCode.INVALID_CREDENTIALS.getCode(),
                        ErrorCode.INVALID_CREDENTIALS.getMessage()));
    }

    /**
     * Acceso denegado → {@code 403 FORBIDDEN}.
     *
     * @param ex excepción de autorización insuficiente
     * @return respuesta con código {@link ErrorCode#ACCESS_DENIED}
     */
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(
                        ErrorCode.ACCESS_DENIED.getCode(),
                        ErrorCode.ACCESS_DENIED.getMessage()));
    }

    /**
     * Cualquier otra excepción no contemplada → {@code 500 INTERNAL_SERVER_ERROR}.
     *
     * <p>El detalle del error se registra en el log pero no se expone al cliente.
     *
     * @param ex excepción no controlada
     * @return respuesta con código {@link ErrorCode#INTERNAL_ERROR}
     */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        ErrorCode.INTERNAL_ERROR.getCode(),
                        ErrorCode.INTERNAL_ERROR.getMessage()));
    }
}
