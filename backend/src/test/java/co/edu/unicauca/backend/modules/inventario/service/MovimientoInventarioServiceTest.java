package co.edu.unicauca.backend.modules.inventario.service;

import co.edu.unicauca.backend.modules.inventario.dto.request.AjusteInventarioRequest;
import co.edu.unicauca.backend.modules.inventario.dto.response.AjusteInventarioResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.ItemAjusteInventarioResponse;
import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import co.edu.unicauca.backend.modules.inventario.entity.MovimientoInventario;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.mapper.MovimientoInventarioMapper;
import co.edu.unicauca.backend.modules.inventario.repository.InsumoRepository;
import co.edu.unicauca.backend.modules.inventario.repository.MovimientoInventarioRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.inventario.repository.RecetaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaProduccionEventoWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.TipoEventoProduccion;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.modules.notificaciones.service.InventarioWsPublisher;
import co.edu.unicauca.backend.modules.notificaciones.service.MesaWsPublisher;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoMovimiento;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.enums.UnidadMedida;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MovimientoInventarioService")
class MovimientoInventarioServiceTest {

    @Mock ProductoRepository productoRepository;
    @Mock InsumoRepository insumoRepository;
    @Mock RecetaRepository recetaRepository;
    @Mock MovimientoInventarioRepository movimientoRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock ComandaItemRepository comandaItemRepository;
    @Mock MesaRepository mesaRepository;
    @Mock NotificacionRepository notificacionRepository;
    @Mock EmpleadoRepository empleadoRepository;
    @Spy  MovimientoInventarioMapper mapper;
    @Mock NotificacionWsPublisher wsPublisher;
    @Mock MesaWsPublisher mesaWsPublisher;
    @Mock InventarioWsPublisher inventarioWsPublisher;
    @InjectMocks MovimientoInventarioService service;

    // ── helpers ─────────────────────────────────────────────────────────────

    private Authentication auth(String email) {
        Authentication a = mock(Authentication.class);
        when(a.getName()).thenReturn(email);
        return a;
    }

    private Empleado empleado() {
        return Empleado.builder().build();
    }

    private Producto producto(Long id, BigDecimal stock) {
        return Producto.builder()
                .productoId(id)
                .productoNombre("Producto-" + id)
                .productoEstado(EstadoGenerico.ACTIVO)
                .stockActual(stock)
                .build();
    }

    private Insumo insumo(Long id, BigDecimal stock) {
        return Insumo.builder()
                .insumoId(id)
                .insumoNombre("Insumo-" + id)
                .insumoEstado(EstadoGenerico.ACTIVO)
                .insumoStockActual(stock)
                .insumoUnidad(UnidadMedida.KG)
                .build();
    }

    private Comanda comandaPendiente(Long id, Long visitaId) {
        return Comanda.builder()
                .comandaId(id)
                .visita(Visita.builder().visitaId(visitaId).build())
                .comandaEstado(EstadoComanda.PENDIENTE)
                .comandaEstacion(EstacionComanda.COCINA)
                .build();
    }

    private void stubActor(String email) {
        when(empleadoRepository.findByUsuario_UsuarioEmail(email))
                .thenReturn(Optional.of(empleado()));
    }

