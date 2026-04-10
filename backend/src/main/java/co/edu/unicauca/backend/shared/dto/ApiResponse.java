package co.edu.unicauca.backend.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * Plantilla estándar para todas las respuestas API REST.
 *
 * <p>Estructura JSON resultante:
 * <pre>
 * { "success": true,  "message": "Reserva creada", "data": { ... } }
 * { "success": false, "code": "ENT-001",            "message": "Mesa número 5 no encontrada." }
 * </pre>
 *
 * <p>Los campos {@code code} y {@code data} se omiten en la serialización cuando son {@code null}.
 * Usar los métodos de fábrica estáticos para construir las respuestas desde los controladores.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** {@code true} si la operación se completó correctamente; {@code false} en caso de error. */
    private final boolean success;

    /** Código identificador del error; permite al frontend reaccionar programáticamente. */
    private final String code;

    /** Descripción del resultado o del error. Puede ser {@code null} si no aplica. */
    private final String message;

    /** Payload de la respuesta. {@code null} se omite en la serialización JSON. */
    private final T data;

    /**
     * Respuesta exitosa con datos, sin mensaje.
     *
     * @param data payload de la respuesta
     * @return {@code ApiResponse} con {@code success = true}
     */
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * Respuesta exitosa con mensaje y datos.
     *
     * @param message descripción del resultado
     * @param data    payload de la respuesta
     * @return {@code ApiResponse} con {@code success = true}
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Alias de {@link #ok(String, Object)} para respuestas de creación (HTTP 201).
     *
     * @param message descripción del recurso creado
     * @param data    payload del recurso creado
     * @return {@code ApiResponse} con {@code success = true}
     */
    public static <T> ApiResponse<T> created(String message, T data) {
        return ok(message, data);
    }

    /**
     * Respuesta exitosa con solo mensaje, sin datos.
     *
     * @param message descripción del resultado
     * @return {@code ApiResponse} con {@code success = true} y sin {@code data}
     */
    public static <T> ApiResponse<T> message(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .build();
    }

    /**
     * Respuesta de error con código identificador y mensaje descriptivo.
     *
     * @param code    código del error (ver {@link co.edu.unicauca.backend.shared.exception.ErrorCode})
     * @param message descripción del error
     * @return {@code ApiResponse} con {@code success = false}
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .build();
    }
}
