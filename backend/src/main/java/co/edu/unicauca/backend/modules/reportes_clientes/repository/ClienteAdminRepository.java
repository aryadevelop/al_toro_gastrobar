package co.edu.unicauca.backend.modules.reportes_clientes.repository;

import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio de busqueda de clientes para reportes administrativos.
 */
public interface ClienteAdminRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findByClienteNombreContainingIgnoreCase(String nombre);

    List<Cliente> findByUsuario_UsuarioEmailContainingIgnoreCase(String email);

    List<Cliente> findByClienteTelefonoContaining(String telefono);
}
