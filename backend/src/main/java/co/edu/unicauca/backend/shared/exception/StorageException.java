package co.edu.unicauca.backend.shared.exception;

/**
 * Excepción lanzada cuando una operación de almacenamiento de archivos falla
 * por razones de I/O, tipo de archivo no permitido o archivo vacío.
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
