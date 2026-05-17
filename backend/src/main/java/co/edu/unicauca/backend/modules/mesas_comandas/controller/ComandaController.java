package co.edu.unicauca.backend.modules.mesas_comandas.controller;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.AgregarItemRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.ModificarItemRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.NotasRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.BorradorComandaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.service.ComandaBorradorService;
import co.edu.unicauca.backend.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST del formulario de modificar comanda.
 */
@RestController
@RequestMapping("/api/comandas")
@RequiredArgsConstructor
@Tag(name = "Comandas", description = "Modificación de comandas desde el formulario del mesero")
public class ComandaController {

    /** Servicio que concentra la lógica de borrador, envío a producción y cancelación. */
    private final ComandaBorradorService borradorService;

    /**
     * Devuelve el borrador completo de la visita. Si no hay borradores
     * devuelve estructura vacía pero válida.
     *
     * @param visitaId identificador de la visita
     * @param auth     contexto del usuario autenticado
     */
    @GetMapping("/borrador")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Obtener borrador",
            description = "Carga el formulario con ítems precargados o estructura vacía")
    public ResponseEntity<ApiResponse<BorradorComandaResponse>> obtenerBorrador(
            @Parameter(description = "Identificador de la visita") @RequestParam Long visitaId,
            Authentication auth) {
        BorradorComandaResponse data = borradorService.obtenerBorrador(visitaId, auth);
        return ResponseEntity.ok(ApiResponse.ok("Borrador obtenido exitosamente", data));
    }

    /**
     * Agrega un ítem al borrador. Si el producto es bebida va a la comanda de
     * barra; si es plato, a la de cocina. Crea la comanda BORRADOR si no existe.
     *
     * @param request payload del ítem a agregar
     * @param auth    contexto del usuario autenticado
     */
    @PostMapping("/borrador/items")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Agregar ítem al borrador",
            description = "Agrega un producto y opcionalmente una modificación libre con descripción")
    public ResponseEntity<ApiResponse<BorradorComandaResponse>> agregarItem(
            @Valid @RequestBody AgregarItemRequest request,
            Authentication auth) {
        BorradorComandaResponse data = borradorService.agregarItem(request, auth);
        return ResponseEntity.ok(ApiResponse.ok("Item agregado", data));
    }

    /**
     * Modifica cantidad y/o descripción de un ítem existente. Cualquier campo
     * nulo en el payload se interpreta como "no cambiar".
     *
     * @param itemId  identificador del {@code comanda_item}
     * @param request cambios a aplicar
     * @param auth    contexto del usuario autenticado
     */
    @PatchMapping("/borrador/items/{itemId}")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Modificar cantidad o descripción de un ítem",
            description = "Actualiza el ítem con validación de stock y techo de 250 unidades")
    public ResponseEntity<ApiResponse<BorradorComandaResponse>> modificarItem(
            @PathVariable Long itemId,
            @Valid @RequestBody ModificarItemRequest request,
            Authentication auth) {
        BorradorComandaResponse data = borradorService.modificarItem(itemId, request, auth);
        return ResponseEntity.ok(ApiResponse.ok("Item modificado", data));
    }

    /**
     * Elimina un ítem del borrador. Si es parte de un par menú especial elimina
     * ambos ítems (cocina y barra). Si es un ítem base elimina también todos
     * sus ítems modificados con el mismo {@code productoId}.
     *
     * @param itemId identificador del {@code comanda_item}
     * @param auth   contexto del usuario autenticado
     */
    @DeleteMapping("/borrador/items/{itemId}")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Eliminar ítem del borrador",
            description = "Elimina el ítem; en menú especial elimina el par cocina+barra del mismo grupo")
    public ResponseEntity<ApiResponse<BorradorComandaResponse>> eliminarItem(
            @PathVariable Long itemId,
            Authentication auth) {
        BorradorComandaResponse data = borradorService.eliminarItem(itemId, auth);
        return ResponseEntity.ok(ApiResponse.ok("Item eliminado", data));
    }

    /**
     * Envía a producción la comanda BORRADOR de una estación: transiciona a
     * PENDIENTE, valida stock, publica RabbitMQ y eventos WebSocket.
     *
     * @param comandaId identificador de la comanda a enviar
     * @param auth      contexto del usuario autenticado
     */
    @PostMapping("/borrador/{comandaId}/enviar")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Enviar comanda a producción",
            description = "Transición BORRADOR → PENDIENTE con validación final de stock y eventos asincrónicos")
    public ResponseEntity<ApiResponse<BorradorComandaResponse>> enviarAProduccion(
            @PathVariable Long comandaId,
            Authentication auth) {
        BorradorComandaResponse data = borradorService.enviarAProduccion(comandaId, auth);
        return ResponseEntity.ok(ApiResponse.ok("Comanda enviada a producción", data));
    }

    /**
     * Cancela el formulario eliminando todas las comandas BORRADOR de la visita
     * (cocina y barra) junto con sus ítems y notas.
     *
     * @param visitaId identificador de la visita
     * @param auth     contexto del usuario autenticado
     */
    @DeleteMapping("/borrador")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Cancelar formulario",
            description = "Elimina todas las comandas BORRADOR de la visita descartando los cambios")
    public ResponseEntity<ApiResponse<Void>> cancelarFormulario(
            @RequestParam Long visitaId,
            Authentication auth) {
        borradorService.cancelarFormulario(visitaId, auth);
        return ResponseEntity.ok(ApiResponse.ok("Cambios descartados", null));
    }

    /**
     * Persiste las notas de una comanda BORRADOR (cocina o barra). Cada estación
     * tiene su propio campo de notas, por lo que la petición identifica la
     * comanda y no la visita.
     *
     * @param comandaId identificador de la comanda BORRADOR
     * @param request   payload con el nuevo valor de notas (puede ser nulo para borrar)
     * @param auth      contexto del usuario autenticado
     */
    @PatchMapping("/borrador/{comandaId}/notas")
    @PreAuthorize("hasAnyRole('MESERO', 'ADMIN')")
    @Operation(summary = "Actualizar notas de la comanda",
            description = "Persiste en tiempo real las notas de cocina o barra del borrador")
    public ResponseEntity<ApiResponse<BorradorComandaResponse>> actualizarNotas(
            @PathVariable Long comandaId,
            @Valid @RequestBody NotasRequest request,
            Authentication auth) {
        BorradorComandaResponse data = borradorService.actualizarNotas(comandaId, request, auth);
        return ResponseEntity.ok(ApiResponse.ok("Notas actualizadas", data));
    }
}
