package co.edu.unicauca.backend.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Swagger UI / OpenAPI 3 para la documentación interactiva de la API.
 *
 * <p>Registra el esquema de seguridad {@code BearerAuth} (JWT) de modo que todos
 * los endpoints protegidos muestren el candado en Swagger UI y permitan enviar
 * el token directamente desde la interfaz sin herramientas externas.
 *
 * <p>Disponible en desarrollo en: {@code http://localhost:8080/swagger-ui.html}
 *
 * @see <a href="https://springdoc.org/">springdoc-openapi</a>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    /**
     * Define la especificación OpenAPI 3 de la aplicación.
     *
     * <p>Incluye la información general de la API (título, descripción y versión)
     * y aplica el esquema de seguridad {@code BearerAuth} de forma global, de modo
     * que todos los endpoints requieran el header {@code Authorization: Bearer <token>}.
     *
     * @return instancia configurada de {@link OpenAPI}
     */
    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Al Toro Gastro-Bar API")
                        .description("API REST para la gestión operativa del restaurante: " +
                                "usuarios, reservas, mesas, comandas, producción, caja e inventario.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
