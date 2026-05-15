package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.EstadoVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VisitaEstadoMapperTest {

    private VisitaEstadoMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new VisitaEstadoMapper();
    }

    private Visita visitaAbierta() {
        return Visita.builder()
                .visitaId(10L)
                .visitaFechaHoraInicio(LocalDateTime.of(2026, 5, 11, 19, 0))
                .build();
    }

    private Visita visitaCerrada() {
        return Visita.builder()
                .visitaId(11L)
                .visitaFechaHoraInicio(LocalDateTime.of(2026, 5, 11, 19, 0))
                .visitaFechaHoraFin(LocalDateTime.of(2026, 5, 11, 21, 0))
                .build();
    }

    private Producto producto() {
        return Producto.builder()
                .productoNombre("Limonada")
                .productoCategoria(CategoriaProducto.BEBIDA)
                .build();
    }

    private ComandaItem itemConEstado(EstadoComanda estado, long id) {
        Comanda comanda = Comanda.builder()
                .comandaId(id)
                .comandaEstado(estado)
                .build();
        return ComandaItem.builder()
                .comandaItemId(id)
                .comanda(comanda)
                .producto(producto())
                .comandaItemCantidad(2)
                .comandaItemPrecio(new BigDecimal("5000"))
                .comandaItemDescripcion("Sin azucar")
                .build();
    }

    // ---------- toItemsVisitaResponse: ramas de guard clause y switch ----------

    @Test
    @DisplayName("toItemsVisitaResponse → null → lista vacia")
    void toItemsVisitaResponse_null_retornaVacia() {
        List<ItemVisitaResponse> result = mapper.toItemsVisitaResponse(null);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("toItemsVisitaResponse → lista vacia → lista vacia")
    void toItemsVisitaResponse_vacia_retornaVacia() {
        List<ItemVisitaResponse> result = mapper.toItemsVisitaResponse(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("toItemsVisitaResponse → comanda BORRADOR → estadoItem 'En espera'")
    void toItemsVisitaResponse_borrador_mapeaEnEspera() {
        ComandaItem item = itemConEstado(EstadoComanda.BORRADOR, 1L);

        List<ItemVisitaResponse> result = mapper.toItemsVisitaResponse(List.of(item));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEstadoItem()).isEqualTo("En espera");
        assertThat(result.get(0).getNombreProducto()).isEqualTo("Limonada");
        assertThat(result.get(0).getSubtotal()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(result.get(0).getCategoriaProducto()).isEqualTo("BEBIDA");
    }

    @Test
    @DisplayName("toItemsVisitaResponse → comanda LISTO → estadoItem 'Servido'")
    void toItemsVisitaResponse_listo_mapeaServido() {
        ComandaItem item = itemConEstado(EstadoComanda.LISTO, 2L);

        List<ItemVisitaResponse> result = mapper.toItemsVisitaResponse(List.of(item));

        assertThat(result.get(0).getEstadoItem()).isEqualTo("Servido");
    }

    @Test
    @DisplayName("toItemsVisitaResponse → comanda COMPLETADO → estadoItem 'Servido'")
    void toItemsVisitaResponse_completado_mapeaServido() {
        ComandaItem item = itemConEstado(EstadoComanda.COMPLETADO, 3L);

        List<ItemVisitaResponse> result = mapper.toItemsVisitaResponse(List.of(item));

        assertThat(result.get(0).getEstadoItem()).isEqualTo("Servido");
    }

    @Test
    @DisplayName("toItemsVisitaResponse → comanda PENDIENTE → estadoItem 'En espera'")
    void toItemsVisitaResponse_pendiente_mapeaEnEspera() {
        ComandaItem item = itemConEstado(EstadoComanda.PENDIENTE, 4L);

        List<ItemVisitaResponse> result = mapper.toItemsVisitaResponse(List.of(item));

        assertThat(result.get(0).getEstadoItem()).isEqualTo("En espera");
    }

    @Test
    @DisplayName("toItemsVisitaResponse → comanda EN_PREPARACION → estadoItem 'En preparación'")
    void toItemsVisitaResponse_enPreparacion_mapeaEnPreparacion() {
        ComandaItem item = itemConEstado(EstadoComanda.EN_PREPARACION, 5L);

        List<ItemVisitaResponse> result = mapper.toItemsVisitaResponse(List.of(item));

        assertThat(result.get(0).getEstadoItem()).isEqualTo("En preparación");
    }

    @Test
    @DisplayName("toItemsVisitaResponse → comanda PRE_RESERVA → estadoItem 'En espera' (defensivo)")
    void toItemsVisitaResponse_preReserva_mapeaEnEspera() {
        ComandaItem item = itemConEstado(EstadoComanda.PRE_RESERVA, 6L);

        List<ItemVisitaResponse> result = mapper.toItemsVisitaResponse(List.of(item));

        assertThat(result.get(0).getEstadoItem()).isEqualTo("En espera");
    }

    // ---------- toEstadoVisitaResponse: rama visitaCerrada ----------

    @Test
    @DisplayName("toEstadoVisitaResponse → visita con fechaHoraFin → visitaCerrada=true")
    void toEstadoVisitaResponse_visitaCerrada_true() {
        EstadoVisitaResponse resp = mapper.toEstadoVisitaResponse(
                visitaCerrada(), "M1", List.of(), Optional.empty());

        assertThat(resp.isVisitaCerrada()).isTrue();
        assertThat(resp.getVisitaId()).isEqualTo(11L);
        assertThat(resp.getMesaIdentificador()).isEqualTo("M1");
        assertThat(resp.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.isAsistenciaSolicitada()).isFalse();
        assertThat(resp.getNotificacionAsistenciaId()).isNull();
    }

    @Test
    @DisplayName("toEstadoVisitaResponse → visita sin fechaHoraFin → visitaCerrada=false")
    void toEstadoVisitaResponse_visitaAbierta_false() {
        EstadoVisitaResponse resp = mapper.toEstadoVisitaResponse(
                visitaAbierta(), null, List.of(), Optional.empty());

        assertThat(resp.isVisitaCerrada()).isFalse();
        assertThat(resp.getMesaIdentificador()).isNull();
    }

    @Test
    @DisplayName("toEstadoVisitaResponse → con asistencia activa → mapea id y flag")
    void toEstadoVisitaResponse_conAsistencia_mapeaIdYFlag() {
        Notificacion notif = Notificacion.builder()
                .notificacionId(99L)
                .build();
        ItemVisitaResponse item = ItemVisitaResponse.builder()
                .comandaItemId(1L)
                .subtotal(new BigDecimal("7000"))
                .build();

        EstadoVisitaResponse resp = mapper.toEstadoVisitaResponse(
                visitaAbierta(), "M2", List.of(item), Optional.of(notif));

        assertThat(resp.isAsistenciaSolicitada()).isTrue();
        assertThat(resp.getNotificacionAsistenciaId()).isEqualTo(99L);
        assertThat(resp.getTotal()).isEqualByComparingTo(new BigDecimal("7000"));
    }
}
