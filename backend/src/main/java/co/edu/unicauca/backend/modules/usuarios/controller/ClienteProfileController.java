package co.edu.unicauca.backend.modules.usuarios.controller;

import co.edu.unicauca.backend.modules.usuarios.dto.request.ChangePasswordRequest;
import co.edu.unicauca.backend.modules.usuarios.dto.request.UpdateClienteRequest;
import co.edu.unicauca.backend.modules.usuarios.dto.response.ChangePasswordResponse;
import co.edu.unicauca.backend.modules.usuarios.dto.response.UpdateClienteResponse;
import co.edu.unicauca.backend.modules.usuarios.service.ClienteProfileService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la gestión del perfil del cliente autenticado.
 */
@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@Tag(name = "Perfil Cliente", description = "Gestión de datos personales del cliente autenticado")
public class ClienteProfileController {

    private final ClienteProfileService clienteProfileService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Obtener mi perfil completo")
    public ResponseEntity<ApiResponse<UpdateClienteResponse.ClienteData>> obtenerMiPerfil(
            Authentication authentication) {
        Long clienteId = extraerClienteIdDelToken(authentication);
        UpdateClienteResponse.ClienteData perfil = clienteProfileService.obtenerMiPerfil(clienteId);
        return ResponseEntity.ok(ApiResponse.ok(perfil));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Actualizar mi perfil")
    public ResponseEntity<ApiResponse<UpdateClienteResponse>> actualizarMiPerfil(
            @Valid @RequestBody UpdateClienteRequest request,
            Authentication authentication) {
        Long clienteId = extraerClienteIdDelToken(authentication);
        UpdateClienteResponse response = clienteProfileService.actualizarPerfil(clienteId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/me/cambiar-contraseña")
    @PreAuthorize("hasRole('CLIENTE')")
    @Operation(summary = "Cambiar mi contraseña")
    public ResponseEntity<ApiResponse<ChangePasswordResponse>> cambiarContraseña(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        Long clienteId = extraerClienteIdDelToken(authentication);
        ChangePasswordResponse response = clienteProfileService.cambiarContraseña(clienteId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    private Long extraerClienteIdDelToken(Authentication authentication) {
        String email = authentication.getName();
        return clienteProfileService.getClienteIdByEmail(email);
    }
}