    private void stubMovimientoSave() {
        when(movimientoRepository.save(any(MovimientoInventario.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private AjusteInventarioRequest reqProducto(Long productoId, String cantidad, TipoMovimiento tipo) {
        return AjusteInventarioRequest.builder()
                .productoId(productoId).cantidad(new BigDecimal(cantidad)).tipo(tipo).build();
    }

    private AjusteInventarioRequest reqInsumo(Long insumoId, String cantidad, TipoMovimiento tipo) {
        return AjusteInventarioRequest.builder()
                .insumoId(insumoId).cantidad(new BigDecimal(cantidad)).tipo(tipo).build();
    }

    // ── buscarItemsAjuste ────────────────────────────────────────────────────

    @Nested
    @DisplayName("buscarItemsAjuste")
    class BuscarItemsAjuste {

        @Test
        @DisplayName("retorna productos e insumos fusionados y ordenados alfabéticamente")
        void retornaProductosEInsumos_ordenadosAlfabeticamente() {
            when(productoRepository.buscarParaAjuste("o", EstadoGenerico.ACTIVO.name()))
                    .thenReturn(List.of(producto(1L, new BigDecimal("5"))));
            when(insumoRepository.buscarPorNombreActivo("o", EstadoGenerico.ACTIVO.name()))
                    .thenReturn(List.of(insumo(10L, new BigDecimal("2"))));

            List<ItemAjusteInventarioResponse> result = service.buscarItemsAjuste("o");

            assertThat(result).hasSize(2);
            // "Insumo-10" precede a "Producto-1" alfabéticamente
            assertThat(result.get(0).getTipo()).isEqualTo("INSUMO");
            assertThat(result.get(1).getTipo()).isEqualTo("PRODUCTO");
        }

        @Test
        @DisplayName("producto mapeado con unidadMedida fija UNIDAD")
        void productoMapeadoConUnidadFija() {
            when(productoRepository.buscarParaAjuste("test", EstadoGenerico.ACTIVO.name()))
                    .thenReturn(List.of(producto(5L, new BigDecimal("3"))));
            when(insumoRepository.buscarPorNombreActivo("test", EstadoGenerico.ACTIVO.name()))
                    .thenReturn(List.of());

            List<ItemAjusteInventarioResponse> result = service.buscarItemsAjuste("test");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(5L);
            assertThat(result.get(0).getUnidadMedida()).isEqualTo("UNIDAD");
            assertThat(result.get(0).getStockActual()).isEqualByComparingTo("3");
        }

        @Test
        @DisplayName("sin coincidencias retorna lista vacía")
        void sinCoincidencias_retornaListaVacia() {
            when(productoRepository.buscarParaAjuste("zzz", EstadoGenerico.ACTIVO.name())).thenReturn(List.of());
            when(insumoRepository.buscarPorNombreActivo("zzz", EstadoGenerico.ACTIVO.name())).thenReturn(List.of());

            assertThat(service.buscarItemsAjuste("zzz")).isEmpty();
        }
    }

    // ── registrarAjuste – producto ───────────────────────────────────────────

    @Nested
    @DisplayName("registrarAjuste – producto")
    class RegistrarAjusteProducto {

        @Test
        @DisplayName("egreso descuenta stock, guarda movimiento y difunde el evento a /topic/inventario")
        void egresoProducto_descuentaStockYEmiteStockActualizado() {
            Producto p = producto(1L, new BigDecimal("10"));
            stubActor("cocinero@test.com");
            when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(p));
            when(productoRepository.save(any())).thenReturn(p);
            stubMovimientoSave();
            when(comandaRepository.findPendientesByProductoId(1L)).thenReturn(List.of());

            AjusteInventarioResponse resp = service.registrarAjuste(
                    reqProducto(1L, "3", TipoMovimiento.EGRESO), auth("cocinero@test.com"));

            assertThat(p.getStockActual()).isEqualByComparingTo("7");
            assertThat(resp.getStockActualizado()).isEqualByComparingTo("7");
            assertThat(resp.getComandasNotificadas()).isZero();
            verify(movimientoRepository).save(any(MovimientoInventario.class));
            verify(inventarioWsPublisher)
                    .publicarStockActualizado(eq(1L), isNull(), any(BigDecimal.class));
        }

        @Test
        @DisplayName("egreso sin stock suficiente lanza BusinessException 409")
        void egresoProductoSinStock_lanzaBusinessException() {
            Producto p = producto(145L, BigDecimal.ZERO);
            stubActor("cocinero@test.com");
            when(productoRepository.findByIdForUpdate(145L)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> service.registrarAjuste(
                    reqProducto(145L, "1", TipoMovimiento.EGRESO), auth("cocinero@test.com")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Stock insuficiente");

            verify(productoRepository, never()).save(any());
            verify(movimientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("egreso con stockActual null se trata como ZERO y lanza stock insuficiente")
        void egresoProductoConStockNull_lanzaBusinessException() {
            Producto p = producto(1L, null);
            stubActor("cocinero@test.com");
            when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(p));

            assertThatThrownBy(() -> service.registrarAjuste(
                    reqProducto(1L, "1", TipoMovimiento.EGRESO), auth("cocinero@test.com")))
                    .isInstanceOf(BusinessException.class);

            verify(movimientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("ingreso suma stock sin validar y no consulta comandas")
        void ingresoProducto_sumaStockSinValidar() {
            Producto p = producto(1L, BigDecimal.ZERO);
            stubActor("cocinero@test.com");
            when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(p));
            when(productoRepository.save(any())).thenReturn(p);
            stubMovimientoSave();

            AjusteInventarioResponse resp = service.registrarAjuste(
                    reqProducto(1L, "5", TipoMovimiento.INGRESO), auth("cocinero@test.com"));

            assertThat(p.getStockActual()).isEqualByComparingTo("5");
            assertThat(resp.getComandasNotificadas()).isZero();
            verify(comandaRepository, never()).findPendientesByProductoId(any());
        }

        @Test
        @DisplayName("egreso crea notificación CAMBIO, publica ELIMINADA y refresca el mapa de mesas")
        void egresoProducto_creaNotificacionCambioYPublicaEliminada() {
            Producto p = producto(1L, new BigDecimal("10"));
            Comanda comanda = comandaPendiente(50L, 200L);
            stubActor("cocinero@test.com");
            when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(p));
            when(productoRepository.save(any())).thenReturn(p);
            stubMovimientoSave();
            when(comandaRepository.findPendientesByProductoId(1L)).thenReturn(List.of(comanda));
            // FIFO: demanda comanda 50 = 10 > stockRestante 8 → notificar
            when(comandaItemRepository.sumCantidadProductoByComandaIds(List.of(50L), 1L))
                    .thenReturn(List.<Object[]>of(new Object[]{50L, 10L}));
            when(comandaRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(comanda));
            when(notificacionRepository.findFirstByComanda_ComandaIdAndNotificacionTipoAndNotificacionEstado(
                    50L, TipoNotificacion.CAMBIO, EstadoNotificacion.ACTIVA)).thenReturn(Optional.empty());
            when(mesaRepository.findByVisita_VisitaId(200L))
                    .thenReturn(Optional.of(Mesa.builder().visitaId(200L).build()));
            when(notificacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AjusteInventarioResponse resp = service.registrarAjuste(
                    reqProducto(1L, "2", TipoMovimiento.EGRESO), auth("cocinero@test.com"));

            assertThat(resp.getComandasNotificadas()).isEqualTo(1);

            ArgumentCaptor<Notificacion> n = ArgumentCaptor.forClass(Notificacion.class);
            verify(notificacionRepository).save(n.capture());
            assertThat(n.getValue().getNotificacionTipo()).isEqualTo(TipoNotificacion.CAMBIO);
            assertThat(n.getValue().getNotificacionEstado()).isEqualTo(EstadoNotificacion.ACTIVA);

            ArgumentCaptor<ComandaProduccionEventoWsMessage> ws =
                    ArgumentCaptor.forClass(ComandaProduccionEventoWsMessage.class);
            verify(wsPublisher, org.mockito.Mockito.atLeastOnce())
                    .publicarEventoProduccion(eq(EstacionComanda.COCINA), ws.capture());
            assertThat(ws.getAllValues()).anyMatch(m -> m.tipo() == TipoEventoProduccion.ELIMINADA);

            verify(mesaWsPublisher).publicarActualizacionMesa(200L, MesaWsPublisher.TipoEventoMesa.NOTIFICACION);
        }

        @Test
        @DisplayName("egreso con CAMBIO activo existente no duplica notificación")
        void egresoProducto_conCambioActivoExistente_noDuplica() {
            Producto p = producto(1L, new BigDecimal("10"));
            Comanda comanda = comandaPendiente(50L, 200L);
            stubActor("cocinero@test.com");
            when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(p));
            when(productoRepository.save(any())).thenReturn(p);
            stubMovimientoSave();
            when(comandaRepository.findPendientesByProductoId(1L)).thenReturn(List.of(comanda));
            // FIFO: demanda > stockRestante para intentar notificar
            when(comandaItemRepository.sumCantidadProductoByComandaIds(List.of(50L), 1L))
                    .thenReturn(List.<Object[]>of(new Object[]{50L, 10L}));
            when(comandaRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(comanda));
            when(notificacionRepository.findFirstByComanda_ComandaIdAndNotificacionTipoAndNotificacionEstado(
                    50L, TipoNotificacion.CAMBIO, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.of(Notificacion.builder()
                            .notificacionTipo(TipoNotificacion.CAMBIO)
                            .notificacionEstado(EstadoNotificacion.ACTIVA).build()));

            AjusteInventarioResponse resp = service.registrarAjuste(
                    reqProducto(1L, "2", TipoMovimiento.EGRESO), auth("cocinero@test.com"));

            assertThat(resp.getComandasNotificadas()).isZero();
            verify(notificacionRepository, never()).save(any(Notificacion.class));
            verify(mesaWsPublisher, never()).publicarActualizacionMesa(anyLong(), any());
        }

        @Test
        @DisplayName("egreso de comanda sin mesa no crea notificación")
        void egresoProducto_comandaSinMesa_noNotifica() {
            Producto p = producto(1L, new BigDecimal("10"));
            Comanda comanda = comandaPendiente(50L, 200L);
            stubActor("cocinero@test.com");
            when(productoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(p));
            when(productoRepository.save(any())).thenReturn(p);
            stubMovimientoSave();
            when(comandaRepository.findPendientesByProductoId(1L)).thenReturn(List.of(comanda));
            // FIFO: demanda > stockRestante para intentar notificar
            when(comandaItemRepository.sumCantidadProductoByComandaIds(List.of(50L), 1L))
                    .thenReturn(List.<Object[]>of(new Object[]{50L, 10L}));
            when(comandaRepository.findByIdForUpdate(50L)).thenReturn(Optional.of(comanda));
            when(notificacionRepository.findFirstByComanda_ComandaIdAndNotificacionTipoAndNotificacionEstado(
                    50L, TipoNotificacion.CAMBIO, EstadoNotificacion.ACTIVA)).thenReturn(Optional.empty());
            when(mesaRepository.findByVisita_VisitaId(200L)).thenReturn(Optional.empty());

            AjusteInventarioResponse resp = service.registrarAjuste(
                    reqProducto(1L, "2", TipoMovimiento.EGRESO), auth("cocinero@test.com"));

            assertThat(resp.getComandasNotificadas()).isZero();
            verify(notificacionRepository, never()).save(any(Notificacion.class));
        }

        @Test
        @DisplayName("producto inexistente lanza ResourceNotFoundException")
        void productoInexistente_lanzaResourceNotFound() {
            stubActor("cocinero@test.com");
            when(productoRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registrarAjuste(
                    reqProducto(999L, "1", TipoMovimiento.EGRESO), auth("cocinero@test.com")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── registrarAjuste – insumo ─────────────────────────────────────────────

    @Nested
    @DisplayName("registrarAjuste – insumo")
    class RegistrarAjusteInsumo {

        @Test
        @DisplayName("egreso notifica comandas afectadas por receta")
        void egresoInsumo_notificaComandasAfectadasPorReceta() {
            Insumo ins = insumo(10L, new BigDecimal("5"));
            Comanda comanda = comandaPendiente(70L, 300L);
            stubActor("cocinero@test.com");
            when(insumoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(ins));
            when(insumoRepository.save(any())).thenReturn(ins);
            stubMovimientoSave();
            when(recetaRepository.findProductoIdsByInsumoId(10L)).thenReturn(List.of(55L));
            when(comandaRepository.findPendientesByProductoIds(List.of(55L))).thenReturn(List.of(comanda));
            // FIFO: demanda insumo = 5 > stockRestante 3 → notificar
            when(comandaItemRepository.sumDemandaInsumoByComandaIds(List.of(70L), 10L))
                    .thenReturn(List.<Object[]>of(new Object[]{70L, new BigDecimal("5")}));
            when(comandaRepository.findByIdForUpdate(70L)).thenReturn(Optional.of(comanda));
            when(notificacionRepository.findFirstByComanda_ComandaIdAndNotificacionTipoAndNotificacionEstado(
                    70L, TipoNotificacion.CAMBIO, EstadoNotificacion.ACTIVA)).thenReturn(Optional.empty());
            when(mesaRepository.findByVisita_VisitaId(300L))
                    .thenReturn(Optional.of(Mesa.builder().visitaId(300L).build()));
            when(notificacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AjusteInventarioResponse resp = service.registrarAjuste(
                    reqInsumo(10L, "2", TipoMovimiento.EGRESO), auth("cocinero@test.com"));

            assertThat(resp.getComandasNotificadas()).isEqualTo(1);
            assertThat(ins.getInsumoStockActual()).isEqualByComparingTo("3");
            verify(comandaRepository).findPendientesByProductoIds(List.of(55L));
        }

        @Test
        @DisplayName("egreso sin stock suficiente lanza BusinessException")
        void egresoInsumoSinStock_lanzaBusinessException() {
            Insumo ins = insumo(10L, new BigDecimal("1"));
            stubActor("cocinero@test.com");
            when(insumoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(ins));

            assertThatThrownBy(() -> service.registrarAjuste(
                    reqInsumo(10L, "2", TipoMovimiento.EGRESO), auth("cocinero@test.com")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Stock insuficiente");

            verify(movimientoRepository, never()).save(any());
        }

        @Test
        @DisplayName("egreso de insumo sin recetas asociadas notifica cero comandas")
        void egresoInsumo_sinRecetas_ceroNotificadas() {
            Insumo ins = insumo(10L, new BigDecimal("5"));
            stubActor("cocinero@test.com");
            when(insumoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(ins));
            when(insumoRepository.save(any())).thenReturn(ins);
            stubMovimientoSave();
            when(recetaRepository.findProductoIdsByInsumoId(10L)).thenReturn(List.of());

            AjusteInventarioResponse resp = service.registrarAjuste(
                    reqInsumo(10L, "2", TipoMovimiento.EGRESO), auth("cocinero@test.com"));

            assertThat(resp.getComandasNotificadas()).isZero();
            verify(comandaRepository, never()).findPendientesByProductoIds(any());
        }

        @Test
        @DisplayName("ingreso suma stock del insumo")
        void ingresoInsumo_sumaStock() {
            Insumo ins = insumo(10L, new BigDecimal("2"));
            stubActor("cocinero@test.com");
            when(insumoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(ins));
            when(insumoRepository.save(any())).thenReturn(ins);
            stubMovimientoSave();

            AjusteInventarioResponse resp = service.registrarAjuste(
                    reqInsumo(10L, "3", TipoMovimiento.INGRESO), auth("cocinero@test.com"));

            assertThat(resp.getStockActualizado()).isEqualByComparingTo("5");
        }

        @Test
        @DisplayName("insumo inexistente lanza ResourceNotFoundException")
        void insumoInexistente_lanzaResourceNotFound() {
            stubActor("cocinero@test.com");
            when(insumoRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.registrarAjuste(
                    reqInsumo(999L, "1", TipoMovimiento.EGRESO), auth("cocinero@test.com")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── registrarAjuste – validaciones de entrada ────────────────────────────

    @Nested
    @DisplayName("registrarAjuste – validaciones de entrada")
    class ValidacionesEntrada {

        @Test
        @DisplayName("sin productoId ni insumoId lanza BusinessException")
        void sinProductoNiInsumo_lanzaBusinessException() {
            AjusteInventarioRequest req = AjusteInventarioRequest.builder()
                    .cantidad(new BigDecimal("1")).tipo(TipoMovimiento.EGRESO).build();

            assertThatThrownBy(() -> service.registrarAjuste(req, auth("cocinero@test.com")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("obligatorio");
        }

        @Test
        @DisplayName("con productoId e insumoId a la vez lanza BusinessException")
        void conProductoEInsumo_lanzaBusinessException() {
            AjusteInventarioRequest req = AjusteInventarioRequest.builder()
                    .productoId(1L).insumoId(10L)
                    .cantidad(new BigDecimal("1")).tipo(TipoMovimiento.EGRESO).build();

            assertThatThrownBy(() -> service.registrarAjuste(req, auth("cocinero@test.com")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Solo se puede ajustar");
        }
    }

    // ── Notificación FIFO condicional ────────────────────────────────────────

    @Nested
    @DisplayName("Notificación FIFO condicional")
    class NotificacionFifoTests {

        private Comanda comanda(long id, long visitaId, LocalDateTime createdAt) {
            Comanda c = Comanda.builder()
                    .comandaId(id)
                    .visita(Visita.builder().visitaId(visitaId).build())
                    .comandaEstacion(EstacionComanda.COCINA)
                    .comandaEstado(EstadoComanda.PENDIENTE)
                    .build();
            c.setCreatedAt(createdAt);
            return c;
        }

        @Test
        @DisplayName("FIFO: egreso deja stock suficiente para todas las comandas → 0 notificadas")
        void egreso_stockCubreTodas_ceroNotificadas() {
            LocalDateTime t = LocalDateTime.now();
            Comanda c1 = comanda(1L, 101L, t.minusMinutes(20));
            Comanda c2 = comanda(2L, 102L, t.minusMinutes(10));
            Producto p = producto(10L, new BigDecimal("5")); // stock post-egreso = 4 tras qty=1

            stubActor("cocinero@test.com");
            when(productoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(p));
            when(productoRepository.save(any())).thenReturn(p);
            stubMovimientoSave();
            when(comandaRepository.findPendientesByProductoId(10L)).thenReturn(List.of(c1, c2));
            // c1 demanda=1, c2 demanda=1, stock=4 → ambas cubiertas → 0 notificadas
            when(comandaItemRepository.sumCantidadProductoByComandaIds(List.of(1L, 2L), 10L))
                    .thenReturn(List.<Object[]>of(new Object[]{1L, 1L}, new Object[]{2L, 1L}));

            AjusteInventarioResponse resp = service.registrarAjuste(
                    reqProducto(10L, "1", TipoMovimiento.EGRESO), auth("cocinero@test.com"));

            assertThat(resp.getComandasNotificadas()).isZero();
            verify(notificacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("FIFO: primera comanda cubierta, segunda no → 1 notificada")
        void egreso_fifo_primeraCubiertaSegundaNo() {
            LocalDateTime t = LocalDateTime.now();
            Comanda c1 = comanda(1L, 101L, t.minusMinutes(20)); // más antigua
            Comanda c2 = comanda(2L, 102L, t.minusMinutes(10)); // más reciente
            Producto p = producto(10L, new BigDecimal("3")); // stock post-egreso = 1 tras qty=2

            stubActor("cocinero@test.com");
            when(productoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(p));
            when(productoRepository.save(any())).thenReturn(p);
            stubMovimientoSave();
            when(comandaRepository.findPendientesByProductoId(10L)).thenReturn(List.of(c1, c2));
            // c1 demanda=1, stock=1 → cubierta (disponible→0); c2 demanda=1, disponible=0 → notificada
            when(comandaItemRepository.sumCantidadProductoByComandaIds(List.of(1L, 2L), 10L))
                    .thenReturn(List.<Object[]>of(new Object[]{1L, 1L}, new Object[]{2L, 1L}));
            when(comandaRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(c2));
            when(notificacionRepository.findFirstByComanda_ComandaIdAndNotificacionTipoAndNotificacionEstado(
                    eq(2L), any(), any())).thenReturn(Optional.empty());
            when(mesaRepository.findByVisita_VisitaId(102L))
                    .thenReturn(Optional.of(Mesa.builder().visitaId(102L).build()));
            when(notificacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AjusteInventarioResponse resp = service.registrarAjuste(
                    reqProducto(10L, "2", TipoMovimiento.EGRESO), auth("cocinero@test.com"));

            assertThat(resp.getComandasNotificadas()).isEqualTo(1);
            verify(notificacionRepository, times(1)).save(any(Notificacion.class));
        }

        @Test
        @DisplayName("FIFO: stock agotado, 2 comandas → 2 notificadas")
        void egreso_stockAgotado_dosNotificadas() {
            LocalDateTime t = LocalDateTime.now();
            Comanda c1 = comanda(1L, 101L, t.minusMinutes(20));
            Comanda c2 = comanda(2L, 102L, t.minusMinutes(10));
            Producto p = producto(10L, new BigDecimal("2")); // stock post-egreso = 0 tras qty=2

            stubActor("cocinero@test.com");
            when(productoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(p));
            when(productoRepository.save(any())).thenReturn(p);
            stubMovimientoSave();
            when(comandaRepository.findPendientesByProductoId(10L)).thenReturn(List.of(c1, c2));
            // ambas demandan 1, stock=0 → ambas no cubiertas
            when(comandaItemRepository.sumCantidadProductoByComandaIds(List.of(1L, 2L), 10L))
                    .thenReturn(List.<Object[]>of(new Object[]{1L, 1L}, new Object[]{2L, 1L}));
            for (Comanda c : List.of(c1, c2)) {
                when(comandaRepository.findByIdForUpdate(c.getComandaId())).thenReturn(Optional.of(c));
                when(notificacionRepository.findFirstByComanda_ComandaIdAndNotificacionTipoAndNotificacionEstado(
                        eq(c.getComandaId()), any(), any())).thenReturn(Optional.empty());
                when(mesaRepository.findByVisita_VisitaId(c.getVisita().getVisitaId()))
                        .thenReturn(Optional.of(Mesa.builder().visitaId(c.getVisita().getVisitaId()).build()));
            }
            when(notificacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AjusteInventarioResponse resp = service.registrarAjuste(
                    reqProducto(10L, "2", TipoMovimiento.EGRESO), auth("cocinero@test.com"));

            assertThat(resp.getComandasNotificadas()).isEqualTo(2);
            verify(notificacionRepository, times(2)).save(any(Notificacion.class));
        }
    }
}
