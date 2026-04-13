package co.edu.unicauca.backend.modules.auth.repository;

import co.edu.unicauca.backend.modules.auth.entity.Sesion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SesionRepository extends JpaRepository<Sesion, Long> {
    List<Sesion> findByUsuarioUsuarioIdAndSesionActivaTrue(Long usuarioId);

    Optional<Sesion> findBySesionTokenAndSesionActivaTrue(String sesionToken);
}