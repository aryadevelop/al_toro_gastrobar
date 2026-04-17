package co.edu.unicauca.backend.modules.auth.repository;

import co.edu.unicauca.backend.modules.usuarios.entity.UsuarioRol;
import co.edu.unicauca.backend.shared.enums.RolEstado;
import co.edu.unicauca.backend.shared.enums.RolNombre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de acceso a datos para la entidad {@link UsuarioRol}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y consultas
 * para filtrar los roles de un usuario, usadas durante la autenticación y la
 * verificación de permisos.
 *
 * @see UsuarioRol
 */
public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, UsuarioRol.UsuarioRolId> {

    /**
     * Devuelve todos los roles de un usuario con el estado indicado.
     *
     * @param usuarioId identificador del usuario
     * @param rolEstado estado del rol ({@code ACTIVO} o {@code INACTIVO})
     * @return lista de roles del usuario con el estado indicado; vacía si no tiene ninguno
     */
    List<UsuarioRol> findByUsuarioIdAndRolEstado(Long usuarioId, RolEstado rolEstado);

    /**
     * Verifica si un usuario tiene un rol específico con el estado indicado.
     *
     * @param usuarioId  identificador del usuario
     * @param rolNombre  nombre del rol a verificar
     * @param rolEstado  estado requerido del rol
     * @return {@code true} si el usuario tiene el rol con el estado indicado; {@code false} en caso contrario
     */
    boolean existsByUsuarioIdAndRolNombreAndRolEstado(Long usuarioId, RolNombre rolNombre, RolEstado rolEstado);
}