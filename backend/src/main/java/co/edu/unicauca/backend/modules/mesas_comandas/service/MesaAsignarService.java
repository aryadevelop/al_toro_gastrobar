package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.AsignarMesaRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.VisitaEstadoMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.MesaAsignadaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ZonaDisponibleMesaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ReservaActualizadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.modules.notificaciones.service.MesaWsPublisher;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio para asignación de identificador a mesas.
 *
 * <p>Gestiona la creación de visitas y mesas tanto para walk-in como para
 * reservas confirmadas. Coordina validaciones, persistencia y notificaciones WebSocket.
 */
@Service
@RequiredArgsConstructor
public class MesaAsignarService {

    private final VisitaEstadoMapper visitaEstadoMapper;
    private final MesaValidador mesaValidador;
    private final MesaRepository mesaRepository;
    private final VisitaRepository visitaRepository;
    private final ZonaRepository zonaRepository;
    private final ReservaRepository reservaRepository;
    private final ComandaRepository comandaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final MesaWsPublisher mesaWsPublisher;
    private final NotificacionWsPublisher notificacionWsPublisher;
    private final NotificacionRepository notificacionRepository;

    /**
     * Asigna identificador a una nueva mesa.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Validar horario de atención 17:00-22:00 (SIEMPRE - walk-in y reservas)</li>
     *   <li>Validar identificador no duplicado en el día</li>
     *   <li>Validar zona existe</li>
     *   <li>Si reservaId presente: validar reserva (CONFIRMADA, día actual)</li>
     *   <li>Crear Visita (con/sin reserva, con/sin cliente)</li>
     *   <li>Crear Mesa (estado ESPERA)</li>
     *   <li>Si reserva: cambiar comanda PRE_RESERVA a BORRADOR, cambiar reserva a ATENDIDA</li>
     *   <li>Publicar eventos WebSocket</li>
     * </ol>
     *
     * @param request datos de asignación
     * @param emailMesero email del mesero autenticado
     * @return MesaAsignadaResponse con datos de la mesa creada
     * @throws BusinessException si validaciones fallan
     */
    @Transactional
    public MesaAsignadaResponse asignarMesa(AsignarMesaRequest request, String emailMesero) {

        // 1. Validar horario de atención (17:00-22:00) - aplica para walk-in Y reservas
        mesaValidador.validarHorarioAtencion();

        // 2. Validar identificador no duplicado
        mesaValidador.validarIdentificadorNoOcupado(request.getMesaIdentificador());

        // 3. Validar zona existe
        mesaValidador.validarZonaExiste(request.getZonaId());
        Zona zona = zonaRepository.findById(request.getZonaId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "Zona no encontrada",
                        HttpStatus.NOT_FOUND));

