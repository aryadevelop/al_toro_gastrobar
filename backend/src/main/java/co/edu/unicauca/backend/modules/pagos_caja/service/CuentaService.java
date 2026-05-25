package co.edu.unicauca.backend.modules.pagos_caja.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.pagos_caja.dto.response.CuentaPreliminarResponse;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.mapper.CuentaMapper;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio que arma la cuenta preliminar de una visita para el cierre de venta.
 *
 * <p>Consolida ítems, totales (pre-orden + decoración), abonos y puntos del cliente.
 * Es de solo lectura: no muta estado.
 */
@Service
@RequiredArgsConstructor
public class CuentaService {

    private final VisitaRepository visitaRepository;
    private final ComandaRepository comandaRepository;
    private final AbonoRepository abonoRepository;
    private final MesaRepository mesaRepository;
    private final CuentaMapper cuentaMapper;

    /**
     * Obtiene la cuenta preliminar de una visita.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Verifica que la visita exista.</li>
     *   <li>Carga los ítems activos y el total de la pre-orden (la suma ignora precios nulos).</li>
     *   <li>Si la visita proviene de una reserva, carga sus abonos; si no, lista vacía.</li>
     *   <li>Carga la mesa asignada (si existe) para mesero/identificador.</li>
     *   <li>Delega el ensamblado del DTO al mapper.</li>
     * </ol>
     *
     * @param visitaId identificador de la visita
     * @return la cuenta preliminar
     * @throws ResourceNotFoundException si la visita no existe
     */
    @Transactional(readOnly = true)
    public CuentaPreliminarResponse obtenerCuenta(Long visitaId) {
        // 1. La visita debe existir
        Visita visita = visitaRepository.findById(visitaId)
                .orElseThrow(() -> new ResourceNotFoundException("Visita", visitaId));

        // 2. Ítems activos de la visita (fuente única para líneas y total; el mapper deriva el total)
        List<ComandaItem> items = comandaRepository.findAllItemsActivosByVisita(visitaId);

        // 3. Abonos solo si la visita proviene de una reserva
        List<Abono> abonos = (visita.getReserva() != null)
                ? abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(visita.getReserva().getReservaId())
                : List.of();

        // 4. Mesa asignada (mesero, identificador); puede no existir
        Optional<Mesa> mesa = mesaRepository.findById(visitaId);

        // 5. Ensamblar la respuesta
        return cuentaMapper.toCuenta(visita, items, abonos, mesa);
    }
}
