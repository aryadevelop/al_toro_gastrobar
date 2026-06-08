package co.edu.unicauca.backend.modules.usuarios.service;

import co.edu.unicauca.backend.modules.usuarios.dto.response.ClienteBusquedaResponse;
import co.edu.unicauca.backend.modules.usuarios.mapper.ClienteBusquedaMapper;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/**
 * Servicio de búsqueda de clientes por coincidencia parcial de correo.
 *
 * <p>Usado por el cajero al relacionar la cuenta de una mesa con un cliente.
 * La búsqueda es insensible a mayúsculas y a espacios extremos.
 */
@Service
@RequiredArgsConstructor
public class ClienteBusquedaService {

    private final ClienteRepository clienteRepository;
    private final ClienteBusquedaMapper clienteBusquedaMapper;

    /**
     * Busca clientes cuyo correo contiene el fragmento indicado.
     *
     * <p>Flujo:
     * <ol>
     *   <li>Valida que el fragmento no sea nulo ni vacío.</li>
     *   <li>Normaliza el fragmento (trim + minúsculas), igual que el registro de usuarios.</li>
     *   <li>Consulta por coincidencia parcial insensible a mayúsculas.</li>
     *   <li>Mapea cada coincidencia a su DTO.</li>
     * </ol>
     *
     * @param correo fragmento del correo a buscar
     * @return lista de clientes coincidentes; vacía si ninguno coincide
     * @throws BusinessException si el fragmento es nulo o vacío
     */
    @Transactional(readOnly = true)
    public List<ClienteBusquedaResponse> buscarPorEmail(String correo) {
        // El fragmento es obligatorio: sin él no hay criterio de búsqueda
        if (correo == null || correo.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "El correo de búsqueda es obligatorio.", HttpStatus.BAD_REQUEST);
        }
        // Normalización consistente con AuthService.normalizeEmail (trim + minúsculas)
        String fragmento = correo.trim().toLowerCase(Locale.ROOT);
        // Coincidencia parcial; IgnoreCase cubre mayúsculas, los correos se guardan normalizados
        return clienteRepository.findByUsuario_UsuarioEmailContainingIgnoreCase(fragmento).stream()
                .map(clienteBusquedaMapper::toBusqueda)
                .toList();
    }
}
