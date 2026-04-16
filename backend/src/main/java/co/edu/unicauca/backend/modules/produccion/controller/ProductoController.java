package co.edu.unicauca.backend.modules.produccion.controller;

import co.edu.unicauca.backend.modules.produccion.dto.response.CategoriaCartaResponse;
import co.edu.unicauca.backend.modules.produccion.dto.response.MenuEspecialResponse;
import co.edu.unicauca.backend.modules.produccion.service.ProductoService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Catálogo de productos para pre-orden")
public class ProductoController {

    private final ProductoService productoService;

    /**
     * Retorna los productos activos de la carta (platos y bebidas),
     * agrupados por categoría de carta.
     * Accesible por CLIENTE para el formulario de pre-orden (CA-02).
     */
    @GetMapping("/carta")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Obtener carta de platos y bebidas agrupada por categoría")
    public ResponseEntity<ApiResponse<List<CategoriaCartaResponse>>> obtenerCarta() {
        return ResponseEntity.ok(ApiResponse.ok(productoService.obtenerCarta()));
    }

    /**
     * Retorna los menús especiales activos con sus opciones de modificación agrupadas por tipo de componente.
     * Solo disponible cuando el número de personas es mayor a 10 (validado en frontend).
     */
    @GetMapping("/menu-especial")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Obtener menús especiales con opciones de modificación")
    public ResponseEntity<ApiResponse<List<MenuEspecialResponse>>> obtenerMenusEspeciales() {
        return ResponseEntity.ok(ApiResponse.ok(productoService.obtenerMenusEspeciales()));
    }
}
