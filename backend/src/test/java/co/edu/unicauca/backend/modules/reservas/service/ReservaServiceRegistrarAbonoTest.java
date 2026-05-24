package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.ReservaActualizadaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.request.RegistrarAbonoRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.RegistrarAbonoResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ResumenPagoResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.AbonoMapper;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link ReservaService#registrarAbono}.
 *
 * <p>Cubre el registro de anticipos y devoluciones: persistencia del abono, transición
 * a {@code DEVUELTA} cuando la devolución salda el neto, publicación WebSocket con el tipo
 * de evento correcto, uso del bloqueo pesimista (D8) y propagación de errores de validación.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReservaService — registrarAbono")
class ReservaServiceRegistrarAbonoTest {

    @Mock ReservaRepository reservaRepository;
    @Mock DecoracionRepository decoracionRepository;
    @Mock ZonaRepository zonaRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock ComandaItemRepository comandaItemRepository;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock AbonoRepository abonoRepository;
    @Mock ReservaMapper reservaMapper;
    @Mock AbonoMapper abonoMapper;
    @Mock ReservaValidador reservaValidador;
    @Mock DisponibilidadConsultador disponibilidadConsultador;
    @Mock PreOrdenGestor preOrdenGestor;
    @Mock MensajeWhatsAppBuilder mensajeWhatsAppBuilder;
    @Mock NotificacionWsPublisher wsPublisher;

    @InjectMocks
    ReservaService service;

    private static final Long RESERVA_ID = 1L;
    private static final Long COMANDA_ID = 10L;
    private static final String EMAIL_CAJERO = "cajero@test.com";

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Reserva buildReserva(TipoReserva tipo, EstadoReserva estado) {
        Usuario usuario = new Usuario();
        setField(usuario, Usuario.class, "usuarioEmail", "cliente@test.com");
        Cliente cliente = new Cliente();
        setField(cliente, Cliente.class, "usuario", usuario);
        setField(cliente, Cliente.class, "clienteNombre", "Cliente Prueba");

        Reserva r = new Reserva();
        setField(r, Reserva.class, "reservaId", RESERVA_ID);
        setField(r, Reserva.class, "cliente", cliente);
        setField(r, Reserva.class, "reservaTipo", tipo);
        setField(r, Reserva.class, "reservaEstado", estado);
        setField(r, Reserva.class, "reservaFechaHoraLlegada", LocalDateTime.now().plusDays(2));
        setField(r, Reserva.class, "reservaNumeroPersonas", 2);
        setField(r, Reserva.class, "reservaFechaCreacion", LocalDateTime.now().minusDays(2));
        return r;
    }

    private Empleado buildCajero() {
        return new Empleado();
    }

    private ComandaItem buildItem(BigDecimal precio, int cantidad) {
        Producto producto = new Producto();
        setField(producto, Producto.class, "menuEspecial", false);
        ComandaItem item = new ComandaItem();
        setField(item, ComandaItem.class, "producto", producto);
        setField(item, ComandaItem.class, "comandaItemPrecio", precio);
        setField(item, ComandaItem.class, "comandaItemCantidad", cantidad);
        return item;
    }

    private Abono buildAbono(BigDecimal monto, TipoAbono tipo) {
        Abono a = new Abono();
        setField(a, Abono.class, "abonoId", 99L);
        setField(a, Abono.class, "abonoMonto", monto);
        setField(a, Abono.class, "abonoTipo", tipo);
        return a;
    }

    private RegistrarAbonoRequest buildRequest(TipoAbono tipo, BigDecimal monto) {
        return RegistrarAbonoRequest.builder()
                .tipo(tipo)
                .monto(monto)
                .metodo(MetodoPago.EFECTIVO)
                .fechaHora(LocalDateTime.now())
                .build();
    }

