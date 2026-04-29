package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Mesa Entity Tests")
class MesaTest {

    @Test
    @DisplayName("equals → mismo ID → retorna true")
    void equals_mismoId_retornaTrue() throws Exception {
        Mesa entity1 = Mesa.builder()
                .mesaIdentificador("T-01")
                .mesaNumeroPersonas(4)
                .mesaEstado(EstadoMesa.ESPERA)
                .build();
        Mesa entity2 = Mesa.builder()
                .mesaIdentificador("T-02")
                .mesaNumeroPersonas(2)
                .mesaEstado(EstadoMesa.ATENDIDA)
                .build();

        setPrivateField(entity1, "visitaId", 1L);
        setPrivateField(entity2, "visitaId", 1L);

        assertThat(entity1).isEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → diferente ID → retorna false")
    void equals_diferenteId_retornaFalse() throws Exception {
        Mesa entity1 = Mesa.builder().mesaEstado(EstadoMesa.ESPERA).build();
        Mesa entity2 = Mesa.builder().mesaEstado(EstadoMesa.ESPERA).build();

        setPrivateField(entity1, "visitaId", 1L);
        setPrivateField(entity2, "visitaId", 2L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → ID null → retorna false")
    void equals_idNull_retornaFalse() throws Exception {
        Mesa entity1 = Mesa.builder().mesaEstado(EstadoMesa.ESPERA).build();
        Mesa entity2 = Mesa.builder().mesaEstado(EstadoMesa.ESPERA).build();

        setPrivateField(entity1, "visitaId", null);
        setPrivateField(entity2, "visitaId", 1L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → misma instancia → retorna true")
    void equals_mismaInstancia_retornaTrue() {
        Mesa entity = Mesa.builder().mesaEstado(EstadoMesa.ESPERA).build();

        assertThat(entity).isEqualTo(entity);
    }

    @Test
    @DisplayName("equals → tipo diferente → retorna false")
    void equals_tipoDiferente_retornaFalse() {
        Mesa entity = Mesa.builder().mesaEstado(EstadoMesa.ESPERA).build();

        assertThat(entity).isNotEqualTo("not a Mesa");
    }

    @Test
    @DisplayName("hashCode → es consistente con la clase")
    void hashCode_esConsistente() {
        Mesa entity1 = Mesa.builder().mesaEstado(EstadoMesa.ESPERA).build();
        Mesa entity2 = Mesa.builder().mesaEstado(EstadoMesa.ATENDIDA).build();

        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    // Helper method to set private fields using reflection
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
