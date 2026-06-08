package co.edu.unicauca.backend.modules.reservas.entity;

import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Reserva Entity Tests")
class ReservaTest {

    @Test
    @DisplayName("onCreate → llama super.onCreate() y asigna reservaFechaCreacion si es null")
    void onCreate_llamaSuperOnCreate_asignaCamposAuditablesYFechaCreacion() throws Exception {
        Reserva entity = Reserva.builder()
                .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(1))
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();

        // Asegurar que los campos estén null antes de onCreate
        setPrivateField(entity, "createdAt", null);
        setPrivateField(entity, "updatedAt", null);
        setPrivateField(entity, "reservaFechaCreacion", null);

        Method onCreate = entity.getClass().getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);

        // Verificar que super.onCreate() asignó createdAt/updatedAt
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();

        // Verificar que reservaFechaCreacion fue asignada
        assertThat(entity.getReservaFechaCreacion()).isNotNull();
        assertThat(entity.getReservaFechaCreacion()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("onCreate → reservaFechaCreacion establecida → mantiene fecha")
    void onCreate_reservaFechaCreacionEstablecida_mantieneFecha() throws Exception {
        LocalDateTime fechaPrevia = LocalDateTime.of(2026, 1, 1, 12, 0);
        Reserva entity = Reserva.builder()
                .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(1))
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .reservaFechaCreacion(fechaPrevia)
                .build();

        Method onCreate = entity.getClass().getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(entity);

        assertThat(entity.getReservaFechaCreacion()).isEqualTo(fechaPrevia);
    }

    @Test
    @DisplayName("equals → mismo ID → retorna true")
    void equals_mismoId_retornaTrue() throws Exception {
        Reserva entity1 = Reserva.builder()
                .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(1))
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();
        Reserva entity2 = Reserva.builder()
                .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(2))
                .reservaNumeroPersonas(2)
                .reservaEstado(EstadoReserva.CONFIRMADA)
                .reservaTipo(TipoReserva.ESPECIAL)
                .build();

        setPrivateField(entity1, "reservaId", 1L);
        setPrivateField(entity2, "reservaId", 1L);

        assertThat(entity1).isEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → diferente ID → retorna false")
    void equals_diferenteId_retornaFalse() throws Exception {
        Reserva entity1 = Reserva.builder()
                .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(1))
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();
        Reserva entity2 = Reserva.builder()
                .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(1))
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();

        setPrivateField(entity1, "reservaId", 1L);
        setPrivateField(entity2, "reservaId", 2L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → ID null → retorna false")
    void equals_idNull_retornaFalse() throws Exception {
        Reserva entity1 = Reserva.builder()
                .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(1))
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();
        Reserva entity2 = Reserva.builder()
                .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(1))
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();

        setPrivateField(entity1, "reservaId", null);
        setPrivateField(entity2, "reservaId", 1L);

        assertThat(entity1).isNotEqualTo(entity2);
    }

    @Test
    @DisplayName("equals → misma instancia → retorna true")
    void equals_mismaInstancia_retornaTrue() {
        Reserva entity = Reserva.builder()
                .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(1))
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();

        assertThat(entity).isEqualTo(entity);
    }

    @Test
    @DisplayName("equals → tipo diferente → retorna false")
    void equals_tipoDiferente_retornaFalse() {
        Reserva entity = Reserva.builder()
                .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(1))
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();

        assertThat(entity).isNotEqualTo("not a Reserva");
    }

    @Test
    @DisplayName("hashCode → es consistente con la clase")
    void hashCode_esConsistente() {
        Reserva entity1 = Reserva.builder()
                .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(1))
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();
        Reserva entity2 = Reserva.builder()
                .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(2))
                .reservaNumeroPersonas(2)
                .reservaEstado(EstadoReserva.CONFIRMADA)
                .reservaTipo(TipoReserva.ESPECIAL)
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
