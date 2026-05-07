package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.repository.MenuBebidaDisponibleRepository;
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
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

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

    /** Repositorio de bebidas disponibles por menú: valida que la bebida elegida pertenezca al menú especial. */
    private final MenuBebidaDisponibleRepository menuBebidaDisponibleRepository;

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
     *   <li>El ítem de menú especial debe incluir {@code bebidaProductoId} no nulo.</li>
     *   <li>La bebida seleccionada debe estar disponible en {@code menu_bebida_disponible}
     *       para el menú indicado.</li>
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

        // Validar bebida obligatoria y disponible para ítems de menú especial
        for (PreOrdenItemRequest item : items) {
            if (Boolean.TRUE.equals(item.getEsMenuEspecial())) {
                // La bebida es obligatoria cuando se elige un menú especial
                if (item.getBebidaProductoId() == null) {
                    throw new BusinessException(ErrorCode.INVALID_STATE,
                            "El menú especial requiere seleccionar una bebida.",
                            HttpStatus.UNPROCESSABLE_ENTITY);
                }
                // La bebida debe estar disponible en la tabla menu_bebida_disponible para este menú
                if (!menuBebidaDisponibleRepository.existsByMenuIdAndBebidaId(
                        item.getProductoId(), item.getBebidaProductoId())) {
                    throw new BusinessException(ErrorCode.INVALID_STATE,
                            "La bebida seleccionada no está disponible para este menú.",
                            HttpStatus.UNPROCESSABLE_ENTITY);
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Persistencia de la pre-orden
    // -----------------------------------------------------------------------

    /**
     * Persiste la pre-orden creando hasta dos {@link Comanda} en estado {@code PRE_RESERVA}:
     * una para la estación COCINA (platos y ítems de carta no-bebida) y otra para la
     * estación BARRA (bebidas de carta y bebidas de menú especial).
     *
     * <p>Para ítems de menú especial, el plato se enruta a COCINA y la bebida elegida
     * se enruta a BARRA con precio 0; ambos ítems comparten el mismo UUID de grupo
     * ({@code comandaItemMenuGrupo}) para que el mapper pueda fusionarlos en la respuesta.
     *
     * <p>Para ítems de carta, el enrutamiento depende de la categoría del producto:
     * {@code BEBIDA} → BARRA, cualquier otra categoría → COCINA.
     *
     * @param reserva reserva ya persistida a la que se asocia la pre-orden
     * @param items   lista de ítems a guardar
     * @throws ResourceNotFoundException si un producto o una opción no existen
     * @throws BusinessException         si el producto no está activo o la opción no pertenece al menú
     */
    public void persistirPreOrden(Reserva reserva, List<PreOrdenItemRequest> items) {
        // Comandas lazy: se crean solo cuando hay al menos un ítem para esa estación
        Comanda cocina = null;
        Comanda barra = null;

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

            if (Boolean.TRUE.equals(item.getEsMenuEspecial())) {
                // UUID compartido que vincula el plato (COCINA) con su bebida (BARRA)
                String grupo = UUID.randomUUID().toString();

                // Plato del menú → COCINA
                if (cocina == null) cocina = crearComanda(reserva, EstacionComanda.COCINA);
                ComandaItem itemCocina = ComandaItem.builder()
                        .comanda(cocina)
                        .producto(producto)
                        .comandaItemCantidad(item.getCantidad())
                        .comandaItemPrecio(producto.getProductoPrecio())
                        .comandaItemDescripcion(item.getDescripcion())
                        .comandaItemMenuGrupo(grupo)
                        .build();
                ComandaItem savedCocina = comandaItemRepository.save(itemCocina);

                // Persistir opciones de modificación (salsa, arroz, etc.)
                if (item.getOpcionesModificacion() != null && !item.getOpcionesModificacion().isEmpty()) {
                    for (Long opcionId : item.getOpcionesModificacion()) {
                        // Validar que la opción exista
                        OpcionModificacion opcion = opcionModificacionRepository.findById(opcionId)
                                .orElseThrow(() -> new ResourceNotFoundException("OpcionModificacion", opcionId));
                        // Validar que la opción pertenezca al producto del menú especial
                        if (!productoOpcionModificacionRepository.existsByProductoIdAndOpcionId(
                                producto.getProductoId(), opcionId)) {
                            throw new BusinessException(ErrorCode.INVALID_STATE,
                                    "La opción de modificación '" + opcion.getOpcionNombre() +
                                    "' no pertenece al menú seleccionado.",
                                    HttpStatus.UNPROCESSABLE_ENTITY);
                        }
                        // Enlazar la modificación al ítem de cocina
                        ComandaMenuModificacion mod = ComandaMenuModificacion.builder()
                                .comandaItem(savedCocina)
                                .opcion(opcion)
                                .build();
                        comandaMenuModificacionRepository.save(mod);
                    }
                }

                // Bebida del menú → BARRA con precio 0 (el cliente no paga la bebida por separado)
                Producto bebida = productoRepository.findById(item.getBebidaProductoId())
                        .orElseThrow(() -> new ResourceNotFoundException("Producto", item.getBebidaProductoId()));
                if (barra == null) barra = crearComanda(reserva, EstacionComanda.BARRA);
                ComandaItem itemBarra = ComandaItem.builder()
                        .comanda(barra)
                        .producto(bebida)
                        .comandaItemCantidad(item.getCantidad())
                        .comandaItemPrecio(BigDecimal.ZERO)
                        .comandaItemDescripcion(null)
                        .comandaItemMenuGrupo(grupo)
                        .build();
                comandaItemRepository.save(itemBarra);

            } else {
                // Ítem a la carta: enrutar por categoría del producto
                EstacionComanda estacion = (producto.getProductoCategoria() == CategoriaProducto.BEBIDA)
                        ? EstacionComanda.BARRA : EstacionComanda.COCINA;
                if (estacion == EstacionComanda.COCINA) {
                    if (cocina == null) cocina = crearComanda(reserva, EstacionComanda.COCINA);
                    guardarItemCarta(cocina, producto, item);
                } else {
                    if (barra == null) barra = crearComanda(reserva, EstacionComanda.BARRA);
                    guardarItemCarta(barra, producto, item);
                }
            }
        }
    }

    /**
     * Crea y persiste una nueva {@link Comanda} en estado {@code PRE_RESERVA} para la estación indicada.
     *
     * @param reserva  reserva a la que se asocia la comanda
     * @param estacion estación de producción destino
     * @return comanda persistida con ID asignado
     */
    private Comanda crearComanda(Reserva reserva, EstacionComanda estacion) {
        Comanda c = Comanda.builder()
                .reserva(reserva)
                .comandaEstado(EstadoComanda.PRE_RESERVA)
                .comandaEstacion(estacion)
                .build();
        return comandaRepository.save(c);
    }

    /**
     * Persiste un {@link ComandaItem} de carta (sin grupo de menú especial) en la comanda indicada.
     *
     * @param comanda  comanda destino
     * @param producto producto del catálogo
     * @param item     datos del ítem enviados por el cliente
     */
    private void guardarItemCarta(Comanda comanda, Producto producto, PreOrdenItemRequest item) {
        ComandaItem ci = ComandaItem.builder()
                .comanda(comanda)
                .producto(producto)
                .comandaItemCantidad(item.getCantidad())
                .comandaItemPrecio(producto.getProductoPrecio())
                .comandaItemDescripcion(item.getDescripcion())
                .build();
        comandaItemRepository.save(ci);
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
        // Buscar todas las comandas PRE_RESERVA (puede haber más de una tras el split por estación)
        List<Comanda> comandas = comandaRepository
                .findByReserva_ReservaIdAndComandaEstado(reservaId, EstadoComanda.PRE_RESERVA);
        for (Comanda comanda : comandas) {
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
        }
    }
}
