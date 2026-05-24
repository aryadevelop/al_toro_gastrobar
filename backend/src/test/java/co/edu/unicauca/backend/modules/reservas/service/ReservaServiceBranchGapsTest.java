package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.request.CrearReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.request.ModificarReservaRequest;
import co.edu.unicauca.backend.modules.reservas.dto.request.PreOrdenItemRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.DecoracionDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.DisponibilidadResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ModificarReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ZonaDisponibleResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests dirigidos a cubrir las ramas residuales reportadas por JaCoCo
 * en {@link ReservaService}, no cubiertas por los otros tests del módulo.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReservaService — branch coverage gaps")
class ReservaServiceBranchGapsTest {

    @Mock ReservaRepository reservaRepository;
    @Mock DecoracionRepository decoracionRepository;
    @Mock ZonaRepository zonaRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock ComandaItemRepository comandaItemRepository;
    @Mock ReservaMapper reservaMapper;
    @Mock AbonoRepository abonoRepository;
    @Mock ReservaValidador reservaValidador;
    @Mock DisponibilidadConsultador disponibilidadConsultador;
    @Mock PreOrdenGestor preOrdenGestor;
    @Mock MensajeWhatsAppBuilder mensajeWhatsAppBuilder;
    @Mock NotificacionWsPublisher wsPublisher;

    @InjectMocks ReservaService service;

    private static final String EMAIL = "cliente@test.com";
    private static final Long CLIENTE_ID = 10L;
    private static final Long RESERVA_ID = 1L;

    private Cliente clienteMock;
    private LocalDateTime fechaFutura;

