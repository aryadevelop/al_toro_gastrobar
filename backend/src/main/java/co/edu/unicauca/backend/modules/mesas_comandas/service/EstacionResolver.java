package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRepository;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRolRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.UsuarioRol;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.RolEstado;
import co.edu.unicauca.backend.shared.enums.RolNombre;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Resuelve el conjunto de estaciones de producción a las que un usuario tiene
 * acceso a partir de sus roles activos.
 *
 * <p>Aplica el siguiente mapeo entre roles persistidos y estaciones funcionales:
 * <ul>
 *   <li>{@code COCINERO} habilita {@link EstacionComanda#COCINA}.</li>
 *   <li>{@code BARTENDER} habilita {@link EstacionComanda#BARRA}.</li>
 *   <li>Un usuario con ambos roles activos accede a las dos estaciones.</li>
 *   <li>Cualquier otro perfil queda fuera del dominio de producción.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class EstacionResolver {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioRolRepository usuarioRolRepository;

    /**
     * Determina las estaciones del usuario autenticado y exige que el resultado
     * sea no vacío; en caso contrario el acceso se considera denegado.
     *
     * @param auth contexto de autenticación de Spring Security; el atributo
     *             {@code name} corresponde al email del usuario
     * @return conjunto inmutable de estaciones del usuario; siempre con al
     *         menos un elemento
     * @throws BusinessException con código {@code AUTH-002} y estado
     *                           {@code 403 FORBIDDEN} cuando el usuario no
     *                           existe o no tiene rol de producción
     */
    public Set<EstacionComanda> resolverEstaciones(Authentication auth) {
        // El nombre del Authentication corresponde al email del usuario en Spring Security
        Set<EstacionComanda> estaciones = estacionesDeUsuarioEmail(auth.getName());

        // Un usuario sin rol de producción se trata como acceso denegado
        if (estaciones.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.ACCESS_DENIED,
                    "El usuario no tiene un rol de producción asignado.",
                    HttpStatus.FORBIDDEN);
        }
        return estaciones;
    }

    /**
     * Devuelve el conjunto de estaciones del usuario sin lanzar excepción 
     * cuando el resultado es vacío.
     *
     * @param email correo electrónico del usuario
     * @return conjunto inmutable de estaciones del usuario; vacío cuando el
     *         usuario no existe o no tiene rol de producción
     */
    public Set<EstacionComanda> estacionesDeUsuarioEmail(String email) {
        // Un email desconocido no debe propagar excepciones para no romper el flujo de login
        Optional<Usuario> usuario = usuarioRepository.findByUsuarioEmail(email);
        if (usuario.isEmpty()) {
            return Set.of();
        }
        
        // Solo los roles ACTIVO conceden acceso al tablero
        List<UsuarioRol> activos = usuarioRolRepository.findByUsuarioIdAndRolEstado(
                usuario.get().getUsuarioId(), RolEstado.ACTIVO);

        // Mapea cada rol de producción a su estación correspondiente; los demás roles se ignoran
        EnumSet<EstacionComanda> resultado = EnumSet.noneOf(EstacionComanda.class);
        for (UsuarioRol ur : activos) {
            if (ur.getRolNombre() == RolNombre.COCINERO) {
                resultado.add(EstacionComanda.COCINA);
            } else if (ur.getRolNombre() == RolNombre.BARTENDER) {
                resultado.add(EstacionComanda.BARRA);
            }
        }
        return resultado;
    }
}
