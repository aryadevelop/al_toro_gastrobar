package co.edu.unicauca.backend.modules.inventario.entity;

import co.edu.unicauca.backend.shared.enums.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MovimientoInventario Entity Tests")
class MovimientoInventarioTest {

    @Test
    @DisplayName("onCreate → movimientoFechaHora null → asigna fecha actual")
    void onCreate_movimientoFechaHoraNullAsignaFechaActual() throws Exception {
        MovimientoInventario entity = MovimientoInventario.builder()
                .movimientoCantidad(BigDecimal.valueOf(10.5))
                .movimientoTipo(TipoMovimiento.INGRESO)
                .build();
        setPrivateField(entity, "movimientoFechaHora", null);
        setPrivateField(entity, "createdAt", LocalDateTime.now());

        Method onCreate = entity.getClass().getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);

        assertThat(entity.getMovimientoFechaHora()).isNotNull();
        assertThat(entity.getMovimientoFechaHora()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("onCreate → createdAt null → asigna fecha actual")
    void onCreate_createdAtNull_asignaFechaActual() throws Exception {
        MovimientoInventario entity = MovimientoInventario.builder()
                .movimientoCantidad(BigDecimal.valueOf(10.5))
                .movimientoTipo(TipoMovimiento.EGRESO)
                .build();
        setPrivateField(entity, "movimientoFechaHora", LocalDateTime.now());
        setPrivateField(entity, "createdAt", null);

        Method onCreate = entity.getClass().getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);

        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("onCreate → ambos campos null → asigna fechas actuales")
    void onCreate_ambosNull_asignaFechasActuales() throws Exception {
        MovimientoInventario entity = MovimientoInventario.builder()
                .movimientoCantidad(BigDecimal.valueOf(10.5))
                .movimientoTipo(TipoMovimiento.INGRESO)
                .build();
        setPrivateField(entity, "movimientoFechaHora", null);
        setPrivateField(entity, "createdAt", null);

        Method onCreate = entity.getClass().getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);

        assertThat(entity.getMovimientoFechaHora()).isNotNull();
        assertThat(entity.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("onCreate → campos establecidos → mantiene fechas")
    void onCreate_camposEstablecidos_mantieneFechas() throws Exception {
        LocalDateTime fechaMovimiento = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 1, 1, 9, 0);
        MovimientoInventario entity = MovimientoInventario.builder()
                .movimientoCantidad(BigDecimal.valueOf(10.5))
                .movimientoTipo(TipoMovimiento.INGRESO)
                .movimientoFechaHora(fechaMovimiento)
                .createdAt(fechaCreacion)
                .build();

        Method onCreate = entity.getClass().getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);

        assertThat(entity.getMovimientoFechaHora()).isEqualTo(fechaMovimiento);
        assertThat(entity.getCreatedAt()).isEqualTo(fechaCreacion);
    }

    @Test
    @DisplayName("equals → mismo ID → retorna true")
    void equals_mismoId_retornaTrue() throws Exception {
        MovimientoInventario entity1 = MovimientoInventario.builder().build();
        MovimientoInventario entity2 = MovimientoInventario.builder().build();

        setPrivateField(entity1, "movimientoId", 1L);
        setPrivateField(entity2, "movimientoId", 1L);

        assertThat(entity1).isEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → diferente ID → retorna false")
    void equals_diferenteId_retornaFalse() throws Exception {
        MovimientoInventario entity1 = MovimientoInventario.builder().build();
        MovimientoInventario entity2 = MovimientoInventario.builder().build();

        setPrivateField(entity1, "movimientoId", 1L);
        setPrivateField(entity2, "movimientoId", 2L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → ID null → retorna false")
    void equals_idNull_retornaFalse() throws Exception {
        MovimientoInventario entity1 = MovimientoInventario.builder().build();
        MovimientoInventario entity2 = MovimientoInventario.builder().build();

        setPrivateField(entity1, "movimientoId", null);
        setPrivateField(entity2, "movimientoId", 1L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → misma instancia → retorna true")
    void equals_mismaInstancia_retornaTrue() {
        MovimientoInventario entity = MovimientoInventario.builder().build();

        assertThat(entity).isEqualTo(entity);
    }

    @Test
    @DisplayName("equals → tipo diferente → retorna false")
    void equals_tipoDiferente_retornaFalse() {
        MovimientoInventario entity = MovimientoInventario.builder().build();

        assertThat(entity).isNotEqualTo("not a MovimientoInventario");
    }

    @Test
    @DisplayName("hashCode → es consistente con la clase")
    void hashCode_esConsistente() {
        MovimientoInventario entity1 = MovimientoInventario.builder().build();
        MovimientoInventario entity2 = MovimientoInventario.builder().build();

        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
