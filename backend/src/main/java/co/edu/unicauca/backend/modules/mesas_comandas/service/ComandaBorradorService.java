package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.AgregarItemRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.AdvertenciaPreordenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.ModificarItemRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.NotasRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.BorradorComandaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.VisitaEstadoMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.ComandaBorradorMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaNuevaMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.ComandaProduccionMapper;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaProduccionEventoWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.TipoEventoProduccion;
import co.edu.unicauca.backend.modules.notificaciones.service.MesaWsPublisher;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.shared.config.RabbitMQConfig;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lógica de negocio del formulario de modificar comanda.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Cargar el borrador (precargado desde pre-orden o vacío).</li>
 *   <li>Agregar, modificar y eliminar ítems con validación de stock y techo
 *       de cantidad.</li>
 *   <li>Persistir modificaciones libres como ítems con el mismo
 *       {@code productoId} del base y descripción no nula.</li>
 *   <li>Persistir notas por estación (cocina/barra).</li>
 *   <li>Enviar la comanda a producción (transición BORRADOR → PENDIENTE) y
 *       cancelar el formulario descartando los borradores.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ComandaBorradorService {

    private final ComandaRepository comandaRepository;
    private final ComandaItemRepository comandaItemRepository;
    private final ProductoRepository productoRepository;
    private final MesaRepository mesaRepository;

    private final ComandaBorradorMapper borradorMapper;
    private final VisitaEstadoMapper visitaEstadoMapper;
    private final ComandaBorradorValidador validador;
    private final MesaValidador mesaValidador;

    private final MesaWsPublisher mesaWsPublisher;
    private final NotificacionWsPublisher notificacionWsPublisher;
    private final ComandaProduccionMapper comandaProduccionMapper;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Devuelve el borrador completo de la visita: ítems agrupados por estación,
     * notas por estación, total acumulado y flags para habilitar los botones de
     * envío a producción. Si la visita no tiene borradores devuelve estructura
     * vacía pero válida.
     *
     * @param visitaId identificador de la visita
     * @param auth     contexto del usuario autenticado
     * @return DTO listo para el frontend
     */
    @Transactional(readOnly = true)
    public BorradorComandaResponse obtenerBorrador(Long visitaId, Authentication auth) {
        Mesa mesa = mesaValidador.validarOwnership(visitaId, auth);
        return obtenerBorradorInterno(mesa);
    }

    /**
     * Agrega un ítem al borrador de la estación correspondiente al producto
     * (PLATO → COCINA, BEBIDA → BARRA). Si la comanda BORRADOR de la estación
     * no existe, la crea. Si ya existe un ítem con el mismo {@code productoId}
     * y la misma {@code descripcion} acumula la cantidad.
     *
     * @param req  payload con producto, cantidad y descripción opcional
     * @param auth contexto del usuario autenticado
     * @return borrador completo tras la operación
     */
    @Transactional
    public BorradorComandaResponse agregarItem(AgregarItemRequest req, Authentication auth) {
        Mesa mesa = mesaValidador.validarOwnership(req.getVisitaId(), auth);

        // Obtiene el producto correspondiente
        Producto producto = productoRepository.findById(req.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto", req.getProductoId()));

        // No se permiten productos de menú especial en el flujo de borrador: deben cargarse vía pre-orden.
        if (Boolean.TRUE.equals(producto.getMenuEspecial())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "Los menús especiales se cargan vía pre-orden, no se agregan al borrador",
                    HttpStatus.BAD_REQUEST);
        }

        // Valida que la cantidad solicitada no supere el stock disponible
        validador.validarStock(producto, req.getCantidad(), 0);

        // Determina la estación destino a partir de la categoría del producto.
        EstacionComanda estacion = validador.resolverEstacion(producto);

        // Obtener-o-crear comanda BORRADOR de la estación.
        Comanda comanda = comandaRepository
                .findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion(
                        req.getVisitaId(), EstadoComanda.BORRADOR, estacion)
                .orElseGet(() -> comandaRepository.save(Comanda.builder()
                        .visita(mesa.getVisita())
                        .reserva(mesa.getVisita().getReserva())
                        .comandaEstado(EstadoComanda.BORRADOR)
                        .comandaEstacion(estacion)
                        .build()));

        // Si ya existe un ítem con el mismo productoId y descripción equivalente, se acumula la cantidad.
        // La comparación normaliza mayúsculas, espacios extremos y espacios internos consecutivos.
        String descripcionNormalizada = DescripcionNormalizer.normalizar(req.getDescripcion());
        Optional<ComandaItem> existente = comandaItemRepository
                .findByComanda_ComandaIdAndProducto_ProductoId(comanda.getComandaId(), req.getProductoId())
                .stream()
                .filter(ci -> DescripcionNormalizer.normalizar(ci.getComandaItemDescripcion())
                        .equals(descripcionNormalizada))
                .findFirst();
        if (existente.isPresent()) {
            ComandaItem ci = existente.get();
            int nuevaCantidad = ci.getComandaItemCantidad() + req.getCantidad();
            validador.validarStock(producto, nuevaCantidad, ci.getComandaItemCantidad());
            ci.setComandaItemCantidad(nuevaCantidad);
            comandaItemRepository.save(ci);
        } else {
            // Cuando la descripción está presente el ítem se persiste como modificación libre:
            // mismo productoId del base, precio propio (igual al catálogo en este flujo).
            ComandaItem item = ComandaItem.builder()
                    .comanda(comanda)
                    .producto(producto)
                    .comandaItemCantidad(req.getCantidad())
                    .comandaItemPrecio(producto.getProductoPrecio())
                    .comandaItemDescripcion(req.getDescripcion())
                    .build();
            comandaItemRepository.save(item);
        }

        // Publicar actualización de mapa y de la orden visible por el cliente
        publicarMesasActualizada(req.getVisitaId());
        publicarOrdenClienteActualizada(req.getVisitaId());

        // Devolver el borrador completo tras la modificación
        return obtenerBorradorInterno(mesa);
    }

    /**
     * Modifica la cantidad y/o la descripción de un ítem existente del borrador.
     * Cualquier campo {@code null} en la petición se interpreta como "no cambiar".
     * Revalida el stock cuando se altera la cantidad.
     *
     * @param itemId identificador del {@code comanda_item}
     * @param req    cambios solicitados
     * @param auth   contexto del usuario autenticado
     * @return borrador completo tras la operación
     */
    @Transactional
    public BorradorComandaResponse modificarItem(Long itemId, ModificarItemRequest req, Authentication auth) {
        // Validar que el item exista 
        ComandaItem item = comandaItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("ComandaItem", itemId));

        // Validar que el item pertenezca a una comanda BORRADOR
        Comanda comanda = item.getComanda();
        if (comanda.getComandaEstado() != EstadoComanda.BORRADOR) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "El item no pertenece a una comanda en BORRADOR", HttpStatus.CONFLICT);
        }

        // Validar que el usuario tenga acceso a la visita
        Long visitaId = comanda.getVisita().getVisitaId();
        Mesa mesa = mesaValidador.validarOwnership(visitaId, auth);

        // Si se modifica la cantidad, validar que no supere el stock disponible
        if (req.getCantidad() != null) {
            if (req.getCantidad() == 0) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "Use DELETE para eliminar items; cantidad 0 no es válida",
                        HttpStatus.CONFLICT);
            }
            validador.validarStock(item.getProducto(), req.getCantidad(), item.getComandaItemCantidad());
            item.setComandaItemCantidad(req.getCantidad());
        }
        
        // Si se modifica la descripción, antes de persistir se verifica que el cambio no genere un
        // duplicado contra otro ítem del mismo producto en la comanda (comparación normalizada).
        if (req.getDescripcion() != null) {
            String descripcionNormalizada = DescripcionNormalizer.normalizar(req.getDescripcion());
            boolean colision = comandaItemRepository
                    .findByComanda_ComandaIdAndProducto_ProductoId(
                            comanda.getComandaId(), item.getProducto().getProductoId())
                    .stream()
                    .filter(otro -> !otro.getComandaItemId().equals(item.getComandaItemId()))
                    .anyMatch(otro -> DescripcionNormalizer.normalizar(otro.getComandaItemDescripcion())
                            .equals(descripcionNormalizada));
            if (colision) {
                throw new BusinessException(ErrorCode.INVALID_STATE,
                        "Ya existe otro ítem del mismo producto con esa descripción.",
                        HttpStatus.CONFLICT);
            }
            item.setComandaItemDescripcion(req.getDescripcion());
        }

        // Guardar el item modificado
        comandaItemRepository.save(item);

        // Publicar actualización de mapa y de la orden visible por el cliente
        publicarMesasActualizada(visitaId);
        publicarOrdenClienteActualizada(visitaId);

        // Devolver el borrador completo tras la modificación
        return obtenerBorradorInterno(mesa);
    }

    /**
     * Elimina un ítem del borrador. Reglas:
     * <ul>
     *   <li>Si el ítem es parte de un par menú especial ({@code menuGrupo} no nulo)
     *       se eliminan ambos ítems del par (cocina y barra).</li>
     *   <li>Si es un ítem base ({@code descripcion == null}) se eliminan también
     *       todos los ítems modificados con el mismo {@code productoId}.</li>
     *   <li>Si es un ítem modificado solo se elimina ese ítem.</li>
     *   <li>Si la comanda queda sin ítems se elimina la comanda.</li>
     * </ul>
     *
     * @param itemId identificador del {@code comanda_item}
     * @param auth   contexto del usuario autenticado
     * @return borrador completo tras la operación
     */
    @Transactional
    public BorradorComandaResponse eliminarItem(Long itemId, Authentication auth) {
        // Validar que el item exista
        ComandaItem item = comandaItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("ComandaItem", itemId));

        // Validar que el item pertenezca a una comanda BORRADOR
        Comanda comanda = item.getComanda();
        if (comanda.getComandaEstado() != EstadoComanda.BORRADOR) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "El item no pertenece a una comanda en BORRADOR", HttpStatus.CONFLICT);
        }

        // Validar que el usuario tenga acceso a la visita
        Long visitaId = comanda.getVisita().getVisitaId();
        Mesa mesa = mesaValidador.validarOwnership(visitaId, auth);

        // Reglas de eliminación según el tipo de ítem
        String grupo = item.getComandaItemMenuGrupo();
        if (grupo != null) {
            eliminarParMenu(visitaId, grupo);
        } else if (item.getComandaItemDescripcion() == null) {
            // Ítem base: arrastra a todos los modificados del mismo producto en la misma comanda.
            List<ComandaItem> modificados = comandaItemRepository
                    .findByComanda_ComandaIdAndProducto_ProductoIdAndComandaItemDescripcionIsNotNull(
                            comanda.getComandaId(), item.getProducto().getProductoId());
            comandaItemRepository.deleteAll(modificados);
            comandaItemRepository.delete(item);
            eliminarComandaSiVacia(comanda);
        } else {
            // Ítem modificado: solo se borra esa fila; el base y otros modificados permanecen.
            comandaItemRepository.delete(item);
            eliminarComandaSiVacia(comanda);
        }

        // Publicar actualización de mapa y de la orden visible por el cliente
        publicarMesasActualizada(visitaId);
        publicarOrdenClienteActualizada(visitaId);

        //  Devolver el borrador completo tras la modificación
        return obtenerBorradorInterno(mesa);
    }

    /**
     * Actualiza las notas de una comanda BORRADOR. Cada estación tiene sus propias
     * notas, por lo que la petición identifica la comanda y no la visita. 
     * Persistir {@code null} borra las notas existentes.
     *
     * @param comandaId identificador de la comanda BORRADOR
     * @param req       payload con el nuevo valor del campo {@code notas}
     * @param auth      contexto del usuario autenticado
     * @return borrador completo tras la operación
     */
    @Transactional
    public BorradorComandaResponse actualizarNotas(Long comandaId, NotasRequest req, Authentication auth) {
        // Validar que la comanda exista
        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda", comandaId));

        // Validar que la comanda esté en estado BORRADOR
        if (comanda.getComandaEstado() != EstadoComanda.BORRADOR) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Solo comandas en BORRADOR admiten notas", HttpStatus.CONFLICT);
        }

        // Validar que el usuario tenga acceso a la visita
        Long visitaId = comanda.getVisita().getVisitaId();
        Mesa mesa = mesaValidador.validarOwnership(visitaId, auth);

        // Actualizar las notas de la comanda
        comanda.setComandaNotas(req.getNotas());
        comandaRepository.save(comanda);

        // No se publica /topic/mesas: las notas no alteran el ícono de borrador ni el estado de la mesa.
        return obtenerBorradorInterno(mesa);
    }

    /**
     * Envía la comanda BORRADOR de una estación a producción: transiciona a
     * PENDIENTE, sella {@code fechaHoraInicio}, valida el stock final (sin
     * decrementarlo), publica el evento de impresión por RabbitMQ y los
     * eventos WebSocket para mapa, cliente y dashboard de estación. Si la
     * mesa estaba en ESPERA pasa a EN_PREPARACION.
     *
     * @param comandaId identificador de la comanda BORRADOR a enviar
     * @param auth      contexto del usuario autenticado
     * @return borrador remanente tras el envío (la otra estación, o estructura vacía)
     */
    @Transactional
    public BorradorComandaResponse enviarAProduccion(Long comandaId, Authentication auth) {
        // Validar que la comanda exista
        Comanda comanda = comandaRepository.findById(comandaId)
                .orElseThrow(() -> new ResourceNotFoundException("Comanda", comandaId));

        // Validar que la comanda esté en estado BORRADOR
        if (comanda.getComandaEstado() != EstadoComanda.BORRADOR) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Solo comandas en BORRADOR pueden enviarse a producción", HttpStatus.CONFLICT);
        }

        // Validar que el usuario tenga acceso a la visita
        Long visitaId = comanda.getVisita().getVisitaId();
        Mesa mesa = mesaValidador.validarOwnership(visitaId, auth);

        // Validar que la comanda tenga ítems: no se pueden enviar a producción comandas vacías.
        List<ComandaItem> items = comandaItemRepository
                .findByComanda_ComandaIdOrderByProductoNombreAsc(comandaId);
        validador.validarTieneItems(items);

        // Validación final de stock sin decrementar: el descuento real ocurre cuando
        // producción transiciona PENDIENTE → EN_PREPARACION. Los ítems de menú especial
        // (menuGrupo presente) están excluidos: no consumen inventario.
        for (ComandaItem it : items) {
            if (it.getComandaItemMenuGrupo() == null) {
                // El ítem ya está contabilizado en BORRADOR; 
                // cantidadAnterior = cantidad actual para que la fórmula no lo doblecuente.
                validador.validarStock(it.getProducto(), it.getComandaItemCantidad(),
                        it.getComandaItemCantidad());
            }
        }

        // Transicionar la comanda a PENDIENTE y sellar fechaHoraInicio
        comanda.setComandaEstado(EstadoComanda.PENDIENTE);
        comanda.setComandaFechaHoraInicio(LocalDateTime.now());
        comandaRepository.save(comanda);

        // Transicionar la mesa a EN_PREPARACION si estaba en ESPERA.
        if (mesa.getMesaEstado() == EstadoMesa.ESPERA) {
            mesa.setMesaEstado(EstadoMesa.EN_PREPARACION);
            mesaRepository.save(mesa);
            mesaWsPublisher.publicarCambioEstadoMesa(visitaId, EstadoMesa.EN_PREPARACION);
        }

        // RabbitMQ: bridge de impresión de tickets.
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.RK_COMANDA_NUEVA,
                ComandaNuevaMessage.builder()
                        .comandaId(comanda.getComandaId())
                        .visitaId(visitaId)
                        .estacion(comanda.getComandaEstacion().name())
                        .fechaHoraInicio(comanda.getComandaFechaHoraInicio())
                        .build());

        // Anuncia la aparición de la comanda en el tablero de producción.
        int totalItems = items.stream()
                .mapToInt(ComandaItem::getComandaItemCantidad)
                .sum();

        ComandaProduccionResumenResponse resumen = comandaProduccionMapper.toResumen(comanda, mesa, totalItems);
        
        notificacionWsPublisher.publicarEventoProduccion(
                comanda.getComandaEstacion(),
                new ComandaProduccionEventoWsMessage(
                        TipoEventoProduccion.CREADA,
                        comanda.getComandaEstacion().name(),
                        comanda.getComandaId(),
                        resumen,
                        null));

        // Publicar actualización del mapa y de la orden del cliente para reflejar el cambio.
        publicarMesasActualizada(visitaId);
        publicarOrdenClienteActualizada(visitaId);

        return obtenerBorradorInterno(mesa);
    }

    /**
     * Cancela el formulario eliminando todas las comandas BORRADOR de la visita
     * (cocina y barra) junto con sus ítems y notas. Operación idempotente: si no
     * había borradores no hace nada y el WebSocket {@code /topic/mesas} se
     * publica de todas formas para mantener al mapa sincronizado.
     *
     * @param visitaId identificador de la visita
     * @param auth     contexto del usuario autenticado
     */
    @Transactional
    public void cancelarFormulario(Long visitaId, Authentication auth) {
        mesaValidador.validarOwnership(visitaId, auth);
        List<Comanda> comandas = comandaRepository
                .findByVisita_VisitaIdAndComandaEstado(visitaId, EstadoComanda.BORRADOR);
        if (!comandas.isEmpty()) {
            comandaRepository.deleteAll(comandas);
        }
        publicarMesasActualizada(visitaId);
        publicarOrdenClienteActualizada(visitaId);
    }

    /**
     * Carga el borrador completo a partir de la mesa ya validada. Centraliza el
     * fetch de comandas, el cálculo del total acumulado y el armado del DTO,
     * para que cada operación pueda devolver el estado fresco al final.
     */
    private BorradorComandaResponse obtenerBorradorInterno(Mesa mesa) {
        List<Comanda> comandas = comandaRepository
                .findByVisita_VisitaIdAndComandaEstado(mesa.getVisitaId(), EstadoComanda.BORRADOR);
        BigDecimal totalAcumulado = comandaItemRepository.sumTotalActivosByVisita(mesa.getVisitaId());

        List<ComandaItem> todosLosItems = new ArrayList<>();
        for (Comanda c : comandas) {
            List<ComandaItem> items = comandaItemRepository
                    .findByComanda_ComandaIdOrderByProductoNombreAsc(c.getComandaId());
            if (items != null) todosLosItems.addAll(items);
        }
        List<AdvertenciaPreordenResponse> advertencias = calcularAdvertencias(todosLosItems);

        return borradorMapper.toBorradorResponse(mesa, comandas, totalAcumulado, advertencias);
    }

    /**
     * Evalúa el stock de cada ítem del borrador y produce una advertencia
     * para los que superan las unidades disponibles.
     *
     * <p>Los ítems de menú especial ({@code comandaItemMenuGrupo} no nulo) se omiten
     * porque no consumen inventario directo.
     *
     * @param items todos los ítems de los borradores de la visita
     * @return lista (nunca nula) de advertencias; vacía si todos caben en stock
     */
    private List<AdvertenciaPreordenResponse> calcularAdvertencias(List<ComandaItem> items) {
        List<AdvertenciaPreordenResponse> advertencias = new ArrayList<>();
        for (ComandaItem item : items) {
            if (item.getComandaItemMenuGrupo() != null) {
                continue;
            }
            Integer disponible = validador.evaluarDisponibilidad(
                    item.getProducto(), item.getComandaItemCantidad());
            if (disponible != null && disponible < item.getComandaItemCantidad()) {
                advertencias.add(AdvertenciaPreordenResponse.builder()
                        .productoId(item.getProducto().getProductoId())
                        .productoNombre(item.getProducto().getProductoNombre())
                        .cantidadEnComanda(item.getComandaItemCantidad())
                        .cantidadDisponible(Math.max(disponible, 0))
                        .build());
            }
        }
        return advertencias;
    }

    /**
     * Publica la actualización del mapa de mesas para refrescar
     * {@code tieneBorrador} y el estado visual de la mesa en el frontend.
     */
    private void publicarMesasActualizada(Long visitaId) {
        mesaWsPublisher.publicarActualizacionMesa(visitaId, MesaWsPublisher.TipoEventoMesa.ACTUALIZAR);
    }

    /**
     * Publica el estado completo de la orden del cliente: ítems en borrador y en
     * producción con su estado visible, más el total acumulado.
     */
    private void publicarOrdenClienteActualizada(Long visitaId) {
        List<ComandaItem> items = comandaRepository.findAllItemsActivosByVisita(visitaId);
        List<ItemVisitaResponse> itemsResponse = visitaEstadoMapper.toItemsVisitaResponse(items);
        BigDecimal total = items.stream()
                .filter(ci -> ci.getComandaItemPrecio() != null)
                .map(ci -> ci.getComandaItemPrecio().multiply(BigDecimal.valueOf(ci.getComandaItemCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        notificacionWsPublisher.publicarVisitaActualizada(visitaId,
                VisitaActualizadaWsMessage.builder()
                        .visitaId(visitaId)
                        .items(itemsResponse)
                        .total(total)
                        .build());
    }

    /**
     * Elimina los dos ítems COCINA+BARRA del mismo {@code comandaItemMenuGrupo}
     * en la visita y borra cualquier comanda que quede sin ítems.
     */
    private void eliminarParMenu(Long visitaId, String grupo) {
        List<Comanda> comandas = comandaRepository
                .findByVisita_VisitaIdAndComandaEstado(visitaId, EstadoComanda.BORRADOR);
        for (Comanda c : comandas) {
            List<ComandaItem> items = comandaItemRepository
                    .findByComanda_ComandaIdOrderByProductoNombreAsc(c.getComandaId());
            for (ComandaItem ci : items) {
                if (grupo.equals(ci.getComandaItemMenuGrupo())) {
                    comandaItemRepository.delete(ci);
                }
            }
            eliminarComandaSiVacia(c);
        }
    }

    /**
     * Borra la comanda si ya no tiene ítems asociados, para que el invariante
     * "una comanda BORRADOR existe solo cuando tiene contenido" se mantenga.
     */
    private void eliminarComandaSiVacia(Comanda comanda) {
        long restantes = comandaItemRepository
                .findByComanda_ComandaIdOrderByProductoNombreAsc(comanda.getComandaId()).size();
        if (restantes == 0) {
            comandaRepository.delete(comanda);
        }
    }
}
