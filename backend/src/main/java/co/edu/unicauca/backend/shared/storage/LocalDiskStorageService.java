package co.edu.unicauca.backend.shared.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import co.edu.unicauca.backend.shared.exception.StorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Implementación de {@link StorageService} que persiste archivos en el sistema
 * de ficheros local, dentro del directorio configurado por {@code storage.upload-dir}.
 *
 * <p>En producción ese directorio es un volumen Docker montado en
 * {@code /opt/altoro/uploads}, lo que garantiza persistencia entre reinicios
 * del contenedor. En desarrollo apunta a {@code ./uploads/} en el directorio
 * de trabajo del proceso.
 *
 * <p>Los archivos se sirven públicamente en {@code /uploads/{subfolder}/{uuid}.ext}
 * gracias al {@code ResourceHandler} registrado en {@code StorageConfig}.
 *
 * <p>Tipos MIME permitidos: JPEG, PNG y WEBP. El tamaño máximo lo controla
 * {@code spring.servlet.multipart.max-file-size} (actualmente 5 MB).
 *
 * @see StorageConfig
 */
@Service
public class LocalDiskStorageService implements StorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    @Value("${storage.upload-dir}")
    private String uploadDir;

    @Override
    public String store(MultipartFile file, String subfolder) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("El archivo no puede estar vacío");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new StorageException("Tipo de archivo no permitido. Se aceptan: JPEG, PNG, WEBP");
        }

        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
        );
        String extension = extractExtension(originalFilename);
        String filename = UUID.randomUUID() + extension;

        try {
            Path targetDir = Paths.get(uploadDir).resolve(subfolder);
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("No se pudo guardar el archivo: " + filename, e);
        }

        return "/uploads/" + subfolder + "/" + filename;
    }

    @Override
    public void delete(String publicUrl) {
        if (publicUrl == null || !publicUrl.startsWith("/uploads/")) {
            return;
        }
        
        String relativePath = publicUrl.substring("/uploads/".length());
        Path target = Paths.get(uploadDir).resolve(relativePath);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new StorageException("No se pudo eliminar el archivo: " + publicUrl, e);
        }
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex >= 0) ? filename.substring(dotIndex).toLowerCase() : "";
    }
}
