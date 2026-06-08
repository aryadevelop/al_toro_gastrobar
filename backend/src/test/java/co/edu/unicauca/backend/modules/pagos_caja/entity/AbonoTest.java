package co.edu.unicauca.backend.modules.pagos_caja.entity;

import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Abono - Entity Tests")
class AbonoTest {

    @Test
    @DisplayName("onCreate() con abonoFechaHora null → asigna LocalDateTime.now()")
    void onCreate_abonoFechaHoraNull_asignaFechaActual() throws Exception {
        Abono abono = Abono.builder()
                .cajero(Empleado.builder().build())
                .reserva(Reserva.builder().build())
                .abonoMonto(new BigDecimal("50000.00"))
                .abonoMetodo(MetodoPago.EFECTIVO)
                .abonoTipo(TipoAbono.ANTICIPO)
                .build();

        // Forzar abonoFechaHora a null usando reflection
        setPrivateField(abono, "abonoFechaHora", null);

        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);
        abono.onCreate();
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);

        assertThat(abono.getAbonoFechaHora()).isNotNull();
        assertThat(abono.getAbonoFechaHora()).isAfterOrEqualTo(antes);
        assertThat(abono.getAbonoFechaHora()).isBeforeOrEqualTo(despues);
    }

    @Test
    @DisplayName("onCreate() con abonoFechaHora establecida → mantiene valor original")
    void onCreate_abonoFechaHoraEstablecida_mantieneFecha() throws Exception {
        LocalDateTime fechaEspecifica = LocalDateTime.of(2026, 1, 15, 14, 30, 0);

        Abono abono = Abono.builder()
                .cajero(Empleado.builder().build())
                .reserva(Reserva.builder().build())
                .abonoMonto(new BigDecimal("100000.00"))
                .abonoFechaHora(fechaEspecifica)
                .abonoMetodo(MetodoPago.TARJETA)
                .abonoTipo(TipoAbono.ANTICIPO)
                .build();

        abono.onCreate();

        assertThat(abono.getAbonoFechaHora()).isEqualTo(fechaEspecifica);
    }

    @Test
    @DisplayName("onCreate() con createdAt null → asigna LocalDateTime.now()")
    void onCreate_createdAtNull_asignaFechaActual() throws Exception {
        Abono abono = Abono.builder()
                .cajero(Empleado.builder().build())
                .reserva(Reserva.builder().build())
                .abonoMonto(new BigDecimal("75000.00"))
                .abonoMetodo(MetodoPago.EFECTIVO)
                .abonoTipo(TipoAbono.DEVOLUCION)
                .build();

        // Forzar createdAt a null usando reflection
        setPrivateField(abono, "createdAt", null);

        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);
        abono.onCreate();
        LocalDateTime despues = LocalDateTime.now().plusSeconds(1);

        assertThat(abono.getCreatedAt()).isNotNull();
        assertThat(abono.getCreatedAt()).isAfterOrEqualTo(antes);
        assertThat(abono.getCreatedAt()).isBeforeOrEqualTo(despues);
    }

    @Test
    @DisplayName("onCreate() con createdAt establecido → mantiene valor original")
    void onCreate_createdAtEstablecido_mantieneFecha() throws Exception {
        LocalDateTime fechaCreacion = LocalDateTime.of(2026, 2, 1, 10, 0, 0);

        Abono abono = Abono.builder()
                .cajero(Empleado.builder().build())
                .reserva(Reserva.builder().build())
                .abonoMonto(new BigDecimal("50000.00"))
                .abonoMetodo(MetodoPago.TRANSFERENCIA)
                .abonoTipo(TipoAbono.ANTICIPO)
                .createdAt(fechaCreacion)
                .build();

        abono.onCreate();

        assertThat(abono.getCreatedAt()).isEqualTo(fechaCreacion);
    }

    @Test
    @DisplayName("equals() con mismo ID → retorna true")
    void equals_mismoId_retornaTrue() {
        Abono abono1 = Abono.builder().abonoId(1L).build();
        Abono abono2 = Abono.builder().abonoId(1L).build();

        assertThat(abono1).isEqualTo(abono2);
    }

    @Test
    @DisplayName("equals() con diferente ID → retorna false")
    void equals_diferenteId_retornaFalse() {
        Abono abono1 = Abono.builder().abonoId(1L).build();
        Abono abono2 = Abono.builder().abonoId(2L).build();

        assertThat(abono1).isNotEqualTo(abono2);
    }

    @Test
    @DisplayName("equals() misma referencia → retorna true")
    void equals_mismaReferencia_retornaTrue() {
        Abono abono = Abono.builder().abonoId(1L).build();

        assertThat(abono.equals(abono)).isTrue();
    }

    @Test
    @DisplayName("equals() con objeto de distinto tipo → retorna false")
    @SuppressWarnings("unlikely-arg-type")
    void equals_tipoDistinto_retornaFalse() {
        Abono abono = Abono.builder().abonoId(1L).build();

        assertThat(abono.equals("no soy un Abono")).isFalse();
    }

    @Test
    @DisplayName("equals() con ID null → retorna false")
    void equals_idNull_retornaFalse() {
        Abono abono1 = Abono.builder().abonoId(null).build();
        Abono abono2 = Abono.builder().abonoId(1L).build();

        assertThat(abono1).isNotEqualTo(abono2);
    }

    @Test
    @DisplayName("hashCode() es consistente")
    void hashCode_esConsistente() {
        Abono abono = Abono.builder().abonoId(1L).build();

        int hashCode1 = abono.hashCode();
        int hashCode2 = abono.hashCode();

        assertThat(hashCode1).isEqualTo(hashCode2);
    }

    // Helper method para setear campos privados usando reflection
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
