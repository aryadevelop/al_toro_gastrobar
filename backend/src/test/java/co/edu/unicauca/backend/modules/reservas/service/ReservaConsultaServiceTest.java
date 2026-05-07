package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.modules.reservas.dto.response.ListadoReservasResponse;
import co.edu.unicauca.backend.modules.reservas.dto.response.ReservaDetalleResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaConsultaMapper;
import co.edu.unicauca.backend.modules.reservas.mapper.ReservaMapper;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.TipoReserva;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Test unitario para {@link ReservaConsultaService}.
 *
 * <p>Verifica el listado de reservas del día con resumen por zona y el detalle
 * de una reserva con pre-orden y abonos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ReservaConsultaService")
class ReservaConsultaServiceTest {

    @Mock ReservaRepository reservaRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock ComandaItemRepository comandaItemRepository;
    @Mock AbonoRepository abonoRepository;
    @Spy ReservaConsultaMapper reservaConsultaMapper;
    @Mock ReservaMapper reservaMapper;

    @InjectMocks ReservaConsultaService reservaConsultaService;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Cliente cliente(Long id, String nombre, String telefono) {
        Cliente c = new Cliente();
        c.setUsuarioId(id);
        c.setClienteNombre(nombre);
        c.setClienteTelefono(telefono);
        return c;
    }

    private Zona zona(Long id, String nombre) {
        Zona z = new Zona();
        z.setZonaId(id);
        z.setZonaNombre(nombre);
        return z;
    }

    private Decoracion decoracion(Long id, String nombre) {
        Decoracion d = new Decoracion();
        d.setDecoracionId(id);
        d.setDecoracionNombre(nombre);
        return d;
    }

    private Reserva reserva(Long id, LocalDateTime llegada, Integer personas, EstadoReserva estado,
                            TipoReserva tipo, Cliente cliente, Zona zona, Decoracion decoracion) {
        return Reserva.builder()
                .reservaId(id)
                .reservaFechaHoraLlegada(llegada)
                .reservaNumeroPersonas(personas)
                .reservaEstado(estado)
                .reservaTipo(tipo)
                .cliente(cliente)
                .zona(zona)
                .decoracion(decoracion)
                .build();
    }

    private Comanda comanda(Long id, Long reservaId, EstadoComanda estado) {
        return Comanda.builder()
                .comandaId(id)
                .comandaEstado(estado)
                .build();
    }

    private ComandaItem comandaItem(Long id, Comanda comanda, String productoNombre,
                                     Integer cantidad, BigDecimal precio) {
        Producto producto = Producto.builder()
                .productoNombre(productoNombre)
                .build();
        return ComandaItem.builder()
                .comandaItemId(id)
                .comanda(comanda)
                .producto(producto)
                .comandaItemCantidad(cantidad)
                .comandaItemPrecio(precio)
                .build();
    }

    private Abono abono(Long id, BigDecimal monto) {
        return Abono.builder()
                .abonoId(id)
                .abonoMonto(monto)
                .abonoFechaHora(LocalDateTime.now())
                .build();
    }

    // ── Tests de listarReservasDelDia ───────────────────────────────────────

    @Test
    @DisplayName("listarReservasDelDia sin parámetros retorna listado del día actual")
    void listarReservasDelDia_sinParametros_retornaListadoDelDiaActual() {
        // Arrange
        LocalDate hoy = LocalDate.now();
        Zona zonaTerraza = zona(1L, "TERRAZA");
        Zona zonaInterior = zona(2L, "INTERIOR");
        Cliente c1 = cliente(10L, "Juan Pérez", "3001234567");
        Cliente c2 = cliente(11L, "Ana Gómez", "3107654321");

        Reserva r1 = reserva(100L, hoy.atTime(19, 0), 4, EstadoReserva.CONFIRMADA,
                TipoReserva.BASICA, c1, zonaTerraza, null);
        Reserva r2 = reserva(101L, hoy.atTime(20, 0), 2, EstadoReserva.PENDIENTE,
                TipoReserva.BASICA, c2, zonaInterior, null);

        when(reservaRepository.findReservasActivasDelDia(any(), any(), any()))
                .thenReturn(List.of(r1, r2));

        // Act
        ListadoReservasResponse response = reservaConsultaService.listarReservasDelDia(null, null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getReservas()).hasSize(2);
        assertThat(response.getReservas().get(0).getReservaId()).isEqualTo(100L);
        assertThat(response.getReservas().get(0).getClienteNombre()).isEqualTo("Juan Pérez");
        assertThat(response.getReservas().get(0).getZonaId()).isEqualTo(1L);
        assertThat(response.getReservas().get(0).getZonaNombre()).isEqualTo("TERRAZA");
        assertThat(response.getReservas().get(1).getReservaId()).isEqualTo(101L);
        assertThat(response.getReservas().get(1).getZonaId()).isEqualTo(2L);
        assertThat(response.getReservas().get(1).getZonaNombre()).isEqualTo("INTERIOR");

        assertThat(response.getResumenZonas()).hasSize(2);
        assertThat(response.getResumenZonas().get(0).getZonaId()).isEqualTo(1L);
        assertThat(response.getResumenZonas().get(0).getZonaNombre()).isEqualTo("TERRAZA");
        assertThat(response.getResumenZonas().get(0).getCantidadReservas()).isEqualTo(1);
        assertThat(response.getResumenZonas().get(1).getZonaId()).isEqualTo(2L);
        assertThat(response.getResumenZonas().get(1).getZonaNombre()).isEqualTo("INTERIOR");
        assertThat(response.getResumenZonas().get(1).getCantidadReservas()).isEqualTo(1);
    }

