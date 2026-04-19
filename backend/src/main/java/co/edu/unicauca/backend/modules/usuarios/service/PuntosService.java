package co.edu.unicauca.backend.modules.usuarios.service;

import co.edu.unicauca.backend.modules.usuarios.dto.response.ClientePuntosResponse;
import co.edu.unicauca.backend.modules.usuarios.entity.CanjePuntos;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.repository.CanjePuntosRepository;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para la consulta y canje de puntos de fidelización.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Retornar el saldo actual y el total acumulado de un cliente.</li>
 *   <li>Ejecutar el canje: pone {@code clientePuntos} a 0 y registra la
 *       auditoría en {@code canje_puntos}. El campo
 *       {@code clientePuntosAcumulados} nunca cambia en el canje.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PuntosService {

    private final ClienteRepository clienteRepository;
    private final EmpleadoRepository empleadoRepository;
    private final CanjePuntosRepository canjePuntosRepository;

    /**
     * Devuelve los puntos actuales y acumulados del cliente identificado por su correo.
     *
     * @param emailCliente correo del cliente autenticado
     * @return {@link ClientePuntosResponse} con ambos campos
     * @throws ResourceNotFoundException si el cliente no existe
     */
    @Transactional(readOnly = true)
    public ClientePuntosResponse obtenerPuntos(String emailCliente) {
        Cliente cliente = clienteRepository.findByUsuario_UsuarioEmail(emailCliente)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente", "email", emailCliente));
        return toResponse(cliente);
    }

    /**
     * Devuelve los puntos actuales y acumulados del cliente identificado por su ID.
     * Uso exclusivo de CAJERO y ADMIN.
     *
     * @param clienteId identificador del cliente
     * @return {@link ClientePuntosResponse} con ambos campos
     * @throws ResourceNotFoundException si el cliente no existe
     */
    @Transactional(readOnly = true)
    public ClientePuntosResponse obtenerPuntosPorId(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", clienteId));
        return toResponse(cliente);
    }

    /**
     * Canjea los puntos del cliente: resetea {@code clientePuntos} a 0 y registra
     * la auditoría en {@code canje_puntos}.
     *
     * <p>Reglas:
     * <ul>
     *   <li>El cliente debe tener al menos 1 punto para poder canjear.</li>
     *   <li>{@code clientePuntosAcumulados} no cambia (es contador de vida).</li>
     * </ul>
     *
     * @param clienteId     identificador del cliente a quien se canjean los puntos
     * @param emailEmpleado correo del empleado (CAJERO o ADMIN) autenticado
     * @param request       datos opcionales del canje (observación)
     * @return {@link ClientePuntosResponse} con el saldo actualizado (puntosActuales = 0)
     * @throws ResourceNotFoundException si el cliente o el empleado no existen
     * @throws BusinessException         si el cliente no tiene puntos para canjear
     */
    @Transactional
    public ClientePuntosResponse canjearPuntos(Long clienteId, String emailEmpleado) {
        // Verifica que el cliente existe
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", clienteId));

        // El canje solo es posible si el cliente tiene al menos 1 punto acumulado
        if (cliente.getClientePuntos() == 0) {
            throw new BusinessException(
                    ErrorCode.INVALID_STATE,
                    "El cliente no tiene puntos para canjear.",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // Verifica que el empleado existe
        Empleado empleado = empleadoRepository.findByUsuario_UsuarioEmail(emailEmpleado)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empleado", "email", emailEmpleado));

        // Registra la auditoría antes de resetear el saldo
        CanjePuntos auditoria = CanjePuntos.builder()
                .cliente(cliente)
                .empleado(empleado)
                .canjePuntosCanjeados(cliente.getClientePuntos())
                .build();
        canjePuntosRepository.save(auditoria);

        // Resetea el saldo canjeable
        cliente.setClientePuntos(0);

        return toResponse(cliente);
    }

    /**
     * Convierte una entidad {@link Cliente} en el DTO de puntos de fidelización.
     *
     * @param cliente entidad con los datos de puntos
     * @return {@link ClientePuntosResponse} con los puntos actuales y acumulados
     */
    private ClientePuntosResponse toResponse(Cliente cliente) {
        return ClientePuntosResponse.builder()
                .puntosActuales(cliente.getClientePuntos())
                .puntosAcumulados(cliente.getClientePuntosAcumulados())
                .build();
    }
}
