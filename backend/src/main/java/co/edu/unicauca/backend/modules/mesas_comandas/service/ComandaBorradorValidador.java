package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.entity.Receta;
import co.edu.unicauca.backend.modules.inventario.repository.RecetaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.TipoProducto;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Validaciones de negocio aplicables al borrador de comanda. Concentra:
 * <ul>
 *   <li>Validación de stock disponible vs cantidad propuesta, diferenciando entre
 *       productos de {@link TipoProducto#VENTA_DIRECTA} (stock del producto) y
 *       {@link TipoProducto#PREPARACION} (stock por insumo según receta).</li>
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

    /** Acceso a las recetas para calcular el consumo de insumos en productos de preparación. */
    private final RecetaRepository recetaRepository;

    /**
     * Valida que la cantidad propuesta no exceda el stock disponible.
     *
     * <p>La ramificación depende del {@link TipoProducto} del producto:
     * <ul>
     *   <li>{@code PREPARACION} — delega en {@link #validarStockPreparacion}: recorre
     *       los insumos de la receta y verifica que cada uno tenga saldo suficiente.</li>
     *   <li>Cualquier otro tipo (incluyendo {@code VENTA_DIRECTA}) — delega en
     *       {@link #validarStockVentaDirecta}: comprueba {@code stockActual} del producto
     *       descontando las unidades ya comprometidas en otros ítems.</li>
     * </ul>
     *
     * @param producto         producto del ítem
     * @param nuevaCantidad    cantidad propuesta tras aplicar la operación
     * @param cantidadAnterior cantidad ya contabilizada del mismo ítem; {@code 0}
     *                         cuando el ítem es nuevo
     * @throws BusinessException con {@code INSUFFICIENT_STOCK} si la operación
     *         excedería el stock disponible
     */
    public void validarStock(Producto producto, int nuevaCantidad, int cantidadAnterior) {
        if (producto.getProductoTipo() == TipoProducto.PREPARACION) {
            validarStockPreparacion(producto, nuevaCantidad, cantidadAnterior);
            return;
        }
        validarStockVentaDirecta(producto, nuevaCantidad, cantidadAnterior);
    }

    /**
     * Verifica la disponibilidad de stock para un producto de venta directa.
     * La fórmula es {@code stockActual - (sumaComprometida - cantidadAnterior)};
     * si {@code stockActual} es {@code null} el control de stock no aplica y se omite.
     *
     * @param producto         producto de venta directa
     * @param nuevaCantidad    cantidad propuesta
     * @param cantidadAnterior unidades ya contabilizadas del mismo ítem
     */
    private void validarStockVentaDirecta(Producto producto, int nuevaCantidad, int cantidadAnterior) {
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
     * Verifica la disponibilidad de insumos para un producto de preparación.
     *
     * <p>Para cada insumo en la receta del producto calcula:
     * <ol>
     *   <li>{@code requerido = recetaCantidad × nuevaCantidad}</li>
     *   <li>{@code yaContabilizado = recetaCantidad × cantidadAnterior} (evita doble-conteo)</li>
     *   <li>{@code disponible = insumoStockActual - (comprometidoTotal - yaContabilizado)}</li>
     * </ol>
     * Si el insumo no tiene receta registrada, la validación se omite.
     *
     * @param producto         producto de preparación
     * @param nuevaCantidad    cantidad propuesta
     * @param cantidadAnterior unidades ya contabilizadas del mismo ítem; {@code 0} si es nuevo
     */
    private void validarStockPreparacion(Producto producto, int nuevaCantidad, int cantidadAnterior) {
        List<Receta> recetas = recetaRepository.findByProductoIdFetchInsumo(producto.getProductoId());
        if (recetas.isEmpty()) {
            return;
        }

        BigDecimal nueva = BigDecimal.valueOf(nuevaCantidad);
        BigDecimal anterior = BigDecimal.valueOf(cantidadAnterior);

        for (Receta r : recetas) {
            Insumo insumo = r.getInsumo();
            BigDecimal requerido = r.getRecetaCantidad().multiply(nueva);
            BigDecimal yaContabilizado = r.getRecetaCantidad().multiply(anterior);
            BigDecimal comprometido = comandaItemRepository
                    .sumCantidadInsumoComprometida(insumo.getInsumoId())
                    .subtract(yaContabilizado);
            BigDecimal disponible = insumo.getInsumoStockActual().subtract(comprometido);
            if (requerido.compareTo(disponible) > 0) {
                throw new BusinessException(
                        ErrorCode.INSUFFICIENT_STOCK,
                        "Stock insuficiente del insumo '" + insumo.getInsumoNombre()
                                + "' para preparar '" + producto.getProductoNombre() + "'.",
                        HttpStatus.CONFLICT);
            }
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
