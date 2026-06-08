package co.edu.unicauca.backend.shared.exception;

import lombok.Getter;

/**
 * Excepción lanzada cuando un recurso solicitado no existe en la base de datos.
 *
 * <p>El {@link GlobalExceptionHandler} la intercepta y devuelve {@code 404 NOT_FOUND}
 * con el código {@link ErrorCode#ENTITY_NOT_FOUND}.
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    /** Código identificador del error para el frontend. */
    private final String code = ErrorCode.ENTITY_NOT_FOUND.getCode();

    /**
     * Crea la excepción indicando el recurso y su identificador.
     *
     * @param resource nombre del recurso (p. ej. {@code "Reserva"})
     * @param id       valor del identificador buscado
     */
    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " con id '" + id + "' no encontrado.");
    }

    /**
     * Crea la excepción indicando el recurso, el campo y el valor buscado.
     *
     * @param resource nombre del recurso (p. ej. {@code "Mesa"})
     * @param field    nombre del campo (p. ej. {@code "numero"})
     * @param value    valor del campo buscado
     */
    public ResourceNotFoundException(String resource, String field, Object value) {
        super(resource + " con " + field + " '" + value + "' no encontrado.");
    }
}
