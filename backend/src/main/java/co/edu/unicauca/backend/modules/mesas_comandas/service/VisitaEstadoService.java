package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.EstadoVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.VisitaEstadoMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio de negocio para la consulta del estado de la visita activa del cliente.
 *
 * <p>Permite al cliente (y a empleados autorizados) ver los ítems pedidos,
 * el total acumulado y si hay una solicitud de asistencia pendiente.
 *
 * @see EstadoVisitaResponse
 * @see VisitaEstadoMapper
 */
@Service
@RequiredArgsConstructor
public class VisitaEstadoService {

    private final VisitaRepository visitaRepository;
    private final MesaRepository mesaRepository;
    private final ComandaRepository comandaRepository;
    private final NotificacionRepository notificacionRepository;
    private final VisitaEstadoMapper visitaEstadoMapper;

    /**
     * Devuelve el estado completo de la visita activa identificada por el email del cliente.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Busca la visita con {@code visitaFechaHoraFin IS NULL} para el email dado.</li>
     *   <li>Recupera la mesa asignada (puede ser null si el mesero aún no la asignó).</li>
     *   <li>Consolida los ítems de todas las comandas, excluyendo estado PRE_RESERVA.</li>
     *   <li>Verifica si existe una notificación de asistencia ACTIVA para la mesa.</li>
     *   <li>Delega la construcción del DTO al mapper.</li>
     * </ol>
     *
     * @param emailCliente correo del cliente cuya visita activa se consulta
     * @return estado de la visita activa con ítems, total y estado de asistencia
     * @throws BusinessException con HTTP 404 si el cliente no tiene visita activa
     */
    @Transactional(readOnly = true)
    public EstadoVisitaResponse obtenerEstadoVisitaActiva(String emailCliente) {

        // Busca la visita activa del cliente (sin fecha de fin)
        Visita visita = visitaRepository.findActiveByClienteEmail(emailCliente)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BUSINESS_ERROR,
                        "No tienes una visita activa en este momento.",
                        HttpStatus.NOT_FOUND));

        Long visitaId = visita.getVisitaId();

        // La mesa puede no estar asignada aún si el mesero no la ha registrado
        Optional<Mesa> mesaOpt = mesaRepository.findByVisita_VisitaId(visitaId);
        String mesaIdentificador = mesaOpt.map(Mesa::getMesaIdentificador).orElse(null);

        // Obtiene todas las comandas de la visita
        List<Comanda> comandas = comandaRepository.findByVisita_VisitaId(visitaId);

        // Mapea ítems ordenados por categoría (delegado al mapper)
        List<ItemVisitaResponse> items = visitaEstadoMapper.mapearItemsOrdenados(comandas);

        // Verifica si hay una solicitud de asistencia sin atender para esta mesa
        Optional<Notificacion> asistenciaActiva = notificacionRepository
                .findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                        visitaId, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA);

        return visitaEstadoMapper.toEstadoVisitaResponse(visita, mesaIdentificador, items, asistenciaActiva);
    }
}
