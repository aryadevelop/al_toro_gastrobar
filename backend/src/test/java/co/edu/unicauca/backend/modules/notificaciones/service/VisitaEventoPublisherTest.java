package co.edu.unicauca.backend.modules.notificaciones.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.VisitaEstadoMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VisitaEventoPublisher")
class VisitaEventoPublisherTest {

    @Mock ComandaRepository comandaRepository;
    @Mock VisitaEstadoMapper visitaEstadoMapper;
    @Mock NotificacionWsPublisher wsPublisher;
    @InjectMocks VisitaEventoPublisher publisher;

    @Test
    @DisplayName("publica items activos y total a /orden")
    void publicarOrden_conItems_publicaTotalYItems() {
        ComandaItem item = ComandaItem.builder()
                .comandaItemPrecio(new BigDecimal("10.00"))
                .comandaItemCantidad(2).build();
        when(comandaRepository.findAllItemsActivosByVisita(5L)).thenReturn(List.of(item));
        when(visitaEstadoMapper.toItemsVisitaResponse(any())).thenReturn(List.of());

        publisher.publicarOrden(5L);

        ArgumentCaptor<VisitaActualizadaWsMessage> cap = ArgumentCaptor.forClass(VisitaActualizadaWsMessage.class);
        verify(wsPublisher).publicarVisitaActualizada(eq(5L), cap.capture());
        assertThat(cap.getValue().getTotal()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("ignora ítems con precio nulo en el total")
    void publicarOrden_precioNulo_seIgnoraEnTotal() {
        ComandaItem conPrecio = ComandaItem.builder()
                .comandaItemPrecio(new BigDecimal("8.00")).comandaItemCantidad(1).build();
        ComandaItem sinPrecio = ComandaItem.builder()
                .comandaItemPrecio(null).comandaItemCantidad(3).build();
        when(comandaRepository.findAllItemsActivosByVisita(5L)).thenReturn(List.of(conPrecio, sinPrecio));
        when(visitaEstadoMapper.toItemsVisitaResponse(any())).thenReturn(List.of());

        publisher.publicarOrden(5L);

        ArgumentCaptor<VisitaActualizadaWsMessage> cap = ArgumentCaptor.forClass(VisitaActualizadaWsMessage.class);
        verify(wsPublisher).publicarVisitaActualizada(eq(5L), cap.capture());
        assertThat(cap.getValue().getTotal()).isEqualByComparingTo("8.00");
    }
}
