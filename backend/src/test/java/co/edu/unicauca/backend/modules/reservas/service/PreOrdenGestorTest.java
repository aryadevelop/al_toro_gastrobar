package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.inventario.entity.OpcionModificacion;
import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.repository.MenuBebidaDisponibleRepository;
import co.edu.unicauca.backend.modules.inventario.repository.OpcionModificacionRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoOpcionModificacionRepository;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaMenuModificacionRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.reservas.dto.request.PreOrdenItemRequest;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Mock
    MenuBebidaDisponibleRepository menuBebidaDisponibleRepository;

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
        item.setCantidad(11);
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
                .productoCategoria(CategoriaProducto.PLATO)
                .productoPrecio(BigDecimal.valueOf(10))
                .build();
    }

    private Producto productoActivoCarta(long id, CategoriaProducto categoria) {
        return Producto.builder()
                .productoId(id)
                .productoNombre("Producto " + id)
                .productoEstado(EstadoGenerico.ACTIVO)
                .menuEspecial(false)
                .productoCategoria(categoria)
                .productoPrecio(BigDecimal.valueOf(10))
                .build();
    }

    private Producto productoInactivo(long id) {
        return Producto.builder()
                .productoId(id)
                .productoNombre("Producto " + id)
                .productoEstado(EstadoGenerico.INACTIVO)
                .menuEspecial(false)
                .productoCategoria(CategoriaProducto.PLATO)
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
        @DisplayName("Menú especial con 11 personas y bebida válida → sin excepción")
        void menuEspecialConMasDe10Personas_sinExcepcion() {
            PreOrdenItemRequest item = itemMenuEspecial(1L);
            // La bebida es obligatoria para que no falle la nueva validación
            item.setBebidaProductoId(100L);
            Producto producto = productoActivo(1L, true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(menuBebidaDisponibleRepository.existsByMenuIdAndBebidaId(1L, 100L)).thenReturn(true);

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

        @Test
        @DisplayName("Menú especial sin bebidaProductoId → BusinessException con 'bebida'")
        void validar_rejectsMenuWithoutBebida() {
            PreOrdenItemRequest item = itemMenuEspecial(1L);
            // bebidaProductoId es null (no se asigna)
            Producto producto = productoActivo(1L, true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            assertThatThrownBy(() -> preOrdenGestor.validarPreOrden(List.of(item), 11))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("bebida");
        }

        @Test
        @DisplayName("Bebida no disponible para el menú → BusinessException con 'disponible'")
        void validar_rejectsBebidaNotInMenu() {
            PreOrdenItemRequest item = itemMenuEspecial(1L);
            item.setBebidaProductoId(999L);
            Producto producto = productoActivo(1L, true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            // La bebida 999 no pertenece al menú 1
            when(menuBebidaDisponibleRepository.existsByMenuIdAndBebidaId(1L, 999L)).thenReturn(false);

            assertThatThrownBy(() -> preOrdenGestor.validarPreOrden(List.of(item), 11))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("disponible");
        }

        @Test
        @DisplayName("Menú especial con cantidad > 10 → no lanza excepción")
        void validarPreOrden_menuEspecialCantidadMayorA10_noLanza() {
            PreOrdenItemRequest item = itemMenuEspecial(1L);
            item.setCantidad(11);
            item.setBebidaProductoId(100L);
            Producto producto = productoActivo(1L, true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(menuBebidaDisponibleRepository.existsByMenuIdAndBebidaId(1L, 100L)).thenReturn(true);

            assertThatCode(() -> preOrdenGestor.validarPreOrden(List.of(item), 11))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Menú especial con cantidad = 10 → BusinessException con mensaje exacto")
        void validarPreOrden_menuEspecialCantidadIgualA10_lanza422() {
            PreOrdenItemRequest item = itemMenuEspecial(1L);
            item.setCantidad(10);
            Producto producto = productoActivo(1L, true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            assertThatThrownBy(() -> preOrdenGestor.validarPreOrden(List.of(item), 11))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("La cantidad del menú especial debe ser mayor a 10.");
        }

        @Test
        @DisplayName("Menú especial con cantidad < 10 → BusinessException con mensaje exacto")
        void validarPreOrden_menuEspecialCantidadMenorA10_lanza422() {
            PreOrdenItemRequest item = itemMenuEspecial(1L);
            item.setCantidad(5);
            Producto producto = productoActivo(1L, true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            assertThatThrownBy(() -> preOrdenGestor.validarPreOrden(List.of(item), 11))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("La cantidad del menú especial debe ser mayor a 10.");
        }
    }

    // ========================================================================
    // persistirPreOrden
    // ========================================================================

    @Nested
    @DisplayName("persistirPreOrden")
    class PersistirPreOrden {

        @Test
        @DisplayName("Producto inactivo → BusinessException (sin crear comanda)")
        void productoInactivo_lanzaBusinessException() {
            Reserva reserva = mock(Reserva.class);

            PreOrdenItemRequest item = itemNormal(1L);
            Producto producto = productoInactivo(1L);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            assertThatThrownBy(() -> preOrdenGestor.persistirPreOrden(reserva, List.of(item)))
                    .isInstanceOf(BusinessException.class);

            // La comanda no se crea porque la validación falla antes
            verify(comandaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Ítem normal PLATO activo → persiste 1 comanda COCINA y 1 item")
        void itemNormal_persisteComandaYDetalle() {
            Reserva reserva = mock(Reserva.class);
            Comanda cocinaMock = mock(Comanda.class);
            when(comandaRepository.save(any())).thenReturn(cocinaMock);

            PreOrdenItemRequest item = itemNormal(1L);
            Producto producto = productoActivo(1L, false);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(comandaItemRepository.save(any())).thenReturn(mock(ComandaItem.class));

            preOrdenGestor.persistirPreOrden(reserva, List.of(item));

            // Solo se crea la comanda COCINA
            verify(comandaRepository, times(1)).save(any());
            verify(comandaItemRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Ítem de menú especial con opciones → persiste modificaciones y 2 comandas")
        void itemMenuEspecial_persisteModificaciones() {
            Reserva reserva = mock(Reserva.class);
            Comanda cocinaMock = mock(Comanda.class);
            Comanda barraMock = mock(Comanda.class);
            // Primera llamada crea COCINA, segunda crea BARRA
            when(comandaRepository.save(any())).thenReturn(cocinaMock, barraMock);

            PreOrdenItemRequest item = itemMenuEspecialConOpciones(1L, List.of(1L));
            item.setBebidaProductoId(2L);
            Producto producto = productoActivo(1L, true);
            Producto bebida = productoActivoCarta(2L, CategoriaProducto.BEBIDA);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(productoRepository.findById(2L)).thenReturn(Optional.of(bebida));

            ComandaItem itemGuardado = mock(ComandaItem.class);
            when(comandaItemRepository.save(any())).thenReturn(itemGuardado);

            OpcionModificacion opcion = OpcionModificacion.builder()
                    .opcionId(1L)
                    .opcionNombre("Opción A")
                    .build();
            when(opcionModificacionRepository.findById(1L)).thenReturn(Optional.of(opcion));
            when(productoOpcionModificacionRepository.existsByProductoIdAndOpcionId(1L, 1L)).thenReturn(true);

            preOrdenGestor.persistirPreOrden(reserva, List.of(item));

            // Se crean 2 comandas (COCINA y BARRA) y 1 modificación
            verify(comandaRepository, times(2)).save(any());
            verify(comandaMenuModificacionRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Opción de modificación no existe → ResourceNotFoundException")
        void opcionNoExiste_lanzaResourceNotFoundException() {
            Reserva reserva = mock(Reserva.class);
            Comanda comandaMock = mock(Comanda.class);
            when(comandaRepository.save(any())).thenReturn(comandaMock);

            PreOrdenItemRequest item = itemMenuEspecialConOpciones(1L, List.of(99L));
            item.setBebidaProductoId(2L);
            Producto producto = productoActivo(1L, true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            ComandaItem itemGuardado = mock(ComandaItem.class);
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
            item.setBebidaProductoId(2L);
            Producto producto = productoActivo(1L, true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

            ComandaItem itemGuardado = mock(ComandaItem.class);
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
        @DisplayName("Ítem de menú especial sin opciones → no guarda modificaciones, crea 2 comandas")
        void itemSinOpciones_soloGuardaDetalle() {
            Reserva reserva = mock(Reserva.class);
            Comanda cocinaMock = mock(Comanda.class);
            Comanda barraMock = mock(Comanda.class);
            when(comandaRepository.save(any())).thenReturn(cocinaMock, barraMock);

            PreOrdenItemRequest item = itemMenuEspecial(1L);
            item.setBebidaProductoId(2L);
            // opcionesModificacion es null por defecto
            Producto producto = productoActivo(1L, true);
            Producto bebida = productoActivoCarta(2L, CategoriaProducto.BEBIDA);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(productoRepository.findById(2L)).thenReturn(Optional.of(bebida));
            when(comandaItemRepository.save(any())).thenReturn(mock(ComandaItem.class));

            preOrdenGestor.persistirPreOrden(reserva, List.of(item));

            verify(comandaMenuModificacionRepository, never()).save(any());
            // Se crean 2 comandas (COCINA para el plato, BARRA para la bebida)
            verify(comandaRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("Producto inexistente en persistirPreOrden → ResourceNotFoundException")
        void productoInexistente_lanzaResourceNotFound() {
            Reserva reserva = mock(Reserva.class);
            PreOrdenItemRequest item = itemNormal(99L);
            when(productoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> preOrdenGestor.persistirPreOrden(reserva, List.of(item)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Menú especial con bebida inexistente → ResourceNotFoundException")
        void bebidaMenuInexistente_lanzaResourceNotFound() {
            Reserva reserva = mock(Reserva.class);
            Comanda cocinaMock = mock(Comanda.class);
            when(comandaRepository.save(any())).thenReturn(cocinaMock);

            PreOrdenItemRequest item = itemMenuEspecial(1L);
            item.setBebidaProductoId(77L);
            Producto producto = productoActivo(1L, true);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(productoRepository.findById(77L)).thenReturn(Optional.empty());
            when(comandaItemRepository.save(any())).thenReturn(mock(ComandaItem.class));

            assertThatThrownBy(() -> preOrdenGestor.persistirPreOrden(reserva, List.of(item)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Ítem carta BEBIDA → crea comanda BARRA")
        void itemBebidaCarta_creaBarra() {
            Reserva reserva = mock(Reserva.class);
            Comanda barraMock = mock(Comanda.class);
            when(comandaRepository.save(any())).thenReturn(barraMock);

            PreOrdenItemRequest item = itemNormal(5L);
            Producto bebida = productoActivoCarta(5L, CategoriaProducto.BEBIDA);
            when(productoRepository.findById(5L)).thenReturn(Optional.of(bebida));
            when(comandaItemRepository.save(any())).thenReturn(mock(ComandaItem.class));

            preOrdenGestor.persistirPreOrden(reserva, List.of(item));

            ArgumentCaptor<Comanda> captor = ArgumentCaptor.forClass(Comanda.class);
            verify(comandaRepository).save(captor.capture());
            assertThat(captor.getValue().getComandaEstacion()).isEqualTo(EstacionComanda.BARRA);
        }

        @Test
        @DisplayName("Múltiples ítems del mismo tipo → no recrea comanda COCINA/BARRA")
        void multiplesItems_reutilizaComandas() {
            Reserva reserva = mock(Reserva.class);
            // Solo se esperan 2 saves de comanda: una COCINA, una BARRA
            when(comandaRepository.save(any())).thenReturn(mock(Comanda.class), mock(Comanda.class));
            when(comandaItemRepository.save(any())).thenReturn(mock(ComandaItem.class));

            // 2 ítems carta PLATO (cubre línea 245: cocina ya creada en 2da iteración)
            PreOrdenItemRequest plato1 = itemNormal(1L);
            PreOrdenItemRequest plato2 = itemNormal(2L);
            // 2 ítems carta BEBIDA (cubre línea 248: barra ya creada en 2da iteración)
            PreOrdenItemRequest bebida1 = itemNormal(3L);
            PreOrdenItemRequest bebida2 = itemNormal(4L);
            // 2 ítems menú especial (cubre líneas 192/229: cocina y barra ya creadas)
            PreOrdenItemRequest menu1 = itemMenuEspecial(5L);
            menu1.setBebidaProductoId(6L);
            PreOrdenItemRequest menu2 = itemMenuEspecial(7L);
            menu2.setBebidaProductoId(8L);

            when(productoRepository.findById(1L)).thenReturn(Optional.of(productoActivoCarta(1L, CategoriaProducto.PLATO)));
            when(productoRepository.findById(2L)).thenReturn(Optional.of(productoActivoCarta(2L, CategoriaProducto.PLATO)));
            when(productoRepository.findById(3L)).thenReturn(Optional.of(productoActivoCarta(3L, CategoriaProducto.BEBIDA)));
            when(productoRepository.findById(4L)).thenReturn(Optional.of(productoActivoCarta(4L, CategoriaProducto.BEBIDA)));
            when(productoRepository.findById(5L)).thenReturn(Optional.of(productoActivo(5L, true)));
            when(productoRepository.findById(6L)).thenReturn(Optional.of(productoActivoCarta(6L, CategoriaProducto.BEBIDA)));
            when(productoRepository.findById(7L)).thenReturn(Optional.of(productoActivo(7L, true)));
            when(productoRepository.findById(8L)).thenReturn(Optional.of(productoActivoCarta(8L, CategoriaProducto.BEBIDA)));

            preOrdenGestor.persistirPreOrden(reserva,
                    List.of(plato1, plato2, bebida1, bebida2, menu1, menu2));

            // Solo 2 comandas creadas (1 COCINA + 1 BARRA), no 6
            verify(comandaRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("Menú especial con lista de opciones vacía → no guarda modificaciones")
        void menuEspecialConOpcionesVacias_noGuardaModificaciones() {
            Reserva reserva = mock(Reserva.class);
            when(comandaRepository.save(any())).thenReturn(mock(Comanda.class), mock(Comanda.class));

            PreOrdenItemRequest item = itemMenuEspecialConOpciones(1L, List.of());
            item.setBebidaProductoId(2L);
            Producto producto = productoActivo(1L, true);
            Producto bebida = productoActivoCarta(2L, CategoriaProducto.BEBIDA);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
            when(productoRepository.findById(2L)).thenReturn(Optional.of(bebida));
            when(comandaItemRepository.save(any())).thenReturn(mock(ComandaItem.class));

            preOrdenGestor.persistirPreOrden(reserva, List.of(item));

            verify(comandaMenuModificacionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Menú especial crea Comanda COCINA y BARRA con mismo UUID grupo")
        void menuEspecial_createsTwoComandasConMismoGrupo() {
            Reserva reserva = mock(Reserva.class);
            Comanda cocinaMock = mock(Comanda.class);
            Comanda barraMock = mock(Comanda.class);
            // Primera llamada devuelve COCINA, segunda devuelve BARRA
            when(comandaRepository.save(any())).thenReturn(cocinaMock, barraMock);

            PreOrdenItemRequest item = itemMenuEspecial(1L);
            item.setBebidaProductoId(2L);
            Producto menu = productoActivo(1L, true);
            Producto bebida = productoActivoCarta(2L, CategoriaProducto.BEBIDA);
            when(productoRepository.findById(1L)).thenReturn(Optional.of(menu));
            when(productoRepository.findById(2L)).thenReturn(Optional.of(bebida));
            when(comandaItemRepository.save(any())).thenReturn(mock(ComandaItem.class));

            preOrdenGestor.persistirPreOrden(reserva, List.of(item));

            // Capturar las 2 comandas guardadas y verificar sus estaciones
            ArgumentCaptor<Comanda> comandaCaptor = ArgumentCaptor.forClass(Comanda.class);
            verify(comandaRepository, times(2)).save(comandaCaptor.capture());
            List<Comanda> comandas = comandaCaptor.getAllValues();
            assertThat(comandas).extracting(Comanda::getComandaEstacion)
                    .containsExactly(EstacionComanda.COCINA, EstacionComanda.BARRA);

            // Capturar los 2 ítems guardados y verificar que comparten el grupo y tienen precios correctos
            ArgumentCaptor<ComandaItem> itemCaptor = ArgumentCaptor.forClass(ComandaItem.class);
            verify(comandaItemRepository, times(2)).save(itemCaptor.capture());
            List<ComandaItem> items = itemCaptor.getAllValues();
            // Ambos items deben tener el mismo UUID de grupo (no nulo)
            assertThat(items.get(0).getComandaItemMenuGrupo()).isNotNull();
            assertThat(items.get(0).getComandaItemMenuGrupo())
                    .isEqualTo(items.get(1).getComandaItemMenuGrupo());
            // El plato (COCINA) tiene el precio del producto; la bebida (BARRA) tiene precio 0
            assertThat(items.get(0).getComandaItemPrecio()).isEqualByComparingTo(BigDecimal.valueOf(10));
            assertThat(items.get(1).getComandaItemPrecio()).isEqualByComparingTo(BigDecimal.ZERO);
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
                    .thenReturn(List.of());

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
                    .thenReturn(List.of(comanda));

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
                    .thenReturn(List.of(comanda));

            when(comandaItemRepository.findByComanda_ComandaId(1L)).thenReturn(Collections.emptyList());

            preOrdenGestor.eliminarPreOrdenExistente(1L);

            verify(comandaMenuModificacionRepository, never()).deleteByComandaItem_ComandaItemId(any());
            verify(comandaItemRepository, times(1)).deleteAll(Collections.emptyList());
            verify(comandaRepository, times(1)).delete(comanda);
        }
    }
}
