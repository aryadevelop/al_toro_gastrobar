package co.edu.unicauca.backend.modules.reservas.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BloqueDisponibilidad Entity Tests")
class BloqueDisponibilidadTest {

    @Test
    @DisplayName("equals → mismo ID → retorna true")
    void equals_mismoId_retornaTrue() throws Exception {
        BloqueDisponibilidad entity1 = BloqueDisponibilidad.builder()
                .fechaInicio(LocalDate.of(2026, 6, 1))
                .fechaFin(LocalDate.of(2026, 6, 5))
                .build();
        BloqueDisponibilidad entity2 = BloqueDisponibilidad.builder()
                .fechaInicio(LocalDate.of(2026, 7, 1))
                .fechaFin(LocalDate.of(2026, 7, 10))
                .build();

        setPrivateField(entity1, "bloqueId", 1L);
        setPrivateField(entity2, "bloqueId", 1L);

        assertThat(entity1).isEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → diferente ID → retorna false")
    void equals_diferenteId_retornaFalse() throws Exception {
        BloqueDisponibilidad entity1 = BloqueDisponibilidad.builder()
                .fechaInicio(LocalDate.of(2026, 6, 1))
                .fechaFin(LocalDate.of(2026, 6, 5))
                .horaInicio(LocalTime.of(12, 0))
                .horaFin(LocalTime.of(14, 0))
                .motivo("Mantenimiento")
                .build();
        BloqueDisponibilidad entity2 = BloqueDisponibilidad.builder()
                .fechaInicio(LocalDate.of(2026, 6, 1))
                .fechaFin(LocalDate.of(2026, 6, 5))
                .horaInicio(LocalTime.of(12, 0))
                .horaFin(LocalTime.of(14, 0))
                .motivo("Mantenimiento")
                .build();

        setPrivateField(entity1, "bloqueId", 1L);
        setPrivateField(entity2, "bloqueId", 2L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → ID null → retorna false")
    void equals_idNull_retornaFalse() throws Exception {
        BloqueDisponibilidad entity1 = BloqueDisponibilidad.builder()
                .fechaInicio(LocalDate.of(2026, 6, 1))
                .fechaFin(LocalDate.of(2026, 6, 5))
                .build();
        BloqueDisponibilidad entity2 = BloqueDisponibilidad.builder()
                .fechaInicio(LocalDate.of(2026, 6, 1))
                .fechaFin(LocalDate.of(2026, 6, 5))
                .build();

        setPrivateField(entity1, "bloqueId", null);
        setPrivateField(entity2, "bloqueId", 1L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → misma instancia → retorna true")
    void equals_mismaInstancia_retornaTrue() {
        BloqueDisponibilidad entity = BloqueDisponibilidad.builder()
                .fechaInicio(LocalDate.of(2026, 6, 1))
                .fechaFin(LocalDate.of(2026, 6, 5))
                .build();

        assertThat(entity).isEqualTo(entity);
    }

    @Test
    @DisplayName("equals → tipo diferente → retorna false")
    void equals_tipoDiferente_retornaFalse() {
        BloqueDisponibilidad entity = BloqueDisponibilidad.builder()
                .fechaInicio(LocalDate.of(2026, 6, 1))
                .fechaFin(LocalDate.of(2026, 6, 5))
                .build();

        assertThat(entity).isNotEqualTo("not a BloqueDisponibilidad");
    }

    @Test
    @DisplayName("hashCode → es consistente con la clase")
    void hashCode_esConsistente() {
        BloqueDisponibilidad entity1 = BloqueDisponibilidad.builder()
                .fechaInicio(LocalDate.of(2026, 6, 1))
                .fechaFin(LocalDate.of(2026, 6, 5))
                .build();
        BloqueDisponibilidad entity2 = BloqueDisponibilidad.builder()
                .fechaInicio(LocalDate.of(2026, 7, 1))
                .fechaFin(LocalDate.of(2026, 7, 10))
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