    @Test
    @DisplayName("listarReservasDelDia con fecha específica retorna listado de la fecha especificada")
    void listarReservasDelDia_conFecha_retornaListadoDeLaFechaEspecificada() {
        // Arrange
        LocalDate fecha = LocalDate.of(2026, 5, 15);
        Zona zonaTerraza = zona(1L, "TERRAZA");
        Cliente c1 = cliente(10L, "Carlos Ruiz", "3009876543");

        Reserva r1 = reserva(200L, fecha.atTime(18, 30), 6, EstadoReserva.CONFIRMADA,
                TipoReserva.ESPECIAL, c1, zonaTerraza, null);

        when(reservaRepository.findReservasActivasDelDia(any(), any(), any()))
                .thenReturn(List.of(r1));

        // Act
        ListadoReservasResponse response = reservaConsultaService.listarReservasDelDia(fecha, null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getReservas()).hasSize(1);
        assertThat(response.getReservas().get(0).getReservaId()).isEqualTo(200L);
        assertThat(response.getReservas().get(0).getClienteNombre()).isEqualTo("Carlos Ruiz");
        assertThat(response.getReservas().get(0).getNumeroPersonas()).isEqualTo(6);

        assertThat(response.getResumenZonas()).hasSize(1);
        assertThat(response.getResumenZonas().get(0).getZonaId()).isEqualTo(1L);
        assertThat(response.getResumenZonas().get(0).getCantidadReservas()).isEqualTo(1);
    }

    @Test
    @DisplayName("listarReservasDelDia con identificador retorna búsqueda por ID")
    void listarReservasDelDia_conIdentificador_retornaBusquedaPorId() {
        // Arrange
        Long identificador = 300L;
        Zona zonaInterior = zona(2L, "INTERIOR");
        Cliente c1 = cliente(12L, "María López", "3112345678");

        Reserva r1 = reserva(300L, LocalDate.now().atTime(21, 0), 3, EstadoReserva.PENDIENTE,
                TipoReserva.BASICA, c1, zonaInterior, null);

        when(reservaRepository.findReservasActivasPorIdentificador(eq(identificador), any()))
                .thenReturn(List.of(r1));

        // Act
        ListadoReservasResponse response = reservaConsultaService.listarReservasDelDia(null, identificador);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getReservas()).hasSize(1);
        assertThat(response.getReservas().get(0).getReservaId()).isEqualTo(300L);
        assertThat(response.getReservas().get(0).getClienteNombre()).isEqualTo("María López");
        assertThat(response.getReservas().get(0).getZonaId()).isEqualTo(2L);

