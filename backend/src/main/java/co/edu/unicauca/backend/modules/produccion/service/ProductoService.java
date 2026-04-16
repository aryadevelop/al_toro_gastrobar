package co.edu.unicauca.backend.modules.produccion.service;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoOpcionModificacionRepository;
import co.edu.unicauca.backend.modules.produccion.dto.response.CategoriaCartaResponse;
import co.edu.unicauca.backend.modules.produccion.dto.response.GrupoModificacionResponse;
import co.edu.unicauca.backend.modules.produccion.dto.response.MenuEspecialResponse;
import co.edu.unicauca.backend.modules.produccion.dto.response.OpcionModificacionResponse;
import co.edu.unicauca.backend.modules.produccion.dto.response.ProductoCartaResponse;
import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.modules.produccion.repository.ProductoRepository;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoOpcionModificacionRepository productoOpcionModificacionRepository;

    /**
     * Retorna los productos activos de la carta (excluye menús especiales),
     * agrupados por CategoriaCarta, ordenados por el campo orden de la categoría.
     */
    @Transactional(readOnly = true)
    public List<CategoriaCartaResponse> obtenerCarta() {
        List<Producto> productos = productoRepository.findProductosCarta(EstadoGenerico.ACTIVO);

        Map<Integer, List<Producto>> porCategoria = productos.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCategoriaCarta().getCategoriacartaId(),
                        Collectors.toList()
                ));

        return porCategoria.entrySet().stream()
                .map(entry -> {
                    List<Producto> grupo = entry.getValue();
                    var categoria = grupo.get(0).getCategoriaCarta();
                    return CategoriaCartaResponse.builder()
                            .categoriaId(categoria.getCategoriacartaId())
                            .categoriaNombre(categoria.getCategoriaNombre())
                            .orden(categoria.getOrden())
                            .productos(grupo.stream()
                                    .map(this::toProductoCartaResponse)
                                    .collect(Collectors.toList()))
                            .build();
                })
                .sorted((a, b) -> a.getOrden().compareTo(b.getOrden()))
                .collect(Collectors.toList());
    }

    /**
     * Retorna los menús especiales activos con sus opciones de modificación
     * agrupadas por tipo de componente.
     */
    @Transactional(readOnly = true)
    public List<MenuEspecialResponse> obtenerMenusEspeciales() {
        List<Producto> menus = productoRepository
                .findByProductoEstadoAndMenuEspecialTrueOrderByProductoNombreAsc(EstadoGenerico.ACTIVO);

        return menus.stream()
                .map(menu -> {
                    List<OpcionModificacion> opciones = productoOpcionModificacionRepository
                            .findOpcionesActivasByProductoId(menu.getProductoId(), EstadoGenerico.ACTIVO);

                    List<GrupoModificacionResponse> grupos = buildGrupos(opciones);

                    return MenuEspecialResponse.builder()
                            .productoId(menu.getProductoId())
                            .productoNombre(menu.getProductoNombre())
                            .productoDescripcion(menu.getProductoDescripcion())
                            .productoPrecio(menu.getProductoPrecio())
                            .modificacionesPorComponente(grupos)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private ProductoCartaResponse toProductoCartaResponse(Producto p) {
        return ProductoCartaResponse.builder()
                .productoId(p.getProductoId())
                .productoNombre(p.getProductoNombre())
                .productoDescripcion(p.getProductoDescripcion())
                .productoPrecio(p.getProductoPrecio())
                .productoCategoria(p.getProductoCategoria().name())
                .build();
    }

    private List<GrupoModificacionResponse> buildGrupos(List<OpcionModificacion> opciones) {
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
