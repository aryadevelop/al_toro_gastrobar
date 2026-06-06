package co.edu.unicauca.backend.modules.reservas.controller;

import co.edu.unicauca.backend.modules.reservas.service.DecoracionAdminService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controlador para la gestión de imágenes asociadas a las decoraciones.
 */
@RestController
@RequestMapping("/api/decoraciones")
@RequiredArgsConstructor
@Tag(name = "Decoraciones", description = "Gestión administrativa de decoraciones")
public class DecoracionAdminController {

    private final DecoracionAdminService decoracionAdminService;

    @PostMapping("/{decoracionId}/imagen")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Subir o reemplazar la imagen de una decoración")
    public ResponseEntity<ApiResponse<String>> subirImagenDecoracion(
            @PathVariable Long decoracionId,
            @RequestParam("imagen") MultipartFile imagen) {

        String imagenUrl = decoracionAdminService.guardarImagenDecoracion(decoracionId, imagen);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Imagen de decoración subida correctamente", imagenUrl));
    }

    @DeleteMapping("/{decoracionId}/imagen")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar la imagen de una decoración")
    public ResponseEntity<ApiResponse<Void>> eliminarImagenDecoracion(
            @PathVariable Long decoracionId) {

        decoracionAdminService.borrarImagenDecoracion(decoracionId);
        return ResponseEntity.ok(ApiResponse.message("Imagen de decoración eliminada correctamente"));
    }
}
