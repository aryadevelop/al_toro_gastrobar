package co.edu.unicauca.backend.modules.reservas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.reservas.dto.response.DecoracionDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ZonaDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.DecoracionZona;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ReservaMapperTest {

    @Mock
    PreOrdenMapper preOrdenMapper;

    @InjectMocks
    ReservaMapper mapper;

    private Reserva reservaBase() {
        return Reserva.builder()
                .reservaId(1L)
                .reservaFechaHoraLlegada(LocalDateTime.now().plusDays(5))
                .reservaNumeroPersonas(4)
                .reservaEstado(EstadoReserva.PENDIENTE)
                .reservaTipo(TipoReserva.BASICA)
                .build();
    }

    @Test
    @DisplayName("sinDisponibilidad → disponible=false, listas vacías")
    void sinDisponibilidad_retornaResponseVacio() {
        DisponibilidadResponse resp = mapper.sinDisponibilidad();

        assertThat(resp.getDisponible()).isFalse();
        assertThat(resp.getZonas()).isEmpty();
        assertThat(resp.getDecoraciones()).isEmpty();
    }

    @Test
    @DisplayName("toZonaDto → mapea nombre y capacidad")
    void toZonaDto_mapeaCampos() {
        Zona zona = Zona.builder()
                .zonaId(1L)
                .zonaNombre("Terraza")
                .zonaCapacidadPersonas(20)
                .build();

        ZonaDisponibleResponse resp = mapper.toZonaDto(zona);

        assertThat(resp.getZonaId()).isEqualTo(1L);
        assertThat(resp.getNombre()).isEqualTo("Terraza");
        assertThat(resp.getCapacidad()).isEqualTo(20);
    }

    @Test
    @DisplayName("toDecoracionDto → decoración con zonas libres muestra zonas compatibles")
    void toDecoracionDto_decoracionConZonasLibres_tieneZonasCompatibles() {
        Decoracion dec = Decoracion.builder()
                .decoracionId(1L)
                .decoracionNombre("Globos")
                .decoracionEstado(EstadoGenerico.ACTIVO)
                .decoracionCostoAdicional(BigDecimal.valueOf(20000))
                .build();
        Zona zona = Zona.builder().zonaId(1L).zonaNombre("Terraza").build();
        DecoracionZona dz = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(1L)
                .zona(zona)
                .build();

        // Two links → puedeSeleccionarZona = true; zona 1L is in idsZonasLibres
        DecoracionZona dz2 = DecoracionZona.builder()
                .decoracionId(1L)
                .zonaId(2L)
                .build();

        DecoracionDisponibleResponse resp = mapper.toDecoracionDto(dec, List.of(dz, dz2), Set.of(1L));

        assertThat(resp.getDecoracionId()).isEqualTo(1L);
        assertThat(resp.getNombre()).isEqualTo("Globos");
        assertThat(resp.getPuedeSeleccionarZona()).isTrue();
        assertThat(resp.getZonaIdsCompatibles()).containsExactly(1L);
    }

    @Test
    @DisplayName("toDecoracionDto → decoración sin zonas libres no tiene zonas compatibles")
    void toDecoracionDto_sinZonasLibres_listaCompatiblesVacia() {
        Decoracion dec = Decoracion.builder()
                .decoracionId(2L)
                .decoracionNombre("Flores")
                .decoracionEstado(EstadoGenerico.ACTIVO)
                .build();
        DecoracionZona dz1 = DecoracionZona.builder().decoracionId(2L).zonaId(3L).build();
        DecoracionZona dz2 = DecoracionZona.builder().decoracionId(2L).zonaId(4L).build();

        DecoracionDisponibleResponse resp = mapper.toDecoracionDto(dec, List.of(dz1, dz2), Set.of());

        assertThat(resp.getZonaIdsCompatibles()).isEmpty();
    }

    @Test
    @DisplayName("toResumen → mapea campos básicos")
    void toResumen_mapeaCamposBasicos() {
        Reserva reserva = reservaBase();

        ReservaDetalleResponse resp = mapper.toResumen(reserva);

        assertThat(resp.getReservaId()).isEqualTo(1L);
        assertThat(resp.getNumeroPersonas()).isEqualTo(4);
    }

    @Test
    @DisplayName("toDetalleResponse → sin items ni abonos")
    void toDetalleResponse_sinItemsNiAbonos() {
        ReservaDetalleResponse resp = mapper.toDetalleResponse(reservaBase(), List.of(), List.of());

        assertThat(resp.getReservaId()).isEqualTo(1L);
    }
}
