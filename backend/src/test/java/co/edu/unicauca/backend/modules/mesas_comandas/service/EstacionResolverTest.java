package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRepository;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRolRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.UsuarioRol;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.RolEstado;
import co.edu.unicauca.backend.shared.enums.RolNombre;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstacionResolver")
class EstacionResolverTest {

    private static final String EMAIL = "user@altoro.com";
    private static final Long USUARIO_ID = 42L;

    @Mock UsuarioRepository usuarioRepository;
    @Mock UsuarioRolRepository usuarioRolRepository;
    @Mock Authentication auth;

    @InjectMocks EstacionResolver estacionResolver;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder()
                .usuarioId(USUARIO_ID)
                .usuarioEmail(EMAIL)
                .usuarioPassword("hash")
                .build();
        lenient().when(usuarioRepository.findByUsuarioEmail(EMAIL)).thenReturn(Optional.of(usuario));
    }

    private UsuarioRol rol(RolNombre rolNombre) {
        return UsuarioRol.builder()
                .usuarioId(USUARIO_ID)
                .rolNombre(rolNombre)
                .rolEstado(RolEstado.ACTIVO)
                .build();
    }

    private void mockAuthName() {
        when(auth.getName()).thenReturn(EMAIL);
    }

    @Test
    @DisplayName("COCINERO → {COCINA}")
    void cocinero_devuelveCocina() {
        mockAuthName();
        when(usuarioRolRepository.findByUsuarioIdAndRolEstado(USUARIO_ID, RolEstado.ACTIVO))
                .thenReturn(List.of(rol(RolNombre.COCINERO)));

        Set<EstacionComanda> resultado = estacionResolver.resolverEstaciones(auth);

        assertThat(resultado).containsExactly(EstacionComanda.COCINA);
    }

    @Test
    @DisplayName("BARTENDER → {BARRA}")
    void bartender_devuelveBarra() {
        mockAuthName();
        when(usuarioRolRepository.findByUsuarioIdAndRolEstado(USUARIO_ID, RolEstado.ACTIVO))
                .thenReturn(List.of(rol(RolNombre.BARTENDER)));

        Set<EstacionComanda> resultado = estacionResolver.resolverEstaciones(auth);

        assertThat(resultado).containsExactly(EstacionComanda.BARRA);
    }

    @Test
    @DisplayName("COCINERO + BARTENDER → {COCINA, BARRA} (tablero combinado)")
    void ambos_devuelveAmbas() {
        mockAuthName();
        when(usuarioRolRepository.findByUsuarioIdAndRolEstado(USUARIO_ID, RolEstado.ACTIVO))
                .thenReturn(List.of(rol(RolNombre.COCINERO), rol(RolNombre.BARTENDER)));

        Set<EstacionComanda> resultado = estacionResolver.resolverEstaciones(auth);

        assertThat(resultado).containsExactlyInAnyOrder(
                EstacionComanda.COCINA, EstacionComanda.BARRA);
    }

    @Test
    @DisplayName("ADMIN → BusinessException AUTH-002 / 403")
    void admin_rechazado() {
        mockAuthName();
        when(usuarioRolRepository.findByUsuarioIdAndRolEstado(USUARIO_ID, RolEstado.ACTIVO))
                .thenReturn(List.of(rol(RolNombre.ADMIN)));

        assertThatThrownBy(() -> estacionResolver.resolverEstaciones(auth))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(be.getCode()).isEqualTo("AUTH-002");
                });
    }

    @Test
    @DisplayName("MESERO → BusinessException AUTH-002 / 403")
    void mesero_rechazado() {
        mockAuthName();
        when(usuarioRolRepository.findByUsuarioIdAndRolEstado(USUARIO_ID, RolEstado.ACTIVO))
                .thenReturn(List.of(rol(RolNombre.MESERO)));

        assertThatThrownBy(() -> estacionResolver.resolverEstaciones(auth))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("usuario sin email registrado → BusinessException 403")
    void usuarioInexistente_rechazado() {
        when(auth.getName()).thenReturn("desconocido@altoro.com");
        when(usuarioRepository.findByUsuarioEmail("desconocido@altoro.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> estacionResolver.resolverEstaciones(auth))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("estacionesDeUsuarioEmail: variante silenciosa devuelve vacío si no aplica")
    void variantesilenciosa_sinRolesDeProduccion() {
        when(usuarioRolRepository.findByUsuarioIdAndRolEstado(USUARIO_ID, RolEstado.ACTIVO))
                .thenReturn(List.of(rol(RolNombre.MESERO)));

        Set<EstacionComanda> resultado = estacionResolver.estacionesDeUsuarioEmail(EMAIL);

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("estacionesDeUsuarioEmail: usuario inexistente → conjunto vacío sin error")
    void variantesilenciosa_usuarioInexistente() {
        when(usuarioRepository.findByUsuarioEmail("noone@altoro.com")).thenReturn(Optional.empty());

        Set<EstacionComanda> resultado = estacionResolver.estacionesDeUsuarioEmail("noone@altoro.com");

        assertThat(resultado).isEmpty();
    }
}
