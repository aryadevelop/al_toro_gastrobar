package co.edu.unicauca.backend.shared.storage;

import org.springframework.web.multipart.MultipartFile;

import co.edu.unicauca.backend.shared.exception.StorageException;

/**
 * Contrato para persistir y eliminar archivos subidos por el sistema.
 *
 * <p>La implementación activa para el piloto es {@link LocalDiskStorageService}
 * (volumen Docker). Cuando el volumen de almacenamiento supere la capacidad
 * del VPS o se requiera CDN, se intercambia por una implementación S3/R2.
 *
 * @see LocalDiskStorageService
 */
public interface StorageService {

    /**
     * Persiste el archivo y devuelve su URL pública.
     *
     * @param file      archivo recibido del cliente; no debe ser nulo ni vacío
     * @param subfolder subdirectorio lógico dentro del almacenamiento
     *                  (p.ej. {@code "decoraciones"}, {@code "zonas"})
     * @return ruta pública del archivo (p.ej. {@code /uploads/decoraciones/a1b2c3.jpg})
     * @throws StorageException si el archivo no puede persistirse
     */
    String store(MultipartFile file, String subfolder);

    /**
     * Elimina el archivo identificado por su URL pública.
     *
     * <p>Si la URL no corresponde a un archivo gestionado por este servicio,
     * la operación no hace nada (no lanza excepción).
     *
     * @param publicUrl URL pública devuelta originalmente por {@link #store}
     */
    void delete(String publicUrl);
}
