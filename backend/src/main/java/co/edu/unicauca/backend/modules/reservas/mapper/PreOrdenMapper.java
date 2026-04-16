package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenItemResumenResponse;
import co.edu.unicauca.backend.modules.reservas.entity.PreOrdenDetalle;
import co.edu.unicauca.backend.modules.reservas.entity.PreOrdenMenuModificacion;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper para convertir entidades de pre-orden en sus DTOs de respuesta.
 */
@Component
public class PreOrdenMapper {

    /**
     * Convierte un {@link PreOrdenDetalle} y sus modificaciones en el DTO de detalle completo.
     *
     * @param detalle detalle de pre-orden a convertir
     * @param mods    lista de opciones de modificación seleccionadas para ese ítem
     * @return {@link PreOrdenDetalleResponse} con producto, cantidad, precio y modificaciones
     */
    public PreOrdenDetalleResponse toDetalleResponse(PreOrdenDetalle detalle, List<PreOrdenMenuModificacion> mods) {
        
        // Mapea cada modificación a su DTO anidado de opción seleccionada
        List<PreOrdenDetalleResponse.OpcionModificacionSeleccionada> modificacionesDto = mods.stream()
                .map(m -> PreOrdenDetalleResponse.OpcionModificacionSeleccionada.builder()
                        .opcionId(m.getOpcion().getOpcionId())
                        .opcionNombre(m.getOpcion().getOpcionNombre())
                        .tipoComponente(m.getOpcion().getTipoComponente().name())
                        .build())
                .collect(Collectors.toList());

        return PreOrdenDetalleResponse.builder()
                .preordenDetalleId(detalle.getPreordenDetalleId())
                .productoId(detalle.getProducto().getProductoId())
                .productoNombre(detalle.getProducto().getProductoNombre())
                .cantidad(detalle.getPreordenDetalleCantidad())
                .precioUnitario(detalle.getProducto().getProductoPrecio())
                .descripcion(detalle.getPreordenDetalleDescripcion())
                .modificaciones(modificacionesDto.isEmpty() ? null : modificacionesDto)
                .build();
    }

    /**
     * Convierte un {@link PreOrdenDetalle} en el resumen ligero de ítem usado dentro de
     * {@link co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse}.
     *
     * @param detalle detalle de pre-orden a resumir
     * @return {@link PreOrdenItemResumenResponse} con producto, cantidad, precio y descripción
     */
    public PreOrdenItemResumenResponse toItemResumen(PreOrdenDetalle detalle) {
        return PreOrdenItemResumenResponse.builder()
                .productoId(detalle.getProducto().getProductoId())
                .productoNombre(detalle.getProducto().getProductoNombre())
                .cantidad(detalle.getPreordenDetalleCantidad())
                .precioUnitario(detalle.getProducto().getProductoPrecio())
                .descripcion(detalle.getPreordenDetalleDescripcion())
                .build();
    }
}
