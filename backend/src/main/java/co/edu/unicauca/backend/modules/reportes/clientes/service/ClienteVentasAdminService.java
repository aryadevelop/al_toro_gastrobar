package co.edu.unicauca.backend.modules.reportes.clientes.service;


import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import co.edu.unicauca.backend.modules.reportes.clientes.dto.response.ClienteBusquedaResponse;
import co.edu.unicauca.backend.modules.reportes.clientes.dto.response.ClienteResumenResponse;
import co.edu.unicauca.backend.modules.reportes.clientes.dto.response.ClienteVentasResponse;
import co.edu.unicauca.backend.modules.reportes.clientes.dto.response.ClienteVentasResumenResponse;
import co.edu.unicauca.backend.modules.reportes.clientes.dto.response.VentaAgrupadaAnioResponse;
import co.edu.unicauca.backend.modules.reportes.clientes.dto.response.VentaAgrupadaMesResponse;
import co.edu.unicauca.backend.modules.reportes.clientes.dto.response.VentaDetalleResponse;
import co.edu.unicauca.backend.modules.reportes.clientes.repository.ClienteAdminRepository;
import co.edu.unicauca.backend.modules.reportes.clientes.repository.VentaAdminRepository;
import co.edu.unicauca.backend.modules.reportes.clientes.repository.VentaAgrupadaAnioView;
import co.edu.unicauca.backend.modules.reportes.clientes.repository.VentaAgrupadaMesView;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClienteVentasAdminService {

    private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy",
            Locale.forLanguageTag("es-CO"));
    private static final String MENSAJE_CUMPLEANOS = "Feliz  Cumpleaños";

    private final ClienteAdminRepository clienteRepository;
    private final VentaAdminRepository ventaRepository;
    private final ClienteRecordatorioService recordatorioService;

    @Transactional(readOnly = true)
    public ClienteVentasResponse obtenerHistorial(Long clienteId) {
        Cliente cliente = obtenerCliente(clienteId);
        List<Venta> ventas = ventaRepository.findByClienteIdOrderByVentaFechaHoraDesc(clienteId);
        ClienteVentasResumenResponse resumen = construirResumen(cliente, ventas);

        String mensajeCumpleanos = obtenerMensajeCumpleanos(cliente.getClienteFechaNacimiento());

        String mensajeInactivo = null;
        boolean mostrarRecordatorio = false;
        if (!ventas.isEmpty()) {
            LocalDateTime ultimaCompra = ventas.get(0).getVentaFechaHora();
            if (esClienteInactivo(ultimaCompra)) {
                mensajeInactivo = "Cliente inactivo. Última compra: " + FECHA_FORMATO.format(ultimaCompra);
                mostrarRecordatorio = true;
            }
        }

        return ClienteVentasResponse.builder()
                .cliente(construirClienteResumen(cliente))
                .resumen(resumen)
                .ventas(mapearVentas(ventas))
                .mensajeCumpleanos(mensajeCumpleanos)
                .mensajeInactivo(mensajeInactivo)
                .mostrarRecordatorio(mostrarRecordatorio)
                .build();
    }

    @Transactional(readOnly = true)
    public ClienteVentasResumenResponse obtenerResumen(Long clienteId) {
        Cliente cliente = obtenerCliente(clienteId);
        List<Venta> ventas = ventaRepository.findByClienteIdOrderByVentaFechaHoraDesc(clienteId);
        return construirResumen(cliente, ventas);
    }

    @Transactional(readOnly = true)
    public List<VentaAgrupadaAnioResponse> obtenerAgrupadoPorAnio(Long clienteId) {
        obtenerCliente(clienteId);
        List<VentaAgrupadaAnioView> agrupado = ventaRepository.obtenerTotalesPorAnio(clienteId);
        return agrupado.stream()
                .map(v -> VentaAgrupadaAnioResponse.builder()
                        .anio(v.getAnio() == null ? 0 : v.getAnio())
                        .total(v.getTotal())
                        .cantidad(v.getCantidad() == null ? 0 : v.getCantidad())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VentaAgrupadaMesResponse> obtenerAgrupadoPorMes(Long clienteId) {
        obtenerCliente(clienteId);
        List<VentaAgrupadaMesView> agrupado = ventaRepository.obtenerTotalesPorMes(clienteId);
        return agrupado.stream()
                .map(v -> VentaAgrupadaMesResponse.builder()
                        .anio(v.getAnio() == null ? 0 : v.getAnio())
                        .mes(v.getMes() == null ? 0 : v.getMes())
                        .total(v.getTotal())
                        .cantidad(v.getCantidad() == null ? 0 : v.getCantidad())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClienteBusquedaResponse> buscarPorNombre(String valor) {
        if (esEntradaInsegura(valor)) {
            return Collections.emptyList();
        }
        return clienteRepository.findByClienteNombreContainingIgnoreCase(valor)
                .stream()
                .map(this::mapearBusqueda)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClienteBusquedaResponse> buscarPorCorreo(String valor) {
        if (esEntradaInsegura(valor)) {
            return Collections.emptyList();
        }
        return clienteRepository.findByUsuario_UsuarioEmailContainingIgnoreCase(valor)
                .stream()
                .map(this::mapearBusqueda)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClienteBusquedaResponse> buscarPorTelefono(String valor) {
        if (esEntradaInsegura(valor)) {
            return Collections.emptyList();
        }
        return clienteRepository.findByClienteTelefonoContaining(valor)
                .stream()
                .map(this::mapearBusqueda)
                .collect(Collectors.toList());
    }

    @Transactional
    public void enviarRecordatorio(Long clienteId) {
        Cliente cliente = obtenerCliente(clienteId);
        Venta ultimaVenta = ventaRepository.findFirstByVisita_Cliente_UsuarioIdOrderByVentaFechaHoraDesc(clienteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_STATE,
                        "El cliente no tiene historial de compras.", HttpStatus.CONFLICT));

        LocalDateTime ultimaCompra = ultimaVenta.getVentaFechaHora();
        if (!esClienteInactivo(ultimaCompra)) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "El cliente no es inactivo.", HttpStatus.CONFLICT);
        }

        String mensaje = "Cliente inactivo. Última compra: " + FECHA_FORMATO.format(ultimaCompra) + ".";
        recordatorioService.enviarRecordatorio(
                cliente.getUsuario().getUsuarioEmail(),
                cliente.getClienteNombre(),
                mensaje);
    }

    private Cliente obtenerCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", clienteId));
    }

    private List<VentaDetalleResponse> mapearVentas(List<Venta> ventas) {
        return ventas.stream()
                .map(v -> VentaDetalleResponse.builder()
                        .visitaId(v.getVisitaId())
                        .subtotal(v.getVentaSubtotal())
                        .descuento(v.getVentaDescuento())
                        .total(v.getVentaTotal())
                        .metodo(v.getVentaMetodo())
                        .fechaHora(v.getVentaFechaHora())
                        .createdAt(v.getCreatedAt())
                        .cajeroNombre(v.getCajero().getEmpleadoNombre())
                        .build())
                .collect(Collectors.toList());
    }

    private ClienteVentasResumenResponse construirResumen(Cliente cliente, List<Venta> ventas) {
        BigDecimal totalGastado = ventas.stream()
                .map(Venta::getVentaTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalVisitas = ventas.size();
        BigDecimal promedio = totalVisitas > 0
                ? totalGastado.divide(BigDecimal.valueOf(totalVisitas), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        LocalDateTime ultimaVisita = ventas.isEmpty() ? null : ventas.get(0).getVentaFechaHora();

        return ClienteVentasResumenResponse.builder()
                .totalVisitas(totalVisitas)
                .totalGastado(totalGastado)
                .promedioPorVisita(promedio)
                .ultimaVisita(ultimaVisita)
                .clienteDesde(cliente.getCreatedAt())
                .build();
    }

    private ClienteResumenResponse construirClienteResumen(Cliente cliente) {
        return ClienteResumenResponse.builder()
                .clienteId(cliente.getUsuarioId())
                .nombre(cliente.getClienteNombre())
                .email(cliente.getUsuario().getUsuarioEmail())
                .telefono(cliente.getClienteTelefono())
                .fechaNacimiento(cliente.getClienteFechaNacimiento())
                .clienteDesde(cliente.getCreatedAt())
                .build();
    }

    private ClienteBusquedaResponse mapearBusqueda(Cliente cliente) {
        return ClienteBusquedaResponse.builder()
                .clienteId(cliente.getUsuarioId())
                .nombre(cliente.getClienteNombre())
                .email(cliente.getUsuario().getUsuarioEmail())
                .telefono(cliente.getClienteTelefono())
                .build();
    }

    private String obtenerMensajeCumpleanos(LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            return null;
        }
        LocalDate hoy = LocalDate.now();
        if (hoy.getMonthValue() == fechaNacimiento.getMonthValue()
                && hoy.getDayOfMonth() == fechaNacimiento.getDayOfMonth()) {
            return MENSAJE_CUMPLEANOS;
        }
        return null;
    }

    private boolean esClienteInactivo(LocalDateTime ultimaCompra) {
        return ultimaCompra.isBefore(LocalDateTime.now().minusMonths(6));
    }

    private boolean esEntradaInsegura(String valor) {
        if (valor == null) {
            return true;
        }
        String normalizado = valor.trim();
        if (normalizado.isEmpty()) {
            return true;
        }
        String lower = normalizado.toLowerCase();
        return lower.contains("'")
                || lower.contains("--")
                || lower.contains(";")
                || lower.contains("/*")
                || lower.contains("*/");
    }
}
