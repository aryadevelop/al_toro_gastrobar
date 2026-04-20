package co.edu.unicauca.backend.modules.usuarios.entity;

import co.edu.unicauca.backend.shared.enums.RolEstado;
import co.edu.unicauca.backend.shared.enums.RolNombre;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UsuarioRolEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        UsuarioRol r = UsuarioRol.builder()
                .usuarioId(1L)
                .rolNombre(RolNombre.CAJERO)
                .rolEstado(RolEstado.ACTIVO)
                .build();

        assertThat(r.getUsuarioId()).isEqualTo(1L);
        assertThat(r.getRolNombre()).isEqualTo(RolNombre.CAJERO);
        assertThat(r.getRolEstado()).isEqualTo(RolEstado.ACTIVO);
    }

    @Test
    void setter_actualizaEstado() {
        UsuarioRol r = new UsuarioRol();
        r.setRolEstado(RolEstado.INACTIVO);
        assertThat(r.getRolEstado()).isEqualTo(RolEstado.INACTIVO);
    }

    @Test
    void equals_mismoIdYRol_retornaTrue() {
        UsuarioRol a = UsuarioRol.builder().usuarioId(1L).rolNombre(RolNombre.CLIENTE).build();
        UsuarioRol b = UsuarioRol.builder().usuarioId(1L).rolNombre(RolNombre.CLIENTE).build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_diferenteRol_retornaFalse() {
        UsuarioRol a = UsuarioRol.builder().usuarioId(1L).rolNombre(RolNombre.CLIENTE).build();
        UsuarioRol b = UsuarioRol.builder().usuarioId(1L).rolNombre(RolNombre.CAJERO).build();
        assertThat(a).isNotEqualTo(b);
    }
}
