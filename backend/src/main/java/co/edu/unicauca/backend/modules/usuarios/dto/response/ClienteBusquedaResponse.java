package co.edu.unicauca.backend.modules.usuarios.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Resultado de la búsqueda de clientes por coincidencia parcial de correo,
 * usado por el cajero al relacionar una cuenta con un cliente.
 */
@Getter
@Builder
public class ClienteBusquedaResponse {

    /** Identificador del cliente (usuarioId). */
    private final Long clienteId;

    /** Nombre completo del cliente. */
    private final String nombre;

    /** Correo electrónico del cliente. */
    private final String email;

    /** Puntos acumulados de por vida del cliente. */
    private final Integer puntosAcumulados;
}
