package co.edu.unicauca.backend.modules.reportes.ventas_detalle.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDiarioResponse {

    private LocalDate fecha;
    private Long totalVentasCerradas;
    private BigDecimal totalIngresos;
    private List<MetodoPagoIngresoResponse> ingresosPorMetodoPago;
    private List<TipoVentaIngresoResponse> ingresosPorTipoVenta;
    private List<TopProductoResponse> productosMasVendidos;

    private Long reservasActivasHoy;
    private Long personasReservadasHoy;
    private Long visitasActivas;

    private Long pedidosListos;
    private List<PedidoListoResponse> pedidosListosDetalle;

    private Long meserosConVisitaActiva;
    private Long bartendersConSesionActiva;
    private Long cocinerosRegistradosActivos;
}
