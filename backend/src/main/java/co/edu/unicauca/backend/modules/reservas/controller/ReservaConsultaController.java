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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Controlador REST para consultas de reservas del módulo de meseros.
 *
 * <p>Provee endpoints de solo lectura para:
 * <ul>
 *   <li>Listar las reservas activas del día con resumen por zona.</li>
 *   <li>Buscar una reserva específica por su identificador.</li>
 *   <li>Obtener el detalle completo de una reserva.</li>
 * </ul>
 *
 * <p>Todos los endpoints requieren autenticación y rol.
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
     * <p>La respuesta incluye:
     * <ul>
     *   <li>{@code reservas}: lista de reservas activas (estados {@code PENDIENTE} o {@code CONFIRMADA}).</li>
     *   <li>{@code resumenZonas}: cantidad de reservas agrupadas por zona.</li>
     * </ul>
     *
     * <p>Solo personal autorizado ({@code MESERO} o {@code ADMIN}) puede acceder.
     *
     * @param fecha         fecha a consultar en formato ISO (YYYY-MM-DD); opcional
     * @param identificador ID de la reserva a buscar; opcional
     * @return {@link ResponseEntity} con {@link ApiResponse} conteniendo {@link ListadoReservasResponse}
     */
    @GetMapping("/consulta")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Listar reservas activas del día o buscar por identificador")
    public ResponseEntity<ApiResponse<ListadoReservasResponse>> listarReservas(
            @Parameter(description = "Fecha a consultar en formato ISO (YYYY-MM-DD)", example = "2026-04-26")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Parameter(description = "ID de la reserva a buscar", example = "42")
            @RequestParam(required = false) Long identificador) {

        ListadoReservasResponse response = reservaConsultaService.listarReservasDelDia(fecha, identificador);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Obtiene el detalle completo de una reserva.
     *
     * <p>Solo personal autorizado ({@code MESERO} o {@code ADMIN}) puede acceder.
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
}
