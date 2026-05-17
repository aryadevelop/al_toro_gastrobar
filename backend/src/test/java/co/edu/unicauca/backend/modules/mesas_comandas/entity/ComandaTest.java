package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Comanda Entity Tests")
class ComandaTest {

    @Test
    @DisplayName("onCreate → llama super.onCreate() → asigna createdAt y updatedAt")
    void onCreate_llamaSuperOnCreate_asignaCamposAuditables() throws Exception {
        Comanda entity = Comanda.builder()
                .comandaEstado(EstadoComanda.PRE_RESERVA)
                .build();

        // Asegurar que createdAt/updatedAt están null antes de onCreate
        setPrivateField(entity, "createdAt", null);
        setPrivateField(entity, "updatedAt", null);

        Method onCreate = entity.getClass().getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);

        // Verificar que super.onCreate() fue llamado y asignó los campos
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("onCreate → createdAt ya establecido → mantiene valor")
    void onCreate_createdAtEstablecido_mantieneValor() throws Exception {
        LocalDateTime fechaPrevia = LocalDateTime.of(2026, 1, 1, 12, 0);
        Comanda entity = Comanda.builder()
                .comandaEstado(EstadoComanda.PENDIENTE)
                .build();

        setPrivateField(entity, "createdAt", fechaPrevia);

        Method onCreate = entity.getClass().getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);

        assertThat(entity.getCreatedAt()).isEqualTo(fechaPrevia);
    }

    @Test
    @DisplayName("equals → mismo ID → retorna true")
    void equals_mismoId_retornaTrue() throws Exception {
        Comanda entity1 = Comanda.builder()
                .comandaEstado(EstadoComanda.PRE_RESERVA)
                .build();
        Comanda entity2 = Comanda.builder()
                .comandaEstado(EstadoComanda.PENDIENTE)
                .build();

        setPrivateField(entity1, "comandaId", 1L);
        setPrivateField(entity2, "comandaId", 1L);

        assertThat(entity1).isEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → diferente ID → retorna false")
    void equals_diferenteId_retornaFalse() throws Exception {
        Comanda entity1 = Comanda.builder().comandaEstado(EstadoComanda.PRE_RESERVA).build();
        Comanda entity2 = Comanda.builder().comandaEstado(EstadoComanda.PRE_RESERVA).build();

        setPrivateField(entity1, "comandaId", 1L);
        setPrivateField(entity2, "comandaId", 2L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → ID null → retorna false")
    void equals_idNull_retornaFalse() throws Exception {
        Comanda entity1 = Comanda.builder().comandaEstado(EstadoComanda.PRE_RESERVA).build();
        Comanda entity2 = Comanda.builder().comandaEstado(EstadoComanda.PRE_RESERVA).build();

        setPrivateField(entity1, "comandaId", null);
        setPrivateField(entity2, "comandaId", 1L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → misma instancia → retorna true")
    void equals_mismaInstancia_retornaTrue() {
        Comanda entity = Comanda.builder().comandaEstado(EstadoComanda.PRE_RESERVA).build();
        assertThat(entity).isEqualTo(entity);
    }

    @Test
    @DisplayName("equals → objeto de clase diferente → retorna false")
    void equals_claseDiferente_retornaFalse() {
        Comanda entity = Comanda.builder().comandaEstado(EstadoComanda.PRE_RESERVA).build();
        assertThat(entity).isNotEqualTo("no es una comanda");
    }

    @Test
    @DisplayName("equals → objeto null → retorna false")
    void equals_objetoNull_retornaFalse() {
        Comanda entity = Comanda.builder().comandaEstado(EstadoComanda.PRE_RESERVA).build();
        assertThat(entity.equals(null)).isFalse();
    }

    @Test
    @DisplayName("hashCode → es consistente con la clase")
    void hashCode_esConsistente() {
        Comanda entity1 = Comanda.builder().comandaEstado(EstadoComanda.PRE_RESERVA).build();
        Comanda entity2 = Comanda.builder().comandaEstado(EstadoComanda.PENDIENTE).build();

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
