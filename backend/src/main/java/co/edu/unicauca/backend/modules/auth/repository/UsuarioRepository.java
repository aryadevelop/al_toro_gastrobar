package co.edu.unicauca.backend.modules.auth.repository;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsuarioEmail(String email);
}
