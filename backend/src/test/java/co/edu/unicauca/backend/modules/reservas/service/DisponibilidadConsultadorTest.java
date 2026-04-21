package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.reservas.dto.response.DecoracionDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ZonaDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionZonaRepository;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link DisponibilidadConsultador}.
 *
 * <p>Cubre el flujo interno de {@code consultarDisponibilidadInterna} accedido a través
 * de los dos métodos públicos: {@code consultarParaNuevaReserva} y {@code consultarParaModificacion}.
 */
@ExtendWith(MockitoExtension.class)
class DisponibilidadConsultadorTest {

    // ── Constantes de prueba ──────────────────────────────────────────────────

    private static final LocalTime APERTURA = LocalTime.of(12, 0);
    private static final LocalTime CIERRE   = LocalTime.of(22, 0);
    private static final LocalDateTime FECHA_HORA = LocalDateTime.of(2026, 6, 15, 19, 0);

    // ── Mocks ─────────────────────────────────────────────────────────────────

    @Mock
    ReservaValidador validador;

    @Mock
    ReservaMapper reservaMapper;

    @Mock
    ReservaRepository reservaRepository;

    @Mock
    ZonaRepository zonaRepository;

    @Mock
    DecoracionRepository decoracionRepository;

    @Mock
    DecoracionZonaRepository decoracionZonaRepository;

    @InjectMocks
    DisponibilidadConsultador consultador;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private final DisponibilidadResponse SIN_DISP =
            DisponibilidadResponse.builder().disponible(false).build();

    /** Configura el validador para que el horario sea válido y no haya bloqueo. */
    private void mockValidadorOk() {
        when(validador.esHorarioValido(any(), any(), any())).thenReturn(true);
        when(validador.estaBloqueda(any())).thenReturn(false);
    }

    /** Crea una zona con el id y capacidad dados. */
    private Zona zonaConCapacidad(Long id, int capacidad) {
        return Zona.builder()
                .zonaId(id)
                .zonaNombre("Zona " + id)
                .zonaCapacidadPersonas(capacidad)
                .build();
    }

    /** Crea una decoracion activa con el id dado. */
    private Decoracion decoracionActiva(Long id) {
        return Decoracion.builder()
                .decoracionId(id)
                .decoracionNombre("Decoracion " + id)
                .decoracionEstado(EstadoGenerico.ACTIVO)
                .build();
    }

    /** Crea una lista de filas Object[] como la que devuelven las queries de personas por zona. */
    private List<Object[]> filasZona(Object[]... filas) {
        return java.util.Arrays.asList(filas);
    }

