package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.VisitaMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.pagos_caja.repository.VentaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio de negocio para la consulta del historial y detalle de visitas del cliente.
 *
 * <p>Implementa los criterios de aceptación CA-05 (historial de visitas) y CA-07
 * (detalle de una visita). Consolida datos de visitas con reserva y walk-ins,
 * accediendo a repositorios de los módulos {@code reservas} y {@code pagos_caja}.
 *
 * @see VisitaMapper
 */
@Service
@RequiredArgsConstructor
public class VisitaService {

    private final ClienteRepository clienteRepository;
    private final VisitaRepository visitaRepository;
    private final MesaRepository mesaRepository;
    private final ComandaRepository comandaRepository;
    private final ComandaItemRepository comandaItemRepository;
    private final VentaRepository ventaRepository;
    private final AbonoRepository abonoRepository;
    private final VisitaMapper visitaMapper;

    /**
     * Devuelve el historial de visitas del cliente, ordenadas de la más reciente a la más antigua.
     *
     * <p>Una visita es:
     * <ul>
     *   <li>Una reserva que se convirtió en visita efectiva (el cliente se presentó).</li>
     *   <li>Una visita walk-in (el cliente llegó sin reserva previa).</li>
     * </ul>
     *
     * <p>No incluye reservas canceladas, devueltas ni inasistencias, ya que esas
     * no generan una visita. Esas se consultan en {@code GET /api/reservas/cliente/canceladas-devueltas}.
     *
     * @param emailCliente correo del cliente
     * @return lista de {@link VisitaResumenResponse} ordenada por fecha descendente
     * @throws ResourceNotFoundException si el cliente no existe
     */
    @Transactional(readOnly = true)
    public List<VisitaResumenResponse> obtenerHistorialVisitas(String emailCliente) {

        // Verifica que el cliente exista en la base de datos
        Cliente cliente = clienteRepository.findByUsuario_UsuarioEmail(emailCliente)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente", "email", emailCliente));

        // Obtiene todas las visitas del cliente, ordenadas por fecha descendente
        return visitaRepository
                .findByCliente_UsuarioIdOrderByVisitaFechaHoraInicioDesc(cliente.getUsuarioId())
                .stream()
                .map(v -> {
                    Optional<Mesa> mesaOpt =
                            mesaRepository.findByVisita_VisitaId(v.getVisitaId());
                    Optional<Venta> ventaOpt =
                            ventaRepository.findByVisita_VisitaId(v.getVisitaId());
                    return visitaMapper.toResumen(v, mesaOpt, ventaOpt);
                })
                .collect(Collectors.toList());
    }

    /**
     * Devuelve el detalle completo de una visita.
     *
     * <p>Si la visita proviene de una reserva, devuelve zona, decoración, notas, comanda, abonos y total.
     * Para walk-ins (sin reserva), los campos {@code reservaId}, {@code decoracionNombre},
     * {@code notas} y {@code abonos} serán {@code null}.
     *
     * <p>Si {@code emailClienteAutenticado} no es {@code null} (el solicitante tiene rol CLIENTE),
     * se valida que la visita pertenezca a ese cliente; de lo contrario se lanza
     * {@link BusinessException} con HTTP 403.
     *
     * @param visitaId               identificador de la visita a detallar
     * @param emailClienteAutenticado email del cliente autenticado, o {@code null} si el rol no es CLIENTE
     * @return {@link VisitaDetalleResponse} con el detalle completo
     * @throws ResourceNotFoundException si la visita no existe
     * @throws BusinessException         si el cliente autenticado no es propietario de la visita
     */
    @Transactional(readOnly = true)
    public VisitaDetalleResponse obtenerDetalleVisita(Long visitaId,
                                                      @Nullable String emailClienteAutenticado) {

        // Verifica que la visita exista en la base de datos
        Visita visita = visitaRepository.findById(visitaId)
                .orElseThrow(() -> new ResourceNotFoundException("Visita", visitaId));

        // Si el solicitante es CLIENTE, valida que la visita le pertenezca
        if (emailClienteAutenticado != null) {
            boolean esDelCliente = visita.getCliente() != null &&
                    emailClienteAutenticado.equals(
                            visita.getCliente().getUsuario().getUsuarioEmail());
            if (!esDelCliente) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED,
                        "No tienes acceso a esta visita.", HttpStatus.FORBIDDEN);
            }
        }

        // Obtiene las comandas de la visita y consolida sus item
        List<Comanda> comandas = comandaRepository.findByVisita_VisitaId(visitaId);
        List<ComandaItem> todosLosDetalles = comandas.stream()
                .flatMap(c -> comandaItemRepository
                        .findByComanda_ComandaId(c.getComandaId()).stream())
                .collect(Collectors.toList());

        // Obtiene el total de la venta (si la cuenta fue cerrada) y la mesa asignada
        Optional<Venta> ventaOpt = ventaRepository.findByVisita_VisitaId(visitaId);
        Optional<Mesa> mesaOpt = mesaRepository.findByVisita_VisitaId(visitaId);

        // Si la visita tiene reserva, obtiene los abonos registrados; si no, deja el campo vacío
        if (visita.getReserva() != null) {
            Long reservaId = visita.getReserva().getReservaId();
            List<Abono> abonos = abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(reservaId);
            return visitaMapper.toDetalle(visita, todosLosDetalles, Optional.of(abonos), ventaOpt, mesaOpt);
        }

        return visitaMapper.toDetalle(visita, todosLosDetalles, Optional.empty(), ventaOpt, mesaOpt);
    }
}
