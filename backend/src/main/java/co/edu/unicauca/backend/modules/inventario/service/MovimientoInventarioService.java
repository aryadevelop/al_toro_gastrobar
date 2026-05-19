package co.edu.unicauca.backend.modules.inventario.service;

import co.edu.unicauca.backend.modules.inventario.dto.request.AjusteInventarioRequest;
import co.edu.unicauca.backend.modules.inventario.dto.response.AjusteInventarioResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ItemAjusteInventarioResponse;
import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import co.edu.unicauca.backend.modules.inventario.entity.MovimientoInventario;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.mapper.MovimientoInventarioMapper;
import co.edu.unicauca.backend.modules.inventario.repository.InsumoRepository;
import co.edu.unicauca.backend.modules.inventario.repository.MovimientoInventarioRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.inventario.repository.RecetaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaProduccionEventoWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.TipoEventoProduccion;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.modules.notificaciones.service.InventarioWsPublisher;
import co.edu.unicauca.backend.modules.notificaciones.service.MesaWsPublisher;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoMovimiento;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de negocio para el ajuste manual de inventario.
 *
 * <p>Gestiona el buscador unificado de productos e insumos y el registro de
 * movimientos de ingreso o egreso. Ante un egreso que afecte comandas
 * {@code PENDIENTE}, crea una notificación {@code CAMBIO} por cada comanda
 * afectada y publica los eventos WebSocket que retiran la comanda del tablero
 * de producción.
 */
@Service
@RequiredArgsConstructor
public class MovimientoInventarioService {

    private final ProductoRepository productoRepository;
    private final InsumoRepository insumoRepository;
    private final RecetaRepository recetaRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final ComandaRepository comandaRepository;
    private final MesaRepository mesaRepository;
    private final NotificacionRepository notificacionRepository;
    private final EmpleadoRepository empleadoRepository;
    private final MovimientoInventarioMapper mapper;
    private final NotificacionWsPublisher wsPublisher;
    private final MesaWsPublisher mesaWsPublisher;
    private final InventarioWsPublisher inventarioWsPublisher;

    /**
     * Busca productos e insumos activos cuyo nombre coincida parcialmente con el
     * texto indicado, para poblar el buscador del formulario de ajuste manual.
     *
     * @param q fragmento de nombre a buscar
     * @return lista unificada de productos e insumos, ordenada alfabéticamente
     */
    @Transactional(readOnly = true)
    public List<ItemAjusteInventarioResponse> buscarItemsAjuste(String q) {
        List<ItemAjusteInventarioResponse> resultado = new ArrayList<>();

        productoRepository.buscarParaAjuste(q, EstadoGenerico.ACTIVO)
                .forEach(p -> resultado.add(mapper.toItemAjusteResponse(p)));
        insumoRepository.buscarPorNombreActivo(q, EstadoGenerico.ACTIVO)
                .forEach(i -> resultado.add(mapper.toItemAjusteResponse(i)));

        // Orden alfabético unificado sobre productos e insumos
        resultado.sort((a, b) -> a.getNombre().compareToIgnoreCase(b.getNombre()));
        return resultado;
    }

