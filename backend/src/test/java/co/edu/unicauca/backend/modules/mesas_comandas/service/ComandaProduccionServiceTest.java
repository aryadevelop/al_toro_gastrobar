package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.service.InventarioDescuentoService;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.TableroProduccionResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.ComandaProduccionMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.TipoEventoProduccion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.modules.notificaciones.service.MesaWsPublisher;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComandaProduccionService")
class ComandaProduccionServiceTest {

    @Mock ComandaRepository comandaRepository;
    @Mock ComandaItemRepository comandaItemRepository;
    @Mock MesaRepository mesaRepository;
    @Mock EstacionResolver estacionResolver;
    @Mock InventarioDescuentoService inventarioDescuentoService;
    @Mock NotificacionWsPublisher wsPublisher;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock co.edu.unicauca.backend.modules.mesas_comandas.mapper.VisitaEstadoMapper visitaEstadoMapper;
    @Mock NotificacionRepository notificacionRepository;
    @Mock MesaWsPublisher mesaWsPublisher;
    @Mock Authentication auth;

    ComandaProduccionService service;

    @BeforeEach
    void setUp() {
        ComandaProduccionMapper realMapper = new ComandaProduccionMapper();
        service = new ComandaProduccionService(
                comandaRepository, comandaItemRepository, mesaRepository,
                estacionResolver, realMapper,
                inventarioDescuentoService, wsPublisher, empleadoRepository,
                visitaEstadoMapper, notificacionRepository, mesaWsPublisher);
    }

    private Visita visita(Long id) {
        return Visita.builder().visitaId(id).build();
    }

    private Mesa mesa(Long visitaId, String identificador, String meseroNombre) {
        return Mesa.builder()
                .visitaId(visitaId)
                .mesaIdentificador(identificador)
                .mesero(Empleado.builder().empleadoNombre(meseroNombre).build())
                .build();
    }

