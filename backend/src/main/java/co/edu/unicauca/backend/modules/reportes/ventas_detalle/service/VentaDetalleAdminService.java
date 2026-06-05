package co.edu.unicauca.backend.modules.reportes.ventas_detalle.service;


import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.ClienteVentaResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.DashboardDiarioResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.ItemVentaResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.MenuEspecialVentaResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.MesaVentaResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.MetodoPagoIngresoResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.PedidoListoResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.TipoVentaIngresoResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.TopProductoResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.ServicioAdicionalResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.VentaDetalleResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.VentaListadoItemResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.VentaListadoResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.repository.ComandaItemDetalleAdminRepository;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.repository.MesaDetalleAdminRepository;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.repository.VentaDetalleAdminRepository;
import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.reservas.repository.ReservaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import co.edu.unicauca.backend.shared.enums.RolNombre;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VentaDetalleAdminService {

    private static final String ALERTA_RESERVA_CANCELADA = "Esta venta esta asociada a una reserva cancelada. Verificar con administracion";

    private final VentaDetalleAdminRepository ventaRepository;
    private final MesaDetalleAdminRepository mesaRepository;
    private final ComandaItemDetalleAdminRepository itemRepository;
    private final ReservaRepository reservaRepository;
    private final VisitaRepository visitaRepository;
    private final ComandaRepository comandaRepository;
    private final SesionRepository sesionRepository;

    @Transactional(readOnly = true)
    public VentaDetalleResponse obtenerDetalle(Long visitaId) {
        Venta venta = ventaRepository.findDetalleByVisitaId(visitaId)
                .orElseThrow(() -> new ResourceNotFoundException("Venta", visitaId));

        Cliente cliente = venta.getVisita().getCliente();
        Reserva reserva = venta.getVisita().getReserva();

        Mesa mesa = mesaRepository.findById(visitaId).orElse(null);

        List<ComandaItem> items = itemRepository.findItemsByVisitaId(visitaId);
        if (items.isEmpty() && reserva != null) {
            items = itemRepository.findItemsByReservaId(reserva.getReservaId());
        }

        boolean menuEspecial = reserva != null
                && reserva.getReservaNumeroPersonas() != null
                && reserva.getReservaNumeroPersonas() >= 10
                && items.stream().anyMatch(i -> Boolean.TRUE.equals(i.getProducto().getMenuEspecial()));

        List<ItemVentaResponse> detalleItems = menuEspecial
                ? Collections.emptyList()
                : mapearItems(items);

        MenuEspecialVentaResponse menuEspecialResponse = menuEspecial
                ? construirMenuEspecial(items, reserva)
                : null;

        List<ServicioAdicionalResponse> serviciosAdicionales = construirServiciosAdicionales(reserva);

        return VentaDetalleResponse.builder()
                .ventaId(venta.getVisitaId())
                .fechaHora(venta.getVentaFechaHora())
                .cliente(construirCliente(cliente, reserva != null))
                .mesa(construirMesa(mesa))
                .meseroNombre(mesa != null && mesa.getMesero() != null
                        ? mesa.getMesero().getEmpleadoNombre()
                        : null)
                .items(detalleItems)
                .menuEspecial(menuEspecialResponse)
                .serviciosAdicionales(serviciosAdicionales)
                .notaReserva(reserva != null ? reserva.getReservaNotas() : null)
                .subtotal(venta.getVentaSubtotal())
                .total(venta.getVentaTotal())
                .metodoPago(venta.getVentaMetodo())
                .estadoReserva(obtenerEstadoReserva(reserva))
                .alertaReservaCancelada(obtenerAlertaReservaCancelada(reserva))
                .build();
    }

    @Transactional(readOnly = true)
    public VentaListadoResponse listarVentas(String ventaId,
                                             String desdeFecha,
                                             String hastaFecha,
                                             String metodoPago) {
        Long ventaIdNumeric = parseVentaId(ventaId);
        LocalDate desde = parseFecha(desdeFecha, "desdeFecha");
        LocalDate hasta = parseFecha(hastaFecha, "hastaFecha");
        validateFechaRange(desde, hasta);
        MetodoPago metodoPagoEnum = parseMetodoPago(metodoPago);

        if (ventaIdNumeric == null && desde == null && hasta == null && metodoPagoEnum == null) {
            LocalDate hoy = LocalDate.now();
            desde = hoy;
            hasta = hoy;
        }

        LocalDateTime desdeDateTime = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDateTime = hasta != null ? hasta.atTime(LocalTime.MAX) : null;

        List<Venta> ventas;
        try {
            ventas = ventaRepository.buscarVentasPorFiltros(
                    ventaIdNumeric, desdeDateTime, hastaDateTime, metodoPagoEnum);
        } catch (DataAccessException ex) {
            log.error("Error al consultar las ventas: ventaId={}, desde={}, hasta={}, metodoPago={}",
                    ventaId, desdeDateTime, hastaDateTime, metodoPagoEnum, ex);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "Error al consultar las ventas. Intenta nuevamente.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (ventas == null) {
            ventas = Collections.emptyList();
        }

        List<VentaListadoItemResponse> items = ventas.stream()
                .map(this::mapearVentaListado)
                .collect(Collectors.toList());

        BigDecimal totalPeriodo = ventas.stream()
                .map(venta -> venta != null && venta.getVentaTotal() != null ? venta.getVentaTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return VentaListadoResponse.builder()
                .ventas(items)
                .totalPeriodo(totalPeriodo)
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardDiarioResponse obtenerDashboardDiario(String fecha) {
        LocalDate targetDate = fecha == null || fecha.isBlank()
                ? LocalDate.now()
                : parseFecha(fecha, "fecha");
        validateFechaIsNotFuture(targetDate);

        LocalDateTime inicio = targetDate.atStartOfDay();
        LocalDateTime fin = targetDate.atTime(LocalTime.MAX);

        List<Venta> ventasRaw = ventaRepository.buscarVentasPorFiltros(null, inicio, fin, null);
        List<Venta> ventas = ventasRaw == null ? Collections.emptyList() : ventasRaw;

        BigDecimal totalIngresos = ventas.stream()
                .map(venta -> venta != null && venta.getVentaTotal() != null ? venta.getVentaTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MetodoPagoIngresoResponse> ingresosPorMetodoPago = calcularIngresosPorMetodoPago(ventas);

        List<Long> visitasMenuEspecial = itemRepository.findVisitasConMenuEspecialEnVentasDelDia(inicio, fin);
        BigDecimal totalMenuEspecial = ventas.stream()
                .filter(v -> v != null && v.getVisita() != null && visitasMenuEspecial.contains(v.getVisitaId()))
                .map(v -> v.getVentaTotal() != null ? v.getVentaTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCarta = totalIngresos.subtract(totalMenuEspecial);

        List<TipoVentaIngresoResponse> ingresosPorTipoVenta = List.of(
                TipoVentaIngresoResponse.builder()
                        .tipoVenta("MENU_ESPECIAL")
                        .total(totalMenuEspecial)
                        .build(),
                TipoVentaIngresoResponse.builder()
                        .tipoVenta("CARTA")
                        .total(totalCarta)
                        .build()
        );

        List<Object[]> topProductosRaw = itemRepository.findTopProductosVendidosEnVentasDelDia(inicio, fin, org.springframework.data.domain.PageRequest.of(0, 3));
        List<TopProductoResponse> productosMasVendidos = topProductosRaw.stream()
                .map(row -> TopProductoResponse.builder()
                        .nombre(row[0] != null ? row[0].toString() : null)
                        .cantidadVendida(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                        .build())
                .collect(Collectors.toList());

        List<co.edu.unicauca.backend.modules.reservas.entity.Reserva> reservasActivas = reservaRepository.findReservasActivasDelDia(
                inicio, fin, List.of(EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE));
        long reservasActivasHoy = reservasActivas.size();
        long personasReservadasHoy = reservasActivas.stream()
                .map(reserva -> reserva.getReservaNumeroPersonas() != null ? reserva.getReservaNumeroPersonas() : 0)
                .mapToLong(Integer::longValue)
                .sum();

        long visitasActivasHoy = visitaRepository.countByVisitaFechaHoraFinIsNull();
        long pedidosListos = comandaRepository.countByComandaEstadoAndVisita_VisitaFechaHoraFinIsNull(co.edu.unicauca.backend.shared.enums.EstadoComanda.LISTO);
        List<Comanda> pedidosListosDetalle = comandaRepository.findTop5ByComandaEstadoAndVisita_VisitaFechaHoraFinIsNull(
                co.edu.unicauca.backend.shared.enums.EstadoComanda.LISTO,
                org.springframework.data.domain.PageRequest.of(0, 5));

        long meserosConVisitaActiva = visitasActivasHoy;
        Long bartendersConSesionActiva = sesionRepository.countActiveSessionsByRoleAndDate(RolNombre.BARTENDER, inicio, fin);
        Long cocinerosRegistradosActivos = sesionRepository.countActiveSessionsByRoleAndDate(RolNombre.COCINERO, inicio, fin);

        List<PedidoListoResponse> pedidosListosDto = pedidosListosDetalle.stream()
                .map(comanda -> PedidoListoResponse.builder()
                        .comandaId(comanda.getComandaId())
                        .visitaId(comanda.getVisita() != null ? comanda.getVisita().getVisitaId() : null)
                        .estacion(comanda.getComandaEstacion() != null ? comanda.getComandaEstacion().name() : null)
                        .fechaHoraListo(comanda.getComandaFechaHoraListo())
                        .build())
                .collect(Collectors.toList());

        return DashboardDiarioResponse.builder()
                .fecha(targetDate)
                .totalVentasCerradas((long) ventas.size())
                .totalIngresos(totalIngresos)
                .ingresosPorMetodoPago(ingresosPorMetodoPago)
                .ingresosPorTipoVenta(ingresosPorTipoVenta)
                .productosMasVendidos(productosMasVendidos)
                .reservasActivasHoy(reservasActivasHoy)
                .personasReservadasHoy(personasReservadasHoy)
                .visitasActivas(visitasActivasHoy)
                .pedidosListos(pedidosListos)
                .pedidosListosDetalle(pedidosListosDto)
                .meserosConVisitaActiva(meserosConVisitaActiva)
                .bartendersConSesionActiva(bartendersConSesionActiva != null ? bartendersConSesionActiva : 0L)
                .cocinerosRegistradosActivos(cocinerosRegistradosActivos)
                .build();
    }

    private void validateFechaIsNotFuture(LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "No es posible consultar el dashboard en una fecha futura",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private List<MetodoPagoIngresoResponse> calcularIngresosPorMetodoPago(List<Venta> ventas) {
        Map<MetodoPago, BigDecimal> totalesPorMetodo = new EnumMap<>(MetodoPago.class);
        for (Venta venta : ventas) {
            MetodoPago metodo = venta != null ? venta.getVentaMetodo() : null;
            if (metodo == null || metodo == MetodoPago.NEQUI) {
                metodo = MetodoPago.OTRO;
            }
            BigDecimal total = venta != null && venta.getVentaTotal() != null ? venta.getVentaTotal() : BigDecimal.ZERO;
            totalesPorMetodo.merge(metodo, total, BigDecimal::add);
        }

        return List.of(MetodoPago.EFECTIVO, MetodoPago.TARJETA, MetodoPago.TRANSFERENCIA, MetodoPago.OTRO).stream()
                .map(metodo -> MetodoPagoIngresoResponse.builder()
                        .metodoPago(metodo.name())
                        .total(totalesPorMetodo.getOrDefault(metodo, BigDecimal.ZERO))
                        .build())
                .collect(Collectors.toList());
    }

    private Long parseVentaId(String ventaId) {
        if (ventaId == null || ventaId.isBlank()) {
            return null;
        }
        String raw = ventaId.trim();
        if (!raw.toUpperCase().matches("^(VENTA-)?\\d+$")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Caracteres no permitidos en la búsqueda", HttpStatus.BAD_REQUEST);
        }
        String digits = raw.toUpperCase().startsWith("VENTA-") ? raw.substring(6) : raw;
        return Long.valueOf(digits);
    }

    private LocalDate parseFecha(String fecha, String fieldName) {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        String normalized = fecha.trim();
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Fecha inválida para '" + fieldName + "'. Use el formato YYYY-MM-DD.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private MetodoPago parseMetodoPago(String metodoPago) {
        if (metodoPago == null || metodoPago.isBlank()) {
            return null;
        }
        String normalized = metodoPago.trim();
        MetodoPago metodo = Arrays.stream(MetodoPago.values())
                .filter(m -> m.name().equalsIgnoreCase(normalized) || m.getDescripcion().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Método de pago inválido: " + metodoPago,
                        HttpStatus.BAD_REQUEST));

        if (metodo != MetodoPago.EFECTIVO
                && metodo != MetodoPago.TARJETA
                && metodo != MetodoPago.TRANSFERENCIA
                && metodo != MetodoPago.OTRO) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Método de pago inválido: " + metodoPago,
                    HttpStatus.BAD_REQUEST);
        }
        return metodo;
    }

    private void validateFechaRange(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        if (hasta != null && hasta.isAfter(hoy)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "No es posible consultar ventas en fechas futuras",
                    HttpStatus.BAD_REQUEST);
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "La fecha 'desdeFecha' no puede ser mayor a 'hastaFecha'.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private VentaListadoItemResponse mapearVentaListado(Venta venta) {
        String clienteNombre = "Cliente ocasional";
        if (venta != null && venta.getVisita() != null && venta.getVisita().getCliente() != null) {
            String nombre = venta.getVisita().getCliente().getClienteNombre();
            if (nombre != null && !nombre.isBlank()) {
                clienteNombre = nombre;
            }
        }
        return VentaListadoItemResponse.builder()
                .ventaId(venta != null ? venta.getVisitaId() : null)
                .fechaHora(venta != null ? venta.getVentaFechaHora() : null)
                .subtotal(venta != null && venta.getVentaSubtotal() != null ? venta.getVentaSubtotal() : BigDecimal.ZERO)
                .descuento(venta != null && venta.getVentaDescuento() != null ? venta.getVentaDescuento() : BigDecimal.ZERO)
                .total(venta != null && venta.getVentaTotal() != null ? venta.getVentaTotal() : BigDecimal.ZERO)
                .metodoPago(venta != null && venta.getVentaMetodo() != null ? venta.getVentaMetodo().name() : null)
                .clienteNombre(clienteNombre)
                .build();
    }

    private ClienteVentaResponse construirCliente(Cliente cliente, boolean esReserva) {
        if (cliente == null || !esReserva) {
            return ClienteVentaResponse.builder()
                    .nombre("Cliente ocasional")
                    .telefono(null)
                    .build();
        }
        return ClienteVentaResponse.builder()
                .nombre(cliente.getClienteNombre())
                .telefono(cliente.getClienteTelefono())
                .build();
    }

    private MesaVentaResponse construirMesa(Mesa mesa) {
        if (mesa == null) {
            return null;
        }
        return MesaVentaResponse.builder()
                .identificador(mesa.getMesaIdentificador())
                .zona(mesa.getZona() != null ? mesa.getZona().getZonaNombre() : null)
                .build();
    }

    private List<ItemVentaResponse> mapearItems(List<ComandaItem> items) {
        return items.stream()
                .map(item -> ItemVentaResponse.builder()
                        .nombre(item.getProducto().getProductoNombre())
                        .cantidad(item.getComandaItemCantidad())
                        .precioUnitario(item.getComandaItemPrecio())
                        .subtotal(
                                item.getComandaItemPrecio().multiply(BigDecimal.valueOf(item.getComandaItemCantidad())))
                        .especificaciones(item.getComandaItemDescripcion())
                        .build())
                .collect(Collectors.toList());
    }

    private MenuEspecialVentaResponse construirMenuEspecial(List<ComandaItem> items, Reserva reserva) {
        Optional<ComandaItem> itemEspecial = items.stream()
                .filter(i -> Boolean.TRUE.equals(i.getProducto().getMenuEspecial()))
                .findFirst();

        if (itemEspecial.isEmpty() || reserva == null) {
            return null;
        }

        BigDecimal valorPorPersona = itemEspecial.get().getComandaItemPrecio();
        Integer numeroPersonas = reserva.getReservaNumeroPersonas();
        BigDecimal totalCalculado = valorPorPersona.multiply(BigDecimal.valueOf(numeroPersonas));

        return MenuEspecialVentaResponse.builder()
                .nombreMenu(itemEspecial.get().getProducto().getProductoNombre())
                .valorPorPersona(valorPorPersona)
                .numeroPersonas(numeroPersonas)
                .totalCalculado(totalCalculado)
                .build();
    }

    private List<ServicioAdicionalResponse> construirServiciosAdicionales(Reserva reserva) {
        if (reserva == null || reserva.getDecoracion() == null) {
            return Collections.emptyList();
        }
        Decoracion decoracion = reserva.getDecoracion();
        if (decoracion.getDecoracionCostoAdicional() == null) {
            return Collections.emptyList();
        }
        return List.of(ServicioAdicionalResponse.builder()
                .nombre(decoracion.getDecoracionNombre())
                .costo(decoracion.getDecoracionCostoAdicional())
                .build());
    }

    private String obtenerEstadoReserva(Reserva reserva) {
        if (reserva == null) {
            return null;
        }
        if (reserva.getReservaEstado() == EstadoReserva.ATENDIDA) {
            return "COMPLETADA";
        }
        return reserva.getReservaEstado().name();
    }

    private String obtenerAlertaReservaCancelada(Reserva reserva) {
        if (reserva == null) {
            return null;
        }
        return reserva.getReservaEstado() == EstadoReserva.CANCELADA
                ? ALERTA_RESERVA_CANCELADA
                : null;
    }
}
