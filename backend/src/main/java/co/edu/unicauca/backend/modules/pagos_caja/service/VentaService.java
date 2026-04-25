package co.edu.unicauca.backend.modules.pagos_caja.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.CuentaCerradaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.pagos_caja.dto.request.CerrarCuentaRequest;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import co.edu.unicauca.backend.modules.pagos_caja.repository.VentaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
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
 * <p>Centraliza la lógica para crear una {@link Venta} al finalizar una visita.
 * Implementa la regla de fidelización: cada vez que se cierra una cuenta, 
 * el cliente asociado a esa visita acumula un punto de fidelidad.
 *
 * <p>Responsabilidades principales:
 * <ul>
 *   <li>Validar que la visita exista y que su cuenta no haya sido cerrada antes.</li>
 *   <li>Persistir la entidad {@link Venta} con los datos del cajero y los montos.</li>
 *   <li>Incrementar en 1 el campo {@code clientePuntos} del cliente de la visita.</li>
 * </ul>
 *
 * @see Venta
 * @see co.edu.unicauca.backend.modules.usuarios.entity.Cliente#getClientePuntos()
 */
@Service
@RequiredArgsConstructor
public class VentaService {

    private final VisitaRepository visitaRepository;
    private final VentaRepository ventaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final NotificacionWsPublisher wsPublisher;

    /**
     * Registra la venta de una visita y cierra su cuenta.
     *
     * <p>Flujo de ejecución:
     * <ol>
     *   <li>Verifica que la visita exista en la base de datos.</li>
     *   <li>Verifica que la cuenta no haya sido cerrada previamente.</li>
     *   <li>Recupera el empleado (cajero) que ejecuta el cierre por su correo electrónico.</li>
     *   <li>Persiste la entidad {@link Venta} con los montos y método de pago.</li>
     *   <li>Incrementa en 1 el contador de puntos del cliente de la visita.</li>
     * </ol>
     *
     * @param request      datos de la venta a registrar (visitaId, subtotal, total, método)
     * @param emailCajero  correo del empleado autenticado que cierra la cuenta
     * @throws ResourceNotFoundException si la visita o el cajero no existen
     * @throws BusinessException         si la cuenta de esa visita ya fue cerrada previamente
     */
    @Transactional
    public void cerrarCuenta(CerrarCuentaRequest request, String emailCajero) {

        // Verifica que la visita exista
        Visita visita = visitaRepository.findById(request.getVisitaId())
                .orElseThrow(() -> new ResourceNotFoundException("Visita", request.getVisitaId()));

        // Verifica que la cuenta no haya sido cerrada previamente
        if (ventaRepository.findByVisita_VisitaId(request.getVisitaId()).isPresent()) {
            throw new BusinessException(
                    ErrorCode.INVALID_STATE,
                    "La cuenta de esta visita ya fue cerrada.",
                    HttpStatus.CONFLICT);
        }

        // Recupera el cajero autenticado
        Empleado cajero = empleadoRepository.findByUsuario_UsuarioEmail(emailCajero)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", "email", emailCajero));

        // Construye y persiste la venta
        Venta venta = Venta.builder()
                .visita(visita)
                .cajero(cajero)
                .ventaSubtotal(request.getSubtotal())
                .ventaDescuento(request.getDescuento() != null
                        ? request.getDescuento()
                        : BigDecimal.ZERO)
                .ventaTotal(request.getTotal())
                .ventaMetodo(request.getMetodo())
                .build();

        ventaRepository.save(venta);

        // Acumula 1 punto por visita cerrada al cliente asociado
        Cliente cliente = visita.getCliente();
        if (cliente != null) {
            incrementarPuntosCliente(cliente);
        }

        // Registrar hora de cierre de la visita
        visita.setVisitaFechaHoraFin(LocalDateTime.now());
        visitaRepository.save(visita);

        // Notificar al cliente vía WebSocket: cuenta cerrada + puntos actualizados
        Integer puntosActuales = (cliente != null) ? cliente.getClientePuntos() : null;
        wsPublisher.publicarCuentaCerrada(visita.getVisitaId(),
                CuentaCerradaWsMessage.builder()
                        .visitaId(visita.getVisitaId())
                        .mensaje("La cuenta ya está cerrada. ¡Gracias por tu visita!")
                        .puntosActuales(puntosActuales)
                        .build());

        // TODO: Cambiar estado de mesa a disponible
        // TODO: Verificar que las notificaciones hayan sido atendidas antes de cerrar
        // TODO: Cerrar comandas abiertas de la visita
        // TODO: Verificar todos los estados relacionados con la visita antes de cerrarla
        // TODO: Actualizar dashboard de mesas: retirar la mesa de ocupadas y marcarla como disponible
    }

    /**
     * Incrementa en 1 los puntos actuales y acumulados del cliente tras un cierre de cuenta.
     *
     * <p>{@code clientePuntosAcumulados} nunca disminuye — solo crece con cada visita cerrada,
     * independientemente de canjes posteriores.</p>
     *
     * @param cliente cliente que realizó la visita
     */
    private void incrementarPuntosCliente(Cliente cliente) {
        // Suma al saldo actual
        cliente.setClientePuntos(cliente.getClientePuntos() + 1);
        // Suma al acumulado histórico
        cliente.setClientePuntosAcumulados(cliente.getClientePuntosAcumulados() + 1);
    }
}
