package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ComandaMenuModificacion Entity Tests")
class ComandaMenuModificacionTest {

    @Test
    @DisplayName("onCreate → createdAt null → asigna fecha actual")
    void onCreate_createdAtNull_asignaFechaActual() throws Exception {
        ComandaMenuModificacion entity = ComandaMenuModificacion.builder().build();
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
        ComandaMenuModificacion entity = ComandaMenuModificacion.builder()
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
        ComandaMenuModificacion entity1 = ComandaMenuModificacion.builder().build();
        ComandaMenuModificacion entity2 = ComandaMenuModificacion.builder().build();

        setPrivateField(entity1, "id", 1L);
        setPrivateField(entity2, "id", 1L);

        assertThat(entity1).isEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → diferente ID → retorna false")
    void equals_diferenteId_retornaFalse() throws Exception {
        ComandaMenuModificacion entity1 = ComandaMenuModificacion.builder().build();
        ComandaMenuModificacion entity2 = ComandaMenuModificacion.builder().build();

        setPrivateField(entity1, "id", 1L);
        setPrivateField(entity2, "id", 2L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → ID null → retorna false")
    void equals_idNull_retornaFalse() throws Exception {
        ComandaMenuModificacion entity1 = ComandaMenuModificacion.builder().build();
        ComandaMenuModificacion entity2 = ComandaMenuModificacion.builder().build();

        setPrivateField(entity1, "id", null);
        setPrivateField(entity2, "id", 1L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("hashCode → es consistente con la clase")
    void hashCode_esConsistente() {
        ComandaMenuModificacion entity1 = ComandaMenuModificacion.builder().build();
        ComandaMenuModificacion entity2 = ComandaMenuModificacion.builder().build();

        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    // Helper method to set private fields using reflection
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
