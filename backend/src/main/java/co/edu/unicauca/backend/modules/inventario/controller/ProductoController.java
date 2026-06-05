package co.edu.unicauca.backend.modules.inventario.controller;

import co.edu.unicauca.backend.modules.inventario.dto.response.CategoriaCartaResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.MenuEspecialResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ProductoBusquedaResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ProductoInventarioResponse;
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
     * Lista todos los productos registrados en inventario.
     *
     * <p>Permite filtrar por categoría de carta y por coincidencia parcial en el nombre.
     * Si la categoría no existe, se devuelve un error de negocio con mensaje descriptivo.
     *
     * @param categoria nombre de categoría de carta
     * @param q         fragmento de nombre a buscar
     * @return lista de productos de inventario para administración
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar productos de inventario",
               description = "Retorna el listado de productos registrados en el inventario de la carta, con filtros opcionales por categoría y por nombre.")
    public ResponseEntity<ApiResponse<List<ProductoInventarioResponse>>> listarProductosInventario(
            @Parameter(description = "Nombre de la categoría de carta para filtrar")
            @RequestParam(value = "categoria", required = false) String categoria,
            @Parameter(description = "Fragmento del nombre del producto para buscar")
            @RequestParam(value = "q", required = false) String q) {

        return ResponseEntity.ok(ApiResponse.ok(productoService.listarProductosInventario(categoria, q)));
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
