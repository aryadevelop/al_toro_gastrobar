package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.repository.OpcionModificacionRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoOpcionModificacionRepository;
import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.modules.produccion.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.request.PreOrdenItemRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.entity.PreOrdenDetalle;
import co.edu.unicauca.backend.modules.reservas.entity.PreOrdenMenuModificacion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.repository.PreOrdenDetalleRepository;
import co.edu.unicauca.backend.modules.reservas.repository.PreOrdenMenuModificacionRepository;
import co.edu.unicauca.backend.modules.reservas.mapper.PreOrdenMapper;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para la gestión de pre-órdenes asociadas a reservas.
 *
 * <p>Centraliza toda la lógica de validación, persistencia y consulta de pre-órdenes,
 * desacoplándola del servicio principal de reservas. El mapeo a DTOs se delega en
 * {@link PreOrdenMapper}.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Validar las reglas de negocio de la pre-orden (CA-01, CA-05, CA-07).</li>
 *   <li>Persistir los ítems de pre-orden y sus opciones de modificación de menú especial.</li>
 *   <li>Exponer el detalle completo de la pre-orden de una reserva para el módulo de comandas.</li>
 *   <li>Proveer los detalles de pre-orden para el mapeo de {@link co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse}.</li>
 * </ul>
 *
 * @see ReservaService
 * @see PreOrdenMapper
 */
@Service
@RequiredArgsConstructor
public class PreOrdenService {

    private final PreOrdenDetalleRepository preOrdenDetalleRepository;
    private final PreOrdenMenuModificacionRepository preOrdenMenuModificacionRepository;
    private final ProductoOpcionModificacionRepository productoOpcionModificacionRepository;
    private final OpcionModificacionRepository opcionModificacionRepository;
    private final ProductoRepository productoRepository;
    private final ReservaRepository reservaRepository;
    private final PreOrdenMapper preOrdenMapper;

    // -----------------------------------------------------------------------
    // Consulta
    // -----------------------------------------------------------------------

    /**
     * Devuelve el detalle completo de la pre-orden asociada a una reserva.
     *
     * <p>Por cada ítem de pre-orden incluye el producto, la cantidad, el precio unitario
     * y las opciones de modificación de menú especial seleccionadas por el cliente.
     * 
     * @param reservaId identificador de la reserva
     * @return lista de {@link PreOrdenDetalleResponse} con los ítems de la pre-orden;
     *         vacía si la reserva no tiene pre-orden
     * @throws ResourceNotFoundException si la reserva no existe
     */
    @Transactional(readOnly = true)
    public List<PreOrdenDetalleResponse> obtenerPreOrden(Long reservaId) {

        // Verificar que la reserva exista; si no, lanzar excepción 404.
        if (!reservaRepository.existsById(reservaId)) {
            throw new ResourceNotFoundException("Reserva", reservaId);
        }

        // Obtiene los detalles de pre-orden asociados a la reserva, ordenados cronológicamente
        List<PreOrdenDetalle> detalles = preOrdenDetalleRepository.findByReserva_ReservaIdOrderByCreatedAtAsc(reservaId);

        // Por cada detalle carga sus modificaciones y delega la conversión al mapper
        return detalles.stream()
                .map(d -> {
                    List<PreOrdenMenuModificacion> mods = preOrdenMenuModificacionRepository
                            .findByPreordenDetalle_PreordenDetalleId(d.getPreordenDetalleId());
                    return preOrdenMapper.toDetalleResponse(d, mods);
                })
                .collect(Collectors.toList());
    }

    /**
     * Devuelve los detalles de pre-orden de una reserva como entidades, ordenados cronológicamente.
     *
     * <p>Usado internamente por {@link ReservaService} para construir el resumen de pre-orden
     * en el {@link co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse}.
     *
     * @param reservaId identificador de la reserva
     * @return lista de {@link PreOrdenDetalle} asociados a la reserva; vacía si no tiene pre-orden
     */
    @Transactional(readOnly = true)
    public List<PreOrdenDetalle> obtenerDetallesPorReservaId(Long reservaId) {
        return preOrdenDetalleRepository.findByReserva_ReservaIdOrderByCreatedAtAsc(reservaId);
    }

    // -----------------------------------------------------------------------
    // Validación
    // -----------------------------------------------------------------------

