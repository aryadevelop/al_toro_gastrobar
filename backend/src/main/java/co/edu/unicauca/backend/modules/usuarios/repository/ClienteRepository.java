package co.edu.unicauca.backend.modules.usuarios.repository;

import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link Cliente}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y la búsqueda por
 * correo del usuario asociado, usada para identificar al cliente autenticado en los
 * flujos de reservas y puntos de fidelización.
 *
 * @see Cliente
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Busca un cliente por el correo electrónico de su {@link co.edu.unicauca.backend.modules.auth.entity.Usuario} asociado.
     *
     * @param email correo electrónico del usuario
     * @return cliente asociado al correo indicado, o vacío si no existe
     */
    Optional<Cliente> findByUsuario_UsuarioEmail(String email);
}
