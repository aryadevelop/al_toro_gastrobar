package co.edu.unicauca.backend.modules.usuarios.controller;

import co.edu.unicauca.backend.modules.usuarios.dto.request.CrearEmpleadoRequest;
import co.edu.unicauca.backend.modules.usuarios.dto.response.EmpleadoListadoResponse;
import co.edu.unicauca.backend.modules.usuarios.dto.response.EmpleadoResponse;
import co.edu.unicauca.backend.modules.usuarios.service.EmpleadoService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/empleados")
@RequiredArgsConstructor
@Tag(name = "Empleados", description = "Gestión de registro de nuevos empleados")
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear un nuevo empleado con roles asignados")
    public ResponseEntity<ApiResponse<EmpleadoResponse>> crearEmpleado(
            @Valid @RequestBody CrearEmpleadoRequest request) {
        EmpleadoResponse response = empleadoService.crearEmpleado(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Empleado creado exitosamente", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar empleados con filtros por rol, estado y nombre")
    public ResponseEntity<ApiResponse<List<EmpleadoListadoResponse>>> listarEmpleados(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String nombre) {
        List<EmpleadoListadoResponse> response = empleadoService.listarEmpleados(rol, estado, nombre);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