        // 4. Obtener empleado mesero
        Empleado mesero = empleadoRepository.findByUsuario_UsuarioEmail(emailMesero)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "Mesero no encontrado",
                        HttpStatus.NOT_FOUND));

        // 5. Determinar si es con reserva o walk-in
        Reserva reserva = null;
        if (request.getReservaId() != null) {
            reserva = mesaValidador.validarReservaParaAsignacion(request.getReservaId());
        }

        // 6. Crear Visita
        Visita visita = Visita.builder()
                .cliente(reserva != null ? reserva.getCliente() : null)
                .reserva(reserva)
                .visitaFechaHoraInicio(LocalDateTime.now())
                .build();
        visita = visitaRepository.save(visita);

        // 7. Crear Mesa (estado ESPERA)
        Mesa mesa = Mesa.builder()
                .visita(visita)
                .zona(zona)
                .mesero(mesero)
                .mesaIdentificador(request.getMesaIdentificador())
                .mesaNumeroPersonas(request.getNumeroPersonas())
                .mesaEstado(EstadoMesa.ESPERA)
                .mesaNotas(request.getMesaNotas())
                .build();
        mesa = mesaRepository.save(mesa);

        // 8. Si es reserva: procesar comanda y cambiar estado
        if (reserva != null) {
            procesarReserva(reserva, visita);
        }

        // 9. Publicar eventos WebSocket
        publicarEventosWebSocket(visita, reserva);

        // 10. Construir respuesta
        return MesaAsignadaResponse.builder()
                .visitaId(visita.getVisitaId())
                .mesaIdentificador(mesa.getMesaIdentificador())
                .zonaId(zona.getZonaId())
                .zonaNombre(zona.getZonaNombre())
                .numeroPersonas(mesa.getMesaNumeroPersonas())
                .estadoMesa(EstadoMesa.ESPERA.name())
                .emailMesero(emailMesero)
                .reservaId(reserva != null ? reserva.getReservaId() : null)
                .build();
    }

    /**
     * Procesa una reserva al asignar mesa: cambia comanda PRE_RESERVA a BORRADOR
     * y actualiza estado de reserva a ATENDIDA.
     *
     * @param reserva reserva a procesar
     * @param visita visita creada
     */
    private void procesarReserva(Reserva reserva, Visita visita) {
        // 1. Buscar comandas PRE_RESERVA (puede haber más de una tras el split por estación)
        List<Comanda> preordenes = comandaRepository.findByReserva_ReservaIdAndComandaEstado(
                reserva.getReservaId(), EstadoComanda.PRE_RESERVA);

        // 2. Cambiar cada comanda a BORRADOR y vincular a visita
        for (Comanda comanda : preordenes) {
            comanda.setComandaEstado(EstadoComanda.BORRADOR);
            comanda.setVisita(visita);
        }
        comandaRepository.saveAll(preordenes);

        // 3. Cambiar estado de reserva a ATENDIDA
        reserva.setReservaEstado(EstadoReserva.ATENDIDA);
        reservaRepository.save(reserva);
    }

    /**
     * Publica eventos WebSocket tras asignar mesa.
     *
     * <p><b>Eventos publicados:</b>
     * <ul>
     *   <li><b>SIEMPRE:</b> /topic/mesas (actualización mapa para todos los meseros)</li>
     *   <li><b>SI hay cliente:</b> /topic/visita/{visitaId}/orden (estado visita para cliente, con items vacíos)</li>
     *   <li><b>SI es reserva:</b> /topic/reservas/cambios (broadcast a meseros para actualizar lista de reservas activas)</li>
     * </ul>
     *
     * @param visita visita creada
     * @param reserva reserva asociada (null si es walk-in)
     */
    private void publicarEventosWebSocket(Visita visita, Reserva reserva) {
        // 1. SIEMPRE: Publicar actualización mapa de mesas
        mesaWsPublisher.publicarActualizacionMesa(
                visita.getVisitaId(),
                MesaWsPublisher.TipoEventoMesa.CREAR);

        // 2. SOLO SI HAY CLIENTE: Publicar estado visita con ítems BORRADOR si existen
        if (visita.getCliente() != null) {
            List<ComandaItem> itemsActivos =
                    comandaRepository.findAllItemsActivosByVisita(visita.getVisitaId());
            List<ItemVisitaResponse> itemsResponse = visitaEstadoMapper.toItemsVisitaResponse(itemsActivos);
            BigDecimal total = itemsActivos.stream()
                    .filter(ci -> ci.getComandaItemPrecio() != null)
                    .map(ci -> ci.getComandaItemPrecio().multiply(BigDecimal.valueOf(ci.getComandaItemCantidad())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            notificacionWsPublisher.publicarVisitaActualizada(
                    visita.getVisitaId(),
                    VisitaActualizadaWsMessage.builder()
                            .visitaId(visita.getVisitaId())
                            .items(itemsResponse)
                            .total(total)
                            .build());
        }

        // 3. SOLO SI ES RESERVA: Publicar cambios en reservas del cliente
        if (reserva != null) {
            String horaLlegada = reserva.getReservaFechaHoraLlegada()
                    .format(DateTimeFormatter.ofPattern("HH:mm"));
            String zonaNombre = reserva.getZona() != null
                    ? reserva.getZona().getZonaNombre()
                    : null;

            notificacionWsPublisher.publicarReservaActualizada(
                    ReservaActualizadaWsMessage.builder()
                            .reservaId(reserva.getReservaId())
                            .tipoEvento("ATENDIDA")
                            .clienteNombre(reserva.getCliente().getClienteNombre())
                            .horaLlegada(horaLlegada)
                            .zonaNombre(zonaNombre)
                            .build());
        }
    }

    /**
     * Lista zonas disponibles con cálculo de ocupación actual.
     *
     * <p>Una zona tiene disponibilidad si:
     * personasOcupadas (en visitas activas) < capacidadTotal
     *
     * @return lista de zonas con disponibilidad calculada
     */
    @Transactional(readOnly = true)
    public List<ZonaDisponibleMesaResponse> listarZonasDisponibles() {

        // 1. Obtener todas las zonas
        List<Zona> todasZonas = zonaRepository.findAll();

        // 2. Obtener ocupación actual por zona
        List<Object[]> ocupacionPorZona = mesaRepository.sumPersonasPorZonaActiva();
        Map<Long, Integer> ocupacionMap = ocupacionPorZona.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        // 3. Construir respuesta
        return todasZonas.stream()
                .map(zona -> {
                    Integer personasOcupadas = ocupacionMap.getOrDefault(zona.getZonaId(), 0);
                    Integer disponibilidad = zona.getZonaCapacidadPersonas() - personasOcupadas;

                    return ZonaDisponibleMesaResponse.builder()
                            .zonaId(zona.getZonaId())
                            .zonaNombre(zona.getZonaNombre())
                            .capacidadTotal(zona.getZonaCapacidadPersonas())
                            .personasOcupadas(personasOcupadas)
                            .disponibilidad(disponibilidad)
                            .build();
                })
                .filter(zona -> zona.getDisponibilidad() > 0)
                .collect(Collectors.toList());
    }

    /**
     * Evalúa si la mesa debe transicionar automáticamente al estado {@code ATENDIDA}
     * y aplica el cambio cuando se cumplen todas las condiciones.
     *
     * <p>Condiciones (todas deben cumplirse simultáneamente):
     * <ol>
     *   <li>No existen notificaciones {@code PLATOS_LISTOS} en estado {@code ACTIVA} para la visita.</li>
     *   <li>No existen notificaciones {@code BEBIDAS_LISTAS} en estado {@code ACTIVA} para la visita.</li>
     *   <li>No existen comandas en estados de producción ({@code PENDIENTE},
     *       {@code EN_PREPARACION} o {@code LISTO}) para la visita.</li>
     * </ol>
     *
     * <p>Las comandas en {@code BORRADOR} o {@code PRE_RESERVA} no afectan la evaluación
     * porque aún no fueron enviadas a producción.
     *
     * <p>El método es <b>idempotente</b>: si la mesa ya está en {@code ATENDIDA},
     * no se persiste cambio ni se publica evento WS.
     *
     * <p>Cuando aplica el cambio, publica al tópico {@code /topic/mesas} para que
     * el frontend actualice el mapa en tiempo real.
     *
     * @param visitaId identificador de la visita (PK de Mesa)
     * @throws ResourceNotFoundException si no existe mesa para esa visita
     */
    @Transactional
    public void evaluarYActualizarEstadoMesa(Long visitaId) {

        // Si hay notificaciones PLATOS_LISTOS activas, la mesa aún espera servicio
        if (notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                visitaId, TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA)) {
            return;
        }

        // Mismo razonamiento para BEBIDAS_LISTAS
        if (notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                visitaId, TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA)) {
            return;
        }

        // Si hay comandas pendientes/en preparación/listas, la mesa aún tiene producción en curso
        if (comandaRepository.existsByVisita_VisitaIdAndComandaEstadoIn(
                visitaId,
                List.of(EstadoComanda.PENDIENTE, EstadoComanda.EN_PREPARACION, EstadoComanda.LISTO))) {
            return;
        }

        Mesa mesa = mesaRepository.findById(visitaId)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa", visitaId));

        // Idempotencia: evita publicaciones WS duplicadas
        if (mesa.getMesaEstado() == EstadoMesa.ATENDIDA) {
            return;
        }

        mesa.setMesaEstado(EstadoMesa.ATENDIDA);
        mesaRepository.save(mesa);

        // Notifica al frontend del mapa de mesas el cambio de estado
        mesaWsPublisher.publicarCambioEstadoMesa(visitaId, EstadoMesa.ATENDIDA);
    }
}
