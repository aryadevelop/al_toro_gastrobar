package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.repository.OpcionModificacionRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoOpcionModificacionRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaMenuModificacionRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.modules.produccion.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.reservas.dto.request.PreOrdenItemRequest;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de {@link PreOrdenGestor}.
 *
 * <p>Cubre los tres métodos públicos del componente:
 * <ul>
 *   <li>{@link PreOrdenGestor#validarPreOrden}</li>
 *   <li>{@link PreOrdenGestor#persistirPreOrden}</li>
 *   <li>{@link PreOrdenGestor#eliminarPreOrdenExistente}</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PreOrdenGestorTest {

    @Mock
    ProductoRepository productoRepository;

    @Mock
    OpcionModificacionRepository opcionModificacionRepository;

    @Mock
    ProductoOpcionModificacionRepository productoOpcionModificacionRepository;

    @Mock
    ComandaRepository comandaRepository;

    @Mock
    ComandaItemRepository comandaItemRepository;

    @Mock
    ComandaMenuModificacionRepository comandaMenuModificacionRepository;

    @InjectMocks
    PreOrdenGestor preOrdenGestor;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PreOrdenItemRequest itemNormal(long productoId) {
        PreOrdenItemRequest item = new PreOrdenItemRequest();
        item.setProductoId(productoId);
        item.setCantidad(1);
        item.setEsMenuEspecial(false);
        return item;
    }

    private PreOrdenItemRequest itemMenuEspecial(long productoId) {
        PreOrdenItemRequest item = new PreOrdenItemRequest();
        item.setProductoId(productoId);
        item.setCantidad(1);
        item.setEsMenuEspecial(true);
        return item;
    }

    private PreOrdenItemRequest itemMenuEspecialConOpciones(long productoId, List<Long> opciones) {
        PreOrdenItemRequest item = itemMenuEspecial(productoId);
        item.setOpcionesModificacion(opciones);
        return item;
    }

    private Producto productoActivo(long id, boolean menuEspecial) {
        return Producto.builder()
                .productoId(id)
                .productoNombre("Producto " + id)
                .productoEstado(EstadoGenerico.ACTIVO)
                .menuEspecial(menuEspecial)
                .productoPrecio(BigDecimal.valueOf(10))
                .build();
    }

    private Producto productoInactivo(long id) {
        return Producto.builder()
                .productoId(id)
                .productoNombre("Producto " + id)
                .productoEstado(EstadoGenerico.INACTIVO)
                .menuEspecial(false)
                .productoPrecio(BigDecimal.valueOf(10))
                .build();
    }

    // ========================================================================
    // validarPreOrden
    // ========================================================================

    @Nested
    @DisplayName("validarPreOrden")
    class ValidarPreOrden {

        @Test
        @DisplayName("Producto no existe → ResourceNotFoundException")
        void productoNoExiste_lanzaResourceNotFoundException() {
            PreOrdenItemRequest item = itemMenuEspecial(99L);
            when(productoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> preOrdenGestor.validarPreOrden(List.of(item), 11))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Ítem marcado como menú especial pero producto no es menú especial → BusinessException")
        void itemMenuEspecialPeroProductoNoEsMenuEspecial_lanzaBusinessException() {
            PreOrdenItemRequest item = itemMenuEspecial(1L);
            Producto producto = productoActivo(1L, false);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            assertThatThrownBy(() -> preOrdenGestor.validarPreOrden(List.of(item), 11))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Más de un ítem de menú especial → BusinessException con 'Solo puede seleccionar'")
        void masDeUnMenuEspecial_lanzaBusinessException() {
            PreOrdenItemRequest item1 = itemMenuEspecial(1L);
            PreOrdenItemRequest item2 = itemMenuEspecial(2L);
            Producto p1 = productoActivo(1L, true);
            Producto p2 = productoActivo(2L, true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(p1));
            when(productoRepository.findById(2L)).thenReturn(Optional.of(p2));

            assertThatThrownBy(() -> preOrdenGestor.validarPreOrden(List.of(item1, item2), 11))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Solo puede seleccionar");
        }

        @Test
        @DisplayName("Menú especial con 5 personas (<=10) → BusinessException con 'más de 10 personas'")
        void menuEspecialConMenosDe11Personas_lanzaBusinessException() {
            PreOrdenItemRequest item = itemMenuEspecial(1L);
            Producto producto = productoActivo(1L, true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            assertThatThrownBy(() -> preOrdenGestor.validarPreOrden(List.of(item), 5))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("más de 10 personas");
        }

        @Test
        @DisplayName("Menú especial con 11 personas → sin excepción")
        void menuEspecialConMasDe10Personas_sinExcepcion() {
            PreOrdenItemRequest item = itemMenuEspecial(1L);
            Producto producto = productoActivo(1L, true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            assertThatCode(() -> preOrdenGestor.validarPreOrden(List.of(item), 11))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Sin menús especiales → sin excepción y sin llamadas a productoRepository")
        void sinMenusEspeciales_cualquierPersonas_sinExcepcion() {
            PreOrdenItemRequest item1 = itemNormal(1L);
            PreOrdenItemRequest item2 = itemNormal(2L);

            assertThatCode(() -> preOrdenGestor.validarPreOrden(List.of(item1, item2), 3))
                    .doesNotThrowAnyException();

            verifyNoInteractions(productoRepository);
        }
    }

    // ========================================================================
    // persistirPreOrden
    // ========================================================================

    @Nested
    @DisplayName("persistirPreOrden")
    class PersistirPreOrden {

        @Test
        @DisplayName("Producto inactivo → BusinessException")
        void productoInactivo_lanzaBusinessException() {
            Reserva reserva = mock(Reserva.class);
            Comanda comandaMock = mock(Comanda.class);
            when(comandaRepository.save(any())).thenReturn(comandaMock);

            PreOrdenItemRequest item = itemNormal(1L);
            Producto producto = productoInactivo(1L);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            assertThatThrownBy(() -> preOrdenGestor.persistirPreOrden(reserva, List.of(item)))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Ítem normal activo → persiste comanda y detalle")
        void itemNormal_persisteComandaYDetalle() {
            Reserva reserva = mock(Reserva.class);
            Comanda comandaMock = mock(Comanda.class);
            when(comandaMock.getComandaId()).thenReturn(1L);
            when(comandaRepository.save(any())).thenReturn(comandaMock);

            PreOrdenItemRequest item = itemNormal(1L);
            Producto producto = productoActivo(1L, false);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            ComandaItem itemGuardado = ComandaItem.builder()
                    .comandaItemId(10L)
                    .comanda(comandaMock)
                    .producto(producto)
                    .comandaItemCantidad(1)
                    .comandaItemPrecio(BigDecimal.valueOf(10))
                    .build();
            when(comandaItemRepository.save(any())).thenReturn(itemGuardado);
            when(comandaItemRepository.findByComanda_ComandaId(1L)).thenReturn(List.of(itemGuardado));

            preOrdenGestor.persistirPreOrden(reserva, List.of(item));

            verify(comandaRepository, times(1)).save(any());
            verify(comandaItemRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Ítem de menú especial con opciones → persiste modificaciones")
        void itemMenuEspecial_persisteModificaciones() {
            Reserva reserva = mock(Reserva.class);
            Comanda comandaMock = mock(Comanda.class);
            when(comandaMock.getComandaId()).thenReturn(1L);
            when(comandaRepository.save(any())).thenReturn(comandaMock);

            PreOrdenItemRequest item = itemMenuEspecialConOpciones(1L, List.of(1L));
            Producto producto = productoActivo(1L, true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            ComandaItem itemGuardado = ComandaItem.builder()
                    .comandaItemId(10L)
                    .comanda(comandaMock)
                    .producto(producto)
                    .comandaItemCantidad(1)
                    .comandaItemPrecio(BigDecimal.valueOf(10))
                    .build();
            when(comandaItemRepository.save(any())).thenReturn(itemGuardado);
            when(comandaItemRepository.findByComanda_ComandaId(1L)).thenReturn(List.of(itemGuardado));

            OpcionModificacion opcion = OpcionModificacion.builder()
                    .opcionId(1L)
                    .opcionNombre("Opción A")
                    .build();
            when(opcionModificacionRepository.findById(1L)).thenReturn(Optional.of(opcion));
            when(productoOpcionModificacionRepository.existsByProductoIdAndOpcionId(1L, 1L)).thenReturn(true);

            preOrdenGestor.persistirPreOrden(reserva, List.of(item));

            verify(comandaMenuModificacionRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Opción de modificación no existe → ResourceNotFoundException")
        void opcionNoExiste_lanzaResourceNotFoundException() {
            Reserva reserva = mock(Reserva.class);
            Comanda comandaMock = mock(Comanda.class);
            when(comandaRepository.save(any())).thenReturn(comandaMock);

            PreOrdenItemRequest item = itemMenuEspecialConOpciones(1L, List.of(99L));
            Producto producto = productoActivo(1L, true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            ComandaItem itemGuardado = ComandaItem.builder()
                    .comandaItemId(10L)
                    .comanda(comandaMock)
                    .producto(producto)
                    .comandaItemCantidad(1)
                    .comandaItemPrecio(BigDecimal.valueOf(10))
                    .build();
            when(comandaItemRepository.save(any())).thenReturn(itemGuardado);
            when(opcionModificacionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> preOrdenGestor.persistirPreOrden(reserva, List.of(item)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Opción no pertenece al producto → BusinessException")
        void opcionNoPerteneceAlProducto_lanzaBusinessException() {
            Reserva reserva = mock(Reserva.class);
            Comanda comandaMock = mock(Comanda.class);
            when(comandaRepository.save(any())).thenReturn(comandaMock);

            PreOrdenItemRequest item = itemMenuEspecialConOpciones(1L, List.of(1L));
            Producto producto = productoActivo(1L, true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            ComandaItem itemGuardado = ComandaItem.builder()
                    .comandaItemId(10L)
                    .comanda(comandaMock)
                    .producto(producto)
                    .comandaItemCantidad(1)
                    .comandaItemPrecio(BigDecimal.valueOf(10))
                    .build();
            when(comandaItemRepository.save(any())).thenReturn(itemGuardado);

            OpcionModificacion opcion = OpcionModificacion.builder()
                    .opcionId(1L)
                    .opcionNombre("Opción A")
                    .build();
            when(opcionModificacionRepository.findById(1L)).thenReturn(Optional.of(opcion));
            when(productoOpcionModificacionRepository.existsByProductoIdAndOpcionId(1L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> preOrdenGestor.persistirPreOrden(reserva, List.of(item)))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("Ítem de menú especial sin opciones → no guarda modificaciones")
        void itemSinOpciones_soloGuardaDetalle() {
            Reserva reserva = mock(Reserva.class);
            Comanda comandaMock = mock(Comanda.class);
            when(comandaMock.getComandaId()).thenReturn(1L);
            when(comandaRepository.save(any())).thenReturn(comandaMock);

            PreOrdenItemRequest item = itemMenuEspecial(1L);
            // opcionesModificacion is null by default
            Producto producto = productoActivo(1L, true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            ComandaItem itemGuardado = ComandaItem.builder()
                    .comandaItemId(10L)
                    .comanda(comandaMock)
                    .producto(producto)
                    .comandaItemCantidad(1)
                    .comandaItemPrecio(BigDecimal.valueOf(10))
                    .build();
            when(comandaItemRepository.save(any())).thenReturn(itemGuardado);
            when(comandaItemRepository.findByComanda_ComandaId(1L)).thenReturn(List.of(itemGuardado));

            preOrdenGestor.persistirPreOrden(reserva, List.of(item));

            verify(comandaMenuModificacionRepository, never()).save(any());
        }
    }

    // ========================================================================
    // eliminarPreOrdenExistente
    // ========================================================================

    @Nested
    @DisplayName("eliminarPreOrdenExistente")
    class EliminarPreOrdenExistente {

        @Test
        @DisplayName("Sin comanda PRE_RESERVA → no hace nada")
        void sinComandaPreReserva_noHaceNada() {
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(1L, EstadoComanda.PRE_RESERVA))
                    .thenReturn(Optional.empty());

            preOrdenGestor.eliminarPreOrdenExistente(1L);

            verify(comandaMenuModificacionRepository, never()).deleteByComandaItem_ComandaItemId(any());
            verify(comandaItemRepository, never()).deleteAll(any(List.class));
            verify(comandaRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Con comanda PRE_RESERVA y un ítem → elimina en orden: modificaciones → ítems → comanda")
        void conComandaPreReserva_eliminaEnOrden() {
            Comanda comanda = mock(Comanda.class);
            when(comanda.getComandaId()).thenReturn(1L);
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(1L, EstadoComanda.PRE_RESERVA))
                    .thenReturn(Optional.of(comanda));

            ComandaItem item = mock(ComandaItem.class);
            when(item.getComandaItemId()).thenReturn(10L);
            when(comandaItemRepository.findByComanda_ComandaId(1L)).thenReturn(List.of(item));

            preOrdenGestor.eliminarPreOrdenExistente(1L);

            InOrder inOrder = inOrder(comandaMenuModificacionRepository, comandaItemRepository, comandaRepository);
            inOrder.verify(comandaMenuModificacionRepository).deleteByComandaItem_ComandaItemId(10L);
            inOrder.verify(comandaItemRepository).deleteAll(List.of(item));
            inOrder.verify(comandaRepository).delete(comanda);
        }

        @Test
        @DisplayName("Comanda sin ítems → elimina solo la comanda, no llama deleteByComandaItemId")
        void sinItems_eliminaSoloComanda() {
            Comanda comanda = mock(Comanda.class);
            when(comanda.getComandaId()).thenReturn(1L);
            when(comandaRepository.findByReserva_ReservaIdAndComandaEstado(1L, EstadoComanda.PRE_RESERVA))
                    .thenReturn(Optional.of(comanda));

            when(comandaItemRepository.findByComanda_ComandaId(1L)).thenReturn(Collections.emptyList());

            preOrdenGestor.eliminarPreOrdenExistente(1L);

            verify(comandaMenuModificacionRepository, never()).deleteByComandaItem_ComandaItemId(any());
            verify(comandaItemRepository, times(1)).deleteAll(Collections.emptyList());
            verify(comandaRepository, times(1)).delete(comanda);
        }
    }
}
