package co.edu.unicauca.backend.modules.reservas.entity;

import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Decoracion Entity Tests")
class DecoracionTest {

    @Test
    @DisplayName("equals → mismo ID → retorna true")
    void equals_mismoId_retornaTrue() throws Exception {
        Decoracion entity1 = Decoracion.builder()
                .decoracionNombre("Globos")
                .decoracionEstado(EstadoGenerico.ACTIVO)
                .build();
        Decoracion entity2 = Decoracion.builder()
                .decoracionNombre("Flores")
                .decoracionEstado(EstadoGenerico.INACTIVO)
                .build();

        setPrivateField(entity1, "decoracionId", 1L);
        setPrivateField(entity2, "decoracionId", 1L);

        assertThat(entity1).isEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → diferente ID → retorna false")
    void equals_diferenteId_retornaFalse() throws Exception {
        Decoracion entity1 = Decoracion.builder()
                .decoracionNombre("Globos")
                .decoracionEstado(EstadoGenerico.ACTIVO)
                .decoracionCostoAdicional(BigDecimal.valueOf(20000))
                .build();
        Decoracion entity2 = Decoracion.builder()
                .decoracionNombre("Globos")
                .decoracionEstado(EstadoGenerico.ACTIVO)
                .decoracionCostoAdicional(BigDecimal.valueOf(20000))
                .build();

        setPrivateField(entity1, "decoracionId", 1L);
        setPrivateField(entity2, "decoracionId", 2L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → ID null → retorna false")
    void equals_idNull_retornaFalse() throws Exception {
        Decoracion entity1 = Decoracion.builder()
                .decoracionNombre("Globos")
                .decoracionEstado(EstadoGenerico.ACTIVO)
                .build();
        Decoracion entity2 = Decoracion.builder()
                .decoracionNombre("Globos")
                .decoracionEstado(EstadoGenerico.ACTIVO)
                .build();

        setPrivateField(entity1, "decoracionId", null);
        setPrivateField(entity2, "decoracionId", 1L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → misma instancia → retorna true")
    void equals_mismaInstancia_retornaTrue() {
        Decoracion entity = Decoracion.builder()
                .decoracionNombre("Globos")
                .decoracionEstado(EstadoGenerico.ACTIVO)
                .build();

        assertThat(entity).isEqualTo(entity);
    }

    @Test
    @DisplayName("equals → tipo diferente → retorna false")
    void equals_tipoDiferente_retornaFalse() {
        Decoracion entity = Decoracion.builder()
                .decoracionNombre("Globos")
                .decoracionEstado(EstadoGenerico.ACTIVO)
                .build();

        assertThat(entity).isNotEqualTo("not a Decoracion");
    }

    @Test
    @DisplayName("hashCode → es consistente con la clase")
    void hashCode_esConsistente() {
        Decoracion entity1 = Decoracion.builder()
                .decoracionNombre("Globos")
                .decoracionEstado(EstadoGenerico.ACTIVO)
                .build();
        Decoracion entity2 = Decoracion.builder()
                .decoracionNombre("Flores")
                .decoracionEstado(EstadoGenerico.INACTIVO)
                .build();

        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    // Helper method to set private fields using reflection
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        Field field = null;

        // Try to find the field in the class hierarchy
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
