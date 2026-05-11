package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validaciones de negocio aplicables al borrador de comanda. Concentra:
 * <ul>
 *   <li>Validación de stock disponible vs cantidad propuesta.</li>
 *   <li>Resolución de estación destino a partir de la categoría del producto.</li>
 *   <li>Verificación de que la comanda tenga al menos un ítem antes de enviarse
 *       a producción.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ComandaBorradorValidador {

    /** Acceso al cómputo de stock comprometido para la fórmula de disponibilidad. */
    private final ComandaItemRepository comandaItemRepository;

    /**
     * Valida que la cantidad propuesta no exceda el stock disponible. La fórmula
     * de disponibilidad es {@code stockActual - (sumaComprometida - cantidadAnterior)}:
     * la {@code cantidadAnterior} se resta del comprometido para no doble-contar
     * el ítem en cuestión cuando ya estaba persistido.
     *
     * @param producto         producto del ítem; si {@code stockActual} es {@code null}
     *                         no se gestiona stock y se omite la validación
     * @param nuevaCantidad    cantidad propuesta tras aplicar la operación
     * @param cantidadAnterior cantidad ya contabilizada del mismo ítem; {@code 0}
     *                         cuando el ítem es nuevo
     * @throws BusinessException con {@code INSUFFICIENT_STOCK} si la operación
     *         excedería el stock disponible
     */
    public void validarStock(Producto producto, int nuevaCantidad, int cantidadAnterior) {
        if (producto.getStockActual() == null) {
            return;
        }
        long comprometido = comandaItemRepository
                .sumCantidadComprometidaByProducto(producto.getProductoId());
        long disponible = producto.getStockActual().longValue() - (comprometido - cantidadAnterior);
        if (nuevaCantidad > disponible) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    "Solo hay " + Math.max(disponible, 0) + " unidades disponibles de este producto",
                    HttpStatus.CONFLICT);
        }
    }

    /**
     * Resuelve la estación destino según la categoría del producto.
     * {@code PLATO} va a COCINA, {@code BEBIDA} a BARRA. Cualquier otra
     * categoría (incluida {@code OTRO}, deprecada) se rechaza.
     *
     * @param producto producto a clasificar
     * @return estación destino
     * @throws BusinessException si la categoría no es soportada
     */
    public EstacionComanda resolverEstacion(Producto producto) {
        CategoriaProducto cat = producto.getProductoCategoria();
        return switch (cat) {
            case PLATO  -> EstacionComanda.COCINA;
            case BEBIDA -> EstacionComanda.BARRA;
            default -> throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "La categoría de producto '" + cat + "' no se admite en este flujo",
                    HttpStatus.BAD_REQUEST);
        };
    }

    /**
     * Verifica que la comanda tenga al menos un ítem antes de enviarse a producción.
     *
     * @param items ítems persistidos de la comanda
     * @throws BusinessException si la lista es nula o vacía
     */
    public void validarTieneItems(List<ComandaItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_ERROR,
                    "La comanda debe tener al menos un producto antes de enviarse a producción",
                    HttpStatus.BAD_REQUEST);
        }
    }
}
