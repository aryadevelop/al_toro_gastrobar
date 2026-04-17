package co.edu.unicauca.backend.modules.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Perfil del usuario autenticado devuelto al frontend tras login, refresh o {@code /me}.
 *
 * <p>{@code role} es el rol de mayor prioridad del usuario (p. ej. {@code "ADMIN"}).
 * {@code roles} contiene todos los roles activos, para que el frontend habilite
 * los módulos correspondientes a cada uno. Los nombres de rol están en el formato
 * del frontend definido por {@link co.edu.unicauca.backend.shared.security.RoleMapper}.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponse {

    /** Identificador único del usuario como string (para uso en el frontend). */
    private String id;

    /**
     * Nombre de presentación del usuario.
     * Para clientes es {@code clienteNombre}; para empleados, {@code empleadoNombre}.
     * Si no se encontró perfil extendido, contiene el email como fallback.
     */
    private String nombre;

    /** Correo electrónico del usuario; usado como identificador en llamadas subsiguientes. */
    private String email;

    /**
     * Rol principal del usuario en formato frontend (p. ej. {@code "ADMIN"}, {@code "CAJERO"}).
     * Corresponde al rol de mayor prioridad entre los roles activos.
     */
    private String role;

    /**
     * Todos los roles activos del usuario en formato frontend.
     * Ordenados alfabéticamente; puede contener más de un elemento en casos multirol.
     */
    private List<String> roles;

    /**
     * Estado de la cuenta; siempre {@code "ACTIVE"} en respuestas exitosas
     * (las cuentas suspendidas no llegan hasta aquí).
     */
    private String status;

    /** Fecha y hora de creación del registro de usuario en el sistema. */
    private LocalDateTime createdAt;
}