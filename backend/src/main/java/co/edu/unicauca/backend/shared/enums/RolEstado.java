package co.edu.unicauca.backend.shared.enums;

/**
 * Estado de la asignación de un rol a un usuario en {@link co.edu.unicauca.backend.modules.usuarios.entity.UsuarioRol}.
 *
 * <ul>
 *   <li>{@code ACTIVO} — el rol está vigente; el usuario tiene los permisos asociados.</li>
 *   <li>{@code INACTIVO} — el rol fue revocado; el usuario no tiene acceso a las funciones del rol.</li>
 * </ul>
 */
public enum RolEstado {
    ACTIVO("Activo"),
    INACTIVO("Inactivo");

    private final String descripcion;

    RolEstado(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
