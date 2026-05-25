package co.edu.unicauca.backend.modules.usuarios.mapper;

import co.edu.unicauca.backend.modules.usuarios.dto.response.ClienteBusquedaResponse;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import org.springframework.stereotype.Component;

/**
 * Convierte entidades {@link Cliente} en {@link ClienteBusquedaResponse} para
 * los resultados de la búsqueda parcial por correo del cajero.
 */
@Component
public class ClienteBusquedaMapper {

    /**
     * Mapea un cliente a su DTO de búsqueda.
     *
     * @param cliente cliente coincidente
     * @return DTO con id, nombre, correo y puntos acumulados
     */
    public ClienteBusquedaResponse toBusqueda(Cliente cliente) {
        return ClienteBusquedaResponse.builder()
                .clienteId(cliente.getUsuarioId())
                .nombre(cliente.getClienteNombre())
                .email(cliente.getUsuario().getUsuarioEmail())
                .puntosAcumulados(cliente.getClientePuntosAcumulados())
                .build();
    }
}
