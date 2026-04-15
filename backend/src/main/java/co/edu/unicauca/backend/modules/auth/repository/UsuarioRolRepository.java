package co.edu.unicauca.backend.modules.auth.repository;

import co.edu.unicauca.backend.modules.usuarios.entity.UsuarioRol;
import co.edu.unicauca.backend.shared.enums.RolEstado;
import co.edu.unicauca.backend.shared.enums.RolNombre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, UsuarioRol.UsuarioRolId> {
    List<UsuarioRol> findByUsuarioIdAndRolEstado(Long usuarioId, RolEstado rolEstado);
    boolean existsByUsuarioIdAndRolNombreAndRolEstado(Long usuarioId, RolNombre rolNombre, RolEstado rolEstado);
}