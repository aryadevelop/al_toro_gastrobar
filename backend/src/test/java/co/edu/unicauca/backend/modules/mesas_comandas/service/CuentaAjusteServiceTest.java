package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.AjustarItemsRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.notificaciones.service.VisitaEventoPublisher;
import co.edu.unicauca.backend.modules.pagos_caja.dto.response.CuentaPreliminarResponse;
import co.edu.unicauca.backend.modules.pagos_caja.service.CuentaService;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CuentaAjusteService")
class CuentaAjusteServiceTest {

    @Mock VisitaRepository visitaRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock ComandaItemRepository comandaItemRepository;
    @Mock VisitaEventoPublisher visitaEventoPublisher;
    @Mock CuentaService cuentaService;
    @InjectMocks CuentaAjusteService service;

    private Visita visitaActiva() {
        return Visita.builder().visitaId(5L).build();
    }

    private ComandaItem carta(Long id, int cant, BigDecimal precio, String desc) {
        Comanda c = Comanda.builder().comandaId(100L + id).visita(visitaActiva()).build();
        Producto p = Producto.builder().productoId(id).productoNombre("P" + id)
                .productoCategoria(CategoriaProducto.PLATO).menuEspecial(false).build();
        return ComandaItem.builder().comandaItemId(id).comanda(c).producto(p)
                .comandaItemCantidad(cant).comandaItemPrecio(precio).comandaItemDescripcion(desc).build();
    }

    private ComandaItem menu(Long id, String grupo, EstacionComanda est, BigDecimal precio) {
        Comanda c = Comanda.builder().comandaId(200L + id).comandaEstacion(est).visita(visitaActiva()).build();
        Producto p = Producto.builder().productoId(id).productoNombre("Menu" + id)
                .productoCategoria(est == EstacionComanda.COCINA ? CategoriaProducto.PLATO : CategoriaProducto.BEBIDA)
                .menuEspecial(est == EstacionComanda.COCINA).build();
        return ComandaItem.builder().comandaItemId(id).comanda(c).producto(p)
                .comandaItemCantidad(2).comandaItemPrecio(precio).comandaItemMenuGrupo(grupo)
                .comandaItemDescripcion(est == EstacionComanda.COCINA ? "sin sal" : null).build();
    }

    @BeforeEach
    void base() {
        when(visitaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(visitaActiva()));
        when(comandaRepository.findByIdForUpdate(anyLong()))
                .thenAnswer(i -> Optional.of(Comanda.builder().comandaId(i.getArgument(0)).build()));
        when(cuentaService.obtenerCuenta(5L))
                .thenReturn(CuentaPreliminarResponse.builder().visitaId(5L).build());
    }

    @Test
    @DisplayName("cambia cantidad de ítem de carta")
    void cambiaCantidadCarta() {
        ComandaItem it = carta(1L, 2, new BigDecimal("10.00"), null);
        when(comandaRepository.findAllItemsActivosByVisita(5L)).thenReturn(List.of(it));
        AjustarItemsRequest req = AjustarItemsRequest.builder()
                .items(List.of(AjustarItemsRequest.ItemAjuste.builder().comandaItemId(1L).cantidad(7).build()))
                .build();

        service.ajustarItems(5L, req);

        assertThat(it.getComandaItemCantidad()).isEqualTo(7);
        verify(comandaItemRepository).save(it);
        verify(visitaEventoPublisher).publicarOrden(5L);
    }

    @Test
    @DisplayName("cambiar cantidad de menú especial actualiza ambos ítems del par")
    void cambiaCantidadMenu_propagaAlPar() {
        ComandaItem plato = menu(1L, "g1", EstacionComanda.COCINA, new BigDecimal("50.00"));
        ComandaItem bebida = menu(2L, "g1", EstacionComanda.BARRA, BigDecimal.ZERO);
        when(comandaRepository.findAllItemsActivosByVisita(5L)).thenReturn(List.of(plato, bebida));
        AjustarItemsRequest req = AjustarItemsRequest.builder()
                .items(List.of(AjustarItemsRequest.ItemAjuste.builder().comandaItemId(1L).cantidad(4).build()))
                .build();

        service.ajustarItems(5L, req);

        assertThat(plato.getComandaItemCantidad()).isEqualTo(4);
        assertThat(bebida.getComandaItemCantidad()).isEqualTo(4);
    }

