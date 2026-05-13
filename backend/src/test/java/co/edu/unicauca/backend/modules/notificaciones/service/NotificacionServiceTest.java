package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.service.MesaAsignarService;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.AtenderCambioResponse;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.NotificacionAsistenciaResponse;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaAtendidaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.AsistenciaSolicitadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaProduccionEventoWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.TipoEventoProduccion;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NotificacionService")
class NotificacionServiceTest {

    @Mock VisitaRepository visitaRepository;
    @Mock MesaRepository mesaRepository;
    @Mock NotificacionRepository notificacionRepository;
    @Mock NotificacionWsPublisher wsPublisher;
    @Mock ComandaRepository comandaRepository;
    @Mock ComandaItemRepository comandaItemRepository;
    @Mock MesaAsignarService mesaAsignarService;
    @Mock MesaWsPublisher mesaWsPublisher;

    @InjectMocks NotificacionService notificacionService;

    private static final Long VISITA_ID = 10L;
    private static final String EMAIL = "cliente@test.com";

    private Visita visitaConCliente() {
        Usuario usuario = new Usuario();
        usuario.setUsuarioEmail(EMAIL);
        Cliente cliente = Cliente.builder().usuarioId(1L).clienteNombre("Juan").build();
        cliente.setUsuario(usuario);
        return Visita.builder().visitaId(VISITA_ID).cliente(cliente).build();
    }

    private Mesa mesaConMesero() {
        Empleado mesero = Empleado.builder().usuarioId(5L).build();
        return Mesa.builder().visitaId(VISITA_ID).mesaIdentificador("T-01").mesero(mesero).build();
    }

    private Comanda comandaListo(EstacionComanda estacion) {
        return Comanda.builder()
                .comandaId(80L)
                .comandaEstacion(estacion)
                .comandaEstado(EstadoComanda.LISTO)
                .build();
    }

    private Notificacion notificacionConComanda(TipoNotificacion tipo,
                                                EstadoNotificacion estado,
                                                Comanda comanda) {
        return Notificacion.builder()
                .notificacionId(50L)
                .mesa(mesaConMesero())
                .empleado(mesaConMesero().getMesero())
                .notificacionTipo(tipo)
                .notificacionEstado(estado)
                .comanda(comanda)
                .build();
    }

    @Nested
    @DisplayName("solicitarAsistencia")
    class SolicitarAsistencia {

        @Test
        @DisplayName("crea notificación ACTIVA y publica broadcast WS")
        void creaNotificacionYPublicaBroadcast() {
            Visita visita = visitaConCliente();
            Mesa mesa = mesaConMesero();

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.of(mesa));
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.empty());
            Notificacion saved = Notificacion.builder().notificacionId(50L)
                    .notificacionEstado(EstadoNotificacion.ACTIVA)
                    .notificacionTipo(TipoNotificacion.ATENCION)
                    .mesa(mesa).empleado(mesa.getMesero()).build();
            when(notificacionRepository.save(any())).thenReturn(saved);

            NotificacionAsistenciaResponse res = notificacionService.solicitarAsistencia(VISITA_ID, EMAIL);

            assertThat(res.getNotificacionId()).isEqualTo(50L);
            assertThat(res.getEstado()).isEqualTo("ACTIVA");

