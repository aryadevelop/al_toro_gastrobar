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
import co.edu.unicauca.backend.modules.reservas.dto.response.ResumenPagoResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.AbonoMapper;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link ReservaService#obtenerResumenPago}.
 *
 * <p>Verifica el cálculo del total tipo-aware (sin doble conteo del menú especial),
 * la consolidación de anticipos y devoluciones, y el manejo de la reserva inexistente.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReservaService — obtenerResumenPago")
class ReservaServiceResumenPagoTest {

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Reserva buildReserva(TipoReserva tipo, EstadoReserva estado, Decoracion decoracion) {
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
        setField(r, Reserva.class, "decoracion", decoracion);
        setField(r, Reserva.class, "reservaFechaCreacion", LocalDateTime.now().minusDays(1));
        return r;
    }

    private Decoracion buildDecoracion(BigDecimal costo) {
        Decoracion d = new Decoracion();
        setField(d, Decoracion.class, "decoracionId", 5L);
        setField(d, Decoracion.class, "decoracionCostoAdicional", costo);
        return d;
    }

    private ComandaItem buildItem(BigDecimal precio, int cantidad, boolean menuEspecial) {
        Producto producto = new Producto();
        setField(producto, Producto.class, "menuEspecial", menuEspecial);
        ComandaItem item = new ComandaItem();
        setField(item, ComandaItem.class, "producto", producto);
        setField(item, ComandaItem.class, "comandaItemPrecio", precio);
        setField(item, ComandaItem.class, "comandaItemCantidad", cantidad);
        return item;
    }

    private Abono buildAbono(BigDecimal monto, TipoAbono tipo) {
        Abono a = new Abono();
        setField(a, Abono.class, "abonoMonto", monto);
        setField(a, Abono.class, "abonoTipo", tipo);
        return a;
    }

    private void stubPreOrden(List<ComandaItem> items) {
        Comanda comanda = new Comanda();
        setField(comanda, Comanda.class, "comandaId", COMANDA_ID);
        when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(RESERVA_ID, EstadoComanda.PRE_RESERVA))
                .thenReturn(List.of(comanda));
        when(comandaItemRepository.findByComanda_ComandaId(COMANDA_ID)).thenReturn(items);
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

    // ── Pruebas ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("BASICA con platos → total es la suma de los platos")
    void resumenPago_reservaBasicaConPlatos_totalEsSumaDePlatos() {
        Reserva reserva = buildReserva(TipoReserva.BASICA, EstadoReserva.CONFIRMADA, null);
        when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        // 2 x 20.000 + 1 x 10.000 = 50.000
        stubPreOrden(List.of(
                buildItem(new BigDecimal("20000.00"), 2, false),
                buildItem(new BigDecimal("10000.00"), 1, false)));
        when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID))
                .thenReturn(List.of());

        service.obtenerResumenPago(RESERVA_ID);

        ArgumentCaptor<BigDecimal> totalCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(abonoMapper).toResumenPago(any(Reserva.class), totalCaptor.capture(),
                any(BigDecimal.class), any(BigDecimal.class));
        assertThat(totalCaptor.getValue()).isEqualByComparingTo(new BigDecimal("50000.00"));
    }

    @Test
    @DisplayName("ESPECIAL con menú y decoración → total sin doble conteo")
    void resumenPago_reservaEspecialConMenuYDecoracion_totalSinDobleConteo() {
        Decoracion decoracion = buildDecoracion(new BigDecimal("30000.00"));
        Reserva reserva = buildReserva(TipoReserva.ESPECIAL, EstadoReserva.CONFIRMADA, decoracion);
        when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        // decoración 30.000 + menú especial 40.000 x 2 (80.000) + plato carta 15.000 x 1 = 125.000
        stubPreOrden(List.of(
                buildItem(new BigDecimal("40000.00"), 2, true),
                buildItem(new BigDecimal("15000.00"), 1, false)));
        when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID))
                .thenReturn(List.of());

        service.obtenerResumenPago(RESERVA_ID);

        ArgumentCaptor<BigDecimal> totalCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(abonoMapper).toResumenPago(any(Reserva.class), totalCaptor.capture(),
                any(BigDecimal.class), any(BigDecimal.class));
        // El menú especial se cuenta una sola vez (vía calcularValorEspecial), no se duplica
        assertThat(totalCaptor.getValue()).isEqualByComparingTo(new BigDecimal("125000.00"));
    }

    @Test
    @DisplayName("Con anticipos y devoluciones → calcula totales agregados")
    void resumenPago_conAnticiposYDevoluciones_calculaNetoYPendientes() {
        Reserva reserva = buildReserva(TipoReserva.BASICA, EstadoReserva.CONFIRMADA, null);
        when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        stubPreOrden(List.of(buildItem(new BigDecimal("100000.00"), 1, false)));
        when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID))
                .thenReturn(List.of(
                        buildAbono(new BigDecimal("60000.00"), TipoAbono.ANTICIPO),
                        buildAbono(new BigDecimal("20000.00"), TipoAbono.ANTICIPO),
                        buildAbono(new BigDecimal("10000.00"), TipoAbono.DEVOLUCION)));

        service.obtenerResumenPago(RESERVA_ID);

        ArgumentCaptor<BigDecimal> totalCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> anticipadoCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> devueltoCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(abonoMapper).toResumenPago(any(Reserva.class), totalCaptor.capture(),
                anticipadoCaptor.capture(), devueltoCaptor.capture());
        assertThat(totalCaptor.getValue()).isEqualByComparingTo(new BigDecimal("100000.00"));
        assertThat(anticipadoCaptor.getValue()).isEqualByComparingTo(new BigDecimal("80000.00"));
        assertThat(devueltoCaptor.getValue()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }

    @Test
    @DisplayName("Sin abonos → totalAnticipado y totalDevuelto en cero")
    void resumenPago_sinAbonos_pendienteAbonarIgualTotal() {
        Reserva reserva = buildReserva(TipoReserva.BASICA, EstadoReserva.CONFIRMADA, null);
        when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.of(reserva));
        stubPreOrden(List.of(buildItem(new BigDecimal("75000.00"), 1, false)));
        when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(RESERVA_ID))
                .thenReturn(List.of());
        ResumenPagoResponse stub = ResumenPagoResponse.builder().reservaId(RESERVA_ID).build();
        when(abonoMapper.toResumenPago(any(), any(), any(), any())).thenReturn(stub);

        ResumenPagoResponse resultado = service.obtenerResumenPago(RESERVA_ID);

        ArgumentCaptor<BigDecimal> totalCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> anticipadoCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> devueltoCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(abonoMapper).toResumenPago(any(Reserva.class), totalCaptor.capture(),
                anticipadoCaptor.capture(), devueltoCaptor.capture());
        assertThat(resultado).isSameAs(stub);
        assertThat(totalCaptor.getValue()).isEqualByComparingTo(new BigDecimal("75000.00"));
        assertThat(anticipadoCaptor.getValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(devueltoCaptor.getValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Reserva inexistente → ResourceNotFoundException")
    void resumenPago_reservaInexistente_lanza404() {
        when(reservaRepository.findById(RESERVA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerResumenPago(RESERVA_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(abonoMapper, org.mockito.Mockito.never())
                .toResumenPago(any(), any(), any(), any());
    }
}
