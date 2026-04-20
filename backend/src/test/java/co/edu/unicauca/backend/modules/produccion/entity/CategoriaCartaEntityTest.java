package co.edu.unicauca.backend.modules.produccion.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CategoriaCartaEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        CategoriaCarta c = CategoriaCarta.builder()
                .categoriacartaId(1)
                .categoriaNombre("Entradas")
                .orden(1)
                .activo(true)
                .build();

        assertThat(c.getCategoriacartaId()).isEqualTo(1);
        assertThat(c.getCategoriaNombre()).isEqualTo("Entradas");
        assertThat(c.getOrden()).isEqualTo(1);
        assertThat(c.getActivo()).isTrue();
    }

    @Test
    void setter_actualizaNombre() {
        CategoriaCarta c = new CategoriaCarta();
        c.setCategoriaNombre("Postres");
        assertThat(c.getCategoriaNombre()).isEqualTo("Postres");
    }

    @Test
    void equals_mismoId_retornaTrue() {
        CategoriaCarta a = CategoriaCarta.builder().categoriacartaId(1).build();
        CategoriaCarta b = CategoriaCarta.builder().categoriacartaId(1).build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_diferenteId_retornaFalse() {
        CategoriaCarta a = CategoriaCarta.builder().categoriacartaId(1).build();
        CategoriaCarta b = CategoriaCarta.builder().categoriacartaId(2).build();
        assertThat(a).isNotEqualTo(b);
    }
}
