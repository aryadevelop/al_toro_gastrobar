package co.edu.unicauca.backend.modules.inventario.mapper;

import co.edu.unicauca.backend.modules.inventario.dto.response.CategoriaCartaResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.GrupoModificacionResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.MenuEspecialResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.OpcionModificacionResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ProductoBebidaResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ProductoBusquedaResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ProductoCartaResponse;
import co.edu.unicauca.backend.modules.inventario.entity.CategoriaCarta;
import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entidades del módulo de producción en sus DTOs de respuesta.
 *
 * @see co.edu.unicauca.backend.modules.inventario.service.ProductoService
 */
@Component
public class ProductoMapper {

    /**
     * Convierte una {@link CategoriaCarta} y su lista de productos en el DTO de respuesta
     * de categoría para la carta.
     *
     * <p>Los productos se convierten individualmente con {@link #toProductoCartaResponse(Producto)}.
     *
     * @param categoria categoría de carta a convertir
     * @param productos lista de productos que pertenecen a la categoría
     * @return {@link CategoriaCartaResponse} con los datos de la categoría y sus productos
     */
    public CategoriaCartaResponse toCategoriaCartaResponse(CategoriaCarta categoria,
                                                            List<Producto> productos) {
        return CategoriaCartaResponse.builder()
                .categoriaId(categoria.getCategoriacartaId())
                .categoriaNombre(categoria.getCategoriaNombre())
                .orden(categoria.getOrden())
                .productos(productos.stream()
                        .map(this::toProductoCartaResponse)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Convierte un {@link Producto} en su DTO de respuesta para la carta.
     *
     * @param p producto a convertir
     * @return {@link ProductoCartaResponse} con los campos expuestos al cliente
     */
    public ProductoCartaResponse toProductoCartaResponse(Producto p) {
        return ProductoCartaResponse.builder()
                .productoId(p.getProductoId())
                .productoNombre(p.getProductoNombre())
                .productoDescripcion(p.getProductoDescripcion())
                .productoPrecio(p.getProductoPrecio())
                .productoCategoria(p.getProductoCategoria().name())
                .build();
    }

    /**
     * Convierte un {@link Producto} de tipo menú especial, sus opciones de modificación activas
     * y las bebidas disponibles en el DTO de respuesta.
     *
     * <p>La agrupación de opciones se delega en {@link #toGruposModificacion(List)}.
     * Las bebidas se convierten individualmente a {@link ProductoBebidaResponse}.
     *
     * @param menu               producto de tipo menú especial a convertir
     * @param opciones           opciones de modificación activas asociadas al menú
     * @param bebidasDisponibles bebidas activas asociadas al menú; nunca {@code null}
     * @return {@link MenuEspecialResponse} con los datos del menú, sus grupos de modificación
     *         y las bebidas disponibles
     */
    public MenuEspecialResponse toMenuEspecialResponse(Producto menu,
                                                        List<OpcionModificacion> opciones,
                                                        List<Producto> bebidasDisponibles) {
        List<ProductoBebidaResponse> bebidas = bebidasDisponibles.stream()
                .map(b -> new ProductoBebidaResponse(
                        b.getProductoId(), b.getProductoNombre()))
                .toList();

        return MenuEspecialResponse.builder()
                .productoId(menu.getProductoId())
                .productoNombre(menu.getProductoNombre())
                .productoDescripcion(menu.getProductoDescripcion())
                .productoPrecio(menu.getProductoPrecio())
                .modificacionesPorComponente(toGruposModificacion(opciones))
                .bebidasDisponibles(bebidas)
                .build();
    }

    /**
     * Convierte un {@link Producto} a {@link ProductoBusquedaResponse} para la
     * respuesta del buscador del formulario de modificar comanda.
     *
     * @param p producto persistido a transformar
     * @return DTO con los campos de búsqueda
     */
    public ProductoBusquedaResponse toBusquedaResponse(Producto p) {
        return ProductoBusquedaResponse.builder()
                .productoId(p.getProductoId())
                .productoNombre(p.getProductoNombre())
                .productoPrecio(p.getProductoPrecio())
                .productoCategoria(p.getProductoCategoria().name())
                .build();
    }

    // -----------------------------------------------------------------------
    // Helpers privados
    // -----------------------------------------------------------------------

    /**
     * Agrupa una lista de {@link OpcionModificacion} por tipo de componente y construye
     * la lista de {@link GrupoModificacionResponse} ordenada alfabéticamente por
     * {@code tipoComponente}.
     *
     * @param opciones opciones de modificación activas de un menú especial
     * @return lista de grupos de modificación ordenada por tipo de componente
     */
    private List<GrupoModificacionResponse> toGruposModificacion(List<OpcionModificacion> opciones) {
        Map<String, List<OpcionModificacion>> porTipo = opciones.stream()
                .collect(Collectors.groupingBy(o -> o.getTipoComponente().name()));

        return porTipo.entrySet().stream()
                .map(entry -> GrupoModificacionResponse.builder()
                        .tipoComponente(entry.getKey())
                        .tipoComponenteDescripcion(
                                entry.getValue().get(0).getTipoComponente().getDescripcion())
                        .opciones(entry.getValue().stream()
                                .map(o -> OpcionModificacionResponse.builder()
                                        .opcionId(o.getOpcionId())
                                        .opcionNombre(o.getOpcionNombre())
                                        .build())
                                .collect(Collectors.toList()))
                        .build())
                .sorted((a, b) -> a.getTipoComponente().compareTo(b.getTipoComponente()))
                .collect(Collectors.toList());
    }
}
