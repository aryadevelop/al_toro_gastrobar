package co.edu.unicauca.backend.modules.auth.repository;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link Usuario}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y la búsqueda por
 * correo electrónico usada durante la autenticación.
 *
 * @see Usuario
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca un usuario por su dirección de correo electrónico.
     *
     * @param email correo electrónico del usuario (normalizado a minúsculas)
     * @return usuario con el correo indicado, o vacío si no existe
     */
    Optional<Usuario> findByUsuarioEmail(String email);
}
