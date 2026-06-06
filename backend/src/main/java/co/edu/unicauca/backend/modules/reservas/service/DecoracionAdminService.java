package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class DecoracionAdminService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final long MAX_BYTES = 5L * 1024L * 1024L;

    private final DecoracionRepository decoracionRepository;
    private final Path imagenesDirectory;
    private final String imagenesBaseUrl;

    public DecoracionAdminService(
            DecoracionRepository decoracionRepository,
            @Value("${decoracion.imagenes-dir}") String imagenesDir,
            @Value("${decoracion.imagenes-base-url:}") String imagenesBaseUrl) {
        this.decoracionRepository = decoracionRepository;
        Path path = Paths.get(imagenesDir);
        if (path.isAbsolute()) {
            this.imagenesDirectory = path.toAbsolutePath().normalize();
        } else {
            this.imagenesDirectory = Paths.get(".").toAbsolutePath().resolve(path).normalize();
        }
        this.imagenesBaseUrl = imagenesBaseUrl != null ? imagenesBaseUrl.trim() : "";
    }

    @Transactional
    public String guardarImagenDecoracion(Long decoracionId, MultipartFile imagen) {
        if (imagen == null || imagen.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "La imagen es obligatoria.");
        }

        if (imagen.getSize() > MAX_BYTES) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "El tamaño de la imagen no puede superar 5 MB.");
        }

        String originalFilename = imagen.getOriginalFilename();
        String extension = extraerExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Solo se aceptan imágenes JPG o PNG.");
        }

        Decoracion decoracion = decoracionRepository.findById(decoracionId)
                .orElseThrow(() -> new ResourceNotFoundException("Decoracion", decoracionId));

        try {
            Files.createDirectories(imagenesDirectory);
        } catch (IOException e) {
            log.error("Error al crear el directorio de imágenes de decoración", e);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "No se pudo preparar la carpeta de almacenamiento de imágenes.");
        }

        String fileName = String.format("decoracion-%s-%s.%s", decoracionId, UUID.randomUUID(), extension);
        Path targetFile = imagenesDirectory.resolve(fileName);

        try (InputStream inputStream = imagen.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Error al guardar la imagen de la decoración", e);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "No se pudo guardar la imagen de la decoración.");
        }

        String url = buildImageUrl(fileName);
        String previousUrl = decoracion.getDecoracionImagenUrl();
        decoracion.setDecoracionImagenUrl(url);
        decoracionRepository.save(decoracion);
        borrarArchivoAnteriorSiExiste(previousUrl);
        return url;
    }

    @Transactional
    public void borrarImagenDecoracion(Long decoracionId) {
        Decoracion decoracion = decoracionRepository.findById(decoracionId)
                .orElseThrow(() -> new ResourceNotFoundException("Decoracion", decoracionId));

        String previousUrl = decoracion.getDecoracionImagenUrl();
        decoracion.setDecoracionImagenUrl(null);
        decoracionRepository.save(decoracion);
        borrarArchivoAnteriorSiExiste(previousUrl);
    }

    private String buildImageUrl(String fileName) {
        if (StringUtils.hasText(imagenesBaseUrl)) {
            String prefix = imagenesBaseUrl.endsWith("/")
                    ? imagenesBaseUrl.substring(0, imagenesBaseUrl.length() - 1)
                    : imagenesBaseUrl;
            return prefix + "/" + fileName;
        }

        Path targetFile = imagenesDirectory.resolve(fileName).normalize();
        try {
            Path relativePath = Paths.get(".").toAbsolutePath().normalize().relativize(targetFile);
            return relativePath.toString().replace('\\', '/');
        } catch (IllegalArgumentException e) {
            return targetFile.toString().replace('\\', '/');
        }
    }

    private void borrarArchivoAnteriorSiExiste(String imagenUrl) {
        if (!StringUtils.hasText(imagenUrl)) {
            return;
        }

        Path path;
        try {
            path = resolverRutaLocal(imagenUrl);
        } catch (Exception e) {
            log.warn("No se puede resolver ruta local de la imagen anterior: {}", imagenUrl, e);
            return;
        }

        if (path == null) {
            log.debug("Imagen anterior no corresponde a un archivo local, no se borra del disco: {}", imagenUrl);
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("No se pudo eliminar la imagen anterior de decoración: {}", path, e);
        }
    }

    private Path resolverRutaLocal(String imagenUrl) {
        if (StringUtils.hasText(imagenesBaseUrl) && imagenUrl.startsWith(imagenesBaseUrl)) {
            String relativePath = imagenUrl.substring(imagenesBaseUrl.length());
            relativePath = relativePath.replaceFirst("^/+", "");
            Path candidate = imagenesDirectory.resolve(relativePath).normalize();
            return validarRutaDentroDeDirectorio(candidate);
        }

        if (imagenUrl.startsWith("file:")) {
            try {
                Path candidate = Paths.get(URI.create(imagenUrl));
                return validarRutaDentroDeDirectorio(candidate);
            } catch (Exception ignored) {
                return null;
            }
        }

        if (imagenUrl.startsWith("http:") || imagenUrl.startsWith("https:")) {
            return null;
        }

        try {
            Path candidate = Paths.get(imagenUrl);
            return validarRutaDentroDeDirectorio(candidate);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    private Path validarRutaDentroDeDirectorio(Path candidate) {
        if (!candidate.toAbsolutePath().normalize().startsWith(imagenesDirectory)) {
            return null;
        }
        return candidate;
    }

    private String extraerExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "El nombre del archivo debe contener extensión JPG o PNG.");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        return extension;
    }
}
