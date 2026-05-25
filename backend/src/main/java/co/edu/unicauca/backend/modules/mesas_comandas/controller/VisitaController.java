package co.edu.unicauca.backend.modules.mesas_comandas.controller;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.AjustarItemsRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.EstadoVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.service.CuentaAjusteService;
import co.edu.unicauca.backend.modules.mesas_comandas.service.VisitaEstadoService;
import co.edu.unicauca.backend.modules.mesas_comandas.service.VisitaService;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.NotificacionAsistenciaResponse;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionService;
import co.edu.unicauca.backend.modules.pagos_caja.dto.response.CuentaPreliminarResponse;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la consulta del historial y detalle de visitas del cliente.
 *
 * @see VisitaService
 */
@RestController
@RequestMapping("/api/visitas")
@RequiredArgsConstructor
@Tag(name = "Visitas", description = "Historial y detalle de visitas del cliente")
public class VisitaController {

    private final VisitaService visitaService;
    private final VisitaEstadoService visitaEstadoService;
    private final NotificacionService notificacionService;
    private final CuentaAjusteService cuentaAjusteService;

    /**
     * Retorna el historial de visitas de un cliente, ordenadas de la más reciente a la más antigua.
     *
     * <p>Si el solicitante tiene rol {@code CLIENTE}, solo puede consultar su propio historial
     * (el {@code emailCliente} del parámetro debe coincidir con el email autenticado).
     * El rol {@code ADM} puede consultar el historial de cualquier cliente sin restricción.
     *
     * @param emailCliente   correo del cliente cuyo historial se solicita
     * @param authentication contexto de seguridad del request
     * @return lista de visitas históricas ordenada descendentemente por fecha; vacía si no hay historial
     */
    @GetMapping("/cliente/historial")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    @Operation(summary = "Obtener historial de visitas pasadas del cliente")
    public ResponseEntity<ApiResponse<List<VisitaResumenResponse>>> obtenerHistorialVisitas(
            @RequestParam String emailCliente,
            Authentication authentication) {

        // Solo aplica la restricción cuando el solicitante es CLIENTE
        boolean esCliente = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));

        // Compara ignorando mayúsculas para evitar falsos negativos por capitalización
        if (esCliente && !authentication.getName().equalsIgnoreCase(emailCliente)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "Solo puedes consultar tu propio historial de visitas.", HttpStatus.FORBIDDEN);
        }

        List<VisitaResumenResponse> response = visitaService.obtenerHistorialVisitas(emailCliente);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Retorna el detalle completo de una visita específica.
     *
     * <p>Si el solicitante tiene rol {@code CLIENTE}, el servicio valida que la visita
     * pertenezca al cliente autenticado antes de retornar el detalle.
     * Otros roles (CAJERO, MESERO, ADM) acceden sin restricción de propiedad.
     *
     * @param visitaId       identificador de la visita
     * @param authentication contexto de seguridad del request
     * @return detalle completo de la visita
     */
    @GetMapping("/cliente/{visitaId}/detalle")
    @PreAuthorize("hasAnyRole('CLIENTE', 'CAJERO', 'MESERO', 'ADMIN')")
    @Operation(summary = "Obtener detalle completo de una visita")
    public ResponseEntity<ApiResponse<VisitaDetalleResponse>> obtenerDetalleVisita(
            @PathVariable Long visitaId,
            Authentication authentication) {

        boolean esCliente = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));

        String emailAutenticado = esCliente ? authentication.getName() : null;

        VisitaDetalleResponse response = visitaService.obtenerDetalleVisita(visitaId, emailAutenticado);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Retorna el estado actual de la visita activa del cliente o de la visita indicada.
     *
     * <p>Si el solicitante tiene rol {@code CLIENTE}, el email se extrae del token y se
     * consulta su propia visita activa. Para otros roles ({@code MESERO}, {@code CAJERO},
     * {@code ADMIN}), el parámetro {@code emailCliente} es obligatorio y permite consultar
     * la visita activa de cualquier cliente.
     *
     * @param emailCliente   correo del cliente a consultar; ignorado si el solicitante es CLIENTE
     * @param authentication contexto de seguridad del request
     * @return estado de la visita activa con ítems, total y estado de asistencia
     */
    @GetMapping("/activa")
    @PreAuthorize("hasAnyRole('CLIENTE', 'MESERO', 'CAJERO', 'ADMIN')")
    @Operation(summary = "Obtener estado de la visita activa")
    public ResponseEntity<ApiResponse<EstadoVisitaResponse>> obtenerEstadoVisitaActiva(
            @RequestParam(required = false) String emailCliente,
            Authentication authentication) {

        boolean esCliente = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));

        // CLIENTE siempre consulta su propia visita; otros roles usan el parámetro emailCliente
        String emailAConsultar = esCliente ? authentication.getName() : emailCliente;

        if (emailAConsultar == null || emailAConsultar.isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR,
                    "El parámetro emailCliente es requerido para este rol.", HttpStatus.BAD_REQUEST);
        }

        EstadoVisitaResponse response = visitaEstadoService.obtenerEstadoVisitaActiva(emailAConsultar);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Solicita la asistencia de un mesero para la mesa de la visita activa.
     *
     * <p>Crea una notificación {@code ATENCION} en la DB y publica un broadcast
     * WebSocket en {@code /topic/mesas/asistencia} para todos los empleados conectados.
     * Devuelve 409 si ya existe una solicitud activa para la misma mesa.
     *
     * @param visitaId       identificador de la visita activa del cliente
     * @param authentication contexto de seguridad del request
     * @return DTO con el ID de la notificación creada
     */
    @PostMapping("/{visitaId}/asistencia")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Solicitar asistencia de un mesero para la mesa actual")
    public ResponseEntity<ApiResponse<NotificacionAsistenciaResponse>> solicitarAsistencia(
            @PathVariable Long visitaId,
            Authentication authentication) {

        NotificacionAsistenciaResponse response =
                notificacionService.solicitarAsistencia(visitaId, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Solicitud de asistencia enviada.", response));
    }

    /**
     * Relaciona la cuenta de la visita con un cliente, o la marca como invitado
     * cuando {@code clienteId} se omite.
     *
     * @param visitaId  identificador de la visita
     * @param clienteId identificador del cliente; omitido = invitado
     */
    @PatchMapping("/{visitaId}/cliente")
    @PreAuthorize("hasRole('CAJERO')")
    @Operation(summary = "Asignar cliente a la visita o continuar como invitado")
    public ResponseEntity<ApiResponse<Void>> asignarCliente(
            @PathVariable Long visitaId,
            @RequestParam(required = false) Long clienteId) {
        visitaService.asignarCliente(visitaId, clienteId);
        return ResponseEntity.ok(ApiResponse.ok("Cliente de la visita actualizado", null));
    }

    /**
     * Ajusta en bloque los ítems de la cuenta (cantidades, precios de ítems
     * modificados y eliminaciones) y devuelve la cuenta recalculada.
     *
     * @param visitaId identificador de la visita
     * @param request  cambios a aplicar
     * @return la cuenta preliminar recalculada
     */
    @PatchMapping("/{visitaId}/items")
    @PreAuthorize("hasRole('CAJERO')")
    @Operation(summary = "Ajustar cantidades/precios y eliminar ítems de la cuenta")
    public ResponseEntity<ApiResponse<CuentaPreliminarResponse>> ajustarItems(
            @PathVariable Long visitaId,
            @Valid @RequestBody AjustarItemsRequest request) {
        CuentaPreliminarResponse data = cuentaAjusteService.ajustarItems(visitaId, request);
        return ResponseEntity.ok(ApiResponse.ok("Ajustes guardados", data));
    }
}
