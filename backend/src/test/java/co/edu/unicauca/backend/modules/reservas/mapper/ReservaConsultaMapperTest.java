package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaConsultaResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para {@link ReservaConsultaMapper}.
 */
class ReservaConsultaMapperTest {

    private ReservaConsultaMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ReservaConsultaMapper();
    }

    @Test
    void toConsultaResponse_conZonaYDecoracion_retornaDTOCompleto() {
        // Given
        Usuario usuario = Usuario.builder()
                .usuarioId(1L)
                .usuarioEmail("cliente@altoro.com")
                .build();

        Cliente cliente = Cliente.builder()
                .usuarioId(1L)
                .usuario(usuario)
                .clienteNombre("Juan Pérez")
                .build();

        Zona zona = Zona.builder()
                .zonaId(10L)
                .zonaNombre("Zona VIP")
                .build();

        Decoracion decoracion = Decoracion.builder()
                .decoracionId(5L)
                .decoracionNombre("Romántica")
                .build();

        Reserva reserva = Reserva.builder()
                .reservaId(100L)
                .cliente(cliente)
                .zona(zona)
                .decoracion(decoracion)
                .reservaFechaHoraLlegada(LocalDateTime.of(2026, 4, 25, 19, 30))
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.CONFIRMADA)
                .reservaTipo(TipoReserva.ESPECIAL)
                .build();

        // When
        ReservaConsultaResponse response = mapper.toConsultaResponse(reserva);

        // Then
        assertThat(response.getReservaId()).isEqualTo(100L);
        assertThat(response.getClienteNombre()).isEqualTo("Juan Pérez");
        assertThat(response.getZonaId()).isEqualTo(10L);
        assertThat(response.getZonaNombre()).isEqualTo("Zona VIP");
        assertThat(response.getDecoracionNombre()).isEqualTo("Romántica");
        assertThat(response.getHoraLlegada()).isEqualTo("19:30");
        assertThat(response.getNumeroPersonas()).isEqualTo(4);
        assertThat(response.getEstado()).isEqualTo("CONFIRMADA");
    }

    @Test
    void toConsultaResponse_sinZonaNiDecoracion_retornaNull() {
        // Given
        Usuario usuario = Usuario.builder()
                .usuarioId(2L)
                .usuarioEmail("cliente2@altoro.com")
                .build();

        Cliente cliente = Cliente.builder()
                .usuarioId(2L)
                .usuario(usuario)
                .clienteNombre("María López")
                .build();

        Reserva reserva = Reserva.builder()
                .reservaId(101L)
                .cliente(cliente)
                .zona(null)
                .decoracion(null)
                .reservaFechaHoraLlegada(LocalDateTime.of(2026, 4, 25, 20, 0))
                .reservaNumeroPersonas(2)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();

        // When
        ReservaConsultaResponse response = mapper.toConsultaResponse(reserva);

        // Then
        assertThat(response.getReservaId()).isEqualTo(101L);
        assertThat(response.getClienteNombre()).isEqualTo("María López");
        assertThat(response.getZonaId()).isNull();
        assertThat(response.getZonaNombre()).isNull();
        assertThat(response.getDecoracionNombre()).isNull();
        assertThat(response.getHoraLlegada()).isEqualTo("20:00");
        assertThat(response.getNumeroPersonas()).isEqualTo(2);
        assertThat(response.getEstado()).isEqualTo("PENDIENTE");
    }
}
