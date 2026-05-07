package co.edu.unicauca.backend.modules.inventario.entity;

import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.TipoComponenteMenu;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OpcionModificacionEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        OpcionModificacion o = OpcionModificacion.builder()
                .opcionId(1L)
                .tipoComponente(TipoComponenteMenu.SALSA_PROTEINA_1)
                .opcionNombre("Pollo")
                .opcionEstado(EstadoGenerico.ACTIVO)
                .build();

        assertThat(o.getOpcionId()).isEqualTo(1L);
        assertThat(o.getTipoComponente()).isEqualTo(TipoComponenteMenu.SALSA_PROTEINA_1);
        assertThat(o.getOpcionNombre()).isEqualTo("Pollo");
        assertThat(o.getOpcionEstado()).isEqualTo(EstadoGenerico.ACTIVO);
    }

    @Test
    void setter_actualizaNombre() {
        OpcionModificacion o = new OpcionModificacion();
        o.setOpcionNombre("Carne");
        assertThat(o.getOpcionNombre()).isEqualTo("Carne");
    }

    @Test
    void equals_mismoId_retornaTrue() {
        OpcionModificacion a = OpcionModificacion.builder().opcionId(1L).build();
        OpcionModificacion b = OpcionModificacion.builder().opcionId(1L).build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_diferenteId_retornaFalse() {
        OpcionModificacion a = OpcionModificacion.builder().opcionId(1L).build();
        OpcionModificacion b = OpcionModificacion.builder().opcionId(2L).build();
        assertThat(a).isNotEqualTo(b);
    }
}
