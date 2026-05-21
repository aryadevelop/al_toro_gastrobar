package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.modules.notificaciones.service.MesaWsPublisher;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MesaAsignarService.evaluarYActualizarEstadoMesa")
class MesaAsignarEvaluadorTest {

    @Mock MesaRepository mesaRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock NotificacionRepository notificacionRepository;
    @Mock MesaWsPublisher mesaWsPublisher;
    @InjectMocks MesaAsignarService service;

    private static final Long VISITA_ID = 1L;
    private static final List<EstadoComanda> ESTADOS_PRODUCCION =
            List.of(EstadoComanda.PENDIENTE, EstadoComanda.EN_PREPARACION, EstadoComanda.LISTO);

    private Mesa mesaEnEstado(EstadoMesa estado) {
        return Mesa.builder().visitaId(VISITA_ID).mesaEstado(estado).build();
    }

    @Nested
    @DisplayName("transiciona a ATENDIDA")
    class TransicionaAtendida {

        @Test
        @DisplayName("sin notificaciones activas y sin comandas en producción → mesa ATENDIDA y publica WS")
        void todasLasCondicionesCumplidas_transicionaAtendida() {
            Mesa mesa = mesaEnEstado(EstadoMesa.EN_PREPARACION);
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA)).thenReturn(false);
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA)).thenReturn(false);
            when(comandaRepository.existsByVisita_VisitaIdAndComandaEstadoIn(VISITA_ID, ESTADOS_PRODUCCION))
                    .thenReturn(false);
            when(mesaRepository.findById(VISITA_ID)).thenReturn(Optional.of(mesa));
            when(mesaRepository.save(mesa)).thenReturn(mesa);

            service.evaluarYActualizarEstadoMesa(VISITA_ID);

            assertThat(mesa.getMesaEstado()).isEqualTo(EstadoMesa.ATENDIDA);
            verify(mesaRepository).save(mesa);
            verify(mesaWsPublisher).publicarCambioEstadoMesa(VISITA_ID, EstadoMesa.ATENDIDA);
        }
    }

    @Nested
    @DisplayName("retorna sin cambios (early return por rama)")
    class NoTransiciona {

        @Test
        @DisplayName("rama 1 — hay PLATOS_LISTOS activas → no consulta más")
        void conPlatosListos_retornaTemprano() {
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA)).thenReturn(true);

            service.evaluarYActualizarEstadoMesa(VISITA_ID);

            verify(notificacionRepository, never()).existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA);
            verify(comandaRepository, never()).existsByVisita_VisitaIdAndComandaEstadoIn(any(), any());
            verify(mesaRepository, never()).save(any());
        }

        @Test
        @DisplayName("rama 2 — hay BEBIDAS_LISTAS activas → no consulta comandas")
        void conBebidasListas_retornaTemprano() {
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA)).thenReturn(false);
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA)).thenReturn(true);

            service.evaluarYActualizarEstadoMesa(VISITA_ID);

            verify(comandaRepository, never()).existsByVisita_VisitaIdAndComandaEstadoIn(any(), any());
            verify(mesaRepository, never()).save(any());
        }

        @Test
        @DisplayName("rama 3 — hay comandas en producción → no carga mesa")
        void conComandasEnProduccion_retornaTemprano() {
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA)).thenReturn(false);
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA)).thenReturn(false);
            when(comandaRepository.existsByVisita_VisitaIdAndComandaEstadoIn(VISITA_ID, ESTADOS_PRODUCCION))
                    .thenReturn(true);

            service.evaluarYActualizarEstadoMesa(VISITA_ID);

            verify(mesaRepository, never()).findById(any());
            verify(mesaRepository, never()).save(any());
        }

        @Test
        @DisplayName("rama 4 — mesa ya está ATENDIDA → idempotente, no re-guarda ni publica WS")
        void mesaYaAtendida_idempotente() {
            Mesa mesa = mesaEnEstado(EstadoMesa.ATENDIDA);
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA)).thenReturn(false);
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA)).thenReturn(false);
            when(comandaRepository.existsByVisita_VisitaIdAndComandaEstadoIn(VISITA_ID, ESTADOS_PRODUCCION))
                    .thenReturn(false);
            when(mesaRepository.findById(VISITA_ID)).thenReturn(Optional.of(mesa));

            service.evaluarYActualizarEstadoMesa(VISITA_ID);

            verify(mesaRepository, never()).save(any());
            verify(mesaWsPublisher, never()).publicarCambioEstadoMesa(any(), any());
        }

        @Test
        @DisplayName("mesa no encontrada en BD → lanza ResourceNotFoundException")
        void mesaInexistente_lanzaResourceNotFound() {
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA)).thenReturn(false);
            when(notificacionRepository.existsByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA)).thenReturn(false);
            when(comandaRepository.existsByVisita_VisitaIdAndComandaEstadoIn(VISITA_ID, ESTADOS_PRODUCCION))
                    .thenReturn(false);
            when(mesaRepository.findById(VISITA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.evaluarYActualizarEstadoMesa(VISITA_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
