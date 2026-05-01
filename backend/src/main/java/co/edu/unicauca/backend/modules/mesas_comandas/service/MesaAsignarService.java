package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.AsignarMesaRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.MesaAsignadaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ZonaDisponibleMesaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ReservaActualizadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private final MesaValidador mesaValidador;
    private final MesaRepository mesaRepository;
    private final VisitaRepository visitaRepository;
    private final ZonaRepository zonaRepository;
    private final ReservaRepository reservaRepository;
    private final ComandaRepository comandaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final MesaWsPublisher mesaWsPublisher;
    private final NotificacionWsPublisher notificacionWsPublisher;

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
                .visitaId(visita.getVisitaId())
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
        // 1. Buscar comanda PRE_RESERVA
        Optional<Comanda> comandaOpt = comandaRepository.findByReserva_ReservaIdAndComandaEstado(
                reserva.getReservaId(), EstadoComanda.PRE_RESERVA);

        // 2. Si existe, cambiar a BORRADOR y vincular a visita
        comandaOpt.ifPresent(comanda -> {
            comanda.setComandaEstado(EstadoComanda.BORRADOR);
            comanda.setVisita(visita);
            comandaRepository.save(comanda);
        });

        // 3. Cambiar estado de reserva a ATENDIDA
        reserva.setReservaEstado(EstadoReserva.ATENDIDA);
        reservaRepository.save(reserva);
    }

    /**
     * Publica eventos WebSocket tras asignar mesa.
     *
     * <p><b>Eventos publicados:</b>
     * <ul>
     *   <li>/topic/mesas (actualización mapa para todos los meseros)</li>
     *   <li><b>SI es reserva:</b> /topic/reservas/cambios (broadcast a meseros para actualizar lista de reservas activas)</li>
     * </ul>
     *
     * <p><b>Nota:</b> No se publica estado de visita al cliente porque al momento de asignar
     * la mesa no hay items de comanda aún. La notificación de estado visita se envía cuando
     * se añaden items, no al crear la mesa.
     *
     * @param visita visita creada
     * @param reserva reserva asociada (null si es walk-in)
     */
    private void publicarEventosWebSocket(Visita visita, Reserva reserva) {
        // 1. SIEMPRE: Publicar actualización mapa de mesas (todos los meseros)
        mesaWsPublisher.publicarActualizacionMesa(
                visita.getVisitaId(),
                MesaWsPublisher.TipoEventoMesa.CREAR);

        // 2. SOLO SI ES RESERVA: Publicar cambios en reservas del cliente
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
}