            ArgumentCaptor<AsistenciaSolicitadaWsMessage> captor =
                    ArgumentCaptor.forClass(AsistenciaSolicitadaWsMessage.class);
            verify(wsPublisher).publicarAsistenciaSolicitada(captor.capture());
            assertThat(captor.getValue().getVisitaId()).isEqualTo(VISITA_ID);
            assertThat(captor.getValue().getMesaIdentificador()).isEqualTo("T-01");
        }

        @Test
        @DisplayName("lanza BusinessException si ya hay solicitud activa")
        void lanzaExcepcionSiYaHaySolicitudActiva() {
            Visita visita = visitaConCliente();
            Mesa mesa = mesaConMesero();
            Notificacion activa = Notificacion.builder().notificacionId(1L)
                    .notificacionEstado(EstadoNotificacion.ACTIVA).build();

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.of(mesa));
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.of(activa));

            assertThatThrownBy(() -> notificacionService.solicitarAsistencia(VISITA_ID, EMAIL))
                    .isInstanceOf(BusinessException.class);
            verify(notificacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza BusinessException si el cliente no es dueño de la visita")
        void lanzaExcepcionSiClienteNoEsDueno() {
            Visita visita = visitaConCliente();
            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));

            assertThatThrownBy(() -> notificacionService.solicitarAsistencia(VISITA_ID, "otro@test.com"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("lanza BusinessException ACCESS_DENIED si la visita no tiene cliente")
        void lanzaExcepcionSinCliente() {
            Visita visita = Visita.builder().visitaId(VISITA_ID).cliente(null).build();
            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));

            assertThatThrownBy(() -> notificacionService.solicitarAsistencia(VISITA_ID, EMAIL))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo("AUTH-002"));
        }

        @Test
        @DisplayName("lanza BusinessException si la visita no tiene mesa asignada")
        void lanzaExcepcionSinMesaAsignada() {
            Visita visita = visitaConCliente();

            when(visitaRepository.findById(VISITA_ID)).thenReturn(Optional.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificacionService.solicitarAsistencia(VISITA_ID, EMAIL))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("atenderAsistencia")
    class AtenderAsistencia {

        @Test
        @DisplayName("marca notificación como ATENDIDA y publica WS al cliente")
        void marcaAtendidaYPublicaWs() {
            Mesa mesa = mesaConMesero();
            Notificacion notif = Notificacion.builder()
                    .notificacionId(50L)
                    .notificacionEstado(EstadoNotificacion.ACTIVA)
                    .notificacionTipo(TipoNotificacion.ATENCION)
                    .mesa(mesa).empleado(mesa.getMesero()).build();

            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(notif));
            when(notificacionRepository.save(any())).thenReturn(notif);

            notificacionService.atenderAsistencia(50L, "mesero@test.com");

            assertThat(notif.getNotificacionEstado()).isEqualTo(EstadoNotificacion.ATENDIDA);

            ArgumentCaptor<AsistenciaAtendidaWsMessage> captor =
                    ArgumentCaptor.forClass(AsistenciaAtendidaWsMessage.class);
            verify(wsPublisher).publicarAsistenciaAtendida(eq(VISITA_ID), captor.capture());
            assertThat(captor.getValue().isAsistenciaAtendida()).isTrue();
        }

        @Test
        @DisplayName("lanza BusinessException si la notificación ya fue atendida")
        void lanzaExcepcionSiYaAtendida() {
            Mesa mesa = mesaConMesero();
            Notificacion notif = Notificacion.builder()
                    .notificacionId(50L)
                    .notificacionEstado(EstadoNotificacion.ATENDIDA)
                    .mesa(mesa).empleado(mesa.getMesero()).build();

            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(notif));

            assertThatThrownBy(() -> notificacionService.atenderAsistencia(50L, "mesero@test.com"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("lanza ResourceNotFoundException si la notificación no existe")
        void lanzaNotFoundSiNotificacionNoExiste() {
            when(notificacionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificacionService.atenderAsistencia(99L, "mesero@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("publica WS al mapa de mesas para que todos los meseros eliminen el ícono")
        void publicaRefreshMapaMesas() {
            Mesa mesa = mesaConMesero();
            Notificacion notif = Notificacion.builder()
                    .notificacionId(50L)
                    .notificacionEstado(EstadoNotificacion.ACTIVA)
                    .notificacionTipo(TipoNotificacion.ATENCION)
                    .mesa(mesa).empleado(mesa.getMesero()).build();

            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(notif));
            when(notificacionRepository.save(any())).thenReturn(notif);

            notificacionService.atenderAsistencia(50L, "mesero@test.com");

            verify(mesaWsPublisher).publicarActualizacionMesa(
                    VISITA_ID, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);
        }
    }

    @Nested
    @DisplayName("servirPlatos")
    class ServirPlatos {

        @Test
        @DisplayName("happy path → comanda COMPLETADO, notificación ATENDIDA, WS publicado y evaluador llamado")
        void platosListosActiva_completaComandaYPublicaWs() {
            Comanda comanda = comandaListo(EstacionComanda.COCINA);
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA, comanda);
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));
            when(notificacionRepository.save(any())).thenReturn(n);
            when(comandaRepository.save(any())).thenReturn(comanda);

            notificacionService.servirPlatos(50L, "mesero@test.com");

            assertThat(comanda.getComandaEstado()).isEqualTo(EstadoComanda.COMPLETADO);
            assertThat(n.getNotificacionEstado()).isEqualTo(EstadoNotificacion.ATENDIDA);
            ArgumentCaptor<ComandaProduccionEventoWsMessage> mensajeCaptor =
                    ArgumentCaptor.forClass(ComandaProduccionEventoWsMessage.class);
            verify(wsPublisher).publicarEventoProduccion(eq(EstacionComanda.COCINA), mensajeCaptor.capture());
            assertThat(mensajeCaptor.getValue().tipo()).isEqualTo(TipoEventoProduccion.COMPLETADA);
            assertThat(mensajeCaptor.getValue().comandaId()).isEqualTo(80L);
            assertThat(mensajeCaptor.getValue().estacion()).isEqualTo("COCINA");
            assertThat(mensajeCaptor.getValue().resumen()).isNull();
            verify(mesaWsPublisher).publicarActualizacionMesa(VISITA_ID, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);
            verify(mesaAsignarService).evaluarYActualizarEstadoMesa(VISITA_ID);
        }

        @Test
        @DisplayName("notificación inexistente → ResourceNotFoundException")
        void notificacionNoExiste_lanzaNotFound() {
            when(notificacionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificacionService.servirPlatos(99L, "mesero@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(mesaAsignarService, never()).evaluarYActualizarEstadoMesa(any());
        }

        @Test
        @DisplayName("tipo distinto a PLATOS_LISTOS → BusinessException INVALID_STATE")
        void tipoIncorrecto_lanzaBusinessException() {
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA, comandaListo(EstacionComanda.COCINA));
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

            assertThatThrownBy(() -> notificacionService.servirPlatos(50L, "mesero@test.com"))
                    .isInstanceOf(BusinessException.class);
            verify(notificacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("notificación ya ATENDIDA → BusinessException INVALID_STATE")
        void yaAtendida_lanzaBusinessException() {
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ATENDIDA, comandaListo(EstacionComanda.COCINA));
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

            assertThatThrownBy(() -> notificacionService.servirPlatos(50L, "mesero@test.com"))
                    .isInstanceOf(BusinessException.class);
            verify(comandaRepository, never()).save(any());
        }

        @Test
        @DisplayName("notificación sin comanda asociada → BusinessException BUSINESS_ERROR")
        void sinComanda_lanzaBusinessException() {
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA, null);
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

            assertThatThrownBy(() -> notificacionService.servirPlatos(50L, "mesero@test.com"))
                    .isInstanceOf(BusinessException.class);
            verify(wsPublisher, never()).publicarEventoProduccion(any(), any());
        }
    }

    @Nested
    @DisplayName("servirBebidas")
    class ServirBebidas {

        @Test
        @DisplayName("happy path → comanda BARRA COMPLETADO, WS BARRA publicado y evaluador llamado")
        void bebidasListasActiva_completaComandaBarra() {
            Comanda comanda = comandaListo(EstacionComanda.BARRA);
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA, comanda);
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));
            when(notificacionRepository.save(any())).thenReturn(n);
            when(comandaRepository.save(any())).thenReturn(comanda);

            notificacionService.servirBebidas(50L, "mesero@test.com");

            assertThat(comanda.getComandaEstado()).isEqualTo(EstadoComanda.COMPLETADO);
            ArgumentCaptor<ComandaProduccionEventoWsMessage> mensajeCaptor =
                    ArgumentCaptor.forClass(ComandaProduccionEventoWsMessage.class);
            verify(wsPublisher).publicarEventoProduccion(eq(EstacionComanda.BARRA), mensajeCaptor.capture());
            assertThat(mensajeCaptor.getValue().tipo()).isEqualTo(TipoEventoProduccion.COMPLETADA);
            assertThat(mensajeCaptor.getValue().comandaId()).isEqualTo(80L);
            assertThat(mensajeCaptor.getValue().estacion()).isEqualTo("BARRA");
            verify(mesaWsPublisher).publicarActualizacionMesa(VISITA_ID, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);
            verify(mesaAsignarService).evaluarYActualizarEstadoMesa(VISITA_ID);
        }

        @Test
        @DisplayName("notificación inexistente → ResourceNotFoundException")
        void notificacionNoExiste_lanzaNotFound() {
            when(notificacionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificacionService.servirBebidas(99L, "mesero@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("tipo distinto a BEBIDAS_LISTAS → BusinessException INVALID_STATE")
        void tipoIncorrecto_lanzaBusinessException() {
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA, comandaListo(EstacionComanda.COCINA));
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

            assertThatThrownBy(() -> notificacionService.servirBebidas(50L, "mesero@test.com"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("notificación ya ATENDIDA → BusinessException INVALID_STATE")
        void yaAtendida_lanzaBusinessException() {
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ATENDIDA, comandaListo(EstacionComanda.BARRA));
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

            assertThatThrownBy(() -> notificacionService.servirBebidas(50L, "mesero@test.com"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("notificación sin comanda asociada → BusinessException BUSINESS_ERROR")
        void sinComanda_lanzaBusinessException() {
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.BEBIDAS_LISTAS, EstadoNotificacion.ACTIVA, null);
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

            assertThatThrownBy(() -> notificacionService.servirBebidas(50L, "mesero@test.com"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("atenderCambio")
    class AtenderCambio {

        @Test
        @DisplayName("sin borrador: comanda PENDIENTE vuelve a BORRADOR y se publica ELIMINADA")
        void sinBorrador_comandaVuelveABorrador() {
            Comanda comanda = Comanda.builder()
                    .comandaId(80L)
                    .comandaEstacion(EstacionComanda.COCINA)
                    .comandaEstado(EstadoComanda.PENDIENTE)
                    .comandaFechaHoraInicio(java.time.LocalDateTime.now())
                    .build();
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.CAMBIO, EstadoNotificacion.ACTIVA, comanda);
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));
            when(notificacionRepository.save(any())).thenReturn(n);
            when(comandaRepository.findBorradorActivoByVisitaYEstacion(VISITA_ID, EstacionComanda.COCINA))
                    .thenReturn(Optional.empty());

            AtenderCambioResponse res = notificacionService.atenderCambio(50L, "mesero@test.com");

            assertThat(res.getComandaId()).isEqualTo(80L);
            assertThat(n.getNotificacionEstado()).isEqualTo(EstadoNotificacion.ATENDIDA);
            assertThat(comanda.getComandaEstado()).isEqualTo(EstadoComanda.BORRADOR);
            assertThat(comanda.getComandaFechaHoraInicio()).isNull();
            verify(comandaRepository).save(comanda);
            verify(comandaRepository, never()).delete(any());

            org.mockito.ArgumentCaptor<co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaProduccionEventoWsMessage> captor =
                    org.mockito.ArgumentCaptor.forClass(co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaProduccionEventoWsMessage.class);
            verify(wsPublisher).publicarEventoProduccion(eq(EstacionComanda.COCINA), captor.capture());
            assertThat(captor.getValue().tipo())
                    .isEqualTo(co.edu.unicauca.backend.modules.notificaciones.dto.ws.TipoEventoProduccion.ELIMINADA);
            assertThat(captor.getValue().comandaId()).isEqualTo(80L);

            verify(mesaWsPublisher).publicarActualizacionMesa(VISITA_ID, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);
            verify(mesaAsignarService, never()).evaluarYActualizarEstadoMesa(any());
        }

        @Test
        @DisplayName("con borrador: fusión por (producto, desc normalizada) — match suma cantidades")
        void conBorrador_fusionMatchSumaCantidades() {
            Comanda pendiente = Comanda.builder()
                    .comandaId(80L)
                    .comandaEstacion(EstacionComanda.COCINA)
                    .comandaEstado(EstadoComanda.PENDIENTE)
                    .build();
            Comanda borrador = Comanda.builder()
                    .comandaId(81L)
                    .comandaEstacion(EstacionComanda.COCINA)
                    .comandaEstado(EstadoComanda.BORRADOR)
                    .build();
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.CAMBIO, EstadoNotificacion.ACTIVA, pendiente);
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));
            when(comandaRepository.findBorradorActivoByVisitaYEstacion(VISITA_ID, EstacionComanda.COCINA))
                    .thenReturn(Optional.of(borrador));

            co.edu.unicauca.backend.modules.inventario.entity.Producto p1 =
                    co.edu.unicauca.backend.modules.inventario.entity.Producto.builder()
                            .productoId(1L).productoNombre("Arroz").build();
            co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem itemPendiente =
                    co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem.builder()
                            .comandaItemId(200L).comanda(pendiente).producto(p1)
                            .comandaItemCantidad(2).comandaItemDescripcion("  Sin sal  ")
                            .modificaciones(new java.util.ArrayList<>())
                            .build();
            co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem itemBorrador =
                    co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem.builder()
                            .comandaItemId(300L).comanda(borrador).producto(p1)
                            .comandaItemCantidad(1).comandaItemDescripcion("sin sal")
                            .modificaciones(new java.util.ArrayList<>())
                            .build();
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(80L))
                    .thenReturn(java.util.List.of(itemPendiente));
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(81L))
                    .thenReturn(java.util.List.of(itemBorrador));

            AtenderCambioResponse res = notificacionService.atenderCambio(50L, "mesero@test.com");

            assertThat(res.getComandaId()).isEqualTo(81L);
            assertThat(itemBorrador.getComandaItemCantidad()).isEqualTo(3);
            verify(comandaItemRepository).save(itemBorrador);
            verify(notificacionRepository).delete(n);
            verify(comandaRepository).delete(pendiente);
            verify(wsPublisher).publicarEventoProduccion(eq(EstacionComanda.COCINA), any());
        }

        @Test
        @DisplayName("con borrador: fusión sin match clona el ítem en el borrador")
        void conBorrador_fusionSinMatchClona() {
            Comanda pendiente = Comanda.builder()
                    .comandaId(80L)
                    .comandaEstacion(EstacionComanda.BARRA)
                    .comandaEstado(EstadoComanda.PENDIENTE)
                    .build();
            Comanda borrador = Comanda.builder()
                    .comandaId(81L)
                    .comandaEstacion(EstacionComanda.BARRA)
                    .comandaEstado(EstadoComanda.BORRADOR)
                    .build();
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.CAMBIO, EstadoNotificacion.ACTIVA, pendiente);
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));
            when(comandaRepository.findBorradorActivoByVisitaYEstacion(VISITA_ID, EstacionComanda.BARRA))
                    .thenReturn(Optional.of(borrador));

            co.edu.unicauca.backend.modules.inventario.entity.Producto p =
                    co.edu.unicauca.backend.modules.inventario.entity.Producto.builder()
                            .productoId(7L).productoNombre("Limonada").build();
            co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem itemPendiente =
                    co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem.builder()
                            .comandaItemId(200L).comanda(pendiente).producto(p)
                            .comandaItemCantidad(2).comandaItemPrecio(new java.math.BigDecimal("5000"))
                            .modificaciones(new java.util.ArrayList<>())
                            .build();
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(80L))
                    .thenReturn(java.util.List.of(itemPendiente));
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(81L))
                    .thenReturn(java.util.List.of());
            when(comandaItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AtenderCambioResponse res = notificacionService.atenderCambio(50L, "mesero@test.com");

            assertThat(res.getComandaId()).isEqualTo(81L);
            org.mockito.ArgumentCaptor<co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem> clonCaptor =
                    org.mockito.ArgumentCaptor.forClass(co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem.class);
            verify(comandaItemRepository).save(clonCaptor.capture());
            co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem clon = clonCaptor.getValue();
            assertThat(clon.getComanda()).isSameAs(borrador);
            assertThat(clon.getProducto().getProductoId()).isEqualTo(7L);
            assertThat(clon.getComandaItemCantidad()).isEqualTo(2);
            verify(notificacionRepository).delete(n);
            verify(comandaRepository).delete(pendiente);
        }

        @Test
        @DisplayName("fusión: ítem sin match con modificaciones null se clona sin opciones")
        void fusionSinMatchClonaModificacionesNull() {
            Comanda pendiente = Comanda.builder()
                    .comandaId(80L)
                    .comandaEstacion(EstacionComanda.COCINA)
                    .comandaEstado(EstadoComanda.PENDIENTE)
                    .build();
            Comanda borrador = Comanda.builder()
                    .comandaId(81L)
                    .comandaEstacion(EstacionComanda.COCINA)
                    .comandaEstado(EstadoComanda.BORRADOR)
                    .build();
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.CAMBIO, EstadoNotificacion.ACTIVA, pendiente);
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));
            when(comandaRepository.findBorradorActivoByVisitaYEstacion(VISITA_ID, EstacionComanda.COCINA))
                    .thenReturn(Optional.of(borrador));

            co.edu.unicauca.backend.modules.inventario.entity.Producto p =
                    co.edu.unicauca.backend.modules.inventario.entity.Producto.builder()
                            .productoId(13L).productoNombre("Té").build();
            co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem itemPend =
                    co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem.builder()
                            .comandaItemId(200L).comanda(pendiente).producto(p)
                            .comandaItemCantidad(1)
                            .modificaciones(null)
                            .build();
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(80L))
                    .thenReturn(java.util.List.of(itemPend));
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(81L))
                    .thenReturn(java.util.List.of());
            when(comandaItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            notificacionService.atenderCambio(50L, "mesero@test.com");

            org.mockito.ArgumentCaptor<co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem> captor =
                    org.mockito.ArgumentCaptor.forClass(co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem.class);
            verify(comandaItemRepository).save(captor.capture());
            assertThat(captor.getValue().getModificaciones()).isEmpty();
        }

        @Test
        @DisplayName("fusión: ítem con modificaciones de menú se clona con sus opciones")
        void fusionSinMatchClonaModificaciones() {
            Comanda pendiente = Comanda.builder()
                    .comandaId(80L)
                    .comandaEstacion(EstacionComanda.COCINA)
                    .comandaEstado(EstadoComanda.PENDIENTE)
                    .build();
            Comanda borrador = Comanda.builder()
                    .comandaId(81L)
                    .comandaEstacion(EstacionComanda.COCINA)
                    .comandaEstado(EstadoComanda.BORRADOR)
                    .build();
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.CAMBIO, EstadoNotificacion.ACTIVA, pendiente);
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));
            when(comandaRepository.findBorradorActivoByVisitaYEstacion(VISITA_ID, EstacionComanda.COCINA))
                    .thenReturn(Optional.of(borrador));

            co.edu.unicauca.backend.modules.inventario.entity.Producto p =
                    co.edu.unicauca.backend.modules.inventario.entity.Producto.builder()
                            .productoId(9L).productoNombre("Pasta").build();
            co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion opcion =
                    co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion.builder()
                            .opcionId(5L).opcionNombre("Doble queso").build();
            co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaMenuModificacion mod =
                    co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaMenuModificacion.builder()
                            .opcion(opcion).build();
            co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem itemPend =
                    co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem.builder()
                            .comandaItemId(200L).comanda(pendiente).producto(p)
                            .comandaItemCantidad(1)
                            .comandaItemMenuGrupo("grupo-1")
                            .modificaciones(new java.util.ArrayList<>(java.util.List.of(mod)))
                            .build();
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(80L))
                    .thenReturn(java.util.List.of(itemPend));
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(81L))
                    .thenReturn(java.util.List.of());
            when(comandaItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            notificacionService.atenderCambio(50L, "mesero@test.com");

            org.mockito.ArgumentCaptor<co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem> clonCaptor =
                    org.mockito.ArgumentCaptor.forClass(co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem.class);
            verify(comandaItemRepository).save(clonCaptor.capture());
            co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem clon = clonCaptor.getValue();
            assertThat(clon.getComandaItemMenuGrupo()).isEqualTo("grupo-1");
            assertThat(clon.getModificaciones()).hasSize(1);
            assertThat(clon.getModificaciones().get(0).getOpcion()).isSameAs(opcion);
            assertThat(clon.getModificaciones().get(0).getComandaItem()).isSameAs(clon);
        }

        @Test
        @DisplayName("fusión: ítems con descripción null se emparejan entre sí y se suman")
        void fusionConDescripcionNullCoincide() {
            Comanda pendiente = Comanda.builder()
                    .comandaId(80L)
                    .comandaEstacion(EstacionComanda.COCINA)
                    .comandaEstado(EstadoComanda.PENDIENTE)
                    .build();
            Comanda borrador = Comanda.builder()
                    .comandaId(81L)
                    .comandaEstacion(EstacionComanda.COCINA)
                    .comandaEstado(EstadoComanda.BORRADOR)
                    .build();
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.CAMBIO, EstadoNotificacion.ACTIVA, pendiente);
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));
            when(comandaRepository.findBorradorActivoByVisitaYEstacion(VISITA_ID, EstacionComanda.COCINA))
                    .thenReturn(Optional.of(borrador));

            co.edu.unicauca.backend.modules.inventario.entity.Producto p =
                    co.edu.unicauca.backend.modules.inventario.entity.Producto.builder()
                            .productoId(1L).productoNombre("Arroz").build();
            co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem itemPend =
                    co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem.builder()
                            .comandaItemId(200L).comanda(pendiente).producto(p)
                            .comandaItemCantidad(2).comandaItemDescripcion(null)
                            .modificaciones(null)
                            .build();
            co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem itemBorr =
                    co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem.builder()
                            .comandaItemId(300L).comanda(borrador).producto(p)
                            .comandaItemCantidad(4).comandaItemDescripcion("   ")
                            .modificaciones(new java.util.ArrayList<>())
                            .build();
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(80L))
                    .thenReturn(java.util.List.of(itemPend));
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(81L))
                    .thenReturn(java.util.List.of(itemBorr));

            notificacionService.atenderCambio(50L, "mesero@test.com");

            assertThat(itemBorr.getComandaItemCantidad()).isEqualTo(6);
            verify(comandaItemRepository).save(itemBorr);
            verify(notificacionRepository).delete(n);
            verify(comandaRepository).delete(pendiente);
        }

        @Test
        @DisplayName("comanda no PENDIENTE → solo marca la notificación ATENDIDA")
        void comandaNoPendiente_soloMarcaAtendida() {
            Comanda comanda = Comanda.builder()
                    .comandaId(80L)
                    .comandaEstacion(EstacionComanda.COCINA)
                    .comandaEstado(EstadoComanda.EN_PREPARACION)
                    .build();
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.CAMBIO, EstadoNotificacion.ACTIVA, comanda);
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));
            when(notificacionRepository.save(any())).thenReturn(n);

            AtenderCambioResponse res = notificacionService.atenderCambio(50L, "mesero@test.com");

            assertThat(res.getComandaId()).isEqualTo(80L);
            assertThat(n.getNotificacionEstado()).isEqualTo(EstadoNotificacion.ATENDIDA);
            assertThat(comanda.getComandaEstado()).isEqualTo(EstadoComanda.EN_PREPARACION);
            verify(comandaRepository, never()).delete(any());
            verify(wsPublisher, never()).publicarEventoProduccion(any(), any());
        }

        @Test
        @DisplayName("notificación inexistente → ResourceNotFoundException")
        void notificacionNoExiste_lanzaNotFound() {
            when(notificacionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificacionService.atenderCambio(99L, "mesero@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("tipo distinto a CAMBIO → BusinessException INVALID_STATE")
        void tipoIncorrecto_lanzaBusinessException() {
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.PLATOS_LISTOS, EstadoNotificacion.ACTIVA, comandaListo(EstacionComanda.COCINA));
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

            assertThatThrownBy(() -> notificacionService.atenderCambio(50L, "mesero@test.com"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("notificación ya ATENDIDA → BusinessException INVALID_STATE")
        void yaAtendida_lanzaBusinessException() {
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.CAMBIO, EstadoNotificacion.ATENDIDA, comandaListo(EstacionComanda.COCINA));
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

            assertThatThrownBy(() -> notificacionService.atenderCambio(50L, "mesero@test.com"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("notificación sin comanda → BusinessException BUSINESS_ERROR")
        void sinComanda_lanzaBusinessException() {
            Notificacion n = notificacionConComanda(
                    TipoNotificacion.CAMBIO, EstadoNotificacion.ACTIVA, null);
            when(notificacionRepository.findById(50L)).thenReturn(Optional.of(n));

            assertThatThrownBy(() -> notificacionService.atenderCambio(50L, "mesero@test.com"))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
