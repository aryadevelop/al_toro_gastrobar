package co.edu.unicauca.backend.shared.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditableEntityTest {

    static class ConcreteEntity extends AuditableEntity {}

    @Test
    @DisplayName("onCreate → establece createdAt y updatedAt cuando son null")
    void onCreate_setsCamposAuditoria() {
        ConcreteEntity entity = new ConcreteEntity();
        entity.onCreate();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("onCreate → no sobreescribe createdAt si ya tenía valor")
    void onCreate_noSobreescribeCreatedAt() {
        ConcreteEntity entity = new ConcreteEntity();
        java.time.LocalDateTime fijo = java.time.LocalDateTime.of(2026, 1, 1, 0, 0);
        entity.setCreatedAt(fijo);
        entity.onCreate();
        assertThat(entity.getCreatedAt()).isEqualTo(fijo);
    }

    @Test
    @DisplayName("onUpdate → actualiza updatedAt")
    void onUpdate_actualizaUpdatedAt() {
        ConcreteEntity entity = new ConcreteEntity();
        entity.onUpdate();
        assertThat(entity.getUpdatedAt()).isNotNull();
    }
}
