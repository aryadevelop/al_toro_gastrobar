package co.edu.unicauca.backend.modules.inventario.service;

import co.edu.unicauca.backend.modules.inventario.dto.request.CambioEstadoRequest;
import co.edu.unicauca.backend.modules.inventario.dto.response.CambioEstadoImplicacionesResponse;
import co.edu.unicauca.backend.modules.inventario.dto.response.CambioEstadoResponse;
import co.edu.unicauca.backend.modules.inventario.entity.Insumo;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.repository.InsumoRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.inventario.repository.RecetaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.enums.TipoProducto;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EstadoInventarioService {

    private final ProductoRepository productoRepository;
    private final InsumoRepository insumoRepository;
    private final RecetaRepository recetaRepository;
    private final ComandaRepository comandaRepository;

    @Transactional
    public CambioEstadoResponse cambiarEstadoProducto(Long productoId, CambioEstadoRequest request) {
        Producto producto = productoRepository.findByIdForUpdate(productoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND,
                        "Producto no encontrado: " + productoId));

        EstadoGenerico nuevoEstado = request.getEstado();
        EstadoGenerico actual = producto.getProductoEstado();

        if (actual == nuevoEstado) {
            return CambioEstadoResponse.builder()
                    .id(productoId)
                    .mensaje("El producto ya está " + nuevoEstado.name().toLowerCase() + ".")
                    .build();
        }

        if (nuevoEstado == EstadoGenerico.INACTIVO) {
            int pendientes = comandaRepository.findPendientesByProductoId(productoId).size();
            producto.setProductoEstado(EstadoGenerico.INACTIVO);
            productoRepository.save(producto);
            String mensaje = "Producto suspendido correctamente. Ya no aparece en el catálogo de venta.";
            if (request.getNotificarClientes() != null && request.getNotificarClientes()) {
                mensaje = "Producto suspendido correctamente. Se notificará nuevamente a los clientes cuando se reactive.";
            } else {
                mensaje += " Cuando se vuelva a ofrecer, se notificará nuevamente a los clientes.";
            }
            if (pendientes > 0) {
                mensaje += " Este producto está incluido en " + pendientes + " reservas futuras.";
            }
            return CambioEstadoResponse.builder()
                    .id(productoId)
                    .mensaje(mensaje)
                    .pedidosPendientes(pendientes)
                    .notificarClientes(request.getNotificarClientes())
                    .build();
        }

        producto.setProductoEstado(EstadoGenerico.ACTIVO);
        productoRepository.save(producto);
        return CambioEstadoResponse.builder()
                .id(productoId)
                .mensaje("Producto reactivado correctamente y estará disponible en el catálogo.")
                .build();
    }

    @Transactional
    public CambioEstadoResponse cambiarEstadoInsumo(Long insumoId, CambioEstadoRequest request) {
        Insumo insumo = insumoRepository.findByIdForUpdate(insumoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND,
                        "Insumo no encontrado: " + insumoId));

        EstadoGenerico nuevoEstado = request.getEstado();
        EstadoGenerico actual = insumo.getInsumoEstado();

        if (actual == nuevoEstado) {
            return CambioEstadoResponse.builder()
                    .id(insumoId)
                    .mensaje("El insumo ya está " + nuevoEstado.name().toLowerCase() + ".")
                    .build();
        }

        if (nuevoEstado == EstadoGenerico.INACTIVO) {
            insumo.setInsumoEstado(EstadoGenerico.INACTIVO);
            insumoRepository.save(insumo);
            List<Long> productoIds = recetaRepository.findProductoIdsByInsumoId(insumoId);
            List<Producto> preparados = productoRepository.findAllById(productoIds).stream()
                    .filter(p -> p.getProductoTipo() == TipoProducto.PREPARACION)
                    .collect(Collectors.toList());
            int afectadas = preparados.size();
            if (!preparados.isEmpty()) {
                preparados.forEach(p -> p.setProductoEstado(EstadoGenerico.INACTIVO));
                productoRepository.saveAll(preparados);
            }
            String mensaje = "Insumo desactivado correctamente.";
            if (afectadas > 0) {
                mensaje += " Se desactivaron " + afectadas + " preparaciones que dependían de este insumo.";
            }
            return CambioEstadoResponse.builder()
                    .id(insumoId)
                    .mensaje(mensaje)
                    .preparacionesAfectadas(afectadas)
                    .build();
        }

        insumo.setInsumoEstado(EstadoGenerico.ACTIVO);
        insumoRepository.save(insumo);
        return CambioEstadoResponse.builder()
                .id(insumoId)
                .mensaje("Insumo reactivado correctamente. Las preparaciones afectadas permanecen inactivas hasta su reactivación manual.")
                .build();
    }

    @Transactional
    public CambioEstadoImplicacionesResponse evaluarCambioEstadoProducto(Long productoId, EstadoGenerico estadoSolicitado) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND,
                        "Producto no encontrado: " + productoId));

        int pedidosPendientes = 0;
        String mensaje = "No hay implicaciones adicionales para este cambio de estado.";
        if (estadoSolicitado == EstadoGenerico.INACTIVO) {
            pedidosPendientes = comandaRepository.findPendientesByProductoId(productoId).size();
            if (pedidosPendientes > 0) {
                mensaje = "Este producto está incluido en " + pedidosPendientes + " reservas futuras.";
            }
        }
        return CambioEstadoImplicacionesResponse.builder()
                .id(productoId)
                .estadoActual(producto.getProductoEstado().name())
                .estadoSolicitado(estadoSolicitado.name())
                .pedidosPendientes(pedidosPendientes)
                .mensaje(mensaje)
                .build();
    }

    @Transactional
    public CambioEstadoImplicacionesResponse evaluarCambioEstadoInsumo(Long insumoId, EstadoGenerico estadoSolicitado) {
        Insumo insumo = insumoRepository.findById(insumoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND,
                        "Insumo no encontrado: " + insumoId));

        int preparacionesAfectadas = 0;
        String mensaje = "No hay implicaciones adicionales para este cambio de estado.";
        if (estadoSolicitado == EstadoGenerico.INACTIVO) {
            List<Long> productoIds = recetaRepository.findProductoIdsByInsumoId(insumoId);
            preparacionesAfectadas = (int) productoRepository.findAllById(productoIds).stream()
                    .filter(p -> p.getProductoTipo() == TipoProducto.PREPARACION)
                    .count();
            if (preparacionesAfectadas > 0) {
                mensaje = "Este insumo afecta " + preparacionesAfectadas + " preparaciones que se desactivarán si continúa.";
            }
        }
        return CambioEstadoImplicacionesResponse.builder()
                .id(insumoId)
                .estadoActual(insumo.getInsumoEstado().name())
                .estadoSolicitado(estadoSolicitado.name())
                .preparacionesAfectadas(preparacionesAfectadas)
                .mensaje(mensaje)
                .build();
    }
}
