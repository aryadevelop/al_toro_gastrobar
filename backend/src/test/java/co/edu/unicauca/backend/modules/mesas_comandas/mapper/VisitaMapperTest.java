package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.VisitaResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

class VisitaMapperTest {

    private VisitaMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new VisitaMapper();
    }

    private Visita visitaBase() {
        return Visita.builder()
                .visitaId(1L)
                .visitaFechaHoraInicio(LocalDateTime.of(2026, 4, 1, 19, 0))
                .build();
    }

    @Test
    @DisplayName("toResumen → sin mesa ni venta → estado ATENDIDA, montoTotal null")
    void toResumen_sinMesaNiVenta() {
        VisitaResumenResponse resp = mapper.toResumen(visitaBase(), Optional.empty(), Optional.empty());

        assertThat(resp.getVisitaId()).isEqualTo(1L);
        assertThat(resp.getEstadoVisita()).isEqualTo("ATENDIDA");
        assertThat(resp.getMontoTotal()).isNull();
        assertThat(resp.getMesaIdentificador()).isNull();
    }

    @Test
    @DisplayName("toResumen → con venta → estado CERRADA y montoTotal presente")
    void toResumen_conVenta_estadoCerrada() {
        Venta venta = Venta.builder()
                .ventaTotal(BigDecimal.valueOf(75000))
                .ventaMetodo(MetodoPago.EFECTIVO)
                .build();

        VisitaResumenResponse resp = mapper.toResumen(visitaBase(), Optional.empty(), Optional.of(venta));

        assertThat(resp.getEstadoVisita()).isEqualTo("CERRADA");
        assertThat(resp.getMontoTotal()).isEqualByComparingTo(BigDecimal.valueOf(75000));
    }

    @Test
    @DisplayName("toResumen → con mesa → mapea identificador y numeroPersonas")
    void toResumen_conMesa_mapeaIdentificador() {
        Zona zona = Zona.builder().zonaId(1L).zonaNombre("Terraza").build();
        Mesa mesa = Mesa.builder()
                .mesaIdentificador("T3")
                .mesaNumeroPersonas(6)
                .zona(zona)
                .build();

        VisitaResumenResponse resp = mapper.toResumen(visitaBase(), Optional.of(mesa), Optional.empty());

        assertThat(resp.getMesaIdentificador()).isEqualTo("T3");
        assertThat(resp.getNumeroPersonas()).isEqualTo(6);
        assertThat(resp.getZonaNombre()).isEqualTo("Terraza");
    }

    @Test
    @DisplayName("toDetalle → sin items, sin abonos → itemsComanda null, abonos null")
    void toDetalle_sinItemsNiAbonos() {
        VisitaDetalleResponse resp = mapper.toDetalle(
                visitaBase(), List.of(), Optional.empty(), Optional.empty(), Optional.empty());

        assertThat(resp.getVisitaId()).isEqualTo(1L);
        assertThat(resp.getItemsComanda()).isNull();
        assertThat(resp.getAbonos()).isNull();
        assertThat(resp.getEstadoVisita()).isEqualTo("ATENDIDA");
    }

    @Test
    @DisplayName("toDetalle → con venta → totalCuenta presente")
    void toDetalle_conVenta_totalCuentaPresente() {
        Venta venta = Venta.builder()
                .ventaSubtotal(BigDecimal.valueOf(80000))
                .ventaDescuento(BigDecimal.valueOf(5000))
                .ventaTotal(BigDecimal.valueOf(75000))
                .ventaMetodo(MetodoPago.TARJETA)
                .build();

        VisitaDetalleResponse resp = mapper.toDetalle(
                visitaBase(), List.of(), Optional.empty(), Optional.of(venta), Optional.empty());

        assertThat(resp.getTotalCuenta()).isEqualByComparingTo(BigDecimal.valueOf(75000));
        assertThat(resp.getSubtotalCuenta()).isEqualByComparingTo(BigDecimal.valueOf(80000));
        assertThat(resp.getEstadoVisita()).isEqualTo("CERRADA");
    }
}