    @BeforeEach
    void setUp() {
        clienteMock = buildCliente(CLIENTE_ID, EMAIL);
        fechaFutura = LocalDate.now().plusDays(2).atTime(19, 0);

        when(clienteRepository.findByUsuario_UsuarioEmail(any())).thenReturn(Optional.of(clienteMock));
        when(reservaValidador.esHorarioValido(any(), any(), any())).thenReturn(true);
        when(reservaValidador.estaBloqueda(any())).thenReturn(false);
        when(reservaValidador.tieneDecoracionConCosto(any())).thenReturn(false);
        when(reservaMapper.toResponse(any(), anyBoolean(), any()))
                .thenReturn(ReservaResponse.builder().reservaId(RESERVA_ID).build());
        when(reservaMapper.toModificarResponse(any(), anyBoolean(), any()))
                .thenReturn(ModificarReservaResponse.builder().reservaId(RESERVA_ID).build());
        when(mensajeWhatsAppBuilder.construirMensaje(any(), any())).thenReturn("wa-msg");
        when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(any(), any()))
                .thenReturn(List.of());
    }

    // ────────────────────────────────────────────────────────────────────────
    // calcularAbonoNeto (línea 568): rama ANTICIPO=false → negate()
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("construirNotificacionWhatsApp ejercitando calcularAbonoNeto con DEVOLUCION")
    class CalcularAbonoNetoConDevolucion {

        @Test
        @DisplayName("ESPECIAL → BASICA con abono mixto (ANTICIPO+DEVOLUCION) >0 y sin platos → WhatsApp")
        void especialABasica_conAbonoNetoPositivo_devuelveMensaje() {
            Reserva especial = reservaConTipo(TipoReserva.ESPECIAL, EstadoReserva.PENDIENTE);
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(especial));
            stubDisponibilidadLibreModificar();
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Abonos: anticipo 100, devolución 30 → neto 70 (ejercita ambos lados del ternario)
            Abono anticipo = abono(TipoAbono.ANTICIPO, new BigDecimal("100"));
            Abono devolucion = abono(TipoAbono.DEVOLUCION, new BigDecimal("30"));
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID))
                    .thenReturn(List.of(anticipo, devolucion));

            // No hay items pre-orden → totalPlatos = 0 < totalAbonos = 70
            ModificarReservaRequest req = modificarRequest(fechaFutura, 2, null);
            service.modificarReserva(RESERVA_ID, EMAIL, req);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // determinarTipoReserva (línea 872) y modificarReserva (334)
    // anyMatch con item esMenuEspecial=false
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("preOrden con todos los ítems esMenuEspecial=false (rama anyMatch=false)")
    class PreOrdenSinMenuEspecial {

        @Test
        @DisplayName("crearReserva BASICA con pre-orden sin menú especial → tipo BASICA")
        void crearBasicaConPreOrdenSinMenuEspecial() {
            stubDisponibilidadLibreCrear();

            PreOrdenItemRequest carta = new PreOrdenItemRequest();
            carta.setProductoId(1L);
            carta.setCantidad(1);
            carta.setEsMenuEspecial(false);

            CrearReservaRequest req = crearRequest(fechaFutura, 2, null, null, List.of(carta));
            when(reservaRepository.save(any())).thenAnswer(inv -> {
                Reserva r = inv.getArgument(0);
                setField(r, Reserva.class, "reservaId", RESERVA_ID);
                return r;
            });

            service.crearReserva(EMAIL, req);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // calcularValorEspecial (líneas 758, 762)
    // Ejecutado vía modificarReserva ESPECIAL→ESPECIAL
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calcularValorEspecial — ramas de decoración null y costo null")
    class CalcularValorEspecial {

        @Test
        @DisplayName("ESPECIAL→ESPECIAL con decoración nueva sin costoAdicional + items con producto sin menuEspecial")
        void decoracionConCostoNull_eItemsNoMenuEspecial_noSumaNada() {
            Reserva especial = reservaConTipo(TipoReserva.ESPECIAL, EstadoReserva.PENDIENTE);
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(especial));
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Items de pre-orden original con producto NO menú especial (rama false del for)
            Producto prodNoMenu = Producto.builder().productoId(99L).menuEspecial(false).build();
            ComandaItem item = ComandaItem.builder()
                    .producto(prodNoMenu)
                    .comandaItemPrecio(new BigDecimal("10"))
                    .comandaItemCantidad(1)
                    .build();
            Comanda comanda = Comanda.builder().comandaId(50L).build();
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(RESERVA_ID,
                    co.edu.unicauca.backend.shared.enums.EstadoComanda.PRE_RESERVA))
                    .thenReturn(List.of(comanda));
            when(comandaItemRepository.findByComanda_ComandaId(50L)).thenReturn(List.of(item));

            // Nueva pre-orden con menú especial mantiene ESPECIAL→ESPECIAL
            PreOrdenItemRequest menu = new PreOrdenItemRequest();
            menu.setProductoId(99L);
            menu.setCantidad(1);
            menu.setEsMenuEspecial(true);

            // Decoración nueva sin costo adicional (rama: costoAdicional == null)
            Decoracion decSinCosto = new Decoracion();
            setField(decSinCosto, Decoracion.class, "decoracionId", 5L);
            // decoracionCostoAdicional queda null
            when(decoracionRepository.findById(5L)).thenReturn(Optional.of(decSinCosto));
            // Pero el validador la considera "con costo" para mantener ESPECIAL
            when(reservaValidador.tieneDecoracionConCosto(decSinCosto)).thenReturn(true);

            stubDisponibilidadLibreModificarConDecoraciones(5L);

            ModificarReservaRequest req = modificarRequest(fechaFutura, 2, List.of(menu));
            setField(req, ModificarReservaRequest.class, "decoracionId", 5L);

            service.modificarReserva(RESERVA_ID, EMAIL, req);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // validarDisponibilidadYCapacidad: ramas 670, 713, 715
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validarDisponibilidadYCapacidad — ramas de capacidad")
    class ValidarDisponibilidadYCapacidad {

        @Test
        @DisplayName("Decoración solicitada NO está libre → BusinessException (rama 670)")
        void decoracionNoLibre_lanzaBusinessException() {
            Decoracion dec = new Decoracion();
            setField(dec, Decoracion.class, "decoracionId", 8L);
            setField(dec, Decoracion.class, "decoracionCostoAdicional", new BigDecimal("0"));
            when(decoracionRepository.findById(8L)).thenReturn(Optional.of(dec));

            // Disponibilidad SIN la decoración solicitada
            ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder().zonaId(1L).capacidad(100).build();
            DisponibilidadResponse disp = DisponibilidadResponse.builder()
                    .disponible(true)
                    .zonas(List.of(zonaDto))
                    .decoraciones(List.of()) // vacío → decoracionLibre = false
                    .build();
            when(disponibilidadConsultador.consultarParaNuevaReserva(any(), anyInt(), any(), any()))
                    .thenReturn(disp);

            CrearReservaRequest req = crearRequest(fechaFutura, 2, 8L, null, null);

            assertThatThrownBy(() -> service.crearReserva(EMAIL, req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Sin zona y todas las zonas sin capacidad suficiente → BusinessException (ramas 713, 715)")
        void sinZonaYSinCapacidad_lanzaBusinessException() {
            ZonaDisponibleResponse zonaPequena = ZonaDisponibleResponse.builder()
                    .zonaId(1L).capacidad(5).build();
            DisponibilidadResponse disp = DisponibilidadResponse.builder()
                    .disponible(true)
                    .zonas(List.of(zonaPequena))
                    .decoraciones(List.of())
                    .build();
            when(disponibilidadConsultador.consultarParaNuevaReserva(any(), anyInt(), any(), any()))
                    .thenReturn(disp);
            when(reservaRepository.sumPersonasByZonaEnDia(any(), any(), any(), any())).thenReturn(0);

            // 100 personas: la única zona (cap 5) no cubre → hayZonaConCapacidad = false
            CrearReservaRequest req = crearRequest(fechaFutura, 100, null, null, null);

            assertThatThrownBy(() -> service.crearReserva(EMAIL, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("capacidad");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // crearReserva ramas 161/180: preOrden = lista vacía (no null)
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("crearReserva con pre-orden vacía (no null) → no delega a PreOrdenGestor")
    class CrearReservaPreOrdenVacia {

        @Test
        @DisplayName("preOrden = [] → flujo CONFIRMADA/BASICA sin invocar PreOrdenGestor")
        void preOrdenVacia_noLlamaPreOrdenGestor() {
            stubDisponibilidadLibreCrear();
            CrearReservaRequest req = crearRequest(fechaFutura, 2, null, null, List.of());
            when(reservaRepository.save(any())).thenAnswer(inv -> {
                Reserva r = inv.getArgument(0);
                setField(r, Reserva.class, "reservaId", RESERVA_ID);
                return r;
            });

            service.crearReserva(EMAIL, req);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // modificarReserva ramas 334, 340, 356, 382 — preOrden vacía / sin menú especial
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("modificarReserva — preOrden vacía o sin menú especial")
    class ModificarConPreOrdenSinMenuEspecial {

        @Test
        @DisplayName("BASICA→BASICA con preOrden de ítems carta (todos esMenuEspecial=false)")
        void basicaABasica_conPreOrdenSinMenu() {
            Reserva basica = reservaConTipo(TipoReserva.BASICA, EstadoReserva.CONFIRMADA);
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(basica));
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            stubDisponibilidadLibreModificar();
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(anyLong()))
                    .thenReturn(List.of());

            PreOrdenItemRequest carta = new PreOrdenItemRequest();
            carta.setProductoId(1L);
            carta.setCantidad(1);
            carta.setEsMenuEspecial(false);

            ModificarReservaRequest req = modificarRequest(fechaFutura, 2, List.of(carta));
            service.modificarReserva(RESERVA_ID, EMAIL, req);
        }

        @Test
        @DisplayName("BASICA→BASICA con preOrden = [] (no null)")
        void basicaABasica_conPreOrdenVacia() {
            Reserva basica = reservaConTipo(TipoReserva.BASICA, EstadoReserva.CONFIRMADA);
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(basica));
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            stubDisponibilidadLibreModificar();
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(anyLong()))
                    .thenReturn(List.of());

            ModificarReservaRequest req = modificarRequest(fechaFutura, 2, List.of());
            service.modificarReserva(RESERVA_ID, EMAIL, req);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // crearReserva rama 155: hoy + BASICA (short-circuit segundo operando)
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("crearReserva — combinaciones de fecha y tipo")
    class CrearReservaFechaTipoCombinaciones {

        @Test
        @DisplayName("Fecha hoy + BASICA → no lanza (cond1 true, cond2 false)")
        void hoyBasica_noLanza() {
            stubDisponibilidadLibreCrear();
            LocalDateTime hoy = LocalDate.now().atTime(20, 0);
            CrearReservaRequest req = crearRequest(hoy, 2, null, null, null);
            when(reservaRepository.save(any())).thenAnswer(inv -> {
                Reserva r = inv.getArgument(0);
                setField(r, Reserva.class, "reservaId", RESERVA_ID);
                return r;
            });

            service.crearReserva(EMAIL, req);
        }

        @Test
        @DisplayName("Fecha hoy + tipo ESPECIAL (preOrden con menú) → lanza anticipación mínima")
        void hoyEspecial_lanzaAnticipaciónMinima() {
            stubDisponibilidadLibreCrear();
            LocalDateTime hoy = LocalDate.now().atTime(20, 0);
            PreOrdenItemRequest menu = new PreOrdenItemRequest();
            menu.setProductoId(1L);
            menu.setCantidad(1);
            menu.setEsMenuEspecial(true);

            CrearReservaRequest req = crearRequest(hoy, 2, null, null, List.of(menu));

            assertThatThrownBy(() -> service.crearReserva(EMAIL, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("anticipación");
        }
    }

    @Nested
    @DisplayName("crearReserva con decoración Y zona → validarCompatibilidadDecoracionZona")
    class DecoracionYZonaSimultaneos {

        @Test
        @DisplayName("Ambos decoracionId y zonaId no-null → ejercita validarCompatibilidadDecoracionZona")
        void decoracionYZona_invocaCompatibilidad() {
            Decoracion dec = new Decoracion();
            setField(dec, Decoracion.class, "decoracionId", 2L);
            when(decoracionRepository.findById(2L)).thenReturn(Optional.of(dec));

            co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona zona =
                    new co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona();
            setField(zona, co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona.class, "zonaId", 3L);
            setField(zona, co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona.class, "zonaCapacidadPersonas", 50);
            when(zonaRepository.findById(3L)).thenReturn(Optional.of(zona));

            ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder().zonaId(3L).capacidad(100).build();
            DecoracionDisponibleResponse decDto = DecoracionDisponibleResponse.builder().decoracionId(2L).build();
            when(disponibilidadConsultador.consultarParaNuevaReserva(any(), anyInt(), any(), any()))
                    .thenReturn(DisponibilidadResponse.builder()
                            .disponible(true)
                            .zonas(List.of(zonaDto))
                            .decoraciones(List.of(decDto))
                            .build());
            when(reservaRepository.sumPersonasByZonaEnDia(any(), any(), any(), any())).thenReturn(0);
            when(reservaRepository.save(any())).thenAnswer(inv -> {
                Reserva r = inv.getArgument(0);
                setField(r, Reserva.class, "reservaId", RESERVA_ID);
                return r;
            });

            CrearReservaRequest req = crearRequest(fechaFutura, 2, 2L, 3L, null);
            service.crearReserva(EMAIL, req);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // validarYCargarDecoracionZona línea 628 — combinaciones decoración/zona
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("crearReserva — combinaciones de decoración y zona")
    class DecoracionYZonaCombinaciones {

        @Test
        @DisplayName("Solo zona (sin decoración) → no invoca validarCompatibilidad")
        void soloZona_sinDecoracion() {
            co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona zona =
                    new co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona();
            setField(zona, co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona.class, "zonaId", 5L);
            setField(zona, co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona.class, "zonaCapacidadPersonas", 100);
            when(zonaRepository.findById(5L)).thenReturn(Optional.of(zona));

            ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder().zonaId(5L).capacidad(100).build();
            when(disponibilidadConsultador.consultarParaNuevaReserva(any(), anyInt(), any(), any()))
                    .thenReturn(DisponibilidadResponse.builder()
                            .disponible(true)
                            .zonas(List.of(zonaDto))
                            .decoraciones(List.of())
                            .build());
            when(reservaRepository.sumPersonasByZonaEnDia(any(), any(), any(), any())).thenReturn(0);
            when(reservaRepository.save(any())).thenAnswer(inv -> {
                Reserva r = inv.getArgument(0);
                setField(r, Reserva.class, "reservaId", RESERVA_ID);
                return r;
            });

            CrearReservaRequest req = crearRequest(fechaFutura, 2, null, 5L, null);
            service.crearReserva(EMAIL, req);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // construirNotificacionWhatsApp ramas 840/849 — abonos = 0
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("construirNotificacionWhatsApp — sin abonos")
    class ConstruirNotificacionSinAbonos {

        @Test
        @DisplayName("ESPECIAL → BASICA sin abonos → totalAbonos=0, rama corto-circuita")
        void especialABasica_sinAbonos() {
            Reserva especial = reservaConTipo(TipoReserva.ESPECIAL, EstadoReserva.PENDIENTE);
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(especial));
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            stubDisponibilidadLibreModificar();
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID))
                    .thenReturn(List.of());

            ModificarReservaRequest req = modificarRequest(fechaFutura, 2, null);
            service.modificarReserva(RESERVA_ID, EMAIL, req);
        }

        @Test
        @DisplayName("ESPECIAL → BASICA con abonos > 0 pero platos >= abonos → no mensaje")
        void especialABasica_conAbonosPeroPlatosMayores() {
            Reserva especial = reservaConTipo(TipoReserva.ESPECIAL, EstadoReserva.PENDIENTE);
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(especial));
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            stubDisponibilidadLibreModificar();

            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID))
                    .thenReturn(List.of(abono(TipoAbono.ANTICIPO, new BigDecimal("50"))));

            // Items en pre-orden nueva con precio total > abono
            Producto prod = Producto.builder().productoId(1L).menuEspecial(false).build();
            ComandaItem item = ComandaItem.builder()
                    .producto(prod)
                    .comandaItemPrecio(new BigDecimal("100"))
                    .comandaItemCantidad(1)
                    .build();
            Comanda c = Comanda.builder().comandaId(33L).build();
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(eq(RESERVA_ID), any()))
                    .thenReturn(List.of(c));
            when(comandaItemRepository.findByComanda_ComandaId(33L)).thenReturn(List.of(item));

            ModificarReservaRequest req = modificarRequest(fechaFutura, 2, null);
            service.modificarReserva(RESERVA_ID, EMAIL, req);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Líneas 628, 686, 693, 698 — combinaciones decoración/zona en modificar
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("modificarReserva con zona y decoración (ramas validar/capacidad)")
    class ModificarConZonaYDecoracion {

        @Test
        @DisplayName("Modificar con solo decoración (zona=null) → ejercita rama 628 decoracion!=null zona==null")
        void soloDecoracion() {
            Reserva basica = reservaConTipo(TipoReserva.BASICA, EstadoReserva.CONFIRMADA);
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(basica));
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Decoracion dec = new Decoracion();
            setField(dec, Decoracion.class, "decoracionId", 9L);
            when(decoracionRepository.findById(9L)).thenReturn(Optional.of(dec));

            // Disponibilidad incluye la decoración solicitada
            ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder().zonaId(1L).capacidad(100).build();
            DecoracionDisponibleResponse decDto = DecoracionDisponibleResponse.builder().decoracionId(9L).build();
            when(disponibilidadConsultador.consultarParaModificacion(any(), anyInt(), anyLong(), any(), any()))
                    .thenReturn(DisponibilidadResponse.builder()
                            .disponible(true)
                            .zonas(List.of(zonaDto))
                            .decoraciones(List.of(decDto))
                            .build());
            when(reservaRepository.sumPersonasByZonaEnDia(any(), any(), any(), any())).thenReturn(0);
            when(reservaRepository.sumPersonasByZonaEnDiaExcluyendo(anyLong(), any(), any(), any(), anyLong()))
                    .thenReturn(0);
            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(anyLong()))
                    .thenReturn(List.of());

            ModificarReservaRequest req = modificarRequest(fechaFutura, 2, null);
            setField(req, ModificarReservaRequest.class, "decoracionId", 9L);

            service.modificarReserva(RESERVA_ID, EMAIL, req);
        }

        @Test
        @DisplayName("Modificar con zona NO presente en disponibilidad → BusinessException (686)")
        void zonaNoLibre_lanzaException() {
            Reserva basica = reservaConTipo(TipoReserva.BASICA, EstadoReserva.CONFIRMADA);
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(basica));

            co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona zona =
                    new co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona();
            setField(zona, co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona.class, "zonaId", 7L);
            setField(zona, co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona.class, "zonaCapacidadPersonas", 100);
            when(zonaRepository.findById(7L)).thenReturn(Optional.of(zona));

            // Disponibilidad SIN la zona 7L
            ZonaDisponibleResponse otraZona = ZonaDisponibleResponse.builder().zonaId(99L).capacidad(50).build();
            when(disponibilidadConsultador.consultarParaModificacion(any(), anyInt(), anyLong(), any(), any()))
                    .thenReturn(DisponibilidadResponse.builder()
                            .disponible(true)
                            .zonas(List.of(otraZona))
                            .decoraciones(List.of())
                            .build());

            ModificarReservaRequest req = modificarRequest(fechaFutura, 2, null);
            setField(req, ModificarReservaRequest.class, "zonaId", 7L);

            assertThatThrownBy(() -> service.modificarReserva(RESERVA_ID, EMAIL, req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Modificar con zona excede capacidad → BusinessException (693, 698)")
        void zonaExcedeCapacidad_lanzaException() {
            Reserva basica = reservaConTipo(TipoReserva.BASICA, EstadoReserva.CONFIRMADA);
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(basica));

            co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona zona =
                    new co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona();
            setField(zona, co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona.class, "zonaId", 4L);
            setField(zona, co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona.class, "zonaCapacidadPersonas", 10);
            when(zonaRepository.findById(4L)).thenReturn(Optional.of(zona));

            ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder().zonaId(4L).capacidad(10).build();
            when(disponibilidadConsultador.consultarParaModificacion(any(), anyInt(), anyLong(), any(), any()))
                    .thenReturn(DisponibilidadResponse.builder()
                            .disponible(true)
                            .zonas(List.of(zonaDto))
                            .decoraciones(List.of())
                            .build());
            // 8 personas existentes + 5 nuevas > capacidad 10
            when(reservaRepository.sumPersonasByZonaEnDiaExcluyendo(anyLong(), any(), any(), any(), anyLong()))
                    .thenReturn(8);

            ModificarReservaRequest req = modificarRequest(fechaFutura, 5, null);
            setField(req, ModificarReservaRequest.class, "zonaId", 4L);

            assertThatThrownBy(() -> service.modificarReserva(RESERVA_ID, EMAIL, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("capacidad");
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Línea 763 — item con producto.menuEspecial=true en calcularValorEspecial
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calcularValorEspecial — rama producto.menuEspecial=true")
    class CalcularValorEspecialConMenu {

        @Test
        @DisplayName("ESPECIAL→ESPECIAL con item de producto menuEspecial=true → suma al valor")
        void itemMenuEspecial_sumaValor() {
            Reserva especial = reservaConTipo(TipoReserva.ESPECIAL, EstadoReserva.PENDIENTE);
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(especial));
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Item original CON menuEspecial=true (ejercita rama true del for)
            Producto prodMenu = Producto.builder().productoId(50L).menuEspecial(true).build();
            ComandaItem item = ComandaItem.builder()
                    .producto(prodMenu)
                    .comandaItemPrecio(new BigDecimal("20"))
                    .comandaItemCantidad(1)
                    .build();
            Comanda c = Comanda.builder().comandaId(60L).build();
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(eq(RESERVA_ID), any()))
                    .thenReturn(List.of(c));
            when(comandaItemRepository.findByComanda_ComandaId(60L)).thenReturn(List.of(item));

            // Nueva pre-orden mantiene ESPECIAL
            PreOrdenItemRequest menu = new PreOrdenItemRequest();
            menu.setProductoId(50L);
            menu.setCantidad(1);
            menu.setEsMenuEspecial(true);

            stubDisponibilidadLibreModificar();
            ModificarReservaRequest req = modificarRequest(fechaFutura, 2, List.of(menu));
            service.modificarReserva(RESERVA_ID, EMAIL, req);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // calcularValorEspecial — escala por cantidad del ítem, no por personas
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calcularValorEspecial — escala por cantidad del ítem")
    class CalcularValorEspecialEscalaPorCantidad {

        @Test
        @DisplayName("ESPECIAL→ESPECIAL con cantidad=15 y personas=2: usa cantidad del ítem para calcular el valor")
        void calcularValorEspecial_menuEspecial_escalaPorCantidadNoPorPersonas() {
            Reserva especial = reservaConTipo(TipoReserva.ESPECIAL, EstadoReserva.PENDIENTE);
            // personas=2, menú especial con cantidad=15 y precio=10
            // Implementación correcta: valorAnterior = 10 × 15 = 150
            // Implementación con bug (×personas): valorAnterior = 10 × 2 = 20
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(especial));
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Producto prodMenu = Producto.builder().productoId(50L).menuEspecial(true).build();
            ComandaItem itemAntes = ComandaItem.builder()
                    .producto(prodMenu)
                    .comandaItemPrecio(new BigDecimal("10"))
                    .comandaItemCantidad(15)
                    .build();
            Comanda c = Comanda.builder().comandaId(55L).build();
            // El mismo stub se usa para calcularValorEspecial (valorAnterior) y para valorNuevo
            // → valorNuevo = precio × cantidad = 10 × 15 = 150 siempre
            // Con código correcto: valorAnterior=150 == valorNuevo=150 → NO se llama a construirMensaje
            // Con bug (×personas): valorAnterior=20 ≠ valorNuevo=150 → sí se llamaría (test fallaría)
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(eq(RESERVA_ID), any()))
                    .thenReturn(List.of(c));
            when(comandaItemRepository.findByComanda_ComandaId(55L)).thenReturn(List.of(itemAntes));

            PreOrdenItemRequest menu = new PreOrdenItemRequest();
            menu.setProductoId(50L);
            menu.setCantidad(15);
            menu.setEsMenuEspecial(true);

            stubDisponibilidadLibreModificar();
            ModificarReservaRequest req = modificarRequest(fechaFutura, 2, List.of(menu));
            service.modificarReserva(RESERVA_ID, EMAIL, req);

            // Si calcularValorEspecial usara personas (2) en lugar de cantidad (15),
            // valorAnterior=20 ≠ valorNuevo=150 y se habría invocado construirMensaje.
            // Con la implementación correcta valorAnterior==valorNuevo → no hay notificación.
            verify(mensajeWhatsAppBuilder, never()).construirMensaje(any(), any());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Línea 849 — BASICA→BASICA con abonos>0 y platos>=abonos
    // ────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("BASICA→BASICA con abonos pero platos cubren")
    class BasicaConAbonosCubiertos {

        @Test
        @DisplayName("totalAbonos>0 y totalPlatos>=totalAbonos → no mensaje WhatsApp")
        void abonosCubiertosPorPlatos_sinMensaje() {
            Reserva basica = reservaConTipo(TipoReserva.BASICA, EstadoReserva.CONFIRMADA);
            when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(basica));
            when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            stubDisponibilidadLibreModificar();

            when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID))
                    .thenReturn(List.of(abono(TipoAbono.ANTICIPO, new BigDecimal("50"))));

            // Items en pre-orden con precio total >= abono (rama false del && segundo operando)
            Producto prod = Producto.builder().productoId(1L).menuEspecial(false).build();
            ComandaItem item = ComandaItem.builder()
                    .producto(prod)
                    .comandaItemPrecio(new BigDecimal("80"))
                    .comandaItemCantidad(1)
                    .build();
            Comanda c = Comanda.builder().comandaId(70L).build();
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(eq(RESERVA_ID), any()))
                    .thenReturn(List.of(c));
            when(comandaItemRepository.findByComanda_ComandaId(70L)).thenReturn(List.of(item));

            ModificarReservaRequest req = modificarRequest(fechaFutura, 2, null);
            service.modificarReserva(RESERVA_ID, EMAIL, req);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    private Cliente buildCliente(Long id, String email) {
        Usuario usuario = new Usuario();
        setField(usuario, Usuario.class, "usuarioEmail", email);
        Cliente cliente = new Cliente();
        setField(cliente, Cliente.class, "usuarioId", id);
        try {
            Field f = Cliente.class.getSuperclass().getDeclaredField("usuario");
            f.setAccessible(true);
            f.set(cliente, usuario);
        } catch (NoSuchFieldException e) {
            try {
                Field f = Cliente.class.getDeclaredField("usuario");
                f.setAccessible(true);
                f.set(cliente, usuario);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cliente;
    }

    private void setField(Object target, Class<?> clazz, String fieldName, Object value) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (NoSuchFieldException e) {
            Class<?> parent = clazz.getSuperclass();
            if (parent != null && parent != Object.class) {
                setField(target, parent, fieldName, value);
            } else {
                throw new RuntimeException("Field not found: " + fieldName, e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CrearReservaRequest crearRequest(LocalDateTime fecha, int personas,
                                              Long decoracionId, Long zonaId,
                                              List<PreOrdenItemRequest> preOrden) {
        CrearReservaRequest r = new CrearReservaRequest();
        setField(r, CrearReservaRequest.class, "fechaHoraLlegada", fecha);
        setField(r, CrearReservaRequest.class, "numeroPersonas", personas);
        if (decoracionId != null) setField(r, CrearReservaRequest.class, "decoracionId", decoracionId);
        if (zonaId != null) setField(r, CrearReservaRequest.class, "zonaId", zonaId);
        if (preOrden != null) setField(r, CrearReservaRequest.class, "preOrden", preOrden);
        return r;
    }

    private ModificarReservaRequest modificarRequest(LocalDateTime fecha, int personas,
                                                       List<PreOrdenItemRequest> preOrden) {
        ModificarReservaRequest r = new ModificarReservaRequest();
        setField(r, ModificarReservaRequest.class, "fechaHoraLlegada", fecha);
        setField(r, ModificarReservaRequest.class, "numeroPersonas", personas);
        if (preOrden != null) setField(r, ModificarReservaRequest.class, "preOrden", preOrden);
        return r;
    }

    private Reserva reservaConTipo(TipoReserva tipo, EstadoReserva estado) {
        return Reserva.builder()
                .reservaId(RESERVA_ID)
                .cliente(clienteMock)
                .reservaFechaHoraLlegada(fechaFutura)
                .reservaNumeroPersonas(2)
                .reservaEstado(estado)
                .reservaTipo(tipo)
                .build();
    }

    private Abono abono(TipoAbono tipo, BigDecimal monto) {
        return Abono.builder().abonoTipo(tipo).abonoMonto(monto).build();
    }

    private void stubDisponibilidadLibreCrear() {
        ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder().zonaId(1L).capacidad(100).build();
        when(disponibilidadConsultador.consultarParaNuevaReserva(any(), anyInt(), any(), any()))
                .thenReturn(DisponibilidadResponse.builder()
                        .disponible(true)
                        .zonas(List.of(zonaDto))
                        .decoraciones(List.of())
                        .build());
        when(reservaRepository.sumPersonasByZonaEnDia(any(), any(), any(), any())).thenReturn(0);
    }

    private void stubDisponibilidadLibreModificar() {
        ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder().zonaId(1L).capacidad(100).build();
        when(disponibilidadConsultador.consultarParaModificacion(any(), anyInt(), anyLong(), any(), any()))
                .thenReturn(DisponibilidadResponse.builder()
                        .disponible(true)
                        .zonas(List.of(zonaDto))
                        .decoraciones(List.of())
                        .build());
        when(reservaRepository.sumPersonasByZonaEnDiaExcluyendo(anyLong(), any(), any(), any(), anyLong())).thenReturn(0);
    }

    private void stubDisponibilidadLibreModificarConDecoraciones(Long... decIds) {
        ZonaDisponibleResponse zonaDto = ZonaDisponibleResponse.builder().zonaId(1L).capacidad(100).build();
        List<DecoracionDisponibleResponse> decs = java.util.Arrays.stream(decIds)
                .map(id -> DecoracionDisponibleResponse.builder().decoracionId(id).build())
                .toList();
        when(disponibilidadConsultador.consultarParaModificacion(any(), anyInt(), anyLong(), any(), any()))
                .thenReturn(DisponibilidadResponse.builder()
                        .disponible(true)
                        .zonas(List.of(zonaDto))
                        .decoraciones(decs)
                        .build());
        when(reservaRepository.sumPersonasByZonaEnDiaExcluyendo(anyLong(), any(), any(), any(), anyLong())).thenReturn(0);
    }
}
