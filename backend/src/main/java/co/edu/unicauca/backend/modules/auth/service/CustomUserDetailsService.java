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

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;

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