        assertThat(response.getResumenZonas()).hasSize(1);
        assertThat(response.getResumenZonas().get(0).getZonaId()).isEqualTo(2L);
        assertThat(response.getResumenZonas().get(0).getCantidadReservas()).isEqualTo(1);
    }

    @Test
    @DisplayName("listarReservasDelDia sin resultados lanza BusinessException con mensaje específico")
    void listarReservasDelDia_sinResultados_lanzaBusinessException() {
        // Arrange
        when(reservaRepository.findReservasActivasDelDia(any(), any(), any()))
                .thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> reservaConsultaService.listarReservasDelDia(null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("No hay reservas activas o pendientes para la fecha o identificador especificado")
                .extracting("code", "status")
                .containsExactly(ErrorCode.ENTITY_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("listarReservasDelDia con identificador inexistente lanza BusinessException")
    void listarReservasDelDia_identificadorInexistente_lanzaBusinessException() {
        // Arrange
        Long identificadorInexistente = 9999L;
        when(reservaRepository.findReservasActivasPorIdentificador(eq(identificadorInexistente), any()))
                .thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> reservaConsultaService.listarReservasDelDia(null, identificadorInexistente))
                .isInstanceOf(BusinessException.class)
                .hasMessage("No hay reservas activas o pendientes para la fecha o identificador especificado")
                .extracting("code", "status")
                .containsExactly(ErrorCode.ENTITY_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
    }

    // ── Tests de obtenerDetalleReserva ──────────────────────────────────────

    @Test
    @DisplayName("obtenerDetalleReserva con reserva existente retorna detalle completo con pre-orden y abonos")
    void obtenerDetalleReserva_conReservaExistente_retornaDetalle() {
        // Arrange
        Long reservaId = 500L;
        Zona zonaTerraza = zona(1L, "TERRAZA");
        Decoracion deco = decoracion(10L, "Romántica");
        Cliente c1 = cliente(20L, "Pedro Martínez", "3201234567");

        Reserva reserva = reserva(reservaId, LocalDate.now().plusDays(1).atTime(19, 30),
                4, EstadoReserva.CONFIRMADA, TipoReserva.ESPECIAL, c1, zonaTerraza, deco);

        Comanda comanda = comanda(1000L, reservaId, EstadoComanda.PRE_RESERVA);
        ComandaItem item1 = comandaItem(1L, comanda, "Bandeja Paisa", 2, new BigDecimal("18000"));
        ComandaItem item2 = comandaItem(2L, comanda, "Jugo Natural", 2, new BigDecimal("5000"));

        Abono abono1 = abono(1L, new BigDecimal("50000"));

        co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenItemResponse preOrdenItem1 =
                co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenItemResponse.builder()
                        .comandaItemId(1L)
                        .productoNombre("Bandeja Paisa")
                        .cantidad(2)
                        .precioUnitario(new BigDecimal("18000"))
                        .build();

        co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenItemResponse preOrdenItem2 =
                co.edu.unicauca.backend.modules.reservas.dto.response.PreOrdenItemResponse.builder()
                        .comandaItemId(2L)
                        .productoNombre("Jugo Natural")
                        .cantidad(2)
                        .precioUnitario(new BigDecimal("5000"))
                        .build();

        co.edu.unicauca.backend.modules.reservas.dto.response.AbonoItemResponse abonoItem =
                co.edu.unicauca.backend.modules.reservas.dto.response.AbonoItemResponse.builder()
                        .abonoId(1L)
                        .monto(new BigDecimal("50000"))
                        .build();

        ReservaDetalleResponse mockResponse = ReservaDetalleResponse.builder()
                .reservaId(500L)
                .numeroPersonas(4)
                .estado("CONFIRMADA")
                .tipo("ESPECIAL")
                .zonaId(1L)
                .zonaNombre("TERRAZA")
                .decoracionId(10L)
                .decoracionNombre("Romántica")
                .clienteTelefono("3201234567")
                .preOrdenItems(List.of(preOrdenItem1, preOrdenItem2))
                .preOrdenTotal(new BigDecimal("46000"))
                .abonos(List.of(abonoItem))
                .totalAbonado(new BigDecimal("50000"))
                .modificable(true)
                .build();

        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));
        when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(reservaId, EstadoComanda.PRE_RESERVA))
                .thenReturn(List.of(comanda));
        when(comandaItemRepository.findByComanda_ComandaId(1000L))
                .thenReturn(List.of(item1, item2));
        when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(reservaId))
                .thenReturn(List.of(abono1));
        when(reservaMapper.toDetalleResponse(eq(reserva), any(), any()))
                .thenReturn(mockResponse);

        // Act
        ReservaDetalleResponse response = reservaConsultaService.obtenerDetalleReserva(reservaId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getReservaId()).isEqualTo(500L);
        assertThat(response.getNumeroPersonas()).isEqualTo(4);
        assertThat(response.getEstado()).isEqualTo("CONFIRMADA");
        assertThat(response.getTipo()).isEqualTo("ESPECIAL");
        assertThat(response.getZonaId()).isEqualTo(1L);
        assertThat(response.getZonaNombre()).isEqualTo("TERRAZA");
        assertThat(response.getDecoracionId()).isEqualTo(10L);
        assertThat(response.getDecoracionNombre()).isEqualTo("Romántica");
        assertThat(response.getClienteTelefono()).isEqualTo("3201234567");

        assertThat(response.getPreOrdenItems()).isNotEmpty();
        assertThat(response.getPreOrdenTotal()).isNotNull();

        assertThat(response.getAbonos()).isNotEmpty();
        assertThat(response.getTotalAbonado()).isEqualTo(new BigDecimal("50000"));
    }

    @Test
    @DisplayName("obtenerDetalleReserva con reserva inexistente lanza BusinessException con ENTITY_NOT_FOUND")
    void obtenerDetalleReserva_reservaNoExiste_lanzaBusinessException() {
        // Arrange
        Long reservaId = 999L;
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> reservaConsultaService.obtenerDetalleReserva(reservaId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Reserva no encontrada con ID: 999")
                .extracting("code", "status")
                .containsExactly(ErrorCode.ENTITY_NOT_FOUND.getCode(), HttpStatus.NOT_FOUND);
    }
}
