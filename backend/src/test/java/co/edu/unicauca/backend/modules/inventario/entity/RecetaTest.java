package co.edu.unicauca.backend.modules.inventario.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Receta Entity Tests")
class RecetaTest {

    @Test
    @DisplayName("equals → mismos IDs (insumoId y productoId) → retorna true")
    void equals_mismosIds_retornaTrue() {
        Receta entity1 = Receta.builder()
                .insumoId(1L)
                .productoId(2L)
                .recetaCantidad(BigDecimal.valueOf(10.5))
                .build();
        Receta entity2 = Receta.builder()
                .insumoId(1L)
                .productoId(2L)
                .recetaCantidad(BigDecimal.valueOf(20.0))
                .build();

        assertThat(entity1).isEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → diferente insumoId → retorna false")
    void equals_diferenteInsumoId_retornaFalse() {
        Receta entity1 = Receta.builder()
                .insumoId(1L)
                .productoId(2L)
                .recetaCantidad(BigDecimal.valueOf(10.5))
                .build();
        Receta entity2 = Receta.builder()
                .insumoId(3L)
                .productoId(2L)
                .recetaCantidad(BigDecimal.valueOf(10.5))
                .build();

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → diferente productoId → retorna false")
    void equals_diferenteProductoId_retornaFalse() {
        Receta entity1 = Receta.builder()
                .insumoId(1L)
                .productoId(2L)
                .recetaCantidad(BigDecimal.valueOf(10.5))
                .build();
        Receta entity2 = Receta.builder()
                .insumoId(1L)
                .productoId(4L)
                .recetaCantidad(BigDecimal.valueOf(10.5))
                .build();

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → ambos IDs diferentes → retorna false")
    void equals_ambosIdsDiferentes_retornaFalse() {
        Receta entity1 = Receta.builder()
                .insumoId(1L)
                .productoId(2L)
                .recetaCantidad(BigDecimal.valueOf(10.5))
                .build();
        Receta entity2 = Receta.builder()
                .insumoId(3L)
                .productoId(4L)
                .recetaCantidad(BigDecimal.valueOf(10.5))
                .build();

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → insumoId null → retorna false")
    void equals_insumoIdNull_retornaFalse() {
        Receta entity1 = Receta.builder()
                .insumoId(null)
                .productoId(2L)
                .recetaCantidad(BigDecimal.valueOf(10.5))
                .build();
        Receta entity2 = Receta.builder()
                .insumoId(1L)
                .productoId(2L)
                .recetaCantidad(BigDecimal.valueOf(10.5))
                .build();

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → productoId null → retorna false")
    void equals_productoIdNull_retornaFalse() {
        Receta entity1 = Receta.builder()
                .insumoId(1L)
                .productoId(null)
                .recetaCantidad(BigDecimal.valueOf(10.5))
                .build();
        Receta entity2 = Receta.builder()
                .insumoId(1L)
                .productoId(2L)
                .recetaCantidad(BigDecimal.valueOf(10.5))
                .build();

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → ambos IDs null → retorna false")
    void equals_ambosIdsNull_retornaFalse() {
        Receta entity1 = Receta.builder()
                .insumoId(null)
                .productoId(null)
                .recetaCantidad(BigDecimal.valueOf(10.5))
                .build();
        Receta entity2 = Receta.builder()
                .insumoId(1L)
                .productoId(2L)
                .recetaCantidad(BigDecimal.valueOf(10.5))
                .build();

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → misma instancia → retorna true")
    void equals_mismaInstancia_retornaTrue() {
        Receta entity = Receta.builder()
                .insumoId(1L)
                .productoId(2L)
                .recetaCantidad(BigDecimal.valueOf(10.5))
                .build();

        assertThat(entity).isEqualTo(entity);
    }

    @Test
    @DisplayName("equals → tipo diferente → retorna false")
    void equals_tipoDiferente_retornaFalse() {
        Receta entity = Receta.builder()
                .insumoId(1L)
                .productoId(2L)
                .recetaCantidad(BigDecimal.valueOf(10.5))
                .build();

        assertThat(entity).isNotEqualTo("not a Receta");
    }

    @Test
    @DisplayName("hashCode → es consistente con la clase")
    void hashCode_esConsistente() {
        Receta entity1 = Receta.builder()
                .insumoId(1L)
                .productoId(2L)
                .recetaCantidad(BigDecimal.valueOf(10.5))
                .build();
        Receta entity2 = Receta.builder()
                .insumoId(3L)
                .productoId(4L)
                .recetaCantidad(BigDecimal.valueOf(99.9))
                .build();

        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    @Test
    @DisplayName("RecetaId → equals con mismos IDs → retorna true")
    void recetaId_equals_mismosIds_retornaTrue() {
        Receta.RecetaId id1 = new Receta.RecetaId(1L, 2L);
        Receta.RecetaId id2 = new Receta.RecetaId(1L, 2L);

        assertThat(id1).isEqualTo(id2);
    }

    @Test
    @DisplayName("RecetaId → equals con diferentes IDs → retorna false")
    void recetaId_equals_diferentesIds_retornaFalse() {
        Receta.RecetaId id1 = new Receta.RecetaId(1L, 2L);
        Receta.RecetaId id2 = new Receta.RecetaId(3L, 4L);

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("RecetaId → hashCode es consistente")
    void recetaId_hashCode_esConsistente() {
        Receta.RecetaId id1 = new Receta.RecetaId(1L, 2L);
        Receta.RecetaId id2 = new Receta.RecetaId(1L, 2L);

        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }
}