    /** Crea una fila Object[] como la que devuelven las queries de personas por zona. */
    private Object[] filaZona(Long zonaId, int personas) {
        return new Object[]{zonaId, (Number) personas};
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Grupo: consultarParaNuevaReserva
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("consultarParaNuevaReserva")
    class ConsultarParaNuevaReserva {

        @Test
        @DisplayName("Hora fuera de rango → retorna sinDisponibilidad")
        void horaFueraDeRango_retornaSinDisponibilidad() {
            when(validador.esHorarioValido(any(), any(), any())).thenReturn(false);
            when(reservaMapper.sinDisponibilidad()).thenReturn(SIN_DISP);

            DisponibilidadResponse result =
                    consultador.consultarParaNuevaReserva(FECHA_HORA, 2, APERTURA, CIERRE);

            assertThat(result.getDisponible()).isFalse();
        }

        @Test
        @DisplayName("Hora dentro del rango con bloqueo → retorna sinDisponibilidad")
        void horaDentroRangoConBloqueo_retornaSinDisponibilidad() {
            when(validador.esHorarioValido(any(), any(), any())).thenReturn(true);
            when(validador.estaBloqueda(any())).thenReturn(true);
            when(reservaMapper.sinDisponibilidad()).thenReturn(SIN_DISP);

            DisponibilidadResponse result =
                    consultador.consultarParaNuevaReserva(FECHA_HORA, 2, APERTURA, CIERRE);

            assertThat(result.getDisponible()).isFalse();
        }

        @Test
        @DisplayName("Sin zonas registradas → retorna sinDisponibilidad")
        void sinZonasRegistradas_retornaSinDisponibilidad() {
            mockValidadorOk();
            when(zonaRepository.findAll()).thenReturn(List.of());
            when(reservaMapper.sinDisponibilidad()).thenReturn(SIN_DISP);

            DisponibilidadResponse result =
                    consultador.consultarParaNuevaReserva(FECHA_HORA, 2, APERTURA, CIERRE);

            assertThat(result.getDisponible()).isFalse();
        }

        @Test
        @DisplayName("Todas las zonas llenas → retorna sinDisponibilidad")
        void todasLasZonasLlenas_retornaSinDisponibilidad() {
            mockValidadorOk();
            Zona zona = zonaConCapacidad(1L, 2);
            when(zonaRepository.findAll()).thenReturn(List.of(zona));
            // 2 personas reservadas = capacidad máxima → zona llena
            when(reservaRepository.findPersonasPorZonaEnDia(any(), any(), anyList()))
                    .thenReturn(filasZona(filaZona(1L, 2)));
            when(reservaMapper.sinDisponibilidad()).thenReturn(SIN_DISP);

            DisponibilidadResponse result =
                    consultador.consultarParaNuevaReserva(FECHA_HORA, 2, APERTURA, CIERRE);

            assertThat(result.getDisponible()).isFalse();
        }

        @Test
        @DisplayName("Zona con cupo, sin decoraciones → retorna disponible con zona")
        void zonaConCupo_sinDecoraciones_retornaDisponible() {
            mockValidadorOk();
            Zona zona = zonaConCapacidad(1L, 10);
            when(zonaRepository.findAll()).thenReturn(List.of(zona));
            when(reservaRepository.findPersonasPorZonaEnDia(any(), any(), anyList()))
                    .thenReturn(List.of()); // sin reservas
            when(reservaRepository.findDecoracionesOcupadasEnDia(any(), any(), anyList()))
                    .thenReturn(List.of());
            when(decoracionRepository.findByDecoracionEstado(EstadoGenerico.ACTIVO))
                    .thenReturn(List.of());
            ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder()
                    .zonaId(1L).nombre("Zona 1").capacidad(10).build();
            when(reservaMapper.toZonaDto(zona)).thenReturn(zonaDto);

            DisponibilidadResponse result =
                    consultador.consultarParaNuevaReserva(FECHA_HORA, 2, APERTURA, CIERRE);

            assertThat(result.getDisponible()).isTrue();
            assertThat(result.getZonas()).isNotEmpty();
            assertThat(result.getDecoraciones()).isEmpty();
        }

        @Test
        @DisplayName("Zona con cupo, decoracion libre → retorna disponible con decoracion")
        void zonaConCupo_decoracionLibre_retornaDisponibleConDecoracion() {
            mockValidadorOk();
            Zona zona = zonaConCapacidad(1L, 10);
            Decoracion dec = decoracionActiva(10L);
            when(zonaRepository.findAll()).thenReturn(List.of(zona));
            when(reservaRepository.findPersonasPorZonaEnDia(any(), any(), anyList()))
                    .thenReturn(List.of());
            when(reservaRepository.findDecoracionesOcupadasEnDia(any(), any(), anyList()))
                    .thenReturn(List.of()); // decoracion no ocupada
            when(decoracionRepository.findByDecoracionEstado(EstadoGenerico.ACTIVO))
                    .thenReturn(List.of(dec));
            when(decoracionZonaRepository.findByDecoracionId(10L)).thenReturn(List.of());
            ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder()
                    .zonaId(1L).nombre("Zona 1").capacidad(10).build();
            DecoracionDisponibleResponse decDto = DecoracionDisponibleResponse.builder()
                    .decoracionId(10L).nombre("Decoracion 10").build();
            when(reservaMapper.toZonaDto(zona)).thenReturn(zonaDto);
            when(reservaMapper.toDecoracionDto(eq(dec), anyList(), any())).thenReturn(decDto);

            DisponibilidadResponse result =
                    consultador.consultarParaNuevaReserva(FECHA_HORA, 2, APERTURA, CIERRE);

            assertThat(result.getDisponible()).isTrue();
            assertThat(result.getDecoraciones()).hasSize(1);
            assertThat(result.getDecoraciones().get(0).getDecoracionId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Decoracion ocupada → no se incluye en la respuesta")
        void decoracionOcupada_noIncluida() {
            mockValidadorOk();
            Zona zona = zonaConCapacidad(1L, 10);
            Decoracion dec = decoracionActiva(10L);
            when(zonaRepository.findAll()).thenReturn(List.of(zona));
            when(reservaRepository.findPersonasPorZonaEnDia(any(), any(), anyList()))
                    .thenReturn(List.of());
            when(reservaRepository.findDecoracionesOcupadasEnDia(any(), any(), anyList()))
                    .thenReturn(List.of(10L)); // id 10 ocupado
            when(decoracionRepository.findByDecoracionEstado(EstadoGenerico.ACTIVO))
                    .thenReturn(List.of(dec));
            ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder()
                    .zonaId(1L).nombre("Zona 1").capacidad(10).build();
            when(reservaMapper.toZonaDto(zona)).thenReturn(zonaDto);

            DisponibilidadResponse result =
                    consultador.consultarParaNuevaReserva(FECHA_HORA, 2, APERTURA, CIERRE);

            assertThat(result.getDisponible()).isTrue();
            assertThat(result.getDecoraciones()).isEmpty();
        }

        @Test
        @DisplayName("Sin excludeReservaId → usa query normal, no la excluyente")
        void sinExcluirId_usaQueryNormal() {
            mockValidadorOk();
            Zona zona = zonaConCapacidad(1L, 10);
            when(zonaRepository.findAll()).thenReturn(List.of(zona));
            when(reservaRepository.findPersonasPorZonaEnDia(any(), any(), anyList()))
                    .thenReturn(List.of());
            when(reservaRepository.findDecoracionesOcupadasEnDia(any(), any(), anyList()))
                    .thenReturn(List.of());
            when(decoracionRepository.findByDecoracionEstado(EstadoGenerico.ACTIVO))
                    .thenReturn(List.of());
            when(reservaMapper.toZonaDto(any())).thenReturn(
                    ZonaDisponibleResponse.builder().zonaId(1L).nombre("Zona 1").capacidad(10).build());

            consultador.consultarParaNuevaReserva(FECHA_HORA, 2, APERTURA, CIERRE);

            verify(reservaRepository).findPersonasPorZonaEnDia(any(), any(), anyList());
            verify(reservaRepository, never()).findPersonasPorZonaEnDiaExcluyendo(any(), any(), anyList(), anyLong());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Grupo: consultarParaModificacion
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("consultarParaModificacion")
    class ConsultarParaModificacion {

        @Test
        @DisplayName("Con excludeReservaId → usa query excluyente, no la normal")
        void usaQueryExcluyendo_cuandoExcludeReservaIdDistintoDeNull() {
            mockValidadorOk();
            Zona zona = zonaConCapacidad(1L, 10);
            when(zonaRepository.findAll()).thenReturn(List.of(zona));
            when(reservaRepository.findPersonasPorZonaEnDiaExcluyendo(any(), any(), anyList(), eq(99L)))
                    .thenReturn(List.of());
            when(reservaRepository.findDecoracionesOcupadasEnDiaExcluyendo(any(), any(), anyList(), eq(99L)))
                    .thenReturn(List.of());
            when(decoracionRepository.findByDecoracionEstado(EstadoGenerico.ACTIVO))
                    .thenReturn(List.of());
            when(reservaMapper.toZonaDto(any())).thenReturn(
                    ZonaDisponibleResponse.builder().zonaId(1L).nombre("Zona 1").capacidad(10).build());

            consultador.consultarParaModificacion(FECHA_HORA, 2, 99L, APERTURA, CIERRE);

            verify(reservaRepository).findPersonasPorZonaEnDiaExcluyendo(any(), any(), anyList(), eq(99L));
            verify(reservaRepository, never()).findPersonasPorZonaEnDia(any(), any(), anyList());
        }

        @Test
        @DisplayName("Decoracion libre con exclusion → se incluye en la respuesta")
        void decoracionLibreConExclusion_seIncluye() {
            mockValidadorOk();
            Zona zona = zonaConCapacidad(1L, 10);
            Decoracion dec = decoracionActiva(20L);
            when(zonaRepository.findAll()).thenReturn(List.of(zona));
            when(reservaRepository.findPersonasPorZonaEnDiaExcluyendo(any(), any(), anyList(), eq(5L)))
                    .thenReturn(List.of());
            // Decoracion 20 NO aparece en el resultado de excluyendo → está libre
            when(reservaRepository.findDecoracionesOcupadasEnDiaExcluyendo(any(), any(), anyList(), eq(5L)))
                    .thenReturn(List.of());
            when(decoracionRepository.findByDecoracionEstado(EstadoGenerico.ACTIVO))
                    .thenReturn(List.of(dec));
            when(decoracionZonaRepository.findByDecoracionId(20L)).thenReturn(List.of());
            DecoracionDisponibleResponse decDto = DecoracionDisponibleResponse.builder()
                    .decoracionId(20L).nombre("Decoracion 20").build();
            when(reservaMapper.toZonaDto(any())).thenReturn(
                    ZonaDisponibleResponse.builder().zonaId(1L).nombre("Zona 1").capacidad(10).build());
            when(reservaMapper.toDecoracionDto(eq(dec), anyList(), any())).thenReturn(decDto);

            DisponibilidadResponse result =
                    consultador.consultarParaModificacion(FECHA_HORA, 2, 5L, APERTURA, CIERRE);

            assertThat(result.getDisponible()).isTrue();
            assertThat(result.getDecoraciones()).hasSize(1);
            assertThat(result.getDecoraciones().get(0).getDecoracionId()).isEqualTo(20L);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Grupo: límites
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("límites")
    class Limites {

        @Test
        @DisplayName("Zona sin ninguna reservacion (sin entrada en query) → tiene cupo (default 0)")
        void zonaConCeroReservaciones_defaultsA0() {
            mockValidadorOk();
            // Zona con capacidad=5 y zonaId=7L, pero la query no devuelve ninguna fila para ella
            Zona zona = zonaConCapacidad(7L, 5);
            when(zonaRepository.findAll()).thenReturn(List.of(zona));
            when(reservaRepository.findPersonasPorZonaEnDia(any(), any(), anyList()))
                    .thenReturn(List.of()); // sin filas → getOrDefault(7L, 0) == 0
            when(reservaRepository.findDecoracionesOcupadasEnDia(any(), any(), anyList()))
                    .thenReturn(List.of());
            when(decoracionRepository.findByDecoracionEstado(EstadoGenerico.ACTIVO))
                    .thenReturn(List.of());
            ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder()
                    .zonaId(7L).nombre("Zona 7").capacidad(5).build();
            when(reservaMapper.toZonaDto(zona)).thenReturn(zonaDto);

            DisponibilidadResponse result =
                    consultador.consultarParaNuevaReserva(FECHA_HORA, 2, APERTURA, CIERRE);

            assertThat(result.getDisponible()).isTrue();
            assertThat(result.getZonas()).hasSize(1);
            assertThat(result.getZonas().get(0).getZonaId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("Dos zonas, una llena y una libre → solo la libre aparece en respuesta")
        void multipleZonas_cadaUnaMapeaCorrectamente() {
            mockValidadorOk();
            // zona1 cap=2, con 2 personas → LLENA
            Zona zona1 = zonaConCapacidad(1L, 2);
            // zona2 cap=5, con 1 persona → LIBRE
            Zona zona2 = zonaConCapacidad(2L, 5);
            when(zonaRepository.findAll()).thenReturn(List.of(zona1, zona2));
            when(reservaRepository.findPersonasPorZonaEnDia(any(), any(), anyList()))
                    .thenReturn(filasZona(filaZona(1L, 2), filaZona(2L, 1)));
            when(reservaRepository.findDecoracionesOcupadasEnDia(any(), any(), anyList()))
                    .thenReturn(List.of());
            when(decoracionRepository.findByDecoracionEstado(EstadoGenerico.ACTIVO))
                    .thenReturn(List.of());
            ZonaDisponibleResponse zona2Dto = ZonaDisponibleResponse.builder()
                    .zonaId(2L).nombre("Zona 2").capacidad(5).build();
            when(reservaMapper.toZonaDto(zona2)).thenReturn(zona2Dto);

            DisponibilidadResponse result =
                    consultador.consultarParaNuevaReserva(FECHA_HORA, 2, APERTURA, CIERRE);

            assertThat(result.getDisponible()).isTrue();
            assertThat(result.getZonas()).hasSize(1);
            assertThat(result.getZonas().get(0).getZonaId()).isEqualTo(2L);
        }
    }
}
