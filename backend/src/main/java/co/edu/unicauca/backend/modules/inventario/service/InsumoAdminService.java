package co.edu.unicauca.backend.modules.inventario.service;

import co.edu.unicauca.backend.modules.inventario.dto.request.ActualizarInsumoRequest;
import co.edu.unicauca.backend.modules.inventario.dto.response.InsumoAdminListResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.InsumoDetalleResponse;
import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import co.edu.unicauca.backend.modules.inventario.repository.InsumoRepository;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de administración de insumos.
 *
 * <p>Expone operaciones de consulta y actualización de insumos para el panel de
 * administración: listado con alerta de vencimiento próximo, detalle por ID
 * y actualización de nombre, costo unitario y fecha de vencimiento.
 */
@Service
@RequiredArgsConstructor
public class InsumoAdminService {

    /** Días umbral para considerar que el vencimiento es "próximo". */
    private static final int DIAS_ALERTA_VENCIMIENTO = 7;

    private final InsumoRepository insumoRepository;

    /**
     * Lista todos los insumos ordenados alfabéticamente, incluyendo la bandera
     * {@code vencimientoProximo} calculada en tiempo de ejecución.
     */
    @Transactional(readOnly = true)
    public List<InsumoAdminListResponse> listarInsumos() {
        LocalDate hoy = LocalDate.now();
        LocalDate umbral = hoy.plusDays(DIAS_ALERTA_VENCIMIENTO);

        return insumoRepository.findAll(Sort.by("insumoNombre"))
                .stream()
                .map(i -> toListResponse(i, hoy, umbral))
                .toList();
    }

    /**
     * Retorna el detalle completo de un insumo por su identificador.
     *
     * @throws BusinessException si el insumo no existe
     */
    @Transactional(readOnly = true)
    public InsumoDetalleResponse obtenerDetalle(Long insumoId) {
        Insumo insumo = insumoRepository.findById(insumoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND,
                        "Insumo no encontrado: " + insumoId));

        LocalDate hoy = LocalDate.now();
        LocalDate umbral = hoy.plusDays(DIAS_ALERTA_VENCIMIENTO);
        Boolean vencimientoProximo = calcularVencimientoProximo(insumo.getInsumoFechaVencimiento(), hoy, umbral);

        return InsumoDetalleResponse.builder()
                .insumoId(insumo.getInsumoId())
                .insumoNombre(insumo.getInsumoNombre())
                .insumoUnidad(insumo.getInsumoUnidad().name())
                .insumoStockActual(insumo.getInsumoStockActual())
                .insumoEstado(insumo.getInsumoEstado().name())
                .tipoInsumo(insumo.getTipoInsumo().name())
                .insumoCosoUnitario(insumo.getInsumoCosoUnitario())
                .insumoFechaVencimiento(insumo.getInsumoFechaVencimiento())
                .vencimientoProximo(vencimientoProximo)
                .build();
    }

    /**
     * Actualiza nombre, costo unitario y fecha de vencimiento del insumo.
     *
     * @throws BusinessException si el insumo no existe
     */
    @Transactional
    public InsumoDetalleResponse actualizarInsumo(Long insumoId, ActualizarInsumoRequest request) {
        Insumo insumo = insumoRepository.findById(insumoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND,
                        "Insumo no encontrado: " + insumoId));

        insumo.setInsumoNombre(request.getInsumoNombre());
        insumo.setInsumoCosoUnitario(request.getInsumoCosoUnitario());
        insumo.setInsumoFechaVencimiento(request.getInsumoFechaVencimiento());
        insumoRepository.save(insumo);

        return obtenerDetalle(insumoId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private InsumoAdminListResponse toListResponse(Insumo i, LocalDate hoy, LocalDate umbral) {
        return InsumoAdminListResponse.builder()
                .insumoId(i.getInsumoId())
                .insumoNombre(i.getInsumoNombre())
                .insumoUnidad(i.getInsumoUnidad().name())
                .insumoStockActual(i.getInsumoStockActual())
                .insumoEstado(i.getInsumoEstado().name())
                .tipoInsumo(i.getTipoInsumo().name())
                .insumoFechaVencimiento(i.getInsumoFechaVencimiento())
                .vencimientoProximo(calcularVencimientoProximo(i.getInsumoFechaVencimiento(), hoy, umbral))
                .build();
    }

    private Boolean calcularVencimientoProximo(LocalDate fecha, LocalDate hoy, LocalDate umbral) {
        if (fecha == null) return null;
        return !fecha.isBefore(hoy) && !fecha.isAfter(umbral);
    }
}
