package co.edu.unicauca.backend.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Excepción para violaciones de reglas de negocio.
 *
 * <p>Permite asociar un {@link HttpStatus} y un {@link ErrorCode} al error.
 * Por defecto usa {@code 400 BAD_REQUEST}; usar el constructor de tres parámetros
 * para otros códigos como {@code 409 CONFLICT}.
 *
 * @see GlobalExceptionHandler
 */
@Getter
public class BusinessException extends RuntimeException {

    /** Código HTTP que se devolverá al cliente. */
    private final HttpStatus status;

    /** Código identificador del error para el frontend. */
    private final String code;

    /**
     * Crea la excepción a partir de un {@link ErrorCode}; usa su mensaje por defecto.
     *
     * @param errorCode código del error con mensaje predefinido
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.status = HttpStatus.BAD_REQUEST;
        this.code = errorCode.getCode();
    }

    /**
     * Crea la excepción con un {@link ErrorCode} y un mensaje dinámico.
     *
     * @param errorCode código del error
     * @param message   mensaje con datos específicos del contexto
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.code = errorCode.getCode();
    }

    /**
     * Crea la excepción con un {@link ErrorCode}, mensaje dinámico y código HTTP explícito.
     *
     * @param errorCode código del error
     * @param message   mensaje con datos específicos del contexto
     * @param status    código HTTP que se enviará en la respuesta
     */
    public BusinessException(ErrorCode errorCode, String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.code = errorCode.getCode();
    }
}
