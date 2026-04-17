package co.edu.unicauca.backend.shared.enums;

/**
 * Roles funcionales del sistema, persistidos en la tabla {@code usuario_rol}.
 *
 * <p>{@code COCINERO} y {@code BARTENDER} se mapean al rol de frontend {@code PRODUCCION}
 * a través de {@link co.edu.unicauca.backend.shared.security.RoleMapper}.
 *
 * <ul>
 *   <li>{@code CLIENTE} — usuario registrado que realiza reservas y consulta su historial.</li>
 *   <li>{@code MESERO} — empleado de sala; gestiona mesas, visitas y comandas.</li>
 *   <li>{@code CAJERO} — empleado de caja; cierra cuentas y gestiona pagos.</li>
 *   <li>{@code COCINERO} — empleado de cocina; atiende comandas de la estación COCINA.</li>
 *   <li>{@code BARTENDER} — empleado de barra; atiende comandas de la estación BARRA.</li>
 *   <li>{@code ADM} — administrador con acceso completo al sistema.</li>
 * </ul>
 */
public enum RolNombre {
    CLIENTE("Cliente"),
    MESERO("Mesero"),
    CAJERO("Cajero"),
    COCINERO("Cocinero"),
    BARTENDER("Bartender"),
    ADM("Administrador");

    private final String descripcion;

    RolNombre(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
