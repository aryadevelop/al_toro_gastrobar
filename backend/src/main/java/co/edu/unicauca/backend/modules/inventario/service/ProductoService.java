package co.edu.unicauca.backend.modules.inventario.service;

import co.edu.unicauca.backend.modules.inventario.dto.response.CategoriaCartaResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.MenuEspecialResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ProductoBusquedaResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ProductoInventarioResponse;
import co.edu.unicauca.backend.modules.inventario.entity.CategoriaCarta;
import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.entity.Receta;
import co.edu.unicauca.backend.modules.inventario.mapper.ProductoMapper;
import co.edu.unicauca.backend.modules.inventario.repository.CategoriaCartaRepository;
import co.edu.unicauca.backend.modules.inventario.repository.MenuBebidaDisponibleRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoOpcionModificacionRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.inventario.repository.RecetaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.TipoProducto;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para la consulta del catálogo de productos del restaurante.
 *
 * <p>Centraliza la lógica de lectura del catálogo, incluyendo la agrupación de productos
 * por categoría de carta y la construcción de los grupos de modificación para menús especiales.
 * La conversión a DTOs se delega en {@link ProductoMapper}.
 *
 * <p>Responsabilidades principales:
 * <ul>
 *   <li>Obtener los productos activos de la carta, agrupados y ordenados por categoría.</li>
 *   <li>Obtener los menús especiales activos con sus opciones de modificación agrupadas
 *       por tipo de componente.</li>
 * </ul>
 *
 * @see ProductoMapper
 * @see co.edu.unicauca.backend.modules.inventario.controller.ProductoController
 * @see co.edu.unicauca.backend.modules.inventario.entity.Producto
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaCartaRepository categoriaCartaRepository;
    private final ProductoOpcionModificacionRepository productoOpcionModificacionRepository;
    private final RecetaRepository recetaRepository;
    private final ComandaItemRepository comandaItemRepository;
    private final ProductoMapper productoMapper;
    private final MenuBebidaDisponibleRepository menuBebidaDisponibleRepository;

    /**
     * Retorna los productos activos de la carta agrupados por {@link CategoriaCarta},
     * ordenados por el campo {@code orden} de la categoría de forma ascendente.
     *
     * <p>Excluye los menús especiales (productos con {@code menuEspecial = true}).
     * Dentro de cada categoría, los productos se ordenan alfabéticamente por nombre.
     * La conversión de cada grupo a DTO se delega en {@link ProductoMapper#toCategoriaCartaResponse}.
     *
     * @return lista de {@link CategoriaCartaResponse} ordenada por {@code orden} de categoría;
     *         vacía si no hay productos activos en la carta
     */
    @Transactional(readOnly = true)
    public List<CategoriaCartaResponse> obtenerCarta() {
        List<Producto> productos = productoRepository.findProductosCarta(EstadoGenerico.ACTIVO);

        // Agrupa los productos por ID de categoría para construir un grupo por cada una
        Map<Integer, List<Producto>> porCategoria = productos.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCategoriaCarta().getCategoriacartaId(),
                        Collectors.toList()
                ));

        // Convierte cada grupo en su DTO de categoría y ordena por el campo orden de la categoría
        return porCategoria.entrySet().stream()
                .map(entry -> {
                    List<Producto> grupo = entry.getValue();
                    CategoriaCarta categoria = grupo.get(0).getCategoriaCarta();
                    return productoMapper.toCategoriaCartaResponse(categoria, grupo);
                })
                .sorted((a, b) -> a.getOrden().compareTo(b.getOrden()))
                .collect(Collectors.toList());
    }

    /**
     * Retorna los menús especiales activos con sus opciones de modificación agrupadas
     * por tipo de componente.
     *
     * <p>Para cada menú especial se consultan las opciones de modificación activas y se
     * delega la conversión a {@link ProductoMapper#toMenuEspecialResponse}, que internamente
     * agrupa las opciones por {@code tipoComponente} para facilitar la selección por parte del cliente.
     *
     * @return lista de {@link MenuEspecialResponse} ordenada alfabéticamente por nombre
     *         de producto; vacía si no hay menús especiales activos
     */
    @Transactional(readOnly = true)
    public List<MenuEspecialResponse> obtenerMenusEspeciales() {
        List<Producto> menus = productoRepository
                .findByProductoEstadoAndMenuEspecialTrueOrderByProductoNombreAsc(EstadoGenerico.ACTIVO);

        return menus.stream()
                .map(menu -> {
                    // Carga las opciones de modificación activas para este menú
                    List<OpcionModificacion> opciones = productoOpcionModificacionRepository
                            .findOpcionesActivasByProductoId(menu.getProductoId(), EstadoGenerico.ACTIVO);
                    // Carga las bebidas disponibles asociadas al menú
                    List<Producto> bebidas = menuBebidaDisponibleRepository
                            .findBebidasByMenuId(menu.getProductoId());
                    return productoMapper.toMenuEspecialResponse(menu, opciones, bebidas);
                })
                .collect(Collectors.toList());
    }

    /**
     * Lista los productos del inventario con los campos usados por administración.
     *
     * <p>Incluye productos activos e inactivos, y permite filtrar por categoría de carta
     * y por coincidencia parcial en el nombre.
     *
     * @param categoria nombre de la categoría de carta; no se filtra si es nulo o vacío
     * @param q         fragmento de nombre; no se filtra si es nulo o vacío
     * @return lista de productos de inventario ordenada por categoría y nombre
     */
    @Transactional(readOnly = true)
    public List<ProductoInventarioResponse> listarProductosInventario(String categoria, String q) {
        Integer categoriaId = null;
        if (categoria != null && !categoria.isBlank()) {
            CategoriaCarta categoriaCarta = categoriaCartaRepository
                    .findByCategoriaNombreIgnoreCase(categoria.trim())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.ENTITY_NOT_FOUND,
                            "No existe la categoría '" + categoria.trim() + "'."));
            categoriaId = categoriaCarta.getCategoriacartaId();
        }

        String nombre = (q == null || q.isBlank()) ? null : q.trim();
        return productoRepository.buscarPorCategoriaYNombre(categoriaId, nombre).stream()
                .map(producto -> productoMapper.toInventarioResponse(producto, obtenerStockInventario(producto)))
                .collect(Collectors.toList());
    }

    private BigDecimal obtenerStockInventario(Producto producto) {
        if (producto.getStockActual() != null) {
            return producto.getStockActual().stripTrailingZeros();
        }

        if (producto.getProductoTipo() != TipoProducto.PREPARACION) {
            return null;
        }

        List<Receta> recetas = recetaRepository.findByProductoIdFetchInsumo(producto.getProductoId());
        if (recetas.isEmpty()) {
            return null;
        }

        int maxUnidades = recetas.stream()
                .mapToInt(receta -> {
                    BigDecimal comprometido = comandaItemRepository
                            .sumCantidadInsumoComprometida(receta.getInsumo().getInsumoId());
                    BigDecimal disponible = receta.getInsumo().getInsumoStockActual().subtract(comprometido);
                    if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
                        return 0;
                    }
                    return disponible.divide(receta.getRecetaCantidad(), 0, RoundingMode.FLOOR).intValue();
                })
                .min()
                .orElse(0);

        return BigDecimal.valueOf(maxUnidades);
    }

    /**
     * Busca productos del catálogo por coincidencia parcial en el nombre,
     * excluyendo menús especiales y productos inactivos.
     *
     * @param q fragmento de nombre; si es nulo o blanco devuelve lista vacía
     * @return lista ordenada alfabéticamente; vacía si no hay coincidencias
     */
    @Transactional(readOnly = true)
    public List<ProductoBusquedaResponse> buscarProductos(String q) {
        if (q == null || q.isBlank()) {
            return List.of();
        }
        return productoRepository.buscarPorNombreSinMenu(q.trim(), EstadoGenerico.ACTIVO.name())
                .stream()
                .map(producto -> productoMapper.toBusquedaResponse(producto, obtenerStockInventario(producto)))
                .collect(Collectors.toList());
    }

    /**
     * Retorna el detalle de un producto por su ID para el panel de administración.
     *
     * @param productoId identificador del producto
     * @return DTO con los datos del producto y su stock actual
     * @throws BusinessException si el producto no existe
     */
    @Transactional(readOnly = true)
    public ProductoInventarioResponse obtenerProductoPorId(Long productoId) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND,
                        "Producto no encontrado: " + productoId));
        return productoMapper.toInventarioResponse(producto, obtenerStockInventario(producto));
    }
}
