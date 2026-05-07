package co.edu.unicauca.backend.modules.produccion.service;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.repository.MenuBebidaDisponibleRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoOpcionModificacionRepository;
import co.edu.unicauca.backend.modules.produccion.dto.response.CategoriaCartaResponse;
import co.edu.unicauca.backend.modules.produccion.dto.response.MenuEspecialResponse;
import co.edu.unicauca.backend.modules.produccion.entity.CategoriaCarta;
import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.modules.produccion.mapper.ProductoMapper;
import co.edu.unicauca.backend.modules.produccion.repository.ProductoRepository;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * @see co.edu.unicauca.backend.modules.produccion.controller.ProductoController
 * @see co.edu.unicauca.backend.modules.produccion.entity.Producto
 */
@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoOpcionModificacionRepository productoOpcionModificacionRepository;
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
     * agrupa las opciones por {@code tipoComponente} (p. ej. proteína, guarnición, bebida)
     * para facilitar la selección por parte del cliente.
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
}
