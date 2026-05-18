package co.edu.unicauca.backend.modules.reportes.ventas_detalle.service;


import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.ClienteVentaResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.ItemVentaResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.MenuEspecialVentaResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.MesaVentaResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.ServicioAdicionalResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response.VentaDetalleResponse;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.repository.ComandaItemDetalleAdminRepository;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.repository.MesaDetalleAdminRepository;
import co.edu.unicauca.backend.modules.reportes.ventas_detalle.repository.VentaDetalleAdminRepository;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.shared.enums.EstadoReserva;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VentaDetalleAdminService {

    private static final String ALERTA_RESERVA_CANCELADA = "Esta venta esta asociada a una reserva cancelada. Verificar con administracion";

    private final VentaDetalleAdminRepository ventaRepository;
    private final MesaDetalleAdminRepository mesaRepository;
    private final ComandaItemDetalleAdminRepository itemRepository;

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
