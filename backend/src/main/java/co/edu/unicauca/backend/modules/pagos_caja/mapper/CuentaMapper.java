package co.edu.unicauca.backend.modules.pagos_caja.mapper;

import co.edu.unicauca.backend.modules.reservas.dto.response.AbonoItemResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.pagos_caja.dto.response.CuentaItemResponse;
import co.edu.unicauca.backend.modules.pagos_caja.dto.response.CuentaPreliminarResponse;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Abono;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.shared.dto.ResumenFinanciero;
import co.edu.unicauca.backend.shared.enums.TipoAbono;
import co.edu.unicauca.backend.shared.util.ResumenFinancieroCalculator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Construye la {@link CuentaPreliminarResponse} para el cierre de venta.
 *
 * <p>Mapea ítems de comanda individuales (conservando {@code comandaItemId} para el ajuste),
 * calcula el total a pagar (pre-orden + decoración con costo) y el saldo pendiente
 * (total − abonos netos). Las bebidas de menú especial tienen precio 0, por lo que la suma
 * de subtotales no produce doble conteo.
 */
@Component
public class CuentaMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** Orden de presentación: PLATO (0) → BEBIDA (1) → OTRO (2). */
    private static final Comparator<ComandaItem> COMPARATOR_POR_CATEGORIA =
            Comparator.comparing(item -> item.getProducto().getProductoCategoria().ordinal());

    /**
     * Ensambla la cuenta preliminar de una visita.
     *
     * @param visita  visita cuya cuenta se arma (con cliente/reserva/decoración)
     * @param items   ítems activos de la visita (misma fuente que las líneas mostradas)
     * @param abonos  abonos de la reserva (vacío si la visita no tiene reserva)
     * @param mesaOpt mesa asignada, si existe
     * @return la cuenta preliminar lista para serializar
     */
    public CuentaPreliminarResponse toCuenta(Visita visita, List<ComandaItem> items,
                                             List<Abono> abonos, Optional<Mesa> mesaOpt) {
        // Total de la pre-orden derivado de los MISMOS ítems que se listan, para garantizar
        // que la suma de subtotales mostrados coincida con el total (sin contar precios nulos).
        BigDecimal totalPreorden = items.stream()
                .filter(i -> i.getComandaItemPrecio() != null)
                .map(i -> i.getComandaItemPrecio().multiply(BigDecimal.valueOf(i.getComandaItemCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Decoración: solo se reporta si la reserva tiene decoración CON costo adicional
        BigDecimal valorDecoracion = null;
        String decoracionNombre = null;
        if (visita.getReserva() != null && visita.getReserva().getDecoracion() != null) {
            Decoracion d = visita.getReserva().getDecoracion();
            if (d.getDecoracionCostoAdicional() != null) {
                valorDecoracion = d.getDecoracionCostoAdicional();
                decoracionNombre = d.getDecoracionNombre();
            }
        }

        // Importes derivados (total a pagar, neto abonado, saldo) vía helper compartido
        BigDecimal anticipado = abonos.stream().filter(a -> a.getAbonoTipo() == TipoAbono.ANTICIPO)
                .map(Abono::getAbonoMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal devuelto = abonos.stream().filter(a -> a.getAbonoTipo() == TipoAbono.DEVOLUCION)
                .map(Abono::getAbonoMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        ResumenFinanciero resumen =
                ResumenFinancieroCalculator.calcular(totalPreorden, valorDecoracion, anticipado, devuelto);

        // Lista de anticipos: null si no hubo abonos para omitir el campo
        List<AbonoItemResponse> anticipos = abonos.isEmpty() ? null : abonos.stream()
                .map(a -> AbonoItemResponse.builder()
                        .abonoId(a.getAbonoId())
                        .monto(a.getAbonoMonto())
                        .fechaHora(a.getAbonoFechaHora().format(FORMATTER))
                        .metodo(a.getAbonoMetodo().name())
                        .tipo(a.getAbonoTipo().name())
                        .build())
                .toList();

        // Cliente: null en visitas de invitado
        boolean tieneCliente = visita.getCliente() != null;

        return CuentaPreliminarResponse.builder()
                .visitaId(visita.getVisitaId())
                .clienteId(tieneCliente ? visita.getCliente().getUsuarioId() : null)
                .clienteNombre(tieneCliente ? visita.getCliente().getClienteNombre() : null)
                .clienteEmail(tieneCliente ? visita.getCliente().getUsuario().getUsuarioEmail() : null)
                .puntosCanjeables(tieneCliente ? visita.getCliente().getClientePuntos() : null)
                .puntosAcumulados(tieneCliente ? visita.getCliente().getClientePuntosAcumulados() : null)
                .fechaHoraLlegada(visita.getVisitaFechaHoraInicio().format(FORMATTER))
                .meseroNombre(mesaOpt.map(m -> m.getMesero().getEmpleadoNombre()).orElse(null))
                .mesaIdentificador(mesaOpt.map(Mesa::getMesaIdentificador).orElse(null))
                .items(mapearItems(items))
                .decoracionNombre(decoracionNombre)
                .valorDecoracion(valorDecoracion)
                .totalPreorden(resumen.totalPreorden())
                .totalAPagar(resumen.totalAPagar())
                .anticipos(anticipos)
                .montoAbonado(resumen.montoAbonado())
                .saldoPendiente(resumen.saldoPendiente())
                .build();
    }

    /**
     * Mapea los ítems a líneas individuales ordenadas por categoría, preservando el
     * {@code comandaItemId} para el ajuste y la metadata de menú especial.
     */
    private List<CuentaItemResponse> mapearItems(List<ComandaItem> items) {
        return items.stream()
                .sorted(COMPARATOR_POR_CATEGORIA)   // ORDENAR ANTES de mapear
                .map(this::toItem)
                .toList();
    }

    /** Convierte un ítem de comanda en su línea de cuenta. */
    private CuentaItemResponse toItem(ComandaItem ci) {
        // Las bebidas de menú especial tienen precio 0; el subtotal resultante es 0
        BigDecimal precio = ci.getComandaItemPrecio() != null ? ci.getComandaItemPrecio() : BigDecimal.ZERO;
        return CuentaItemResponse.builder()
                .comandaItemId(ci.getComandaItemId())
                .nombreProducto(ci.getProducto().getProductoNombre())
                .categoriaProducto(ci.getProducto().getProductoCategoria().name())
                .cantidad(ci.getComandaItemCantidad())
                .precioUnitario(precio)
                .subtotal(precio.multiply(BigDecimal.valueOf(ci.getComandaItemCantidad())))
                .descripcion(ci.getComandaItemDescripcion())
                .esModificado(ci.getComandaItemDescripcion() != null)
                .menuGrupo(ci.getComandaItemMenuGrupo())
                .esMenuEspecial(Boolean.TRUE.equals(ci.getProducto().getMenuEspecial()))
                .build();
    }
}
