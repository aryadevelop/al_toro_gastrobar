package co.edu.unicauca.backend.modules.inventario.mapper;

import co.edu.unicauca.backend.modules.inventario.dto.response.ItemAjusteInventarioResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.MovimientoInventarioHistorialResponse;
import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import co.edu.unicauca.backend.modules.inventario.entity.MovimientoInventario;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import org.springframework.stereotype.Component;

/**
 * Mapper que convierte entidades de inventario en DTOs del formulario de ajuste manual.
 */
@Component
public class MovimientoInventarioMapper {

    /**
     * Convierte un {@link Producto} en el DTO de resultado de búsqueda.
     *
     * <p>La unidad de medida se fija en {@code "UNIDAD"} porque {@link Producto}
     * no gestiona unidades de medida en el modelo actual.
     *
     * @param producto producto del catálogo
     * @return DTO con tipo {@code "PRODUCTO"}
     */
    public ItemAjusteInventarioResponse toItemAjusteResponse(Producto producto) {
        return ItemAjusteInventarioResponse.builder()
                .tipo("PRODUCTO")
                .id(producto.getProductoId())
                .nombre(producto.getProductoNombre())
                .stockActual(producto.getStockActual())
                .unidadMedida("UNIDAD")
                .build();
    }

    /**
     * Convierte un {@link Insumo} en el DTO de resultado de búsqueda.
     *
     * @param insumo ingrediente o preparación intermedia del restaurante
     * @return DTO con tipo {@code "INSUMO"}
     */
    public ItemAjusteInventarioResponse toItemAjusteResponse(Insumo insumo) {
        return ItemAjusteInventarioResponse.builder()
                .tipo("INSUMO")
                .id(insumo.getInsumoId())
                .nombre(insumo.getInsumoNombre())
                .stockActual(insumo.getInsumoStockActual())
                .unidadMedida(insumo.getInsumoUnidad().name())
                .build();
    }

    public MovimientoInventarioHistorialResponse toHistorialResponse(MovimientoInventario movimiento) {
        return MovimientoInventarioHistorialResponse.builder()
                .movimientoId(movimiento.getMovimientoId())
                .tipo(movimiento.getMovimientoTipo().name())
                .cantidad(movimiento.getMovimientoCantidad())
                .movimientoFechaHora(movimiento.getMovimientoFechaHora())
                .observaciones(movimiento.getMovimientoObservaciones())
                .productoId(movimiento.getProducto() != null ? movimiento.getProducto().getProductoId() : null)
                .insumoId(movimiento.getInsumo() != null ? movimiento.getInsumo().getInsumoId() : null)
                .build();
    }
}
