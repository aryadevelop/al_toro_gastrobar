package co.edu.unicauca.backend.modules.usuarios.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class EmpleadoEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        Empleado e = Empleado.builder()
                .usuarioId(1L)
                .empleadoNombre("Maria Lopez")
                .empleadoTelefono("3109876543")
                .empleadoFechaIngreso(LocalDate.of(2024, 3, 15))
                .build();

        assertThat(e.getUsuarioId()).isEqualTo(1L);
        assertThat(e.getEmpleadoNombre()).isEqualTo("Maria Lopez");
        assertThat(e.getEmpleadoTelefono()).isEqualTo("3109876543");
        assertThat(e.getEmpleadoFechaIngreso()).isEqualTo(LocalDate.of(2024, 3, 15));
    }

    @Test
    void setter_actualizaNombre() {
        Empleado e = new Empleado();
        e.setEmpleadoNombre("Carlos Ruiz");
        assertThat(e.getEmpleadoNombre()).isEqualTo("Carlos Ruiz");
    }

    @Test
    void equals_mismoId_retornaTrue() {
        Empleado a = Empleado.builder().usuarioId(1L).build();
        Empleado b = Empleado.builder().usuarioId(1L).build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_diferenteId_retornaFalse() {
        Empleado a = Empleado.builder().usuarioId(1L).build();
        Empleado b = Empleado.builder().usuarioId(2L).build();
        assertThat(a).isNotEqualTo(b);
    }
}
