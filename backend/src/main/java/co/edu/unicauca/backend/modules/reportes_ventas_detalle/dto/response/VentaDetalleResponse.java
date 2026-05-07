package co.edu.unicauca.backend.modules.reportes_ventas_detalle.dto.response;

import co.edu.unicauca.backend.shared.enums.MetodoPago;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class VentaDetalleResponse {
    private final Long ventaId;
    private final LocalDateTime fechaHora;
    private final ClienteVentaResponse cliente;
    private final MesaVentaResponse mesa;
    private final String meseroNombre;
    private final List<ItemVentaResponse> items;
    private final MenuEspecialVentaResponse menuEspecial;
    private final List<ServicioAdicionalResponse> serviciosAdicionales;
    private final String notaReserva;
    private final BigDecimal subtotal;
    private final BigDecimal total;
    private final MetodoPago metodoPago;
    private final String estadoReserva;
    private final String alertaReservaCancelada;
}
