package co.edu.unicauca.backend.modules.inventario.entity;

import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.TipoComponenteMenu;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpcionModificacion Entity Tests")
class OpcionModificacionTest {

    @Test
    @DisplayName("equals → mismo ID → retorna true")
    void equals_mismoId_retornaTrue() throws Exception {
        OpcionModificacion entity1 = OpcionModificacion.builder()
                .opcionNombre("Arroz blanco")
                .opcionEstado(EstadoGenerico.ACTIVO)
                .tipoComponente(TipoComponenteMenu.ARROZ)
                .build();
        OpcionModificacion entity2 = OpcionModificacion.builder()
                .opcionNombre("Arroz marinero")
                .opcionEstado(EstadoGenerico.ACTIVO)
                .tipoComponente(TipoComponenteMenu.ARROZ)
                .build();

        setPrivateField(entity1, "opcionId", 1L);
        setPrivateField(entity2, "opcionId", 1L);

        assertThat(entity1).isEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → diferente ID → retorna false")
    void equals_diferenteId_retornaFalse() throws Exception {
        OpcionModificacion entity1 = OpcionModificacion.builder()
                .opcionNombre("Arroz blanco")
                .opcionEstado(EstadoGenerico.ACTIVO)
                .tipoComponente(TipoComponenteMenu.ARROZ)
                .build();
        OpcionModificacion entity2 = OpcionModificacion.builder()
                .opcionNombre("Arroz blanco")
                .opcionEstado(EstadoGenerico.ACTIVO)
                .tipoComponente(TipoComponenteMenu.ARROZ)
                .build();

        setPrivateField(entity1, "opcionId", 1L);
        setPrivateField(entity2, "opcionId", 2L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → ID null → retorna false")
    void equals_idNull_retornaFalse() throws Exception {
        OpcionModificacion entity1 = OpcionModificacion.builder()
                .opcionNombre("Arroz blanco")
                .opcionEstado(EstadoGenerico.ACTIVO)
                .tipoComponente(TipoComponenteMenu.ARROZ)
                .build();
        OpcionModificacion entity2 = OpcionModificacion.builder()
                .opcionNombre("Arroz blanco")
                .opcionEstado(EstadoGenerico.ACTIVO)
                .tipoComponente(TipoComponenteMenu.ARROZ)
                .build();

        setPrivateField(entity1, "opcionId", null);
        setPrivateField(entity2, "opcionId", 1L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → misma instancia → retorna true")
    void equals_mismaInstancia_retornaTrue() {
        OpcionModificacion entity = OpcionModificacion.builder()
                .opcionNombre("Arroz blanco")
                .opcionEstado(EstadoGenerico.ACTIVO)
                .tipoComponente(TipoComponenteMenu.ARROZ)
                .build();

        assertThat(entity).isEqualTo(entity);
    }

    @Test
    @DisplayName("equals → tipo diferente → retorna false")
    void equals_tipoDiferente_retornaFalse() {
        OpcionModificacion entity = OpcionModificacion.builder()
                .opcionNombre("Arroz blanco")
                .opcionEstado(EstadoGenerico.ACTIVO)
                .tipoComponente(TipoComponenteMenu.ARROZ)
                .build();

        assertThat(entity).isNotEqualTo("not an OpcionModificacion");
    }

    @Test
    @DisplayName("hashCode → es consistente con la clase")
    void hashCode_esConsistente() {
        OpcionModificacion entity1 = OpcionModificacion.builder()
                .opcionNombre("Arroz blanco")
                .opcionEstado(EstadoGenerico.ACTIVO)
                .tipoComponente(TipoComponenteMenu.ARROZ)
                .build();
        OpcionModificacion entity2 = OpcionModificacion.builder()
                .opcionNombre("Pollo")
                .opcionEstado(EstadoGenerico.INACTIVO)
                .tipoComponente(TipoComponenteMenu.SALSA_PROTEINA_1)
                .build();

        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    @Test
    @DisplayName("Enum TipoComponenteMenu no incluye BEBIDA tras refactor de menu_bebida_disponible")
    void tipoComponenteMenu_doesNotIncludeBebida() {
        boolean hasBebida = java.util.Arrays.stream(TipoComponenteMenu.values())
            .anyMatch(t -> t.name().equals("BEBIDA"));
        assertThat(hasBebida).isFalse();
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        Field field = null;

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
