package co.edu.unicauca.backend.shared.bootstrap;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Copia las imágenes del catálogo base (zonas y decoraciones) desde el JAR
 * al directorio de uploads en el primer arranque.
 *
 * <p>La operación es idempotente: si el archivo ya existe en el destino no se
 * sobreescribe, preservando cualquier imagen que el ADMIN haya subido después.
 * Corre antes que {@link ProdAdminBootstrap} (Order 1) para que las URLs del
 * seed sean resolvibles desde el primer request.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class SeedImagesCopyRunner implements ApplicationRunner {

    @Value("${storage.upload-dir}")
    private String uploadDir;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        copyFolder("zonas");
        copyFolder("decoraciones");
    }

    private void copyFolder(String subfolder) throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:static/images/" + subfolder + "/*");

        Path targetDir = Paths.get(uploadDir).resolve(subfolder);
        Files.createDirectories(targetDir);

        for (Resource resource : resources) {
            Path target = targetDir.resolve(resource.getFilename());
            if (!Files.exists(target)) {
                Files.copy(resource.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