    /**
     * Valida las reglas de negocio de la pre-orden antes de persistirla.
     *
     * <p>Reglas aplicadas:
     * <ul>
     *   <li>Si hay algún ítem de menú especial, la reserva debe ser para más de {@code 10} personas.</li>
     *   <li>Solo se permite un ítem de menú especial por reserva.</li>
     * </ul>
     *
     * @param items          lista de ítems de la pre-orden a validar
     * @param numeroPersonas número de comensales de la reserva
     * @throws BusinessException si se incumple alguna de las reglas anteriores
     */
    public void validarPreOrden(List<PreOrdenItemRequest> items, int numeroPersonas) {

        // Cuenta cuántos ítems de la lista son de tipo menú especial
        long menuEspecialCount = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getEsMenuEspecial()))
                .count();

        // Solo se permite un menú especial por reserva (se verifica antes del umbral de personas
        // para que, en casos con ambas violaciones, el mensaje sea sobre la cantidad de menús)
        if (menuEspecialCount > 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Solo puede seleccionar un menú especial por reserva.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // El menú especial requiere más de 10 comensales
        if (menuEspecialCount > 0 && numeroPersonas <= 10) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "El menú especial solo está disponible para reservas de más de 10 personas.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    // -----------------------------------------------------------------------
    // Persistencia
    // -----------------------------------------------------------------------

    /**
     * Persiste los ítems de la pre-orden asociados a una reserva ya guardada.
     *
     * <p>Por cada ítem de la lista:
     * <ul>
     *   <li>Valida que el producto exista y esté en estado {@code ACTIVO}.</li>
     *   <li>Crea y guarda un {@link PreOrdenDetalle}.</li>
     *   <li>Si {@code esMenuEspecial = true}, persiste cada opción de modificación seleccionada
     *       en {@link PreOrdenMenuModificacion}, verificando que la opción pertenezca al menú
     *       indicado.</li>
     * </ul>
     *
     * @param reserva reserva a la que se asocia la pre-orden (ya persistida)
     * @param items   lista de ítems a guardar
     * @throws ResourceNotFoundException si el producto o la opción de modificación no existen
     * @throws BusinessException si el producto no está activo o si una opción no pertenece
     *         al menú seleccionado
     */
    @Transactional
    public void persistirPreOrden(Reserva reserva, List<PreOrdenItemRequest> items) {
        for (PreOrdenItemRequest item : items) {

            // Verifica que el producto exista en la base de datos
            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto", item.getProductoId()));

            // Rechaza el ítem si el producto fue desactivado después de que el cliente eligió
            if (producto.getProductoEstado() != EstadoGenerico.ACTIVO) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "El producto '" + producto.getProductoNombre() + "' no está disponible.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }

            // Construye y persiste el detalle de pre-orden vinculado a la reserva
            PreOrdenDetalle detalle = PreOrdenDetalle.builder()
                    .reserva(reserva)
                    .producto(producto)
                    .preordenDetalleCantidad(item.getCantidad())
                    .preordenDetalleDescripcion(item.getDescripcion())
                    .build();

            PreOrdenDetalle detalleGuardado = preOrdenDetalleRepository.save(detalle);

            // Procesa las opciones de modificación solo si el ítem es de menú especial
            if (Boolean.TRUE.equals(item.getEsMenuEspecial())
                    && item.getOpcionesModificacion() != null
                    && !item.getOpcionesModificacion().isEmpty()) {

                for (Long opcionId : item.getOpcionesModificacion()) {

                    // Verifica que la opción de modificación exista en la base de datos
                    OpcionModificacion opcion = opcionModificacionRepository.findById(opcionId)
                            .orElseThrow(() -> new ResourceNotFoundException("OpcionModificacion", opcionId));

                    // La opción debe pertenecer al menú del producto seleccionado
                    if (!productoOpcionModificacionRepository.existsByProductoIdAndOpcionId(
                            producto.getProductoId(), opcionId)) {
                        throw new BusinessException(ErrorCode.INVALID_STATE,
                                "La opción de modificación '" + opcion.getOpcionNombre() +
                                "' no pertenece al menú seleccionado.",
                                HttpStatus.UNPROCESSABLE_ENTITY);
                    }

                    // Vincula la opción seleccionada al detalle de pre-orden guardado
                    PreOrdenMenuModificacion mod = PreOrdenMenuModificacion.builder()
                            .preordenDetalle(detalleGuardado)
                            .opcion(opcion)
                            .build();

                    preOrdenMenuModificacionRepository.save(mod);
                }
            }
        }
    }
}
