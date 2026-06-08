package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ComandaItem Entity Tests")
class ComandaItemTest {

    @Test
    @DisplayName("onCreate → createdAt null → asigna fecha actual")
    void onCreate_createdAtNull_asignaFechaActual() throws Exception {
        ComandaItem entity = ComandaItem.builder()
                .comandaItemCantidad(1)
                .comandaItemPrecio(BigDecimal.valueOf(15000))
                .build();
        setPrivateField(entity, "createdAt", null);

        Method onCreate = entity.getClass().getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("onCreate → createdAt establecido → mantiene fecha")
    void onCreate_createdAtEstablecido_mantieneFecha() throws Exception {
        LocalDateTime fechaPrevia = LocalDateTime.of(2026, 1, 1, 12, 0);
        ComandaItem entity = ComandaItem.builder()
                .comandaItemCantidad(1)
                .comandaItemPrecio(BigDecimal.valueOf(15000))
                .createdAt(fechaPrevia)
                .build();

        Method onCreate = entity.getClass().getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);

        assertThat(entity.getCreatedAt()).isEqualTo(fechaPrevia);
    }

    @Test
    @DisplayName("equals → mismo ID → retorna true")
    void equals_mismoId_retornaTrue() throws Exception {
        ComandaItem entity1 = ComandaItem.builder().build();
        ComandaItem entity2 = ComandaItem.builder().build();

        setPrivateField(entity1, "comandaItemId", 1L);
        setPrivateField(entity2, "comandaItemId", 1L);

        assertThat(entity1).isEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → diferente ID → retorna false")
    void equals_diferenteId_retornaFalse() throws Exception {
        ComandaItem entity1 = ComandaItem.builder().build();
        ComandaItem entity2 = ComandaItem.builder().build();

        setPrivateField(entity1, "comandaItemId", 1L);
        setPrivateField(entity2, "comandaItemId", 2L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → ID null → retorna false")
    void equals_idNull_retornaFalse() throws Exception {
        ComandaItem entity1 = ComandaItem.builder().build();
        ComandaItem entity2 = ComandaItem.builder().build();

        setPrivateField(entity1, "comandaItemId", null);
        setPrivateField(entity2, "comandaItemId", 1L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → misma instancia → retorna true")
    void equals_mismaInstancia_retornaTrue() {
        ComandaItem entity = ComandaItem.builder().build();
        assertThat(entity).isEqualTo(entity);
    }

    @Test
    @DisplayName("equals → objeto de clase diferente → retorna false")
    void equals_claseDiferente_retornaFalse() {
        ComandaItem entity = ComandaItem.builder().build();
        assertThat(entity).isNotEqualTo("no es un comanda item");
    }

    @Test
    @DisplayName("equals → objeto null → retorna false")
    void equals_objetoNull_retornaFalse() {
        ComandaItem entity = ComandaItem.builder().build();
        assertThat(entity.equals(null)).isFalse();
    }

    @Test
    @DisplayName("hashCode → es consistente con la clase")
    void hashCode_esConsistente() {
        ComandaItem entity1 = ComandaItem.builder().build();
        ComandaItem entity2 = ComandaItem.builder().build();

        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    // Helper method to set private fields using reflection
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
