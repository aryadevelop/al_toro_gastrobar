package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.repository.OpcionModificacionRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoOpcionModificacionRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaMenuModificacion;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaMenuModificacionRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.modules.produccion.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.request.PreOrdenItemRequest;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Componente encargado de gestionar la pre-orden asociada a una reserva.
 *
 * <p>Centraliza la lógica extraída de {@link ReservaService} relacionada con la pre-orden:
 * validación de las reglas de negocio de los ítems seleccionados, persistencia de la
 * {@link Comanda} en estado {@code PRE_RESERVA} junto con sus {@link ComandaItem} y
 * {@link ComandaMenuModificacion}, y eliminación de la pre-orden previa cuando la
 * reserva se modifica.</p>
 *
 * <p>Este componente es stateless: no mantiene información entre invocaciones y solo
 * interactúa con repositorios de persistencia. Las reglas y mensajes de error se
 * preservan exactamente como estaban definidas en {@link ReservaService}.</p>
 *
 * @see ReservaService
 * @see ReservaValidador
 */
@Component
@RequiredArgsConstructor
public class PreOrdenGestor {

    /** Repositorio de productos del catálogo: se usa para cargar y validar el producto de cada ítem. */
    private final ProductoRepository productoRepository;

    /** Repositorio de opciones de modificación: se usa para validar las opciones seleccionadas en el menú especial. */
    private final OpcionModificacionRepository opcionModificacionRepository;

    /** Repositorio que asocia productos con sus opciones de modificación permitidas: valida la pertenencia opción-producto. */
    private final ProductoOpcionModificacionRepository productoOpcionModificacionRepository;

    /** Repositorio de comandas: persiste y localiza la comanda PRE_RESERVA vinculada a la reserva. */
    private final ComandaRepository comandaRepository;

    /** Repositorio de ítems de comanda: persiste, consulta y elimina los detalles de la pre-orden. */
    private final ComandaItemRepository comandaItemRepository;

    /** Repositorio de modificaciones de menú: persiste y elimina las selecciones asociadas a cada ítem. */
    private final ComandaMenuModificacionRepository comandaMenuModificacionRepository;

    // -----------------------------------------------------------------------
    // Validación de reglas de pre-orden
    // -----------------------------------------------------------------------

    /**
     * Valida las reglas de negocio de la pre-orden antes de persistirla.
     *
     * <ul>
     *   <li>Todo ítem marcado con {@code esMenuEspecial=true} debe corresponder a un producto
     *       con {@code menu_especial=true} en la base de datos.</li>
     *   <li>Solo un ítem de menú especial por reserva.</li>
     *   <li>El menú especial requiere más de 10 comensales.</li>
     * </ul>
     *
     * @param items          lista de ítems de la pre-orden
     * @param numeroPersonas número de comensales de la reserva
     * @throws ResourceNotFoundException si el producto no existe
     * @throws BusinessException         si se incumple alguna regla
     */
    public void validarPreOrden(List<PreOrdenItemRequest> items, int numeroPersonas) {

        // Validar que cada ítem marcado como menú especial corresponda a un producto menu_especial en BD
        for (PreOrdenItemRequest item : items) {
            if (Boolean.TRUE.equals(item.getEsMenuEspecial())) {
                Producto producto = productoRepository.findById(item.getProductoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Producto", item.getProductoId()));
                if (!Boolean.TRUE.equals(producto.getMenuEspecial())) {
                    throw new BusinessException(ErrorCode.INVALID_STATE,
                            "El producto '" + producto.getProductoNombre() + "' no es un menú especial.",
                            HttpStatus.UNPROCESSABLE_ENTITY);
                }
            }
        }

        // Contar cuántos ítems de menú especial hay en la pre-orden
        long menuEspecialCount = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getEsMenuEspecial()))
                .count();

        // Validar que no haya más de un ítem de menú especial
        if (menuEspecialCount > 1) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Solo puede seleccionar un menú especial por reserva.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Validar que si hay un menú especial, el número de personas sea mayor a 10
        if (menuEspecialCount > 0 && numeroPersonas <= 10) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "El menú especial solo está disponible para reservas de más de 10 personas.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    // -----------------------------------------------------------------------
    // Persistencia de la pre-orden
    // -----------------------------------------------------------------------

