package co.edu.unicauca.backend.modules.reservas.controller;

import co.edu.unicauca.backend.modules.reservas.dto.response.ListadoReservasResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.service.ReservaConsultaService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Controlador REST para consultas de reservas del módulo de meseros.
 *
 * @see ReservaConsultaService
 */
@RestController
@RequestMapping("/api/reservas/mesero")
@RequiredArgsConstructor
@Tag(name = "Reservas - Mesero", description = "Consulta de reservas para meseros")
public class ReservaConsultaController {

    private final ReservaConsultaService reservaConsultaService;

    /**
     * Lista las reservas activas del día o busca por identificador.
     *
     * <p>Comportamiento según parámetros:
     * <ul>
     *   <li>Sin parámetros: retorna las reservas del día actual.</li>
     *   <li>Con {@code fecha}: retorna las reservas de la fecha especificada.</li>
     *   <li>Con {@code identificador}: ignora la fecha y busca solo la reserva con ese ID.</li>
     * </ul>
     *
     * <p>La respuesta varía según el rol del solicitante:
     * <ul>
     *   <li><b>MESERO/ADMIN</b>: reservas activas (estados {@code PENDIENTE} o {@code CONFIRMADA})
     *       con el indicador de inasistencia por ítem.</li>
     *   <li><b>CAJERO</b>: reservas en cualquier estado, con el {@code tipo} y los botones de
     *       acción del cajero por ítem.</li>
     * </ul>
     * En ambos casos incluye {@code resumenZonas} con la cantidad de reservas agrupadas por zona.
     *
     * @param fecha          fecha a consultar en formato ISO (YYYY-MM-DD); opcional
     * @param identificador  ID de la reserva a buscar; opcional
     * @param authentication contexto de seguridad del request, usado para determinar la vista por rol
     * @return {@link ResponseEntity} con {@link ApiResponse} conteniendo {@link ListadoReservasResponse}
     */
    @GetMapping("/consulta")
    @PreAuthorize("hasAnyRole('MESERO', 'CAJERO', 'ADMIN')")
    @Operation(summary = "Listar reservas del día o buscar por identificador")
    public ResponseEntity<ApiResponse<ListadoReservasResponse>> listarReservas(
            @Parameter(description = "Fecha a consultar en formato ISO (YYYY-MM-DD)", example = "2026-04-26")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Parameter(description = "ID de la reserva a buscar", example = "42")
            @RequestParam(required = false) Long identificador,
            Authentication authentication) {

        ListadoReservasResponse response = reservaConsultaService
                .listarReservasDelDia(fecha, identificador, esCajero(authentication));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Obtiene el detalle completo de una reserva.
     *
     * @param reservaId identificador de la reserva
     * @return {@link ResponseEntity} con {@link ApiResponse} conteniendo {@link ReservaDetalleResponse}
     */
    @GetMapping("/{reservaId}/detalle")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Obtener detalle completo de una reserva")
    public ResponseEntity<ApiResponse<ReservaDetalleResponse>> obtenerDetalle(
            @Parameter(description = "ID de la reserva", example = "10")
            @PathVariable Long reservaId) {

        ReservaDetalleResponse response = reservaConsultaService.obtenerDetalleReserva(reservaId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Determina si el usuario autenticado tiene rol {@code CAJERO}, para seleccionar la vista
     * de cajero (todas las reservas y botones de acción) frente a la de mesero.
     *
     * @param authentication contexto de autenticación del request actual
     * @return {@code true} si el usuario posee el rol {@code CAJERO}; {@code false} en caso contrario
     */
    private boolean esCajero(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_CAJERO"));
    }
}