    private Comanda comanda(Long id, Long visitaId, EstacionComanda estacion,
                            EstadoComanda estado, LocalDateTime inicio, LocalDateTime listo,
                            LocalDateTime createdAt) {
        Comanda c = Comanda.builder()
                .comandaId(id)
                .visita(visita(visitaId))
                .comandaEstacion(estacion)
                .comandaEstado(estado)
                .comandaFechaHoraInicio(inicio)
                .comandaFechaHoraListo(listo)
                .build();
        c.setCreatedAt(createdAt);
        return c;
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("obtenerTableroProduccion")
    class TableroTests {

        @Test
        @DisplayName("Cocinero: tres columnas pobladas con orden cronológico ascendente")
        void cocinero_treColumnas() {
            when(estacionResolver.resolverEstaciones(auth))
                    .thenReturn(Set.of(EstacionComanda.COCINA));

            LocalDateTime t = LocalDateTime.of(2026, 5, 12, 18, 0);
            Comanda p1 = comanda(1L, 11L, EstacionComanda.COCINA, EstadoComanda.PENDIENTE, t.plusMinutes(1), null, t);
            Comanda p2 = comanda(2L, 12L, EstacionComanda.COCINA, EstadoComanda.PENDIENTE, t.plusMinutes(5), null, t);
            Comanda e1 = comanda(3L, 13L, EstacionComanda.COCINA, EstadoComanda.EN_PREPARACION, t.plusMinutes(2), null, t);
            Comanda l1 = comanda(4L, 14L, EstacionComanda.COCINA, EstadoComanda.LISTO, t.plusMinutes(3), t.plusMinutes(20), t);

            when(comandaRepository.findByEstacionAndEstadoInSinCambioActivo(eq(EstacionComanda.COCINA), anySet()))
                    .thenReturn(List.of(p2, p1, e1, l1));

            when(mesaRepository.findByVisita_VisitaIdIn(any())).thenReturn(List.of(
                    mesa(11L, "T-01", "Ana"),
                    mesa(12L, "T-02", "Beto"),
                    mesa(13L, "T-03", "Cris"),
                    mesa(14L, "T-04", "Dany")));
            when(comandaItemRepository.sumCantidadByComandaIdIn(any()))
                    .thenReturn(List.of(
                            new Object[]{1L, 2L},
                            new Object[]{2L, 3L},
                            new Object[]{3L, 4L},
                            new Object[]{4L, 5L}));

            TableroProduccionResponse t1 = service.obtenerTableroProduccion(auth);

            assertThat(t1.getEstaciones()).containsExactly("COCINA");
            // Pendientes ordenadas por fechaHoraInicio ascendente: p1 (min+1) antes que p2 (min+5)
            assertThat(t1.getPendientes()).extracting(ComandaProduccionResumenResponse::getComandaId)
                    .containsExactly(1L, 2L);
            assertThat(t1.getEnPreparacion()).extracting(ComandaProduccionResumenResponse::getComandaId)
                    .containsExactly(3L);
            assertThat(t1.getListos()).extracting(ComandaProduccionResumenResponse::getComandaId)
                    .containsExactly(4L);
            assertThat(t1.getPendientes().get(0).getMesaIdentificador()).isEqualTo("T-01");
            assertThat(t1.getPendientes().get(0).getMeseroNombre()).isEqualTo("Ana");
            assertThat(t1.getPendientes().get(0).getTotalItems()).isEqualTo(2);
        }

        @Test
        @DisplayName("Sin comandas: tres columnas vacías y estaciones del usuario presentes")
        void sinComandas() {
            when(estacionResolver.resolverEstaciones(auth))
                    .thenReturn(Set.of(EstacionComanda.BARRA));
            when(comandaRepository.findByEstacionAndEstadoInSinCambioActivo(eq(EstacionComanda.BARRA), anySet()))
                    .thenReturn(List.of());

            TableroProduccionResponse r = service.obtenerTableroProduccion(auth);

            assertThat(r.getEstaciones()).containsExactly("BARRA");
            assertThat(r.getPendientes()).isEmpty();
            assertThat(r.getEnPreparacion()).isEmpty();
            assertThat(r.getListos()).isEmpty();
        }

        @Test
        @DisplayName("Usuario con ambas estaciones: tablero combinado")
        void ambasEstaciones_combinado() {
            when(estacionResolver.resolverEstaciones(auth))
                    .thenReturn(Set.of(EstacionComanda.COCINA, EstacionComanda.BARRA));

            LocalDateTime t = LocalDateTime.of(2026, 5, 12, 18, 0);
            Comanda cocina = comanda(1L, 11L, EstacionComanda.COCINA, EstadoComanda.PENDIENTE, t, null, t);
            Comanda barra = comanda(2L, 12L, EstacionComanda.BARRA, EstadoComanda.PENDIENTE, t.plusMinutes(1), null, t);

            when(comandaRepository.findByEstacionAndEstadoInSinCambioActivo(eq(EstacionComanda.COCINA), anySet()))
                    .thenReturn(List.of(cocina));
            when(comandaRepository.findByEstacionAndEstadoInSinCambioActivo(eq(EstacionComanda.BARRA), anySet()))
                    .thenReturn(List.of(barra));
            when(mesaRepository.findByVisita_VisitaIdIn(any())).thenReturn(List.of(
                    mesa(11L, "T-01", "Ana"),
                    mesa(12L, "T-02", "Beto")));
            when(comandaItemRepository.sumCantidadByComandaIdIn(any()))
                    .thenReturn(List.of(new Object[]{1L, 1L}, new Object[]{2L, 1L}));

            TableroProduccionResponse r = service.obtenerTableroProduccion(auth);

            assertThat(r.getEstaciones()).containsExactly("BARRA", "COCINA");
            assertThat(r.getPendientes()).hasSize(2)
                    .extracting(ComandaProduccionResumenResponse::getEstacion)
                    .containsExactlyInAnyOrder("COCINA", "BARRA");
        }

        @Test
        @DisplayName("Sin rol de producción: BusinessException 403 propagada")
        void sinRol_excepcionPropagada() {
            when(estacionResolver.resolverEstaciones(auth))
                    .thenThrow(new BusinessException(
                            co.edu.unicauca.backend.shared.exception.ErrorCode.ACCESS_DENIED,
                            "sin rol", HttpStatus.FORBIDDEN));

            assertThatThrownBy(() -> service.obtenerTableroProduccion(auth))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("obtenerDetalleComanda")
    class DetalleTests {

        private void mockProduccion() {
            lenient().when(estacionResolver.resolverEstaciones(auth))
                    .thenReturn(Set.of(EstacionComanda.COCINA));
        }

        @Test
        @DisplayName("Comanda visible: devuelve detalle con ítems agrupados")
        void visible_devuelveDetalle() {
            mockProduccion();
            LocalDateTime t = LocalDateTime.of(2026, 5, 12, 18, 0);
            Comanda c = comanda(50L, 7L, EstacionComanda.COCINA, EstadoComanda.PENDIENTE, t, null, t);

            when(comandaRepository.findById(50L)).thenReturn(Optional.of(c));
            when(mesaRepository.findByVisita_VisitaId(7L))
                    .thenReturn(Optional.of(mesa(7L, "T-05", "Eva")));

            Producto plato = Producto.builder()
                    .productoId(1L).productoNombre("Arroz")
                    .productoCategoria(CategoriaProducto.PLATO).build();
            ComandaItem item = ComandaItem.builder()
                    .comandaItemId(99L).producto(plato)
                    .comandaItemCantidad(2).modificaciones(new ArrayList<>())
                    .build();
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(50L))
                    .thenReturn(List.of(item));

            ComandaProduccionDetalleResponse d = service.obtenerDetalleComanda(50L, auth);

            assertThat(d.getComandaId()).isEqualTo(50L);
            assertThat(d.getMesaIdentificador()).isEqualTo("T-05");
            assertThat(d.getMeseroNombre()).isEqualTo("Eva");
            assertThat(d.getPlatos()).hasSize(1)
                    .extracting(ItemDetalleResponse::getProductoNombre)
                    .containsExactly("Arroz");
        }

        @Test
        @DisplayName("Comanda inexistente: 404 ENT-001")
        void inexistente_404() {
            mockProduccion();
            when(comandaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerDetalleComanda(99L, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(be.getCode()).isEqualTo("ENT-001");
                    });
        }

        @Test
        @DisplayName("Comanda de otra estación: 403 AUTH-002")
        void otraEstacion_403() {
            mockProduccion();
            LocalDateTime t = LocalDateTime.of(2026, 5, 12, 18, 0);
            Comanda c = comanda(50L, 7L, EstacionComanda.BARRA, EstadoComanda.PENDIENTE, t, null, t);
            when(comandaRepository.findById(50L)).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> service.obtenerDetalleComanda(50L, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(be.getCode()).isEqualTo("AUTH-002");
                    });
        }

        @Test
        @DisplayName("Comanda en estado no visible (COMPLETADO): 404 ENT-001")
        void estadoNoVisible_404() {
            mockProduccion();
            LocalDateTime t = LocalDateTime.of(2026, 5, 12, 18, 0);
            Comanda c = comanda(50L, 7L, EstacionComanda.COCINA, EstadoComanda.COMPLETADO, t, t, t);
            when(comandaRepository.findById(50L)).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> service.obtenerDetalleComanda(50L, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("iniciarPreparacion")
    class IniciarPreparacionTests {

        private static final Long COMANDA_ID = 10L;
        private static final Long VISITA_ID = 5L;
        private static final String ACTOR_EMAIL = "cocinero@altoro.co";

        private Comanda comandaPendienteCocina() {
            return comanda(COMANDA_ID, VISITA_ID, EstacionComanda.COCINA,
                    EstadoComanda.PENDIENTE, null, null,
                    LocalDateTime.of(2026, 5, 14, 19, 0));
        }

        @Test
        @DisplayName("Happy path: PENDIENTE COCINA, descuento OK → EN_PREPARACION + WS publicados")
        void happyPath_pendienteCocinaConDescuentoOK_transicionaYPublica() {
            Comanda comanda = comandaPendienteCocina();
            Empleado empleado = Empleado.builder().build();

            when(auth.getName()).thenReturn(ACTOR_EMAIL);
            when(estacionResolver.resolverEstaciones(auth)).thenReturn(Set.of(EstacionComanda.COCINA));
            when(comandaRepository.findById(COMANDA_ID)).thenReturn(Optional.of(comanda));
            when(empleadoRepository.findByUsuario_UsuarioEmail(ACTOR_EMAIL)).thenReturn(Optional.of(empleado));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(comandaItemRepository.sumCantidadByComandaIdIn(Set.of(COMANDA_ID))).thenReturn(List.of());

            service.iniciarPreparacion(COMANDA_ID, auth);

            assertThat(comanda.getComandaEstado()).isEqualTo(EstadoComanda.EN_PREPARACION);
            assertThat(comanda.getComandaFechaHoraInicio()).isNotNull();
            verify(comandaRepository).save(comanda);
            verify(wsPublisher).publicarEventoProduccion(
                    eq(EstacionComanda.COCINA),
                    argThat(m -> m.tipo() == TipoEventoProduccion.ACTUALIZADA
                            && "EN_PREPARACION".equals(m.nuevoEstado())));
            verify(wsPublisher).publicarVisitaActualizada(eq(VISITA_ID), any());
        }

        @Test
        @DisplayName("Comanda inexistente: lanza ResourceNotFoundException")
        void comandaInexistente_lanzaResourceNotFoundException() {
            when(estacionResolver.resolverEstaciones(auth)).thenReturn(Set.of(EstacionComanda.COCINA));
            when(comandaRepository.findById(COMANDA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.iniciarPreparacion(COMANDA_ID, auth))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Estado no PENDIENTE (EN_PREPARACION): lanza BusinessException INVALID_STATE 409")
        void estadoNoPendiente_lanzaInvalidState_409() {
            Comanda comanda = comanda(COMANDA_ID, VISITA_ID, EstacionComanda.COCINA,
                    EstadoComanda.EN_PREPARACION, LocalDateTime.now(), null,
                    LocalDateTime.of(2026, 5, 14, 19, 0));

            when(estacionResolver.resolverEstaciones(auth)).thenReturn(Set.of(EstacionComanda.COCINA));
            when(comandaRepository.findById(COMANDA_ID)).thenReturn(Optional.of(comanda));

            assertThatThrownBy(() -> service.iniciarPreparacion(COMANDA_ID, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(be.getCode()).isEqualTo("NEG-002");
                    });
        }

        @Test
        @DisplayName("Estación ajena (sólo BARRA, comanda COCINA): lanza BusinessException ACCESS_DENIED 403")
        void estacionAjena_lanzaAccessDenied_403() {
            Comanda comanda = comandaPendienteCocina();

            when(estacionResolver.resolverEstaciones(auth)).thenReturn(Set.of(EstacionComanda.BARRA));
            when(comandaRepository.findById(COMANDA_ID)).thenReturn(Optional.of(comanda));

            assertThatThrownBy(() -> service.iniciarPreparacion(COMANDA_ID, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(be.getCode()).isEqualTo("AUTH-002");
                    });
        }

        @Test
        @DisplayName("Empleado no encontrado: lanza ResourceNotFoundException")
        void empleadoNoEncontrado_lanzaResourceNotFound() {
            Comanda comanda = comandaPendienteCocina();

            when(auth.getName()).thenReturn(ACTOR_EMAIL);
            when(estacionResolver.resolverEstaciones(auth)).thenReturn(Set.of(EstacionComanda.COCINA));
            when(comandaRepository.findById(COMANDA_ID)).thenReturn(Optional.of(comanda));
            when(empleadoRepository.findByUsuario_UsuarioEmail(ACTOR_EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.iniciarPreparacion(COMANDA_ID, auth))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Happy path con items: sumCantidadByComandaIdIn retorna resultado, lambda map ejecutada")
        void happyPath_conItems_mapLambdaEjecutada() {
            Comanda comanda = comandaPendienteCocina();
            Empleado empleado = Empleado.builder().build();
            Producto prod = Producto.builder().productoId(1L).productoNombre("P").build();
            ComandaItem ci = ComandaItem.builder()
                    .producto(prod).comandaItemCantidad(3)
                    .comandaItemPrecio(new BigDecimal("10000")).build();

            when(auth.getName()).thenReturn(ACTOR_EMAIL);
            when(estacionResolver.resolverEstaciones(auth)).thenReturn(Set.of(EstacionComanda.COCINA));
            when(comandaRepository.findById(COMANDA_ID)).thenReturn(Optional.of(comanda));
            when(empleadoRepository.findByUsuario_UsuarioEmail(ACTOR_EMAIL)).thenReturn(Optional.of(empleado));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            List<Object[]> sumRows = new ArrayList<>();
            sumRows.add(new Object[]{COMANDA_ID, 5L});
            when(comandaItemRepository.sumCantidadByComandaIdIn(Set.of(COMANDA_ID))).thenReturn(sumRows);
            when(comandaRepository.findAllItemsActivosByVisita(VISITA_ID)).thenReturn(List.of(ci));

            service.iniciarPreparacion(COMANDA_ID, auth);

            assertThat(comanda.getComandaEstado()).isEqualTo(EstadoComanda.EN_PREPARACION);
        }

        @Test
        @DisplayName("Descuento falla: excepción propagada, save y WS no invocados")
        void descuentoFalla_noTransicionaNiPublica() {
            Comanda comanda = comandaPendienteCocina();
            Empleado empleado = Empleado.builder().build();

            when(auth.getName()).thenReturn(ACTOR_EMAIL);
            when(estacionResolver.resolverEstaciones(auth)).thenReturn(Set.of(EstacionComanda.COCINA));
            when(comandaRepository.findById(COMANDA_ID)).thenReturn(Optional.of(comanda));
            when(empleadoRepository.findByUsuario_UsuarioEmail(ACTOR_EMAIL)).thenReturn(Optional.of(empleado));
            doThrow(new BusinessException(
                    co.edu.unicauca.backend.shared.exception.ErrorCode.INSUFFICIENT_STOCK,
                    "Stock insuficiente.", HttpStatus.CONFLICT))
                    .when(inventarioDescuentoService).descontarPorComanda(comanda, empleado);

            assertThatThrownBy(() -> service.iniciarPreparacion(COMANDA_ID, auth))
                    .isInstanceOf(BusinessException.class);

            verify(comandaRepository, never()).save(any());
            verifyNoInteractions(wsPublisher);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("marcarListo")
    class MarcarListoTests {

        private static final Long COMANDA_ID = 20L;
        private static final Long VISITA_ID = 9L;

        private Comanda comandaEnPreparacion(EstacionComanda estacion) {
            return comanda(COMANDA_ID, VISITA_ID, estacion,
                    EstadoComanda.EN_PREPARACION, LocalDateTime.of(2026, 5, 15, 19, 0), null,
                    LocalDateTime.of(2026, 5, 15, 19, 0));
        }

        /** Stubs comunes al happy path: helper publicarVisitaActualizadaCliente no pete. */
        private void mockHelperVisita() {
            when(comandaRepository.findAllItemsActivosByVisita(VISITA_ID)).thenReturn(List.of());
            when(visitaEstadoMapper.toItemsVisitaResponse(List.of())).thenReturn(List.of());
        }

        @Test
        @DisplayName("Happy COCINA: EN_PREPARACION → LISTO, notificación PLATOS_LISTOS y WS publicados")
        void happyCocina_transicionaCreaPlatosListosYPublica() {
            Comanda comanda = comandaEnPreparacion(EstacionComanda.COCINA);
            Mesa mesa = mesa(VISITA_ID, "T-01", "Mesero1");

            when(estacionResolver.resolverEstaciones(auth)).thenReturn(Set.of(EstacionComanda.COCINA));
            when(comandaRepository.findById(COMANDA_ID)).thenReturn(Optional.of(comanda));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.of(mesa));
            when(comandaItemRepository.sumCantidadByComandaIdIn(Set.of(COMANDA_ID))).thenReturn(List.of());
            mockHelperVisita();

            service.marcarListo(COMANDA_ID, auth);

            // Estado y timestamp
            assertThat(comanda.getComandaEstado()).isEqualTo(EstadoComanda.LISTO);
            assertThat(comanda.getComandaFechaHoraListo()).isNotNull();

            // Persistencia de comanda
            verify(comandaRepository).save(comanda);

            // Notificación con tipo PLATOS_LISTOS y estado ACTIVA
            verify(notificacionRepository).save(argThat(n ->
                    n.getNotificacionTipo() == TipoNotificacion.PLATOS_LISTOS
                    && n.getNotificacionEstado() == EstadoNotificacion.ACTIVA
                    && n.getComanda() == comanda));

            // WS estación de producción
            verify(wsPublisher).publicarEventoProduccion(
                    eq(EstacionComanda.COCINA),
                    argThat(m -> m.tipo() == TipoEventoProduccion.ACTUALIZADA
                            && "LISTO".equals(m.nuevoEstado())));

            // WS cliente (visita)
            verify(wsPublisher).publicarVisitaActualizada(eq(VISITA_ID), any());

            // WS mapa de mesas
            verify(mesaWsPublisher).publicarActualizacionMesa(
                    eq(VISITA_ID), eq(MesaWsPublisher.TipoEventoMesa.NOTIFICACION));
        }

        @Test
        @DisplayName("Happy BARRA: EN_PREPARACION → LISTO, notificación BEBIDAS_LISTAS y WS publicados")
        void happyBarra_creaBebidasListas() {
            Comanda comanda = comandaEnPreparacion(EstacionComanda.BARRA);
            Mesa mesa = mesa(VISITA_ID, "T-02", "Mesero2");

            when(estacionResolver.resolverEstaciones(auth)).thenReturn(Set.of(EstacionComanda.BARRA));
            when(comandaRepository.findById(COMANDA_ID)).thenReturn(Optional.of(comanda));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.of(mesa));
            when(comandaItemRepository.sumCantidadByComandaIdIn(Set.of(COMANDA_ID))).thenReturn(List.of());
            mockHelperVisita();

            service.marcarListo(COMANDA_ID, auth);

            assertThat(comanda.getComandaEstado()).isEqualTo(EstadoComanda.LISTO);
            assertThat(comanda.getComandaFechaHoraListo()).isNotNull();

            verify(notificacionRepository).save(argThat(n ->
                    n.getNotificacionTipo() == TipoNotificacion.BEBIDAS_LISTAS
                    && n.getNotificacionEstado() == EstadoNotificacion.ACTIVA
                    && n.getComanda() == comanda));

            verify(wsPublisher).publicarEventoProduccion(
                    eq(EstacionComanda.BARRA),
                    argThat(m -> m.tipo() == TipoEventoProduccion.ACTUALIZADA
                            && "LISTO".equals(m.nuevoEstado())));

            verify(wsPublisher).publicarVisitaActualizada(eq(VISITA_ID), any());
            verify(mesaWsPublisher).publicarActualizacionMesa(
                    eq(VISITA_ID), eq(MesaWsPublisher.TipoEventoMesa.NOTIFICACION));
        }

        @Test
        @DisplayName("Estado no EN_PREPARACION (PENDIENTE): lanza BusinessException INVALID_STATE 409")
        void estadoNoEnPreparacion_lanzaInvalidState() {
            Comanda comanda = comanda(COMANDA_ID, VISITA_ID, EstacionComanda.COCINA,
                    EstadoComanda.PENDIENTE, null, null,
                    LocalDateTime.of(2026, 5, 15, 19, 0));

            when(estacionResolver.resolverEstaciones(auth)).thenReturn(Set.of(EstacionComanda.COCINA));
            when(comandaRepository.findById(COMANDA_ID)).thenReturn(Optional.of(comanda));

            assertThatThrownBy(() -> service.marcarListo(COMANDA_ID, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(be.getCode()).isEqualTo("NEG-002");
                    });

            verify(comandaRepository, never()).save(any());
            verifyNoInteractions(notificacionRepository);
        }

        @Test
        @DisplayName("Estación ajena (sólo BARRA, comanda COCINA): lanza BusinessException ACCESS_DENIED 403")
        void estacionAjena_lanzaAccessDenied() {
            Comanda comanda = comandaEnPreparacion(EstacionComanda.COCINA);

            when(estacionResolver.resolverEstaciones(auth)).thenReturn(Set.of(EstacionComanda.BARRA));
            when(comandaRepository.findById(COMANDA_ID)).thenReturn(Optional.of(comanda));

            assertThatThrownBy(() -> service.marcarListo(COMANDA_ID, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(be.getCode()).isEqualTo("AUTH-002");
                    });

            verify(comandaRepository, never()).save(any());
            verifyNoInteractions(notificacionRepository);
        }

        @Test
        @DisplayName("Comanda inexistente: lanza ResourceNotFoundException")
        void comandaInexistente_lanzaResourceNotFound() {
            when(estacionResolver.resolverEstaciones(auth)).thenReturn(Set.of(EstacionComanda.COCINA));
            when(comandaRepository.findById(COMANDA_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.marcarListo(COMANDA_ID, auth))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("marcarListo con items preciados: map lambda ejecutada + total en WS cliente")
        void marcarListo_conItemsPreciados_mapLambdaYTotalWs() {
            Comanda comanda = comandaEnPreparacion(EstacionComanda.COCINA);
            Mesa mesa = mesa(VISITA_ID, "T-01", "Mesero1");
            Producto prod = Producto.builder().productoId(1L).productoNombre("P").build();
            ComandaItem ci = ComandaItem.builder()
                    .producto(prod).comandaItemCantidad(2)
                    .comandaItemPrecio(new BigDecimal("20000")).build();

            when(estacionResolver.resolverEstaciones(auth)).thenReturn(Set.of(EstacionComanda.COCINA));
            when(comandaRepository.findById(COMANDA_ID)).thenReturn(Optional.of(comanda));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.of(mesa));
            List<Object[]> sumRows = new ArrayList<>();
            sumRows.add(new Object[]{COMANDA_ID, 3L});
            when(comandaItemRepository.sumCantidadByComandaIdIn(Set.of(COMANDA_ID))).thenReturn(sumRows);
            when(notificacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(comandaRepository.findAllItemsActivosByVisita(VISITA_ID)).thenReturn(List.of(ci));

            service.marcarListo(COMANDA_ID, auth);

            org.mockito.ArgumentCaptor<co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage> captor =
                    org.mockito.ArgumentCaptor.forClass(co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage.class);
            verify(wsPublisher).publicarVisitaActualizada(eq(VISITA_ID), captor.capture());
            assertThat(captor.getValue().getTotal()).isEqualByComparingTo(new BigDecimal("40000"));
        }

        @Test
        @DisplayName("marcarListo cocina → crea notificación PLATOS_LISTOS con empleado null")
        void marcarListo_creaNotificacionConEmpleadoNull() {
            // Given
            Long comandaId = 20L;
            Long visitaId  = 10L;
            Comanda comanda = Comanda.builder()
                    .comandaId(comandaId)
                    .comandaEstacion(EstacionComanda.COCINA)
                    .comandaEstado(EstadoComanda.EN_PREPARACION)
                    .visita(Visita.builder().visitaId(visitaId).build())
                    .build();
            Mesa mesa = Mesa.builder()
                    .visitaId(visitaId).mesaIdentificador("T-01")
                    .mesero(Empleado.builder().usuarioId(4L).build())
                    .build();

            when(estacionResolver.resolverEstaciones(auth))
                    .thenReturn(java.util.Set.of(EstacionComanda.COCINA));
            when(comandaRepository.findById(comandaId)).thenReturn(java.util.Optional.of(comanda));
            when(mesaRepository.findByVisita_VisitaId(visitaId)).thenReturn(java.util.Optional.of(mesa));
            when(comandaItemRepository.sumCantidadByComandaIdIn(any())).thenReturn(java.util.List.of());
            when(comandaRepository.findAllItemsActivosByVisita(visitaId)).thenReturn(java.util.List.of());
            when(visitaEstadoMapper.toItemsVisitaResponse(any())).thenReturn(java.util.List.of());
            when(notificacionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // When
            service.marcarListo(comandaId, auth);

            // Then
            org.mockito.ArgumentCaptor<co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion> cap =
                    org.mockito.ArgumentCaptor.forClass(co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion.class);
            verify(notificacionRepository).save(cap.capture());
            assertThat(cap.getValue().getEmpleado()).isNull();
            assertThat(cap.getValue().getNotificacionTipo()).isEqualTo(TipoNotificacion.PLATOS_LISTOS);
        }
    }
}
