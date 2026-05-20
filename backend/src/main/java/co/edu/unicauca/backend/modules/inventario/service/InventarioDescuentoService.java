package co.edu.unicauca.backend.modules.inventario.service;

import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import co.edu.unicauca.backend.modules.inventario.entity.MovimientoInventario;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.entity.Receta;
import co.edu.unicauca.backend.modules.inventario.repository.InsumoRepository;
import co.edu.unicauca.backend.modules.inventario.repository.MovimientoInventarioRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.inventario.repository.RecetaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.TipoMovimiento;
import co.edu.unicauca.backend.shared.enums.TipoProducto;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aplica el descuento de inventario asociado a iniciar la preparación de una
 * comanda. 
 * 
 * Para productos de venta directa decrementa el stock del producto.
 * Para productos de preparación recorre la receta y decrementa el stock de
 * cada insumo. 
 * En ambos casos persiste un {@link MovimientoInventario} de tipo {@code EGRESO} 
 * para conservar la trazabilidad.
 *
 * <p>Los ítems pertenecientes a un menú especial (con
 * {@code comandaItemMenuGrupo} no nulo) se ignoran.
 */
@Service
@RequiredArgsConstructor
public class InventarioDescuentoService {

    private final ComandaItemRepository comandaItemRepository;
    private final RecetaRepository recetaRepository;
    private final ProductoRepository productoRepository;
    private final InsumoRepository insumoRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    /**
     * Descuenta el inventario consumido por todos los ítems de la comanda y
     * registra los movimientos correspondientes. Cualquier error
     * provoca {@link BusinessException} para que la transacción del llamador
     * revierta los cambios.
     *
     * @param comanda comanda cuyos ítems se procesan
     * @param actor   empleado responsable del descuento, queda registrado en
     *                cada {@link MovimientoInventario}
     * @throws BusinessException con {@link ErrorCode#INSUFFICIENT_STOCK} si
     *                           algún producto o insumo no tiene stock
     *                           suficiente
     */
    @Transactional
    public void descontarPorComanda(Comanda comanda, Empleado actor) {
        // Carga los ítems en orden para que los movimientos queden trazables
        List<ComandaItem> items = comandaItemRepository
                .findByComanda_ComandaIdOrderByProductoNombreAsc(comanda.getComandaId());

        for (ComandaItem item : items) {
            // Los ítems de menú especial nunca decrementan inventario por contrato del módulo
            if (item.getComandaItemMenuGrupo() != null) {
                continue;
            }

            // Obtener el producto de cada comanda
            Producto producto = item.getProducto();
            int cantidad = item.getComandaItemCantidad();

            // Descontar según el tipo de producto
            if (producto.getProductoTipo() == TipoProducto.VENTA_DIRECTA) {
                descontarVentaDirecta(producto, cantidad, actor);
            } else if (producto.getProductoTipo() == TipoProducto.PREPARACION) {
                descontarPreparacion(producto, cantidad, actor);
            }
        }
    }

    private void descontarVentaDirecta(Producto productoRef, int cantidad, Empleado actor) {
        // Adquirir lock pesimista sobre la fila para serializar egresos concurrentes
        Producto producto = productoRepository.findByIdForUpdate(productoRef.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto", productoRef.getProductoId()));

        // Cuando el catálogo no gestiona stock para el producto el descuento se omite silenciosamente
        if (producto.getStockActual() == null) {
            return;
        }

        // Obtener el nuevo stock restando la cantidad pedida al stock actual
        BigDecimal pedido = BigDecimal.valueOf(cantidad);
        BigDecimal nuevo = producto.getStockActual().subtract(pedido);

        // Si el nuevo stock es negativo, se lanza excepción para que la transacción revierta
        if (nuevo.signum() < 0) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_STOCK,
                    "Stock insuficiente del producto '" + producto.getProductoNombre() + "'.",
                    HttpStatus.CONFLICT);
        }

        // Actualizar el stock del producto y guardar el movimiento de inventario
        producto.setStockActual(nuevo);
        productoRepository.save(producto);
        movimientoRepository.save(MovimientoInventario.builder()
                .producto(producto)
                .movimientoCantidad(pedido)
                .movimientoTipo(TipoMovimiento.EGRESO)
                .empleado(actor)
                .build());
    }

    private void descontarPreparacion(Producto producto, int cantidad, Empleado actor) {
        // El producto puede no tener receta registrada; en ese caso no hay nada que descontar
        List<Receta> recetas = recetaRepository.findByProductoIdFetchInsumo(producto.getProductoId());
        BigDecimal cantidadDecimal = BigDecimal.valueOf(cantidad);

        for (Receta receta : recetas) {
            // Adquirir lock pesimista sobre la fila del insumo para serializar descuentos concurrentes
            Insumo insumo = insumoRepository.findByIdForUpdate(receta.getInsumoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Insumo", receta.getInsumoId()));

            // Calcular el stock requerido multiplicando la cantidad de insumo por la cantidad del producto
            BigDecimal requerido = receta.getRecetaCantidad().multiply(cantidadDecimal);
            BigDecimal nuevo = insumo.getInsumoStockActual().subtract(requerido);

            // Si el nuevo stock es negativo, se lanza excepción para que la transacción revierta
            if (nuevo.signum() < 0) {
                throw new BusinessException(
                        ErrorCode.INSUFFICIENT_STOCK,
                        "Stock insuficiente del insumo '" + insumo.getInsumoNombre()
                                + "' para preparar '" + producto.getProductoNombre() + "'.",
                        HttpStatus.CONFLICT);
            }

            // Actualizar el stock del insumo y guardar el movimiento de inventario
            insumo.setInsumoStockActual(nuevo);
            insumoRepository.save(insumo);
            movimientoRepository.save(MovimientoInventario.builder()
                    .insumo(insumo)
                    .movimientoCantidad(requerido)
                    .movimientoTipo(TipoMovimiento.EGRESO)
                    .empleado(actor)
                    .build());
        }
    }
}
