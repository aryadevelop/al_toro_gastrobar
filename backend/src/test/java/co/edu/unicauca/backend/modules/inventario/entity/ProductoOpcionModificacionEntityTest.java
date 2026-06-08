package co.edu.unicauca.backend.modules.inventario.entity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ProductoOpcionModificacionEntityTest {

    @Test
    void builder_creaInstanciaVacia() {
        ProductoOpcionModificacion p = ProductoOpcionModificacion.builder().build();
        assertThat(p).isNotNull();
    }

    @Test
    void onCreate_setsCreatedAtSiEsNull() {
        ProductoOpcionModificacion p = new ProductoOpcionModificacion();
        p.onCreate();
        assertThat(p.getCreatedAt()).isNotNull();
    }

    @Test
    void idClass_equalsYHashCode() {
        ProductoOpcionModificacion.ProductoOpcionModificacionId id1 =
                new ProductoOpcionModificacion.ProductoOpcionModificacionId(1L, 2L);
        ProductoOpcionModificacion.ProductoOpcionModificacionId id2 =
                new ProductoOpcionModificacion.ProductoOpcionModificacionId(1L, 2L);
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }
}
