package co.edu.unicauca.backend.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra el handler de recursos estáticos para archivos subidos por el ADMIN.
 *
 * <p>
 * Las peticiones a {@code /uploads/**} se resuelven contra el directorio
 * configurado en {@code storage.upload-dir}, que en producción corresponde
 * al volumen Docker {@code altoro_uploads} montado en
 * {@code /opt/altoro/uploads}.
 *
 * <p>
 * Las imágenes del catálogo base (seed V2) siguen sirviendo desde el JAR
 * en {@code /images/**} — ruta distinta, sin conflicto.
 */
@Configuration
public class StorageConfig implements WebMvcConfigurer {

    @Value("${storage.upload-dir}")
    private String uploadDir;

    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .addResourceLocations("classpath:/static/images/");
    }
}
