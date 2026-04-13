package co.edu.unicauca.backend.modules.usuarios.repository;

import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByUsuario_UsuarioEmail(String email);
}
