package co.edu.unicauca.backend.modules.reservas.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DecoracionZona Entity Tests")
class DecoracionZonaTest {

    @Test
    @DisplayName("onCreate → createdAt null → asigna fecha actual")
    void onCreate_createdAtNull_asignaFechaActual() throws Exception {
        DecoracionZona entity = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(1L)
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
        DecoracionZona entity = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(1L)
                .createdAt(fechaPrevia)
                .build();

        Method onCreate = entity.getClass().getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);

        assertThat(entity.getCreatedAt()).isEqualTo(fechaPrevia);
    }

    @Test
    @DisplayName("equals → mismos IDs (decoracionId y zonaId) → retorna true")
    void equals_mismosIds_retornaTrue() {
        DecoracionZona entity1 = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(2L)
                .build();
        DecoracionZona entity2 = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(2L)
                .build();

        assertThat(entity1).isEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → diferente decoracionId → retorna false")
    void equals_diferenteDecoracionId_retornaFalse() {
        DecoracionZona entity1 = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(2L)
                .build();
        DecoracionZona entity2 = DecoracionZona.builder()
                .decoracionId(3L)
                .zonaId(2L)
                .build();

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → diferente zonaId → retorna false")
    void equals_diferenteZonaId_retornaFalse() {
        DecoracionZona entity1 = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(2L)
                .build();
        DecoracionZona entity2 = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(3L)
                .build();

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → decoracionId null → retorna false")
    void equals_decoracionIdNull_retornaFalse() {
        DecoracionZona entity1 = DecoracionZona.builder()
                .decoracionId(null)
                .zonaId(2L)
                .build();
        DecoracionZona entity2 = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(2L)
                .build();

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → zonaId null → retorna false")
    void equals_zonaIdNull_retornaFalse() {
        DecoracionZona entity1 = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(null)
                .build();
        DecoracionZona entity2 = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(2L)
                .build();

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → misma instancia → retorna true")
    void equals_mismaInstancia_retornaTrue() {
        DecoracionZona entity = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(2L)
                .build();

        assertThat(entity).isEqualTo(entity);
    }

    @Test
    @DisplayName("equals → tipo diferente → retorna false")
    void equals_tipoDiferente_retornaFalse() {
        DecoracionZona entity = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(2L)
                .build();

        assertThat(entity).isNotEqualTo("not a DecoracionZona");
    }

    @Test
    @DisplayName("hashCode → es consistente con la clase")
    void hashCode_esConsistente() {
        DecoracionZona entity1 = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(2L)
                .build();
        DecoracionZona entity2 = DecoracionZona.builder()
                .decoracionId(3L)
                .zonaId(4L)
                .build();

        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    @Test
    @DisplayName("DecoracionZonaId → equals → mismos IDs → retorna true")
    void decoracionZonaId_equals_mismosIds_retornaTrue() {
        DecoracionZona.DecoracionZonaId id1 = new DecoracionZona.DecoracionZonaId(1L, 2L);
        DecoracionZona.DecoracionZonaId id2 = new DecoracionZona.DecoracionZonaId(1L, 2L);

        assertThat(id1).isEqualTo(id2);
    }

    @Test
    @DisplayName("DecoracionZonaId → hashCode → mismos IDs → mismo hash")
    void decoracionZonaId_hashCode_mismosIds_mismoHash() {
        DecoracionZona.DecoracionZonaId id1 = new DecoracionZona.DecoracionZonaId(1L, 2L);
        DecoracionZona.DecoracionZonaId id2 = new DecoracionZona.DecoracionZonaId(1L, 2L);

        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    // Helper method to set private fields using reflection
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
