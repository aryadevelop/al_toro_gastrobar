package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Visita Entity Tests")
class VisitaTest {

    @Test
    @DisplayName("onCreate → visitaFechaHoraInicio null → asigna fecha actual")
    void onCreate_visitaFechaHoraInicioNull_asignaFechaActual() throws Exception {
        Visita entity = Visita.builder().build();
        setPrivateField(entity, "visitaFechaHoraInicio", null);

        Method onCreate = entity.getClass().getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);

        assertThat(entity.getVisitaFechaHoraInicio()).isNotNull();
        assertThat(entity.getVisitaFechaHoraInicio()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("onCreate → visitaFechaHoraInicio establecido → mantiene fecha")
    void onCreate_visitaFechaHoraInicioEstablecido_mantieneFecha() throws Exception {
        LocalDateTime fechaPrevia = LocalDateTime.of(2026, 1, 1, 12, 0);
        Visita entity = Visita.builder()
                .visitaFechaHoraInicio(fechaPrevia)
                .build();

        Method onCreate = entity.getClass().getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);

        assertThat(entity.getVisitaFechaHoraInicio()).isEqualTo(fechaPrevia);
    }

    @Test
    @DisplayName("equals → mismo ID → retorna true")
    void equals_mismoId_retornaTrue() throws Exception {
        Visita entity1 = Visita.builder().build();
        Visita entity2 = Visita.builder().build();

        setPrivateField(entity1, "visitaId", 1L);
        setPrivateField(entity2, "visitaId", 1L);

        assertThat(entity1).isEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → diferente ID → retorna false")
    void equals_diferenteId_retornaFalse() throws Exception {
        Visita entity1 = Visita.builder().build();
        Visita entity2 = Visita.builder().build();

        setPrivateField(entity1, "visitaId", 1L);
        setPrivateField(entity2, "visitaId", 2L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → ID null → retorna false")
    void equals_idNull_retornaFalse() throws Exception {
        Visita entity1 = Visita.builder().build();
        Visita entity2 = Visita.builder().build();

        setPrivateField(entity1, "visitaId", null);
        setPrivateField(entity2, "visitaId", 1L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → misma instancia → retorna true")
    void equals_mismaInstancia_retornaTrue() {
        Visita entity = Visita.builder().build();

        assertThat(entity).isEqualTo(entity);
    }

    @Test
    @DisplayName("equals → tipo diferente → retorna false")
    void equals_tipoDiferente_retornaFalse() {
        Visita entity = Visita.builder().build();

        assertThat(entity).isNotEqualTo("not a Visita");
    }

    @Test
    @DisplayName("hashCode → es consistente con la clase")
    void hashCode_esConsistente() {
        Visita entity1 = Visita.builder().build();
        Visita entity2 = Visita.builder().build();

        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    // Helper method to set private fields using reflection
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
