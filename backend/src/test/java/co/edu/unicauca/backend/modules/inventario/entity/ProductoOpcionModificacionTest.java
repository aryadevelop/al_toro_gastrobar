package co.edu.unicauca.backend.modules.inventario.entity;

import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.TipoComponenteMenu;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ProductoOpcionModificacion Entity Tests")
class ProductoOpcionModificacionTest {

    @Test
    @DisplayName("onCreate → createdAt null → asigna fecha actual")
    void onCreate_createdAtNull_asignaFechaActual() throws Exception {
        ProductoOpcionModificacion entity = ProductoOpcionModificacion.builder().build();
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
        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 1, 1, 10, 0);
        ProductoOpcionModificacion entity = ProductoOpcionModificacion.builder()
                .createdAt(fechaCreacion)
                .build();

        Method onCreate = entity.getClass().getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);

        assertThat(entity.getCreatedAt()).isEqualTo(fechaCreacion);
    }

    @Test
    @DisplayName("ProductoOpcionModificacionId → equals con mismos IDs → retorna true")
    void productoOpcionModificacionId_equals_mismosIds_retornaTrue() {
        ProductoOpcionModificacion.ProductoOpcionModificacionId id1 =
                new ProductoOpcionModificacion.ProductoOpcionModificacionId(1L, 2L);
        ProductoOpcionModificacion.ProductoOpcionModificacionId id2 =
                new ProductoOpcionModificacion.ProductoOpcionModificacionId(1L, 2L);

        assertThat(id1).isEqualTo(id2);
    }

    @Test
    @DisplayName("ProductoOpcionModificacionId → equals con diferentes IDs → retorna false")
    void productoOpcionModificacionId_equals_diferentesIds_retornaFalse() {
        ProductoOpcionModificacion.ProductoOpcionModificacionId id1 =
                new ProductoOpcionModificacion.ProductoOpcionModificacionId(1L, 2L);
        ProductoOpcionModificacion.ProductoOpcionModificacionId id2 =
                new ProductoOpcionModificacion.ProductoOpcionModificacionId(3L, 4L);

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("ProductoOpcionModificacionId → hashCode es consistente")
    void productoOpcionModificacionId_hashCode_esConsistente() {
        ProductoOpcionModificacion.ProductoOpcionModificacionId id1 =
                new ProductoOpcionModificacion.ProductoOpcionModificacionId(1L, 2L);
        ProductoOpcionModificacion.ProductoOpcionModificacionId id2 =
                new ProductoOpcionModificacion.ProductoOpcionModificacionId(1L, 2L);

        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
