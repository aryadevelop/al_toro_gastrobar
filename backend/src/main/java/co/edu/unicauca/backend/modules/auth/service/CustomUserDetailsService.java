package co.edu.unicauca.backend.modules.auth.service;

import co.edu.unicauca.backend.modules.auth.repository.UsuarioRepository;
import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRolRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.UsuarioRol;
import co.edu.unicauca.backend.shared.enums.RolEstado;
import co.edu.unicauca.backend.shared.security.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación de {@link UserDetailsService} que carga los datos de autenticación
 * de un usuario desde la base de datos.
 *
 * <p>Spring Security invoca {@link #loadUserByUsername} durante el proceso de login
 * para construir el objeto {@link UserDetails} con correo, contraseña y autoridades.
 * Solo los roles con estado {@code ACTIVO} se incluyen como authorities.
 * Si el usuario no existe o no tiene roles activos, lanza {@link UsernameNotFoundException}.
 *
 * @see co.edu.unicauca.backend.shared.security.RoleMapper
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;

    /**
     * Carga los datos de autenticación del usuario identificado por su correo electrónico.
     *
     * @param username correo electrónico del usuario
     * @return {@link UserDetails} con correo, contraseña hasheada y autoridades de roles activos
     * @throws UsernameNotFoundException si el usuario no existe o no tiene roles activos
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsuarioEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + username));

        List<UsuarioRol> rolesActivos = usuarioRolRepository.findByUsuarioIdAndRolEstado(usuario.getUsuarioId(), RolEstado.ACTIVO);
        if (rolesActivos.isEmpty()) {
            throw new UsernameNotFoundException("Usuario sin roles activos con email: " + username);
        }

        List<SimpleGrantedAuthority> authorities = rolesActivos.stream()
            .map(UsuarioRol::getRolNombre)
            .map(RoleMapper::toAuthority)
            .distinct()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());

        return User.builder()
                .username(usuario.getUsuarioEmail())
                .password(usuario.getUsuarioPassword())
            .authorities(authorities)
                .build();
    }
}
