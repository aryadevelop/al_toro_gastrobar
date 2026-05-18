package co.edu.unicauca.backend.modules.auth.repository;

import co.edu.unicauca.backend.modules.auth.entity.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link Sesion}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y consultas
 * personalizadas para gestionar sesiones activas durante los flujos de autenticación,
 * renovación de token y cierre de sesión.
 *
 * @see Sesion
 */
public interface SesionRepository extends JpaRepository<Sesion, Long> {

    /**
     * Devuelve todas las sesiones activas de un usuario.
     *
     * @param usuarioId identificador del usuario
     * @return lista de sesiones activas del usuario; vacía si no tiene ninguna
     */
    List<Sesion> findByUsuarioUsuarioIdAndSesionActivaTrue(Long usuarioId);

    /**
     * Desactiva todas las sesiones activas de un usuario.
     *
     * @param usuarioId identificador del usuario
     * @return número de filas actualizadas
     */
    @Modifying
    @Query("UPDATE Sesion s SET s.sesionActiva = false WHERE s.usuario.usuarioId = :usuarioId AND s.sesionActiva = true")
    int deactivateActiveSessionsByUsuarioId(Long usuarioId);

    /**
     * Busca una sesión activa por su access token.
     *
     * @param sesionToken valor del access token JWT
     * @return sesión activa con el token indicado, o vacío si no existe o está inactiva
     */
    Optional<Sesion> findBySesionTokenAndSesionActivaTrue(String sesionToken);

    /**
     * Busca una sesión activa por su refresh token.
     *
     * @param sesionRefreshToken valor del refresh token
     * @return sesión activa con el refresh token indicado, o vacío si no existe o está inactiva
     */
    Optional<Sesion> findBySesionRefreshTokenAndSesionActivaTrue(String sesionRefreshToken);
}