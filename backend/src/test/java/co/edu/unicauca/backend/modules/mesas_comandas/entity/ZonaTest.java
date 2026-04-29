package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Zona Entity Tests")
class ZonaTest {

    @Test
    @DisplayName("equals → mismo ID → retorna true")
    void equals_mismoId_retornaTrue() throws Exception {
        Zona entity1 = Zona.builder()
                .zonaNombre("Terraza")
                .zonaCapacidadPersonas(20)
                .build();
        Zona entity2 = Zona.builder()
                .zonaNombre("Salón Principal")
                .zonaCapacidadPersonas(50)
                .build();

        setPrivateField(entity1, "zonaId", 1L);
        setPrivateField(entity2, "zonaId", 1L);

        assertThat(entity1).isEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → diferente ID → retorna false")
    void equals_diferenteId_retornaFalse() throws Exception {
        Zona entity1 = Zona.builder()
                .zonaNombre("Terraza")
                .zonaCapacidadPersonas(20)
                .build();
        Zona entity2 = Zona.builder()
                .zonaNombre("Terraza")
                .zonaCapacidadPersonas(20)
                .build();

        setPrivateField(entity1, "zonaId", 1L);
        setPrivateField(entity2, "zonaId", 2L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → ID null → retorna false")
    void equals_idNull_retornaFalse() throws Exception {
        Zona entity1 = Zona.builder()
                .zonaNombre("Terraza")
                .zonaCapacidadPersonas(20)
                .build();
        Zona entity2 = Zona.builder()
                .zonaNombre("Terraza")
                .zonaCapacidadPersonas(20)
                .build();

        setPrivateField(entity1, "zonaId", null);
        setPrivateField(entity2, "zonaId", 1L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → misma instancia → retorna true")
    void equals_mismaInstancia_retornaTrue() {
        Zona entity = Zona.builder()
                .zonaNombre("Terraza")
                .zonaCapacidadPersonas(20)
                .build();

        assertThat(entity).isEqualTo(entity);
    }

    @Test
    @DisplayName("equals → tipo diferente → retorna false")
    void equals_tipoDiferente_retornaFalse() {
        Zona entity = Zona.builder()
                .zonaNombre("Terraza")
                .zonaCapacidadPersonas(20)
                .build();

        assertThat(entity).isNotEqualTo("not a Zona");
    }

    @Test
    @DisplayName("hashCode → es consistente con la clase")
    void hashCode_esConsistente() {
        Zona entity1 = Zona.builder()
                .zonaNombre("Terraza")
                .zonaCapacidadPersonas(20)
                .build();
        Zona entity2 = Zona.builder()
                .zonaNombre("Salón Principal")
                .zonaCapacidadPersonas(50)
                .build();

        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    // Helper method to set private fields using reflection
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        Field field = null;

        // Try to find the field in the class hierarchy (Zona extends AuditableEntity)
        while (clazz != null && field == null) {
            try {
                field = clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }

        if (field == null) {
            throw new NoSuchFieldException("Field " + fieldName + " not found in class hierarchy");
        }

        field.setAccessible(true);
        field.set(target, value);
    }
}