    @Test
    @DisplayName("editar precio de ítem modificado lo actualiza")
    void editaPrecioModificado() {
        ComandaItem it = carta(1L, 1, new BigDecimal("10.00"), "extra queso");
        when(comandaRepository.findAllItemsActivosByVisita(5L)).thenReturn(List.of(it));
        AjustarItemsRequest req = AjustarItemsRequest.builder()
                .items(List.of(AjustarItemsRequest.ItemAjuste.builder()
                        .comandaItemId(1L).cantidad(1).precio(new BigDecimal("12.50")).build()))
                .build();

        service.ajustarItems(5L, req);

        assertThat(it.getComandaItemPrecio()).isEqualByComparingTo("12.50");
    }

    @Test
    @DisplayName("editar precio de ítem NO modificado → BusinessException")
    void editaPrecioNoModificado_lanza() {
        ComandaItem it = carta(1L, 1, new BigDecimal("10.00"), null);
        when(comandaRepository.findAllItemsActivosByVisita(5L)).thenReturn(List.of(it));
        AjustarItemsRequest req = AjustarItemsRequest.builder()
                .items(List.of(AjustarItemsRequest.ItemAjuste.builder()
                        .comandaItemId(1L).cantidad(1).precio(new BigDecimal("12.50")).build()))
                .build();

        assertThatThrownBy(() -> service.ajustarItems(5L, req)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("eliminar ítem de carta")
    void eliminaCarta() {
        ComandaItem it = carta(1L, 1, new BigDecimal("10.00"), null);
        when(comandaRepository.findAllItemsActivosByVisita(5L)).thenReturn(List.of(it));
        AjustarItemsRequest req = AjustarItemsRequest.builder().eliminados(List.of(1L)).build();

        service.ajustarItems(5L, req);

        verify(comandaItemRepository).delete(it);
    }

    @Test
    @DisplayName("eliminar ítem de menú elimina el par completo")
    void eliminaMenu_eliminaPar() {
        ComandaItem plato = menu(1L, "g1", EstacionComanda.COCINA, new BigDecimal("50.00"));
        ComandaItem bebida = menu(2L, "g1", EstacionComanda.BARRA, BigDecimal.ZERO);
        when(comandaRepository.findAllItemsActivosByVisita(5L)).thenReturn(List.of(plato, bebida));
        AjustarItemsRequest req = AjustarItemsRequest.builder().eliminados(List.of(1L)).build();

        service.ajustarItems(5L, req);

        verify(comandaItemRepository).delete(plato);
        verify(comandaItemRepository).delete(bebida);
    }

    @Test
    @DisplayName("ítem que no pertenece a la visita → ResourceNotFoundException")
    void itemForaneo_lanza() {
        when(comandaRepository.findAllItemsActivosByVisita(5L)).thenReturn(List.of());
        AjustarItemsRequest req = AjustarItemsRequest.builder()
                .items(List.of(AjustarItemsRequest.ItemAjuste.builder().comandaItemId(99L).cantidad(1).build()))
                .build();

        assertThatThrownBy(() -> service.ajustarItems(5L, req)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("request vacío → no-op, devuelve la cuenta actual sin publicar")
    void requestVacio_noOp() {
        AjustarItemsRequest req = AjustarItemsRequest.builder().build();

        CuentaPreliminarResponse r = service.ajustarItems(5L, req);

        assertThat(r.getVisitaId()).isEqualTo(5L);
        verify(comandaItemRepository, never()).save(any());
        verify(comandaItemRepository, never()).delete(any());
        verify(visitaEventoPublisher, never()).publicarOrden(anyLong());
    }

    @Test
    @DisplayName("visita inexistente → ResourceNotFoundException")
    void visitaInexistente_lanza() {
        when(visitaRepository.findByIdForUpdate(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.ajustarItems(5L, AjustarItemsRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("visita ya cerrada → BusinessException")
    void visitaCerrada_lanza() {
        Visita cerrada = Visita.builder().visitaId(5L).visitaFechaHoraFin(LocalDateTime.now()).build();
        when(visitaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(cerrada));
        assertThatThrownBy(() -> service.ajustarItems(5L, AjustarItemsRequest.builder().build()))
                .isInstanceOf(BusinessException.class);
    }
}
