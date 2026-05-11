package co.edu.unicauca.backend.modules.inventario.controller;

import co.edu.unicauca.backend.modules.inventario.dto.response.CategoriaCartaResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.MenuEspecialResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ProductoBusquedaResponse;
import co.edu.unicauca.backend.modules.inventario.service.ProductoService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * Controlador REST para la consulta del catálogo de productos del restaurante.
 *
 * <p>Comportamiento general:
 * <ul>
 *   <li><b>Carta:</b> retorna los productos activos (platos y bebidas) agrupados
 *       por categoría de carta, ordenados según el campo de orden de cada categoría.</li>
 *   <li><b>Menú especial:</b> retorna los menús especiales activos con sus opciones
 *       de modificación agrupadas por tipo de componente; disponible solo cuando
 *       el número de personas supera 10 (restricción validada en el frontend).</li>
 * </ul>
 *
 * @see ProductoService
 */
@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Catálogo de productos para pre-orden")
public class ProductoController {

    private final ProductoService productoService;

    /**
     * Retorna los productos activos de la carta, agrupados por categoría de carta.
     *
     * <p>Solo incluye productos cuyo campo {@code menuEspecial} es {@code false} o {@code null}.
     * 
     * @return lista de categorías con sus productos activos
     */
    @GetMapping("/carta")
    @PreAuthorize("hasAnyRole('CLIENTE')")
    @Operation(summary = "Obtener carta de platos y bebidas agrupada por categoría")
    public ResponseEntity<ApiResponse<List<CategoriaCartaResponse>>> obtenerCarta() {
        return ResponseEntity.ok(ApiResponse.ok(productoService.obtenerCarta()));
    }

    /**
     * Retorna los menús especiales activos con sus opciones de modificación agrupadas
     * por tipo de componente.
     *
     * <p>Un menú especial es un Producto cuyo campo {@code menuEspecial} es {@code true}. 
     * Cada menú incluye los grupos de modificación disponibles, con las opciones
     * concretas que el cliente puede elegir para personalizar su pedido.
     *
     * @return lista de menús especiales activos, cada uno con sus grupos de modificación
     *         y las opciones disponibles dentro de cada grupo
     */
    @GetMapping("/menu-especial")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Obtener menús especiales con opciones de modificación")
    public ResponseEntity<ApiResponse<List<MenuEspecialResponse>>> obtenerMenusEspeciales() {
        return ResponseEntity.ok(ApiResponse.ok(productoService.obtenerMenusEspeciales()));
    }

    /**
     * Endpoint del buscador de productos: coincidencia parcial case-insensitive
     * por nombre, excluyendo menús especiales y productos inactivos.
     *
     * @param q fragmento de nombre a buscar
     * @return productos coincidentes ordenados alfabéticamente
     */
    @GetMapping("/buscar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Buscar productos por nombre",
               description = "Búsqueda parcial case-insensitive del catálogo, excluye menús especiales")
    public ResponseEntity<ApiResponse<List<ProductoBusquedaResponse>>> buscarProductos(
            @Parameter(description = "Fragmento de nombre a buscar")
            @RequestParam("q") String q) {

        List<ProductoBusquedaResponse> resultados = productoService.buscarProductos(q);
        return ResponseEntity.ok(ApiResponse.ok("Productos encontrados", resultados));
    }
}
