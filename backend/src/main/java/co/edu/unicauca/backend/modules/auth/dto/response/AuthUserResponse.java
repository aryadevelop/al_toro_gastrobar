package co.edu.unicauca.backend.modules.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Representación del usuario autenticado que consume el frontend.
 *
 * <p>{@code role} es el rol activo principal (el de mayor prioridad).
 * {@code roles} contiene todos los roles activos del usuario, para que
 * el frontend pueda mostrar los módulos correspondientes a cada uno.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponse {

    private String id;
    private String fullName;
    private String email;
    private String phone;
    /** Rol activo principal (mayor prioridad). */
    private String role;
    /** Todos los roles activos del usuario. */
    private List<String> roles;
    private String status;
    private String avatarUrl;
    private LocalDateTime createdAt;
}