    private void stubPreOrden(List<ComandaItem> items) {
        Comanda comanda = new Comanda();
        setField(comanda, Comanda.class, "comandaId", COMANDA_ID);
        when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(RESERVA_ID, EstadoComanda.PRE_RESERVA))
                .thenReturn(List.of(comanda));
        when(comandaItemRepository.findByComanda_ComandaId(COMANDA_ID)).thenReturn(items);
    }

    private void stubAbonos(List<Abono> abonos) {
        when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID))
                .thenReturn(abonos);
    }

    private void setField(Object target, Class<?> clazz, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = clazz.getDeclaredField(fieldName);
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

    @BeforeEach
    void commonStubs() {
        when(empleadoRepository.findByUsuario_UsuarioEmail(EMAIL_CAJERO))
                .thenReturn(Optional.of(buildCajero()));
        when(abonoRepository.save(any(Abono.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(reservaRepository.save(any(Reserva.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(abonoMapper.toEntity(any(), any(), any()))
                .thenReturn(buildAbono(new BigDecimal("1.00"), TipoAbono.ANTICIPO));
        when(abonoMapper.toResumenPago(any(), any(), any(), any()))
                .thenReturn(ResumenPagoResponse.builder().reservaId(RESERVA_ID).build());
        when(abonoMapper.toRegistrarResponse(any(), any()))
                .thenReturn(RegistrarAbonoResponse.builder().abonoId(99L).build());
    }

    // ── Anticipos ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("Anticipo con datos válidos → crea abono y retorna resumen")
    void registrarAnticipo_datosValidos_creaAbonoYRetornaResumen() {
        Reserva reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.CONFIRMADA);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        stubPreOrden(List.of(buildItem(new BigDecimal("100000.00"), 1)));
        stubAbonos(List.of());

        RegistrarAbonoResponse resultado = service.registrarAbono(
                RESERVA_ID, buildRequest(TipoAbono.ANTICIPO, new BigDecimal("50000.00")), EMAIL_CAJERO);

        assertThat(resultado).isNotNull();
        verify(abonoRepository).save(any(Abono.class));
        verify(abonoMapper).toRegistrarResponse(any(Abono.class), any(ResumenPagoResponse.class));
    }

    @Test
    @DisplayName("Anticipo sobre reserva inexistente → 404 y no guarda")
    void registrarAnticipo_reservaInexistente_lanza404() {
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarAbono(
                RESERVA_ID, buildRequest(TipoAbono.ANTICIPO, new BigDecimal("50000.00")), EMAIL_CAJERO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(abonoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Anticipo con cajero inexistente → 404 y no guarda")
    void registrarAnticipo_cajeroInexistente_lanza404() {
        Reserva reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.CONFIRMADA);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        when(empleadoRepository.findByUsuario_UsuarioEmail(EMAIL_CAJERO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarAbono(
                RESERVA_ID, buildRequest(TipoAbono.ANTICIPO, new BigDecimal("50000.00")), EMAIL_CAJERO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(abonoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Anticipo con estado inválido → el validador propaga 409 y no guarda")
    void registrarAnticipo_estadoInvalido_propaga409() {
        Reserva reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.PENDIENTE);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        doThrow(new BusinessException(ErrorCode.INVALID_STATE,
                "Solo se pueden registrar anticipos en reservas confirmadas.", HttpStatus.CONFLICT))
                .when(reservaValidador).validarElegibilidadAbono(reserva, TipoAbono.ANTICIPO);

        assertThatThrownBy(() -> service.registrarAbono(
                RESERVA_ID, buildRequest(TipoAbono.ANTICIPO, new BigDecimal("50000.00")), EMAIL_CAJERO))
                .isInstanceOf(BusinessException.class);

        verify(abonoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Anticipo que excede el total → el validador propaga 409, no guarda ni publica WS")
    void registrarAnticipo_excedeTotal_propaga409() {
        Reserva reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.CONFIRMADA);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        stubPreOrden(List.of(buildItem(new BigDecimal("100000.00"), 1)));
        stubAbonos(List.of());
        doThrow(new BusinessException(ErrorCode.BUSINESS_ERROR,
                "El monto total abonado no puede exceder el valor de la reserva.", HttpStatus.CONFLICT))
                .when(reservaValidador).validarMontoAnticipo(any(), any(), any());

        assertThatThrownBy(() -> service.registrarAbono(
                RESERVA_ID, buildRequest(TipoAbono.ANTICIPO, new BigDecimal("200000.00")), EMAIL_CAJERO))
                .isInstanceOf(BusinessException.class);

        verify(abonoRepository, never()).save(any());
        verify(wsPublisher, never()).publicarReservaActualizada(any());
    }

    @Test
    @DisplayName("Anticipo exitoso → publica WS con tipoEvento ANTICIPO")
    void registrarAnticipo_publicaWsConTipoEventoANTICIPO() {
        Reserva reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.CONFIRMADA);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        stubPreOrden(List.of(buildItem(new BigDecimal("100000.00"), 1)));
        stubAbonos(List.of());

        service.registrarAbono(
                RESERVA_ID, buildRequest(TipoAbono.ANTICIPO, new BigDecimal("50000.00")), EMAIL_CAJERO);

        ArgumentCaptor<ReservaActualizadaWsMessage> captor =
                ArgumentCaptor.forClass(ReservaActualizadaWsMessage.class);
        verify(wsPublisher).publicarReservaActualizada(captor.capture());
        assertThat(captor.getValue().getTipoEvento()).isEqualTo("ANTICIPO");
    }

    // ── Devoluciones ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Devolución parcial → mantiene CANCELADA y crea abono")
    void registrarDevolucion_parcial_mantieneCANCELADA_creaAbono() {
        Reserva reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.CANCELADA);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        stubPreOrden(List.of(buildItem(new BigDecimal("100000.00"), 1)));
        // Neto = 50.000; se devuelven 20.000 → queda 30.000 (parcial)
        stubAbonos(List.of(buildAbono(new BigDecimal("50000.00"), TipoAbono.ANTICIPO)));

        service.registrarAbono(
                RESERVA_ID, buildRequest(TipoAbono.DEVOLUCION, new BigDecimal("20000.00")), EMAIL_CAJERO);

        verify(abonoRepository).save(any(Abono.class));
        assertThat(reserva.getReservaEstado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    @DisplayName("Devolución completa → transiciona a DEVUELTA y guarda la reserva")
    void registrarDevolucion_completa_transicionaADEVUELTA_yGuardaReserva() {
        Reserva reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.CANCELADA);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        stubPreOrden(List.of(buildItem(new BigDecimal("100000.00"), 1)));
        // Neto = 50.000; se devuelve exactamente 50.000 → DEVUELTA
        stubAbonos(List.of(buildAbono(new BigDecimal("50000.00"), TipoAbono.ANTICIPO)));

        service.registrarAbono(
                RESERVA_ID, buildRequest(TipoAbono.DEVOLUCION, new BigDecimal("50000.00")), EMAIL_CAJERO);

        ArgumentCaptor<Reserva> captor = ArgumentCaptor.forClass(Reserva.class);
        verify(reservaRepository).save(captor.capture());
        assertThat(captor.getValue().getReservaEstado()).isEqualTo(EstadoReserva.DEVUELTA);
    }

    @Test
    @DisplayName("Devolución que excede el neto → el validador propaga 409 y no guarda")
    void registrarDevolucion_excedeNeto_propaga409() {
        Reserva reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.CANCELADA);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        stubPreOrden(List.of(buildItem(new BigDecimal("100000.00"), 1)));
        stubAbonos(List.of(buildAbono(new BigDecimal("50000.00"), TipoAbono.ANTICIPO)));
        doThrow(new BusinessException(ErrorCode.BUSINESS_ERROR,
                "El monto a devolver no puede exceder el total abonado.", HttpStatus.CONFLICT))
                .when(reservaValidador).validarMontoDevolucion(any(), any());

        assertThatThrownBy(() -> service.registrarAbono(
                RESERVA_ID, buildRequest(TipoAbono.DEVOLUCION, new BigDecimal("90000.00")), EMAIL_CAJERO))
                .isInstanceOf(BusinessException.class);

        verify(abonoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Devolución exitosa → publica WS con tipoEvento DEVOLUCION")
    void registrarDevolucion_publicaWsConTipoEventoDEVOLUCION() {
        Reserva reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.CANCELADA);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        stubPreOrden(List.of(buildItem(new BigDecimal("100000.00"), 1)));
        stubAbonos(List.of(buildAbono(new BigDecimal("50000.00"), TipoAbono.ANTICIPO)));

        service.registrarAbono(
                RESERVA_ID, buildRequest(TipoAbono.DEVOLUCION, new BigDecimal("20000.00")), EMAIL_CAJERO);

        ArgumentCaptor<ReservaActualizadaWsMessage> captor =
                ArgumentCaptor.forClass(ReservaActualizadaWsMessage.class);
        verify(wsPublisher).publicarReservaActualizada(captor.capture());
        assertThat(captor.getValue().getTipoEvento()).isEqualTo("DEVOLUCION");
    }

    // ── Validaciones transversales ──────────────────────────────────────────

    @Test
    @DisplayName("Fecha inválida → el validador propaga 400 y no guarda")
    void registrarAbono_fechaInvalida_propaga400() {
        Reserva reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.CONFIRMADA);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        doThrow(new BusinessException(ErrorCode.VALIDATION_ERROR,
                "La fecha no puede ser futura ni anterior a la fecha de creación de la reserva",
                HttpStatus.BAD_REQUEST))
                .when(reservaValidador).validarFechaAbono(any(), any());

        assertThatThrownBy(() -> service.registrarAbono(
                RESERVA_ID, buildRequest(TipoAbono.ANTICIPO, new BigDecimal("50000.00")), EMAIL_CAJERO))
                .isInstanceOf(BusinessException.class);

        verify(abonoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Usa findByIdForUpdate (bloqueo pesimista) y nunca findById")
    void registrarAbono_usaFindByIdForUpdate_noFindById() {
        Reserva reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.CONFIRMADA);
        when(reservaRepository.findByIdForUpdate(RESERVA_ID)).thenReturn(Optional.of(reserva));
        stubPreOrden(List.of(buildItem(new BigDecimal("100000.00"), 1)));
        stubAbonos(List.of());

        service.registrarAbono(
                RESERVA_ID, buildRequest(TipoAbono.ANTICIPO, new BigDecimal("50000.00")), EMAIL_CAJERO);

        verify(reservaRepository).findByIdForUpdate(RESERVA_ID);
        verify(reservaRepository, never()).findById(any());
    }
}
