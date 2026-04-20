package co.edu.unicauca.backend.modules.mesas_comandas.entity;

import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MesaEntityTest {

    @Test
    void builder_creaInstanciaConCamposEsperados() {
        Mesa m = Mesa.builder()
                .visitaId(1L)
                .mesaIdentificador("T1")
                .mesaNumeroPersonas(4)
                .mesaEstado(EstadoMesa.ATENDIDA)
                .build();

        assertThat(m.getVisitaId()).isEqualTo(1L);
        assertThat(m.getMesaIdentificador()).isEqualTo("T1");
        assertThat(m.getMesaNumeroPersonas()).isEqualTo(4);
        assertThat(m.getMesaEstado()).isEqualTo(EstadoMesa.ATENDIDA);
    }

    @Test
    void setter_actualizaIdentificador() {
        Mesa m = new Mesa();
        m.setMesaIdentificador("T2");
        assertThat(m.getMesaIdentificador()).isEqualTo("T2");
    }

    @Test
    void equals_mismoId_retornaTrue() {
        Mesa a = Mesa.builder().visitaId(1L).build();
        Mesa b = Mesa.builder().visitaId(1L).build();
        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_diferenteId_retornaFalse() {
        Mesa a = Mesa.builder().visitaId(1L).build();
        Mesa b = Mesa.builder().visitaId(2L).build();
        assertThat(a).isNotEqualTo(b);
    }
}
