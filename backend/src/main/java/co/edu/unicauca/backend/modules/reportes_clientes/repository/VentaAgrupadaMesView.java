package co.edu.unicauca.backend.modules.reportes_clientes.repository;

import java.math.BigDecimal;

public interface VentaAgrupadaMesView {
    Integer getAnio();
    Integer getMes();
    BigDecimal getTotal();
    Long getCantidad();
}