    /**
     * Persiste la pre-orden como una {@link Comanda} en estado {@code PRE_RESERVA}, con
     * sus {@link ComandaItem} y {@link ComandaMenuModificacion} correspondientes.
     *
     * @param reserva reserva ya persistida a la que se asocia la pre-orden
     * @param items   lista de ítems a guardar
     * @return lista de {@link ComandaItem} persistidos
     * @throws ResourceNotFoundException si un producto o una opción no existen
     * @throws BusinessException si el producto no está activo o la opción no pertenece al menú
     */
    public List<ComandaItem> persistirPreOrden(Reserva reserva, List<PreOrdenItemRequest> items) {
        // Crear la comanda en estado PRE_RESERVA vinculada a la reserva
        Comanda preComanda = Comanda.builder()
                .reserva(reserva)
                .comandaEstado(EstadoComanda.PRE_RESERVA)
                .build();

        // Guardar la comanda para obtener su ID y poder asociar los ítems
        Comanda comandaGuardada = comandaRepository.save(preComanda);

        // Iterar sobre los ítems de la pre-orden para validarlos y persistirlos
        for (PreOrdenItemRequest item : items) {
            // Validar que el producto exista
            Producto producto = productoRepository.findById(item.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto", item.getProductoId()));

            // Validar que el producto esté activo
            if (producto.getProductoEstado() != EstadoGenerico.ACTIVO) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "El producto '" + producto.getProductoNombre() + "' no está disponible.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }

            // Crear el detalle de la comanda para este ítem
            ComandaItem detalle = ComandaItem.builder()
                    .comanda(comandaGuardada)
                    .producto(producto)
                    .comandaItemCantidad(item.getCantidad())
                    .comandaItemPrecio(producto.getProductoPrecio())
                    .comandaItemDescripcion(item.getDescripcion())
                    .build();

            // Guardar el detalle para obtener su ID y poder asociar las modificaciones
            ComandaItem detalleGuardado = comandaItemRepository.save(detalle);

            // Si el ítem es un menú especial, validar y persistir las opciones de modificación asociadas
            if (Boolean.TRUE.equals(item.getEsMenuEspecial())
                    && item.getOpcionesModificacion() != null
                    && !item.getOpcionesModificacion().isEmpty()) {

                // Validar cada opción de modificación
                for (Long opcionId : item.getOpcionesModificacion()) {

                    // Validar que la opción de modificación exista
                    OpcionModificacion opcion = opcionModificacionRepository.findById(opcionId)
                            .orElseThrow(() -> new ResourceNotFoundException("OpcionModificacion", opcionId));

                    // Validar que la opción de modificación pertenezca al producto del menú especial
                    if (!productoOpcionModificacionRepository.existsByProductoIdAndOpcionId( producto.getProductoId(), opcionId)) {
                        throw new BusinessException(ErrorCode.INVALID_STATE,
                                "La opción de modificación '" + opcion.getOpcionNombre() +
                                "' no pertenece al menú seleccionado.",
                                HttpStatus.UNPROCESSABLE_ENTITY);
                    }

                    // Crear el enlace entre el detalle de la comanda y la opción de modificación seleccionada
                    ComandaMenuModificacion mod = ComandaMenuModificacion.builder()
                            .comandaItem(detalleGuardado)
                            .opcion(opcion)
                            .build();

                    // Guardar el enlace para persistir la selección de modificación
                    comandaMenuModificacionRepository.save(mod);
                }
            }
        }

        // Devolver la lista de ítems de la pre-orden ya persistidos
        return comandaItemRepository.findByComanda_ComandaId(comandaGuardada.getComandaId());
    }

    // -----------------------------------------------------------------------
    // Eliminación de pre-orden existente
    // -----------------------------------------------------------------------

    /**
     * Elimina la pre-orden ({@code PRE_RESERVA} comanda) de una reserva si existe.
     *
     * <p>Borra en orden: modificaciones de menú → ítems → comanda, para respetar
     * las restricciones de FK de la base de datos.
     *
     * @param reservaId identificador de la reserva cuya pre-orden se va a eliminar
     */
    public void eliminarPreOrdenExistente(Long reservaId) {
        // Buscar la comanda PRE_RESERVA de esta reserva; si no existe, no hay nada que eliminar
        comandaRepository
                .findByReserva_ReservaIdAndComandaEstado(reservaId, EstadoComanda.PRE_RESERVA)
                .ifPresent(comanda -> {
                    List<ComandaItem> items =
                            comandaItemRepository.findByComanda_ComandaId(comanda.getComandaId());
                            
                    // Borrar modificaciones de menú especial antes que los ítems
                    items.forEach(item ->
                            comandaMenuModificacionRepository
                                    .deleteByComandaItem_ComandaItemId(item.getComandaItemId()));

                    // Borrar los ítems antes que la comanda
                    comandaItemRepository.deleteAll(items);
                    
                    // Eliminar la comanda vacía
                    comandaRepository.delete(comanda);
                });
    }
}
