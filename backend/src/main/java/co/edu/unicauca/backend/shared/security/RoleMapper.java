package co.edu.unicauca.backend.shared.security;

import co.edu.unicauca.backend.shared.enums.RolNombre;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

/**
 * Mapea los roles persistidos en backend hacia los roles funcionales usados por la API y el frontend.
 */
public final class RoleMapper {

    private static final Map<RolNombre, String> FRONTEND_ROLE_NAMES = new EnumMap<>(RolNombre.class);
    private static final Map<RolNombre, Integer> ROLE_PRIORITY = new EnumMap<>(RolNombre.class);

    static {
        FRONTEND_ROLE_NAMES.put(RolNombre.ADM, "ADMIN");
        FRONTEND_ROLE_NAMES.put(RolNombre.CAJERO, "CAJERO");
        FRONTEND_ROLE_NAMES.put(RolNombre.MESERO, "MESERO");
        FRONTEND_ROLE_NAMES.put(RolNombre.COCINERO, "PRODUCCION");
        FRONTEND_ROLE_NAMES.put(RolNombre.BARTENDER, "PRODUCCION");
        FRONTEND_ROLE_NAMES.put(RolNombre.CLIENTE, "CLIENTE");

        ROLE_PRIORITY.put(RolNombre.ADM, 1);
        ROLE_PRIORITY.put(RolNombre.CAJERO, 2);
        ROLE_PRIORITY.put(RolNombre.MESERO, 3);
        ROLE_PRIORITY.put(RolNombre.COCINERO, 4);
        ROLE_PRIORITY.put(RolNombre.BARTENDER, 4);
        ROLE_PRIORITY.put(RolNombre.CLIENTE, 5);
    }

    private RoleMapper() {
    }

    public static String toFrontendRole(RolNombre rolNombre) {
        return FRONTEND_ROLE_NAMES.getOrDefault(rolNombre, rolNombre.name());
    }

    public static String toAuthority(RolNombre rolNombre) {
        return "ROLE_" + toFrontendRole(rolNombre);
    }

    public static Comparator<RolNombre> priorityComparator() {
        return Comparator.comparingInt(role -> ROLE_PRIORITY.getOrDefault(role, Integer.MAX_VALUE));
    }
}