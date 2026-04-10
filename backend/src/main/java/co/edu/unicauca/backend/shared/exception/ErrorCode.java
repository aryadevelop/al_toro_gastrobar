package co.edu.unicauca.backend.shared.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Catálogo de códigos de error de la aplicación.
 *
 * <p>Cada código identifica unívocamente un tipo de error para que el frontend
 * pueda reaccionar de forma sin depender del texto del mensaje.
 *
 * <p>Convención de prefijos:
 * <ul>
 *   <li><b>ENT-</b> errores de entidad (no existe, ya existe)</li>
 *   <li><b>AUTH-</b> errores de autenticación y autorización</li>
 *   <li><b>NEG-</b> violaciones de reglas de negocio</li>
 *   <li><b>VAL-</b> errores de validación de entrada</li>
 *   <li><b>SRV-</b> errores internos del servidor</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── Entidad ───────────────────────────────────────────────────────────────
    ENTITY_NOT_FOUND      ("ENT-001", "El recurso solicitado no existe."),
    ENTITY_ALREADY_EXISTS ("ENT-002", "El recurso ya existe."),

    // ── Autenticación y autorización ──────────────────────────────────────────
    INVALID_CREDENTIALS   ("AUTH-001", "Credenciales inválidas."),
    ACCESS_DENIED         ("AUTH-002", "No tienes permisos para realizar esta acción."),

    // ── Negocio ───────────────────────────────────────────────────────────────
    BUSINESS_ERROR        ("NEG-001", "Operación no permitida."),
    INVALID_STATE         ("NEG-002", "El recurso no se encuentra en un estado válido para esta operación."),
    CAPACITY_EXCEEDED     ("NEG-003", "Se ha superado la capacidad máxima permitida."),

    // ── Validación ────────────────────────────────────────────────────────────
    VALIDATION_ERROR      ("VAL-001", "Los datos enviados contienen errores de validación."),

    // ── Servidor ──────────────────────────────────────────────────────────────
    INTERNAL_ERROR        ("SRV-001", "Error interno del servidor. Contacta al administrador.");

    /** Identificador del error; incluido en todas las respuestas de error de la API. */
    private final String code;

    /** Mensaje por defecto asociado al código; puede reemplazarse con un mensaje dinámico. */
    private final String message;
}
