package co.edu.unicauca.backend.modules.notificaciones.entity;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notificacion - Entity Tests")
class NotificacionTest {

    @Test
    @DisplayName("onCreate() con notificacionFechaHora null → asigna LocalDateTime.now()")
    void onCreate_notificacionFechaHoraNull_asignaFechaActual() throws Exception {
        Notificacion notificacion = Notificacion.builder()
                .mesa(Mesa.builder().build())
                .empleado(Empleado.builder().build())
                .notificacionEstado(EstadoNotificacion.ACTIVA)
                .notificacionTipo(TipoNotificacion.ATENCION)
                .build();

        // Forzar notificacionFechaHora a null usando reflection
        setPrivateField(notificacion, "notificacionFechaHora", null);

        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);
        notificacion.onCreate();
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);

        assertThat(notificacion.getNotificacionFechaHora()).isNotNull();
        assertThat(notificacion.getNotificacionFechaHora()).isAfterOrEqualTo(antes);
        assertThat(notificacion.getNotificacionFechaHora()).isBeforeOrEqualTo(despues);
    }

    @Test
    @DisplayName("onCreate() con notificacionFechaHora establecida → mantiene valor original")
    void onCreate_notificacionFechaHoraEstablecida_mantieneFecha() throws Exception {
        LocalDateTime fechaEspecifica = LocalDateTime.of(2026, 3, 20, 19, 0, 0);

        Notificacion notificacion = Notificacion.builder()
                .mesa(Mesa.builder().build())
                .empleado(Empleado.builder().build())
                .notificacionEstado(EstadoNotificacion.ACTIVA)
                .notificacionTipo(TipoNotificacion.PLATOS_LISTOS)
                .notificacionFechaHora(fechaEspecifica)
                .build();

        notificacion.onCreate();

        assertThat(notificacion.getNotificacionFechaHora()).isEqualTo(fechaEspecifica);
    }

    @Test
    @DisplayName("onCreate() con createdAt null → asigna LocalDateTime.now()")
    void onCreate_createdAtNull_asignaFechaActual() throws Exception {
        Notificacion notificacion = Notificacion.builder()
                .mesa(Mesa.builder().build())
                .empleado(Empleado.builder().build())
                .notificacionEstado(EstadoNotificacion.ACTIVA)
                .notificacionTipo(TipoNotificacion.BEBIDAS_LISTAS)
                .build();

        // Forzar createdAt a null usando reflection
        setPrivateField(notificacion, "createdAt", null);

        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);
        notificacion.onCreate();
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);

        assertThat(notificacion.getCreatedAt()).isNotNull();
        assertThat(notificacion.getCreatedAt()).isAfterOrEqualTo(antes);
        assertThat(notificacion.getCreatedAt()).isBeforeOrEqualTo(despues);
    }

    @Test
    @DisplayName("onCreate() con createdAt establecido → mantiene valor original")
    void onCreate_createdAtEstablecido_mantieneFecha() throws Exception {
        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 4, 10, 12, 30, 0);

        Notificacion notificacion = Notificacion.builder()
                .mesa(Mesa.builder().build())
                .empleado(Empleado.builder().build())
                .notificacionEstado(EstadoNotificacion.ATENDIDA)
                .notificacionTipo(TipoNotificacion.CAMBIO)
                .createdAt(fechaCreacion)
                .build();

        notificacion.onCreate();

        assertThat(notificacion.getCreatedAt()).isEqualTo(fechaCreacion);
    }

    @Test
    @DisplayName("equals() con mismo ID → retorna true")
    void equals_mismoId_retornaTrue() {
        Notificacion notif1 = Notificacion.builder().notificacionId(1L).build();
        Notificacion notif2 = Notificacion.builder().notificacionId(1L).build();

        assertThat(notif1).isEqualTo(notif2);
    }

    @Test
    @DisplayName("equals() con diferente ID → retorna false")
    void equals_diferenteId_retornaFalse() {
        Notificacion notif1 = Notificacion.builder().notificacionId(1L).build();
        Notificacion notif2 = Notificacion.builder().notificacionId(2L).build();

        assertThat(notif1).isNotEqualTo(notif2);
    }

    @Test
    @DisplayName("equals() con ID null → retorna false")
    void equals_idNull_retornaFalse() {
        Notificacion notif1 = Notificacion.builder().notificacionId(null).build();
        Notificacion notif2 = Notificacion.builder().notificacionId(1L).build();

        assertThat(notif1).isNotEqualTo(notif2);
    }

    @Test
    @DisplayName("hashCode() es consistente")
    void hashCode_esConsistente() {
        Notificacion notificacion = Notificacion.builder().notificacionId(1L).build();

        int hashCode1 = notificacion.hashCode();
        int hashCode2 = notificacion.hashCode();

        assertThat(hashCode1).isEqualTo(hashCode2);
    }

    // Helper method para setear campos privados usando reflection
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
