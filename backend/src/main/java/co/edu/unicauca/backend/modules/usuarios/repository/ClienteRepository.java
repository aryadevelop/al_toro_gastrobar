package co.edu.unicauca.backend.modules.usuarios.repository;

import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * Verifica si existe un cliente registrado con el teléfono indicado.
     *
     * @param telefono número de teléfono a verificar
     * @return {@code true} si el teléfono ya está registrado; {@code false} en caso contrario
     */
    boolean existsByClienteTelefono(String telefono);

    /**
     * Busca clientes cuyo correo de usuario contiene el fragmento indicado,
     * ignorando mayúsculas/minúsculas. Usado por la búsqueda del cajero al
     * relacionar una cuenta con un cliente (coincidencia parcial por email).
     *
     * @param fragmento porción del correo a buscar (ya normalizada: trim + minúsculas)
     * @return clientes coincidentes; lista vacía si ninguno coincide
     */
    List<Cliente> findByUsuario_UsuarioEmailContainingIgnoreCase(String fragmento);

    /**
     * Adquiere un bloqueo de escritura pesimista sobre la fila del cliente indicado.
     * Serializa el incremento de puntos del cierre de venta con el canje de puntos
     * para evitar actualizaciones perdidas sobre {@code clientePuntos}.
     *
     * @param id identificador (usuarioId) del cliente
     * @return el cliente bloqueado, o {@link Optional#empty()} si no existe
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cliente c WHERE c.usuarioId = :id")
    Optional<Cliente> findByIdForUpdate(@Param("id") Long id);
}
