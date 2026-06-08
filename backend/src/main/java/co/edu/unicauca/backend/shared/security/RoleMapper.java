package co.edu.unicauca.backend.shared.security;

import co.edu.unicauca.backend.shared.enums.RolNombre;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

/**
 * Convierte los {@link RolNombre} persistidos en la base de datos a las cadenas de rol
 * que usa la API REST y el frontend Angular.
 *
 * <p>{@code COCINERO} y {@code BARTENDER} se unifican bajo el rol funcional {@code PRODUCCION}
 * porque comparten los mismos permisos de vista en el frontend. La prioridad de roles
 * determina el rol principal cuando un usuario tiene varios activos.
 */
public final class RoleMapper {

    private static final Map<RolNombre, String> FRONTEND_ROLE_NAMES = new EnumMap<>(RolNombre.class);
    private static final Map<RolNombre, Integer> ROLE_PRIORITY = new EnumMap<>(RolNombre.class);

    static {
        FRONTEND_ROLE_NAMES.put(RolNombre.ADMIN, "ADMIN");
        FRONTEND_ROLE_NAMES.put(RolNombre.CAJERO, "CAJERO");
        FRONTEND_ROLE_NAMES.put(RolNombre.MESERO, "MESERO");
        FRONTEND_ROLE_NAMES.put(RolNombre.COCINERO, "PRODUCCION");
        FRONTEND_ROLE_NAMES.put(RolNombre.BARTENDER, "PRODUCCION");
        FRONTEND_ROLE_NAMES.put(RolNombre.CLIENTE, "CLIENTE");

        ROLE_PRIORITY.put(RolNombre.ADMIN, 1);
        ROLE_PRIORITY.put(RolNombre.CAJERO, 2);
        ROLE_PRIORITY.put(RolNombre.MESERO, 3);
        ROLE_PRIORITY.put(RolNombre.COCINERO, 4);
        ROLE_PRIORITY.put(RolNombre.BARTENDER, 4);
        ROLE_PRIORITY.put(RolNombre.CLIENTE, 5);
    }

    private RoleMapper() {
    }

    /**
     * Devuelve el nombre de rol funcional para el frontend correspondiente al rol de base de datos.
     *
     * @param rolNombre rol persistido en la base de datos
     * @return cadena de rol para el frontend; si no hay mapeo definido, devuelve el nombre del enum
     */
    public static String toFrontendRole(RolNombre rolNombre) {
        return FRONTEND_ROLE_NAMES.getOrDefault(rolNombre, rolNombre.name());
    }

    /**
     * Devuelve la authority de Spring Security con el prefijo {@code ROLE_} para el rol indicado.
     *
     * @param rolNombre rol persistido en la base de datos
     * @return cadena de authority con formato {@code ROLE_<rolFrontend>}
     */
    public static String toAuthority(RolNombre rolNombre) {
        return "ROLE_" + toFrontendRole(rolNombre);
    }

    /**
     * Devuelve un {@link Comparator} que ordena roles por prioridad ascendente.
     *
     * <p>Los roles con mayor prioridad (número menor) aparecen primero; los roles
     * sin mapeo de prioridad se ubican al final.
     *
     * @return comparador de roles por prioridad
     */
    public static Comparator<RolNombre> priorityComparator() {
        return Comparator.comparingInt(role -> ROLE_PRIORITY.getOrDefault(role, Integer.MAX_VALUE));
    }
}