package co.edu.unicauca.backend.shared.bootstrap;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRepository;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRolRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.UsuarioRol;
import co.edu.unicauca.backend.shared.enums.RolEstado;
import co.edu.unicauca.backend.shared.enums.RolNombre;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * Crea el usuario administrador inicial en producción a partir de variables de
 * entorno, únicamente si todavía no existe ningún usuario con rol ADMIN activo.
 * Permite arrancar una instancia productiva sin sembrar credenciales en el
 * repositorio. La operación es idempotente: reinicios sucesivos no la repiten.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdAdminBootstrap implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bootstrap.admin.email:}")
    private String adminEmail;

    @Value("${bootstrap.admin.password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
            return;
        }
        if (usuarioRolRepository.existsByRolNombreAndRolEstado(RolNombre.ADMIN, RolEstado.ACTIVO)) {
            return;
        }

        String email = adminEmail.toLowerCase(Locale.ENGLISH);
        Usuario usuario = usuarioRepository.findByUsuarioEmail(email)
                .orElseGet(() -> usuarioRepository.save(Usuario.builder()
                        .usuarioEmail(email)
                        .usuarioPassword(passwordEncoder.encode(adminPassword))
                        .build()));

        usuarioRolRepository.save(UsuarioRol.builder()
                .usuarioId(usuario.getUsuarioId())
                .rolNombre(RolNombre.ADMIN)
                .rolEstado(RolEstado.ACTIVO)
                .usuario(usuario)
                .build());
    }
}
