package co.edu.unicauca.backend.modules.inventario.entity;

import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.TipoInsumo;
import co.edu.unicauca.backend.shared.enums.UnidadMedida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Insumo Entity Tests")
class InsumoTest {

    @Test
    @DisplayName("equals → mismo ID → retorna true")
    void equals_mismoId_retornaTrue() throws Exception {
        Insumo entity1 = Insumo.builder()
                .insumoNombre("Arroz")
                .tipoInsumo(TipoInsumo.MATERIA_PRIMA)
                .insumoUnidad(UnidadMedida.KG)
                .insumoEstado(EstadoGenerico.ACTIVO)
                .insumoStockActual(BigDecimal.valueOf(50))
                .build();
        Insumo entity2 = Insumo.builder()
                .insumoNombre("Sal")
                .tipoInsumo(TipoInsumo.MATERIA_PRIMA)
                .insumoUnidad(UnidadMedida.G)
                .insumoEstado(EstadoGenerico.ACTIVO)
                .insumoStockActual(BigDecimal.valueOf(100))
                .build();

        setPrivateField(entity1, "insumoId", 1L);
        setPrivateField(entity2, "insumoId", 1L);

        assertThat(entity1).isEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → diferente ID → retorna false")
    void equals_diferenteId_retornaFalse() throws Exception {
        Insumo entity1 = Insumo.builder()
                .insumoNombre("Arroz")
                .tipoInsumo(TipoInsumo.MATERIA_PRIMA)
                .insumoUnidad(UnidadMedida.KG)
                .insumoEstado(EstadoGenerico.ACTIVO)
                .insumoStockActual(BigDecimal.valueOf(50))
                .build();
        Insumo entity2 = Insumo.builder()
                .insumoNombre("Arroz")
                .tipoInsumo(TipoInsumo.MATERIA_PRIMA)
                .insumoUnidad(UnidadMedida.KG)
                .insumoEstado(EstadoGenerico.ACTIVO)
                .insumoStockActual(BigDecimal.valueOf(50))
                .build();

        setPrivateField(entity1, "insumoId", 1L);
        setPrivateField(entity2, "insumoId", 2L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → ID null → retorna false")
    void equals_idNull_retornaFalse() throws Exception {
        Insumo entity1 = Insumo.builder()
                .insumoNombre("Arroz")
                .tipoInsumo(TipoInsumo.MATERIA_PRIMA)
                .insumoUnidad(UnidadMedida.KG)
                .insumoEstado(EstadoGenerico.ACTIVO)
                .insumoStockActual(BigDecimal.valueOf(50))
                .build();
        Insumo entity2 = Insumo.builder()
                .insumoNombre("Arroz")
                .tipoInsumo(TipoInsumo.MATERIA_PRIMA)
                .insumoUnidad(UnidadMedida.KG)
                .insumoEstado(EstadoGenerico.ACTIVO)
                .insumoStockActual(BigDecimal.valueOf(50))
                .build();

        setPrivateField(entity1, "insumoId", null);
        setPrivateField(entity2, "insumoId", 1L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → misma instancia → retorna true")
    void equals_mismaInstancia_retornaTrue() {
        Insumo entity = Insumo.builder()
                .insumoNombre("Arroz")
                .tipoInsumo(TipoInsumo.MATERIA_PRIMA)
                .insumoUnidad(UnidadMedida.KG)
                .insumoEstado(EstadoGenerico.ACTIVO)
                .insumoStockActual(BigDecimal.valueOf(50))
                .build();

        assertThat(entity).isEqualTo(entity);
    }

    @Test
    @DisplayName("equals → tipo diferente → retorna false")
    void equals_tipoDiferente_retornaFalse() {
        Insumo entity = Insumo.builder()
                .insumoNombre("Arroz")
                .tipoInsumo(TipoInsumo.MATERIA_PRIMA)
                .insumoUnidad(UnidadMedida.KG)
                .insumoEstado(EstadoGenerico.ACTIVO)
                .insumoStockActual(BigDecimal.valueOf(50))
                .build();

        assertThat(entity).isNotEqualTo("not an Insumo");
    }

    @Test
    @DisplayName("hashCode → es consistente con la clase")
    void hashCode_esConsistente() {
        Insumo entity1 = Insumo.builder()
                .insumoNombre("Arroz")
                .tipoInsumo(TipoInsumo.MATERIA_PRIMA)
                .insumoUnidad(UnidadMedida.KG)
                .insumoEstado(EstadoGenerico.ACTIVO)
                .insumoStockActual(BigDecimal.valueOf(50))
                .build();
        Insumo entity2 = Insumo.builder()
                .insumoNombre("Sal")
                .tipoInsumo(TipoInsumo.SEMIELABORADO)
                .insumoUnidad(UnidadMedida.G)
                .insumoEstado(EstadoGenerico.INACTIVO)
                .insumoStockActual(BigDecimal.valueOf(100))
                .build();

        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
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