    /**
     * Registra un movimiento de ingreso o egreso en el inventario.
     *
     * <p>En EGRESO valida stock suficiente, descuenta, e identifica las comandas
     * {@code PENDIENTE} afectadas para crear una notificación {@code CAMBIO}
     * (sin duplicar) y emitir el evento {@code ELIMINADA} al tablero. En INGRESO
     * incrementa el stock sin validación. En ambos casos difunde el cambio de
     * stock al tópico {@code /topic/inventario}.
     *
     * @param request datos del ajuste
     * @param auth    contexto del usuario autenticado
     * @return identificador del movimiento, stock resultante y comandas notificadas
     * @throws BusinessException        si productoId/insumoId son ambos nulos o
     *                                  ambos presentes, o el stock es insuficiente
     * @throws ResourceNotFoundException si el empleado, producto o insumo no existe
     */
    @Transactional
    public AjusteInventarioResponse registrarAjuste(AjusteInventarioRequest request, Authentication auth) {
        // Validación: debe venir exactamente un ID, de producto o de insumo
        boolean tieneProducto = request.getProductoId() != null;
        boolean tieneInsumo = request.getInsumoId() != null;
        if (!tieneProducto && !tieneInsumo) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "El producto o insumo es obligatorio.", HttpStatus.BAD_REQUEST);
        }
        if (tieneProducto && tieneInsumo) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Solo se puede ajustar un producto o un insumo, no ambos.", HttpStatus.BAD_REQUEST);
        }

        // El actor es el empleado autenticado que realiza el ajuste
        Empleado actor = empleadoRepository.findByUsuario_UsuarioEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", auth.getName()));

        BigDecimal nuevoStock;
        int notificadas;

        // Construye el movimiento común a producto o insumo
        MovimientoInventario.MovimientoInventarioBuilder movimiento = MovimientoInventario.builder()
                .empleado(actor)
                .movimientoCantidad(request.getCantidad())
                .movimientoTipo(request.getTipo())
                .movimientoProveedor(request.getProveedor())
                .movimientoNumeroFactura(request.getNumeroFactura())
                .movimientoObservaciones(request.getObservaciones());

        // Ajusta stock e identifica comandas afectadas según tipo de ítem
        if (tieneProducto) {
            Producto producto = productoRepository.findById(request.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto", request.getProductoId()));
            nuevoStock = ajustarStockProducto(producto, request.getCantidad(), request.getTipo());
            notificadas = request.getTipo() == TipoMovimiento.EGRESO
                    ? notificarComandasAfectadasPorProducto(producto.getProductoId())
                    : 0;
            movimiento.producto(producto);
            publicarStockActualizado(producto.getProductoId(), null, nuevoStock);
        } else {
            Insumo insumo = insumoRepository.findById(request.getInsumoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Insumo", request.getInsumoId()));
            nuevoStock = ajustarStockInsumo(insumo, request.getCantidad(), request.getTipo());
            notificadas = request.getTipo() == TipoMovimiento.EGRESO
                    ? notificarComandasAfectadasPorInsumo(insumo.getInsumoId())
                    : 0;
            movimiento.insumo(insumo);
            publicarStockActualizado(null, insumo.getInsumoId(), nuevoStock);
        }

        // Registra el movimiento de inventario
        MovimientoInventario guardado = movimientoRepository.save(movimiento.build());
        return AjusteInventarioResponse.builder()
                .movimientoId(guardado.getMovimientoId())
                .stockActualizado(nuevoStock)
                .comandasNotificadas(notificadas)
                .build();
    }

    private BigDecimal ajustarStockProducto(Producto producto, BigDecimal cantidad, TipoMovimiento tipo) {
        BigDecimal actual = producto.getStockActual() != null ? producto.getStockActual() : BigDecimal.ZERO;
        if (tipo == TipoMovimiento.EGRESO && actual.compareTo(cantidad) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Stock insuficiente. Stock actual: " + actual + " UNIDAD.", HttpStatus.CONFLICT);
        }
        BigDecimal nuevo = tipo == TipoMovimiento.EGRESO ? actual.subtract(cantidad) : actual.add(cantidad);
        producto.setStockActual(nuevo);
        productoRepository.save(producto);
        return nuevo;
    }

    private BigDecimal ajustarStockInsumo(Insumo insumo, BigDecimal cantidad, TipoMovimiento tipo) {
        BigDecimal actual = insumo.getInsumoStockActual();
        if (tipo == TipoMovimiento.EGRESO && actual.compareTo(cantidad) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Stock insuficiente. Stock actual: " + actual
                            + " " + insumo.getInsumoUnidad().name() + ".", HttpStatus.CONFLICT);
        }
        BigDecimal nuevo = tipo == TipoMovimiento.EGRESO ? actual.subtract(cantidad) : actual.add(cantidad);
        insumo.setInsumoStockActual(nuevo);
        insumoRepository.save(insumo);
        return nuevo;
    }

    private int notificarComandasAfectadasPorProducto(Long productoId) {
        return contarNotificadas(comandaRepository.findPendientesByProductoId(productoId));
    }

    private int notificarComandasAfectadasPorInsumo(Long insumoId) {
        // Productos de preparación cuya receta usa este insumo
        List<Long> productoIds = recetaRepository.findProductoIdsByInsumoId(insumoId);
        if (productoIds.isEmpty()) {
            return 0;
        }
        return contarNotificadas(comandaRepository.findPendientesByProductoIds(productoIds));
    }

    private int contarNotificadas(List<Comanda> comandas) {
        int creadas = 0;
        for (Comanda comanda : comandas) {
            if (crearNotificacionCambioSiNoExiste(comanda)) {
                creadas++;
            }
        }
        return creadas;
    }

    /**
     * Crea una notificación {@code CAMBIO ACTIVA} para la comanda si no existe ya
     * una, y emite los eventos WebSocket asociados.
     *
     * @return {@code true} si se creó una notificación nueva
     */
    private boolean crearNotificacionCambioSiNoExiste(Comanda comanda) {
        // No se puede crear una segunda notificación de cambio mientras haya una ACTIVA
        boolean existe = notificacionRepository
                .findFirstByComanda_ComandaIdAndNotificacionTipoAndNotificacionEstado(
                        comanda.getComandaId(), TipoNotificacion.CAMBIO, EstadoNotificacion.ACTIVA)
                .isPresent();
        if (existe) {
            return false;
        }

        // La visita debe tener mesa asignada para poder notificar al mesero
        Long visitaId = comanda.getVisita().getVisitaId();
        Mesa mesa = mesaRepository.findByVisita_VisitaId(visitaId).orElse(null);
        
        // Una comanda PENDIENTE siempre tiene mesa asignada
        if (mesa == null) {
            return false;
        }

        // Persiste la notificación de cambio
        notificacionRepository.save(Notificacion.builder()
                .comanda(comanda)
                .mesa(mesa)
                .notificacionTipo(TipoNotificacion.CAMBIO)
                .notificacionEstado(EstadoNotificacion.ACTIVA)
                .build());

        // Retira la comanda del tablero de la estación correspondiente
        wsPublisher.publicarEventoProduccion(comanda.getComandaEstacion(),
                new ComandaProduccionEventoWsMessage(
                        TipoEventoProduccion.ELIMINADA,
                        comanda.getComandaEstacion().name(),
                        comanda.getComandaId(),
                        null, null));
                        
        // Refresca el mapa de mesas para que los meseros vean el ícono de cambio
        mesaWsPublisher.publicarActualizacionMesa(visitaId, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);
        return true;
    }

    private void publicarStockActualizado(Long productoId, Long insumoId, BigDecimal stockActual) {
        // Difunde el cambio de stock a /topic/inventario para refrescar vistas abiertas
        inventarioWsPublisher.publicarStockActualizado(productoId, insumoId, stockActual);
    }
}
