package co.edu.unicauca.backend.modules.reportes.clientes.repository;

import java.math.BigDecimal;

public interface VentaAgrupadaAnioView {
    Integer getAnio();
    BigDecimal getTotal();
    Long getCantidad();
}
