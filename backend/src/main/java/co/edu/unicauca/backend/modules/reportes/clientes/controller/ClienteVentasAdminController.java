package co.edu.unicauca.backend.modules.reportes.clientes.controller;


import co.edu.unicauca.backend.modules.reportes.clientes.dto.response.ClienteBusquedaResponse;
import co.edu.unicauca.backend.modules.reportes.clientes.dto.response.ClienteListadoResponse;
import co.edu.unicauca.backend.modules.reportes.clientes.dto.response.ClienteVentasResponse;
import co.edu.unicauca.backend.modules.reportes.clientes.dto.response.ClienteVentasResumenResponse;
import co.edu.unicauca.backend.modules.reportes.clientes.dto.response.VentaAgrupadaAnioResponse;
import co.edu.unicauca.backend.modules.reportes.clientes.dto.response.VentaAgrupadaMesResponse;
import co.edu.unicauca.backend.modules.reportes.clientes.service.ClienteVentasAdminService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Historial de ventas", description = "Consultas administrativas de ventas por cliente")
public class ClienteVentasAdminController {

    private final ClienteVentasAdminService ventasService;

    @GetMapping("/{clienteId}/ventas")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Historial completo de ventas por cliente")
    public ResponseEntity<ApiResponse<ClienteVentasResponse>> obtenerHistorial(
            @PathVariable Long clienteId) {
        ClienteVentasResponse response = ventasService.obtenerHistorial(clienteId);
        if (response.getVentas().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("Este cliente no tiene historial de compras", response));
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{clienteId}/ventas/resumen")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resumen del historial de ventas del cliente")
    public ResponseEntity<ApiResponse<ClienteVentasResumenResponse>> obtenerResumen(
            @PathVariable Long clienteId) {
        return ResponseEntity.ok(ApiResponse.ok(ventasService.obtenerResumen(clienteId)));
    }

    @GetMapping("/{clienteId}/ventas/agrupadas/anio")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Totales de ventas agrupadas por anio")
    public ResponseEntity<ApiResponse<List<VentaAgrupadaAnioResponse>>> obtenerAgrupadoPorAnio(
            @PathVariable Long clienteId) {
        return ResponseEntity.ok(ApiResponse.ok(ventasService.obtenerAgrupadoPorAnio(clienteId)));
    }

    @GetMapping("/{clienteId}/ventas/agrupadas/mes")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Totales de ventas agrupadas por mes")
    public ResponseEntity<ApiResponse<List<VentaAgrupadaMesResponse>>> obtenerAgrupadoPorMes(
            @PathVariable Long clienteId) {
        return ResponseEntity.ok(ApiResponse.ok(ventasService.obtenerAgrupadoPorMes(clienteId)));
    }

    @PostMapping("/{clienteId}/ventas/recordatorio")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enviar recordatorio a cliente inactivo")
    public ResponseEntity<ApiResponse<Void>> enviarRecordatorio(
            @PathVariable Long clienteId) {
        ventasService.enviarRecordatorio(clienteId);
        return ResponseEntity.ok(ApiResponse.message("Recordatorio enviado correctamente."));
    }

    @GetMapping("/buscar/nombre")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Buscar clientes por nombre")
    public ResponseEntity<ApiResponse<List<ClienteBusquedaResponse>>> buscarPorNombre(
            @RequestParam("valor") String valor) {
        List<ClienteBusquedaResponse> results = ventasService.buscarPorNombre(valor);
        if (results.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("No se encontraron clientes", results));
        }
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/buscar/correo")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Buscar clientes por correo")
    public ResponseEntity<ApiResponse<List<ClienteBusquedaResponse>>> buscarPorCorreo(
            @RequestParam("valor") String valor) {
        List<ClienteBusquedaResponse> results = ventasService.buscarPorCorreo(valor);
        if (results.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("No se encontraron clientes", results));
        }
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping("/buscar/telefono")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Buscar clientes por telefono")
    public ResponseEntity<ApiResponse<List<ClienteBusquedaResponse>>> buscarPorTelefono(
            @RequestParam("valor") String valor) {
        List<ClienteBusquedaResponse> results = ventasService.buscarPorTelefono(valor);
        if (results.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("No se encontraron clientes", results));
        }
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar clientes con filtros administrativos")
    public ResponseEntity<ApiResponse<List<ClienteListadoResponse>>> listarClientes(
            @RequestParam(value = "minVisitas", required = false) Integer minVisitas,
            @RequestParam(value = "maxVisitas", required = false) Integer maxVisitas,
            @RequestParam(value = "desdeRegistro", required = false) String desdeRegistro,
            @RequestParam(value = "hastaRegistro", required = false) String hastaRegistro,
            @RequestParam(value = "estado", required = false) String estado,
            @RequestParam(value = "nombre", required = false) String nombre,
            @RequestParam(value = "correo", required = false) String correo,
            @RequestParam(value = "cumpleanosHoy", required = false) Boolean cumpleanosHoy,
            @RequestParam(value = "reservasUltimosMeses", required = false) Integer reservasUltimosMeses) {
        LocalDate desde = parseRegistroDate(desdeRegistro, "desdeRegistro");
        LocalDate hasta = parseRegistroDate(hastaRegistro, "hastaRegistro");
        validateRegistroDateRange(desde, hasta);
        List<ClienteListadoResponse> results = ventasService.listarClientes(
                minVisitas, maxVisitas, desde, hasta, estado, nombre, correo, cumpleanosHoy, reservasUltimosMeses);
        if (results.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("No se encontraron clientes", results));
        }
        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    private LocalDate parseRegistroDate(String dateValue, String fieldName) {
        if (dateValue == null) {
            return null;
        }
        try {
            return LocalDate.parse(dateValue);
        } catch (DateTimeParseException ex) {
            throw new co.edu.unicauca.backend.shared.exception.BusinessException(
                    co.edu.unicauca.backend.shared.exception.ErrorCode.VALIDATION_ERROR,
                    "Fecha inválida para '" + fieldName + "'. Use el formato YYYY-MM-DD.",
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }

    private void validateRegistroDateRange(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        if (hasta != null && hasta.isAfter(hoy)) {
            throw new co.edu.unicauca.backend.shared.exception.BusinessException(
                    co.edu.unicauca.backend.shared.exception.ErrorCode.VALIDATION_ERROR,
                    "La fecha 'hastaRegistro' no puede ser mayor al día de hoy.",
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new co.edu.unicauca.backend.shared.exception.BusinessException(
                    co.edu.unicauca.backend.shared.exception.ErrorCode.VALIDATION_ERROR,
                    "La fecha 'desdeRegistro' no puede ser mayor a 'hastaRegistro'.",
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
}
