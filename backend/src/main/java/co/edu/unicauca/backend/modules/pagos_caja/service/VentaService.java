package co.edu.unicauca.backend.modules.pagos_caja.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.CuentaCerradaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.service.MesaWsPublisher;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.pagos_caja.dto.request.CerrarCuentaRequest;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import co.edu.unicauca.backend.modules.pagos_caja.repository.VentaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Servicio de negocio para el registro y cierre de cuentas de visitas.
 *
 * <p>Al cerrar la cuenta recalcula el total en el servidor a partir de las comandas
 * persistidas (pre-orden + decoración con costo), valida el descuento, persiste la
 * {@link Venta}, acumula un punto de fidelidad al cliente, cierra la visita y la mesa,
 * marca las comandas como completadas y notifica vía WebSocket al cliente y al mapa.
 *
 * @see Venta
 */
@Service
@RequiredArgsConstructor
public class VentaService {

    private final VisitaRepository visitaRepository;
    private final VentaRepository ventaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ClienteRepository clienteRepository;
    private final MesaRepository mesaRepository;
    private final ComandaRepository comandaRepository;
    private final NotificacionWsPublisher wsPublisher;
    private final MesaWsPublisher mesaWsPublisher;

    /**
     * Registra la venta de una visita y cierra su cuenta.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Bloquea la visita (mutex del cierre) y valida que exista.</li>
     *   <li>Valida que la cuenta no haya sido cerrada previamente.</li>
     *   <li>Recupera el cajero por su correo.</li>
     *   <li>Recalcula subtotal = ítems activos + decoración con costo (server-side).</li>
     *   <li>Valida que el descuento no supere el total y calcula el total final.</li>
     *   <li>Persiste la venta con los montos recalculados.</li>
     *   <li>Si hay cliente, suma 1 punto con bloqueo (serializa con el canje).</li>
     *   <li>Cierra la visita, marca la mesa CERRADA y las comandas COMPLETADO.</li>
     *   <li>Notifica al cliente (cuenta cerrada) y refresca el mapa de mesas.</li>
     * </ol>
     *
     * @param request     datos del cierre (visitaId, descuento, método)
     * @param emailCajero correo del cajero autenticado
     * @throws ResourceNotFoundException si la visita o el cajero no existen
     * @throws BusinessException         si la cuenta ya fue cerrada o el descuento supera el total
     */
    @Transactional
    public void cerrarCuenta(CerrarCuentaRequest request, String emailCajero) {
        Long visitaId = request.getVisitaId();

        // 1. Lock pesimista de la visita: serializa cierre, ajuste y asignación de cliente
        Visita visita = visitaRepository.findByIdForUpdate(visitaId)
                .orElseThrow(() -> new ResourceNotFoundException("Visita", visitaId));

        // 2. Idempotencia: la cuenta no puede haberse cerrado antes
        if (ventaRepository.findByVisita_VisitaId(visitaId).isPresent()) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "La cuenta de esta visita ya fue cerrada.", HttpStatus.CONFLICT);
        }

        // 3. Cajero autenticado
        Empleado cajero = empleadoRepository.findByUsuario_UsuarioEmail(emailCajero)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", "email", emailCajero));

        // 4. Recompute server-side: pre-orden (mismos ítems activos que muestra la cuenta;
        //    bebida de menú = 0 → sin doble conteo) + decoración con costo
        BigDecimal subtotal = comandaRepository.findAllItemsActivosByVisita(visitaId).stream()
                .filter(i -> i.getComandaItemPrecio() != null)
                .map(i -> i.getComandaItemPrecio().multiply(BigDecimal.valueOf(i.getComandaItemCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(obtenerValorDecoracion(visita));

        // 5. Descuento y validación: no puede superar el total a pagar
        BigDecimal descuento = request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO;
        if (descuento.compareTo(subtotal) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "El descuento no puede ser mayor al total.", HttpStatus.CONFLICT);
        }
        BigDecimal total = subtotal.subtract(descuento);

        // 6. Persistir la venta con los montos recalculados (ventaTotal = total bruto de la cuenta)
        Venta venta = Venta.builder()
                .visita(visita)
                .cajero(cajero)
                .ventaSubtotal(subtotal)
                .ventaDescuento(descuento)
                .ventaTotal(total)
                .ventaMetodo(request.getMetodo())
                .build();
        ventaRepository.save(venta);

        // 7. +1 punto al cliente (con lock para evitar lost update frente al canje)
        Integer puntosActuales = null;
        if (visita.getCliente() != null) {
            Cliente cliente = clienteRepository.findByIdForUpdate(visita.getCliente().getUsuarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente", visita.getCliente().getUsuarioId()));
            incrementarPuntosCliente(cliente);
            puntosActuales = cliente.getClientePuntos();
        }

        // 8. Cerrar visita, mesa y comandas
        visita.setVisitaFechaHoraFin(LocalDateTime.now());
        visitaRepository.save(visita);
        mesaRepository.findById(visitaId).ifPresent(mesa -> {
            mesa.setMesaEstado(EstadoMesa.CERRADA);
            mesaRepository.save(mesa);
        });
        for (Comanda comanda : comandaRepository.findByVisita_VisitaId(visitaId)) {
            // Solo transicionar las que no estén ya completadas
            if (comanda.getComandaEstado() != EstadoComanda.COMPLETADO) {
                comanda.setComandaEstado(EstadoComanda.COMPLETADO);
                comandaRepository.save(comanda);
            }
        }

        // 9. WebSocket: confirmación al cliente + refresco del mapa de mesas
        wsPublisher.publicarCuentaCerrada(visitaId,
                CuentaCerradaWsMessage.builder()
                        .visitaId(visitaId)
                        .mensaje("Pago registrado correctamente")
                        .puntosActuales(puntosActuales)
                        .build());
        mesaWsPublisher.publicarActualizacionMesa(visitaId, MesaWsPublisher.TipoEventoMesa.CERRAR);
    }

    /**
     * Obtiene el costo de la decoración de la visita si proviene de una reserva con
     * decoración de pago; en otro caso {@link BigDecimal#ZERO}.
     */
    private BigDecimal obtenerValorDecoracion(Visita visita) {
        if (visita.getReserva() != null
                && visita.getReserva().getDecoracion() != null
                && visita.getReserva().getDecoracion().getDecoracionCostoAdicional() != null) {
            return visita.getReserva().getDecoracion().getDecoracionCostoAdicional();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Incrementa en 1 los puntos actuales y acumulados del cliente tras un cierre de cuenta.
     * {@code clientePuntosAcumulados} nunca disminuye.
     *
     * @param cliente cliente que realizó la visita
     */
    private void incrementarPuntosCliente(Cliente cliente) {
        cliente.setClientePuntos(cliente.getClientePuntos() + 1);
        cliente.setClientePuntosAcumulados(cliente.getClientePuntosAcumulados() + 1);
    }
}
