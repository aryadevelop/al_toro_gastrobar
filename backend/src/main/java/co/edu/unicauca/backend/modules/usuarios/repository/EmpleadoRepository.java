package co.edu.unicauca.backend.modules.usuarios.repository;

import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link Empleado}.
 *
 * <p>Provee las operaciones CRUD heredadas de {@code JpaRepository} y la búsqueda por
 * correo del usuario asociado, usada para identificar al empleado autenticado en los
 * flujos de caja y puntos de fidelización.
 *
 * @see Empleado
 */
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    /**
     * Busca un empleado por el correo electrónico de su {@link co.edu.unicauca.backend.modules.auth.entity.Usuario} asociado.
     *
     * @param email correo electrónico del usuario
     * @return empleado asociado al correo indicado, o vacío si no existe
     */
    Optional<Empleado> findByUsuario_UsuarioEmail(String email);

    /**
     * Verifica si existe un empleado registrado con el teléfono indicado.
     *
     * @param telefono teléfono a verificar
     * @return {@code true} si el teléfono ya está registrado; {@code false} en caso contrario
     */
    boolean existsByEmpleadoTelefono(String telefono);
}