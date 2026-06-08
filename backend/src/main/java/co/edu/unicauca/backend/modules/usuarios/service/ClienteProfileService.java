package co.edu.unicauca.backend.modules.usuarios.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRepository;
import co.edu.unicauca.backend.modules.usuarios.dto.request.ChangePasswordRequest;
import co.edu.unicauca.backend.modules.usuarios.dto.request.UpdateClienteRequest;
import co.edu.unicauca.backend.modules.usuarios.dto.response.ChangePasswordResponse;
import co.edu.unicauca.backend.modules.usuarios.dto.response.UpdateClienteResponse;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Servicio de negocio para la gestión del perfil del cliente.
 *
 * <p>Centraliza la lógica de lectura y actualización de datos personales del cliente,
 * así como el cambio de contraseña. Valida unicidad de email y teléfono, y aplica
 * reglas de negocio sobre cambios de contraseña.
 *
 * <p>Responsabilidades principales:
 * <ul>
 *   <li><b>Obtener perfil:</b> retorna los datos actuales del cliente.</li>
 *   <li><b>Actualizar perfil:</b> valida y actualiza nombre, email, teléfono, dirección
 *       y aceptación de términos, asegurando unicidad de email y teléfono.</li>
 *   <li><b>Cambiar contraseña:</b> valida contraseña actual, verifica requisitos de
 *       fuerza de la nueva contraseña y la actualiza.</li>
 * </ul>
 *
 * @see ClienteRepository
 * @see UsuarioRepository
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ClienteProfileService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /** Patrón de validación para contraseña fuerte: 8+ caracteres, mayúscula, minúscula, número y símbolo. */
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{8,}$");

    /**
     * Obtiene el ID del cliente a partir del email del usuario asociado.
     *
     * @param email correo electrónico del usuario
     * @return identificador único del cliente
     * @throws ResourceNotFoundException si el usuario o cliente no existen
     */
    public Long getClienteIdByEmail(String email) {
        Cliente cliente = clienteRepository.findByUsuario_UsuarioEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "email", email));
        return cliente.getUsuarioId();
    }

    /**
     * Obtiene los datos actuales del cliente identificado por su ID.
     *
     * @param clienteId identificador único del cliente
     * @return datos del cliente (nombre, email, teléfono, dirección, puntos, términos)
     * @throws ResourceNotFoundException si el cliente no existe
     */
    public UpdateClienteResponse.ClienteData obtenerMiPerfil(Long clienteId) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", clienteId.toString()));

        return UpdateClienteResponse.ClienteData.builder()
                .id(cliente.getUsuarioId())
                .nombre(cliente.getClienteNombre())
                .email(cliente.getUsuario().getUsuarioEmail())
                .telefono(cliente.getClienteTelefono())
                .direccion(cliente.getClienteDireccion())
                .puntosActuales(cliente.getClientePuntos())
                .puntosAcumulados(cliente.getClientePuntosAcumulados())
                .aceptaTerminos(cliente.getClienteAceptaTerminos())
                .build();
    }

    /**
     * Actualiza los datos personales del cliente.
     *
     * <p>Valida que:
     * <ul>
     *   <li>El email sea único (no esté registrado en otra cuenta de usuario).</li>
     *   <li>El teléfono sea único (no esté registrado en otro cliente).</li>
     *   <li>El nombre contenga solo letras y espacios.</li>
     *   <li>Los campos requeridos no estén vacíos.</li>
     * </ul>
     *
     * @param clienteId identificador del cliente a actualizar
     * @param request datos actualizados (nombre, email, teléfono, dirección, términos)
     * @return respuesta con los datos actualizados del cliente
     * @throws ResourceNotFoundException si el cliente no existe
     * @throws BusinessException si la validación falla (email/teléfono duplicados, formato inválido, etc.)
     */
    public UpdateClienteResponse actualizarPerfil(Long clienteId, UpdateClienteRequest request) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", clienteId.toString()));

        Usuario usuario = cliente.getUsuario();

        // Validar email duplicado (excluyendo el del usuario actual)
        String emailNormalizado = request.getEmail().toLowerCase(Locale.ENGLISH);
        Optional<Usuario> usuarioExistente = usuarioRepository.findByUsuarioEmail(emailNormalizado);
        if (usuarioExistente.isPresent() && !usuarioExistente.get().getUsuarioId().equals(usuario.getUsuarioId())) {
            throw new BusinessException(
                    ErrorCode.ENTITY_ALREADY_EXISTS,
                    "Este correo electrónico ya está en uso por otro usuario. Por favor utiliza otro",
                    HttpStatus.CONFLICT);
        }

        // Validar teléfono duplicado (excluyendo el del cliente actual)
        if (!request.getTelefono().equals(cliente.getClienteTelefono()) &&
            clienteRepository.existsByClienteTelefono(request.getTelefono())) {
            throw new BusinessException(
                    ErrorCode.ENTITY_ALREADY_EXISTS,
                    "Este número de teléfono ya está en uso por otro usuario. Por favor utiliza otro",
                    HttpStatus.CONFLICT);
        }

        // Actualizar datos del usuario (email)
        usuario.setUsuarioEmail(emailNormalizado);

        // Actualizar datos del cliente
        cliente.setClienteNombre(request.getNombre());
        cliente.setClienteTelefono(request.getTelefono());
        cliente.setClienteDireccion(request.getDireccion());
        cliente.setClienteAceptaTerminos(request.getAceptaTerminos());

        // Persistir cambios
        usuarioRepository.save(usuario);
        cliente = clienteRepository.save(cliente);

        // Construir respuesta
        return UpdateClienteResponse.builder()
                .success(true)
                .message("Datos actualizados correctamente")
                .cliente(UpdateClienteResponse.ClienteData.builder()
                        .id(cliente.getUsuarioId())
                        .nombre(cliente.getClienteNombre())
                        .email(usuario.getUsuarioEmail())
                        .telefono(cliente.getClienteTelefono())
                        .direccion(cliente.getClienteDireccion())
                        .puntosActuales(cliente.getClientePuntos())
                        .puntosAcumulados(cliente.getClientePuntosAcumulados())
                        .aceptaTerminos(cliente.getClienteAceptaTerminos())
                        .build())
                .build();
    }

    /**
     * Cambia la contraseña del usuario autenticado.
     *
     * <p>Valida que:
     * <ul>
     *   <li>La contraseña actual sea correcta.</li>
     *   <li>La nueva contraseña cumpla con los requisitos de fuerza:
     *       mínimo 8 caracteres, al menos 1 mayúscula, 1 minúscula, 1 número, 1 símbolo especial.</li>
     *   <li>La confirmación de la nueva contraseña coincida con la nueva contraseña.</li>
     * </ul>
     *
     * @param clienteId identificador del cliente (usado para obtener su usuario asociado)
     * @param request contraseña actual, nueva y confirmación
     * @return respuesta de éxito con mensaje confirmat ivo
     * @throws ResourceNotFoundException si el cliente no existe
     * @throws BusinessException si la validación falla (contraseña actual incorrecta, no coinciden, no cumple requisitos)
     */
    public ChangePasswordResponse cambiarContraseña(Long clienteId, ChangePasswordRequest request) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", clienteId.toString()));

        Usuario usuario = cliente.getUsuario();

        // 1. Validar que la contraseña actual sea correcta
        if (!passwordEncoder.matches(request.getContraseñaActual(), usuario.getUsuarioPassword())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "La contraseña actual no es correcta",
                    HttpStatus.BAD_REQUEST);
        }

        // 2. Validar que la nueva contraseña y su confirmación coincidan
        if (!request.getNuevaContraseña().equals(request.getConfirmacion())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Las contraseñas no coinciden",
                    HttpStatus.BAD_REQUEST);
        }

        // 3. Validar que la nueva contraseña cumpla con los requisitos de fuerza
        if (!PASSWORD_PATTERN.matcher(request.getNuevaContraseña()).matches()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "La contraseña debe tener al menos 8 caracteres, incluir mayuscula, minuscula y caracter especial",
                    HttpStatus.BAD_REQUEST);
        }

        // 4. Actualizar la contraseña (cifrada)
        usuario.setUsuarioPassword(passwordEncoder.encode(request.getNuevaContraseña()));
        usuarioRepository.save(usuario);

        // 5. Construir respuesta
        return ChangePasswordResponse.builder()
                .success(true)
                .message("Contraseña actualizada correctamente")
                .build();
    }
}
