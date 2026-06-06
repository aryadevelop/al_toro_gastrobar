package co.edu.unicauca.backend.modules.reservas.controller;

import co.edu.unicauca.backend.modules.reservas.dto.request.ActualizarDecoracionRequest;
import co.edu.unicauca.backend.modules.reservas.dto.request.CambioEstadoDecoracionRequest;
import co.edu.unicauca.backend.modules.reservas.dto.request.CrearDecoracionRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.DecoracionAdminResponse;
import co.edu.unicauca.backend.modules.reservas.service.DecoracionAdminService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todas las decoraciones")
    public ResponseEntity<ApiResponse<List<DecoracionAdminResponse>>> listarDecoraciones() {
        return ResponseEntity.ok(ApiResponse.ok(decoracionAdminService.listarDecoraciones()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear una nueva decoración")
    public ResponseEntity<ApiResponse<DecoracionAdminResponse>> crearDecoracion(
            @Valid @RequestBody CrearDecoracionRequest request) {

        DecoracionAdminResponse response = decoracionAdminService.crearDecoracion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Decoración creada correctamente", response));
    }

    @PutMapping("/{decoracionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar una decoración existente")
    public ResponseEntity<ApiResponse<DecoracionAdminResponse>> actualizarDecoracion(
            @PathVariable Long decoracionId,
            @Valid @RequestBody ActualizarDecoracionRequest request) {

        DecoracionAdminResponse response = decoracionAdminService.actualizarDecoracion(decoracionId, request);
        return ResponseEntity.ok(ApiResponse.ok("Decoración actualizada correctamente", response));
    }

    @PatchMapping("/{decoracionId}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cambiar el estado activo/inactivo de una decoración")
    public ResponseEntity<ApiResponse<DecoracionAdminResponse>> cambiarEstadoDecoracion(
            @PathVariable Long decoracionId,
            @Valid @RequestBody CambioEstadoDecoracionRequest request) {

        DecoracionAdminResponse response = decoracionAdminService.cambiarEstadoDecoracion(decoracionId, request.getEstado());
        return ResponseEntity.ok(ApiResponse.ok("Estado de decoración actualizado correctamente", response));
    }

    @DeleteMapping("/{decoracionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar una decoración")
    public ResponseEntity<ApiResponse<Void>> eliminarDecoracion(
            @PathVariable Long decoracionId) {

        decoracionAdminService.eliminarDecoracion(decoracionId);
        return ResponseEntity.ok(ApiResponse.message("Decoración eliminada correctamente"));
    }
}
