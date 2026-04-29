package co.edu.unicauca.backend.modules.usuarios.controller;

import co.edu.unicauca.backend.modules.usuarios.dto.response.ClientePuntosResponse;
import co.edu.unicauca.backend.modules.usuarios.service.PuntosService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la consulta y gestión de puntos de fidelización.
 *
 * <p>Comportamiento general:
 * <ul>
 *   <li><b>CLIENTE:</b> consulta sus propios puntos (actuales y acumulados).</li>
 *   <li><b>CAJERO / ADMIN:</b> consulta los puntos de cualquier cliente por ID
 *       y puede canjearlos.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Consulta y gestión de puntos de fidelización")
public class ClienteController {

    private final PuntosService puntosService;

    /**
     * Retorna los puntos actuales y acumulados del cliente autenticado.
     *
     * <p>Los puntos se incrementan en 1 cada vez que el cajero cierra la cuenta
     * de una visita. {@code puntosActuales} se resetea a 0 en cada canje;
     * {@code puntosAcumulados} nunca disminuye.
     *
     * @param emailCliente   correo del cliente a consultar
     * @param authentication contexto de seguridad del request
     * @return respuesta con {@code puntosActuales} y {@code puntosAcumulados}
     */
    @GetMapping("/me/puntos")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Obtener puntos de fidelidad del cliente autenticado")
    public ResponseEntity<ApiResponse<ClientePuntosResponse>> obtenerMisPuntos(
            @RequestParam String emailCliente,
            Authentication authentication) {

        validarOwnershipCliente(emailCliente, authentication);

        ClientePuntosResponse response = puntosService.obtenerPuntos(emailCliente);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Retorna los puntos actuales y acumulados de un cliente específico.
     *
     * @param clienteId identificador del cliente a consultar
     * @return respuesta con {@code puntosActuales} y {@code puntosAcumulados}
     */
    @GetMapping("/{clienteId}/puntos")
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN')")
    @Operation(summary = "Obtener puntos de fidelidad de un cliente")
    public ResponseEntity<ApiResponse<ClientePuntosResponse>> obtenerPuntosCliente(
            @PathVariable Long clienteId) {

        ClientePuntosResponse response = puntosService.obtenerPuntosPorId(clienteId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Canjea los puntos de un cliente: resetea {@code puntosActuales} a 0 y
     * registra la auditoría en {@code canje_puntos}.
     *
     * <p>Solo se permite canjear si el cliente tiene al menos 1 punto.
     * {@code puntosAcumulados} no cambia con el canje.
     *
     * @param clienteId   identificador del cliente cuyos puntos se canjean
     * @param request     datos opcionales del canje (observación)
     * @param userDetails principal autenticado del que se extrae el correo del empleado
     * @return respuesta con el saldo actualizado ({@code puntosActuales = 0})
     */
    @PostMapping("/{clienteId}/canje-puntos")
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN')")
    @Operation(summary = "Canjear puntos de fidelidad de un cliente")
    public ResponseEntity<ApiResponse<ClientePuntosResponse>> canjearPuntos(
            @PathVariable Long clienteId,
            @RequestParam String emailEmpleado) {

        ClientePuntosResponse response = puntosService.canjearPuntos(clienteId, emailEmpleado);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Valida que el cliente autenticado sea el propietario del recurso solicitado.
     *
     * <p>Solo aplica restricción cuando el solicitante tiene rol {@code CLIENTE}. Otros roles
     * pueden acceder a recursos de cualquier cliente sin restricción.
     *
     * @param emailPropietario email del propietario del recurso
     * @param authentication   contexto de autenticación del request actual
     * @throws BusinessException con HTTP 403 si el cliente autenticado no es el propietario
     */
    private void validarOwnershipCliente(String emailPropietario, Authentication authentication) {
        // Solo aplica la restricción cuando el solicitante es CLIENTE
        boolean esCliente = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));
        // Compara ignorando mayúsculas para evitar falsos negativos por capitalización
        if (esCliente && !authentication.getName().equalsIgnoreCase(emailPropietario)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Solo puedes consultar tus propios puntos.", HttpStatus.FORBIDDEN);
        }
    }
}
