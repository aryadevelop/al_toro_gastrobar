package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.inventario.repository.ProductoRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.AgregarItemRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.ModificarItemRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.NotasRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.BorradorComandaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.ComandaBorradorMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.VisitaEstadoMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.notificaciones.service.MesaWsPublisher;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComandaBorradorService")
class ComandaBorradorServiceTest {

    // ── 12 mocks obligatorios para evitar NPE en @InjectMocks ──────────────────
    @Mock ComandaRepository         comandaRepository;
    @Mock ComandaItemRepository     comandaItemRepository;
    @Mock ProductoRepository        productoRepository;
    @Mock MesaRepository            mesaRepository;
    @Mock ComandaBorradorMapper     borradorMapper;
    @Mock VisitaEstadoMapper        visitaEstadoMapper;
    @Mock ComandaBorradorValidador  validador;
    @Mock MesaValidador             mesaValidador;
    @Mock MesaWsPublisher           mesaWsPublisher;
    @Mock NotificacionWsPublisher   notificacionWsPublisher;
    @Mock RabbitTemplate            rabbitTemplate;
    @Mock co.edu.unicauca.backend.modules.mesas_comandas.mapper.ComandaProduccionMapper comandaProduccionMapper;

    @InjectMocks ComandaBorradorService service;

    // ═══════════════════════════════════════════════════════════════════════════
    // ObtenerBorrador
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("obtenerBorrador")
    class ObtenerBorrador {

        @Test
        @DisplayName("con visita válida delega en validador y devuelve DTO del mapper")
        void conVisitaValida_delegaYDevuelveDTO() {
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            BorradorComandaResponse esperado = mock(BorradorComandaResponse.class);
            Authentication auth = crearAuth();

            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of());
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(eq(mesa), anyList(), any())).thenReturn(esperado);

            BorradorComandaResponse resultado = service.obtenerBorrador(1L, auth);

            assertThat(resultado).isSameAs(esperado);
            verify(mesaValidador).validarOwnership(1L, auth);
        }

        @Test
        @DisplayName("pasa comandas BORRADOR y total acumulado al mapper")
        void pasaComandaYTotalAlMapper() {
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            Authentication auth = crearAuth();

            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(borrador));
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(new BigDecimal("45.00"));
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.obtenerBorrador(1L, auth);

            verify(borradorMapper).toBorradorResponse(eq(mesa), eq(List.of(borrador)), eq(new BigDecimal("45.00")));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // AgregarItem
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("agregarItem")
    class AgregarItem {

        @Test
        @DisplayName("producto no encontrado lanza ResourceNotFoundException")
        void productoNoEncontrado_lanzaResourceNotFound() {
            Authentication auth = crearAuth();
            AgregarItemRequest req = agregarReq(1L, 99L, 2, null);

            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(crearMesa(1L, EstadoMesa.EN_PREPARACION));
            when(productoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.agregarItem(req, auth))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("producto de menú especial lanza BusinessException")
        void productoMenuEspecial_lanzaBusinessException() {
            Authentication auth = crearAuth();
            AgregarItemRequest req = agregarReq(1L, 5L, 1, null);
            Producto menuEsp = producto(5L, CategoriaProducto.PLATO, true);

            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(crearMesa(1L, EstadoMesa.EN_PREPARACION));
            when(productoRepository.findById(5L)).thenReturn(Optional.of(menuEsp));

            assertThatThrownBy(() -> service.agregarItem(req, auth))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("pre-orden");
        }

        @Test
        @DisplayName("mismo productoId y misma descripción acumula cantidad en ítem existente")
        void mismaDescripcion_acumulaCantidad() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            AgregarItemRequest req = agregarReq(1L, 3L, 2, null);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borradorComanda = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem existente = item(20L, borradorComanda, prod, 3, null, null);

            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(productoRepository.findById(3L)).thenReturn(Optional.of(prod));
            when(validador.resolverEstacion(prod)).thenReturn(EstacionComanda.COCINA);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion(
                    1L, EstadoComanda.BORRADOR, EstacionComanda.COCINA))
                    .thenReturn(Optional.of(borradorComanda));
            when(comandaItemRepository.findByComanda_ComandaIdAndProducto_ProductoId(10L, 3L))
                    .thenReturn(List.of(existente));
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(borradorComanda));
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.agregarItem(req, auth);

            // cantidad nueva = 3 + 2 = 5
            assertThat(existente.getComandaItemCantidad()).isEqualTo(5);
            verify(comandaItemRepository).save(existente);
        }

        @Test
        @DisplayName("al acumular revalida stock con nueva cantidad y cantidad anterior")
        void acumular_revalidaStock() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            AgregarItemRequest req = agregarReq(1L, 3L, 2, null);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borradorComanda = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem existente = item(20L, borradorComanda, prod, 3, null, null);

            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(productoRepository.findById(3L)).thenReturn(Optional.of(prod));
            when(validador.resolverEstacion(prod)).thenReturn(EstacionComanda.COCINA);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion(
                    1L, EstadoComanda.BORRADOR, EstacionComanda.COCINA))
                    .thenReturn(Optional.of(borradorComanda));
            when(comandaItemRepository.findByComanda_ComandaIdAndProducto_ProductoId(10L, 3L))
                    .thenReturn(List.of(existente));
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(borradorComanda));
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.agregarItem(req, auth);

            // Primera validarStock: cantidad=2, anterior=0
            verify(validador).validarStock(prod, 2, 0);
            // Segunda validarStock al acumular: nuevaCantidad=5, anterior=3
            verify(validador).validarStock(prod, 5, 3);
        }

        @Test
        @DisplayName("producto nuevo crea un ítem con precio del catálogo")
        void productoNuevo_creaItem() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            AgregarItemRequest req = agregarReq(1L, 3L, 1, null);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borradorComanda = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());

            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(productoRepository.findById(3L)).thenReturn(Optional.of(prod));
            when(validador.resolverEstacion(prod)).thenReturn(EstacionComanda.COCINA);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion(
                    1L, EstadoComanda.BORRADOR, EstacionComanda.COCINA))
                    .thenReturn(Optional.of(borradorComanda));
            when(comandaItemRepository.findByComanda_ComandaIdAndProducto_ProductoId(10L, 3L))
                    .thenReturn(List.of());
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(borradorComanda));
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.agregarItem(req, auth);

            ArgumentCaptor<ComandaItem> captor = ArgumentCaptor.forClass(ComandaItem.class);
            verify(comandaItemRepository).save(captor.capture());
            ComandaItem guardado = captor.getValue();
            assertThat(guardado.getProducto()).isSameAs(prod);
            assertThat(guardado.getComandaItemCantidad()).isEqualTo(1);
            assertThat(guardado.getComandaItemPrecio()).isEqualTo(prod.getProductoPrecio());
        }

        @Test
        @DisplayName("cuando no existe comanda BORRADOR crea una nueva de tipo COCINA")
        void sinBorradorCocina_creaComandaCocina() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            AgregarItemRequest req = agregarReq(1L, 3L, 1, null);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda nuevaComanda = comanda(11L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());

            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(productoRepository.findById(3L)).thenReturn(Optional.of(prod));
            when(validador.resolverEstacion(prod)).thenReturn(EstacionComanda.COCINA);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion(
                    1L, EstadoComanda.BORRADOR, EstacionComanda.COCINA))
                    .thenReturn(Optional.empty());
            when(comandaRepository.save(any(Comanda.class))).thenReturn(nuevaComanda);
            when(comandaItemRepository.findByComanda_ComandaIdAndProducto_ProductoId(11L, 3L))
                    .thenReturn(List.of());
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(nuevaComanda));
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.agregarItem(req, auth);

            ArgumentCaptor<Comanda> captor = ArgumentCaptor.forClass(Comanda.class);
            verify(comandaRepository).save(captor.capture());
            assertThat(captor.getValue().getComandaEstacion()).isEqualTo(EstacionComanda.COCINA);
            assertThat(captor.getValue().getComandaEstado()).isEqualTo(EstadoComanda.BORRADOR);
        }

        @Test
        @DisplayName("cuando no existe comanda BORRADOR crea una nueva de tipo BARRA")
        void sinBorradorBarra_creaComandaBarra() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            AgregarItemRequest req = agregarReq(1L, 4L, 1, null);
            Producto prod = producto(4L, CategoriaProducto.BEBIDA, false);
            Comanda nuevaComanda = comanda(12L, EstadoComanda.BORRADOR, EstacionComanda.BARRA, mesa.getVisita());

            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(productoRepository.findById(4L)).thenReturn(Optional.of(prod));
            when(validador.resolverEstacion(prod)).thenReturn(EstacionComanda.BARRA);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion(
                    1L, EstadoComanda.BORRADOR, EstacionComanda.BARRA))
                    .thenReturn(Optional.empty());
            when(comandaRepository.save(any(Comanda.class))).thenReturn(nuevaComanda);
            when(comandaItemRepository.findByComanda_ComandaIdAndProducto_ProductoId(12L, 4L))
                    .thenReturn(List.of());
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(nuevaComanda));
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.agregarItem(req, auth);

            ArgumentCaptor<Comanda> captor = ArgumentCaptor.forClass(Comanda.class);
            verify(comandaRepository).save(captor.capture());
            assertThat(captor.getValue().getComandaEstacion()).isEqualTo(EstacionComanda.BARRA);
        }

        @Test
        @DisplayName("después de agregar publica /topic/mesas y /topic/visita/{id}/orden")
        void agregarItem_publicaMesasYOrdenCliente() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            AgregarItemRequest req = agregarReq(1L, 3L, 1, null);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borradorComanda = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());

            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(productoRepository.findById(3L)).thenReturn(Optional.of(prod));
            when(validador.resolverEstacion(prod)).thenReturn(EstacionComanda.COCINA);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion(
                    1L, EstadoComanda.BORRADOR, EstacionComanda.COCINA))
                    .thenReturn(Optional.of(borradorComanda));
            when(comandaItemRepository.findByComanda_ComandaIdAndProducto_ProductoId(10L, 3L))
                    .thenReturn(List.of());
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(borradorComanda));
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.agregarItem(req, auth);

            verify(mesaWsPublisher).publicarActualizacionMesa(eq(1L), eq(MesaWsPublisher.TipoEventoMesa.ACTUALIZAR));
            verify(notificacionWsPublisher).publicarVisitaActualizada(eq(1L), any());
        }

        @Test
        @DisplayName("descripción con distinto case/espacios acumula sobre el ítem existente")
        void descripcionEquivalente_acumula() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            AgregarItemRequest req = agregarReq(1L, 3L, 2, "  SIN AZUCAR  ");
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borradorComanda = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem existente = item(20L, borradorComanda, prod, 1, "sin azucar", null);

            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(productoRepository.findById(3L)).thenReturn(Optional.of(prod));
            when(validador.resolverEstacion(prod)).thenReturn(EstacionComanda.COCINA);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstadoAndComandaEstacion(
                    1L, EstadoComanda.BORRADOR, EstacionComanda.COCINA))
                    .thenReturn(Optional.of(borradorComanda));
            when(comandaItemRepository.findByComanda_ComandaIdAndProducto_ProductoId(10L, 3L))
                    .thenReturn(List.of(existente));
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(borradorComanda));
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.agregarItem(req, auth);

            assertThat(existente.getComandaItemCantidad()).isEqualTo(3);
            verify(comandaItemRepository).save(existente);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ModificarItem
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("modificarItem")
    class ModificarItem {

        @Test
        @DisplayName("item no encontrado lanza ResourceNotFoundException")
        void itemNoEncontrado_lanzaResourceNotFound() {
            Authentication auth = crearAuth();
            ModificarItemRequest req = modificarReq(2, null);

            when(comandaItemRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.modificarItem(99L, req, auth))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("comanda no BORRADOR lanza BusinessException")
        void comandaNoEsBorrador_lanzaBusinessException() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Comanda pendiente = comanda(10L, EstadoComanda.PENDIENTE, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem it = item(20L, pendiente, producto(3L, CategoriaProducto.PLATO, false), 2, null, null);
            ModificarItemRequest req = modificarReq(3, null);

            when(comandaItemRepository.findById(20L)).thenReturn(Optional.of(it));

            assertThatThrownBy(() -> service.modificarItem(20L, req, auth))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("BORRADOR");
        }

        @Test
        @DisplayName("cantidad == 0 lanza BusinessException")
        void cantidadCero_lanzaBusinessException() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem it = item(20L, borrador, producto(3L, CategoriaProducto.PLATO, false), 2, null, null);
            ModificarItemRequest req = modificarReq(0, null);

            when(comandaItemRepository.findById(20L)).thenReturn(Optional.of(it));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);

            assertThatThrownBy(() -> service.modificarItem(20L, req, auth))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cantidad 0");
        }

        @Test
        @DisplayName("solo cantidad actualiza cantidad y valida stock")
        void soloCantidad_actualizaCantidad() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem it = item(20L, borrador, prod, 2, null, null);
            ModificarItemRequest req = modificarReq(5, null);

            when(comandaItemRepository.findById(20L)).thenReturn(Optional.of(it));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(borrador));
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.modificarItem(20L, req, auth);

            assertThat(it.getComandaItemCantidad()).isEqualTo(5);
            verify(validador).validarStock(prod, 5, 2);
            verify(comandaItemRepository).save(it);
        }

        @Test
        @DisplayName("solo descripción actualiza descripción y no valida stock")
        void soloDescripcion_actualizaDescripcion() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem it = item(20L, borrador, prod, 2, null, null);
            ModificarItemRequest req = modificarReq(null, "sin cebolla");

            when(comandaItemRepository.findById(20L)).thenReturn(Optional.of(it));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(borrador));
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.modificarItem(20L, req, auth);

            assertThat(it.getComandaItemDescripcion()).isEqualTo("sin cebolla");
            verify(validador, never()).validarStock(any(), anyInt(), anyInt());
            verify(comandaItemRepository).save(it);
        }

        @Test
        @DisplayName("cantidad y descripción actualizan ambos campos")
        void cantidadYDescripcion_actualizaAmbos() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem it = item(20L, borrador, prod, 2, null, null);
            ModificarItemRequest req = modificarReq(4, "extra picante");

            when(comandaItemRepository.findById(20L)).thenReturn(Optional.of(it));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(borrador));
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.modificarItem(20L, req, auth);

            assertThat(it.getComandaItemCantidad()).isEqualTo(4);
            assertThat(it.getComandaItemDescripcion()).isEqualTo("extra picante");
            verify(validador).validarStock(prod, 4, 2);
        }

        @Test
        @DisplayName("descripción que colisiona con otro ítem del mismo producto lanza 409")
        void descripcionColisiona_lanza409() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem it = item(20L, borrador, prod, 2, "sin sal", null);
            ComandaItem otro = item(21L, borrador, prod, 1, "SIN AZUCAR", null);
            ModificarItemRequest req = modificarReq(null, "  sin azucar  ");

            when(comandaItemRepository.findById(20L)).thenReturn(Optional.of(it));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaItemRepository.findByComanda_ComandaIdAndProducto_ProductoId(10L, 3L))
                    .thenReturn(List.of(it, otro));

            assertThatThrownBy(() -> service.modificarItem(20L, req, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(org.springframework.http.HttpStatus.CONFLICT));
            // El cambio no se aplica
            assertThat(it.getComandaItemDescripcion()).isEqualTo("sin sal");
            verify(comandaItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("publica /topic/mesas y /topic/visita/{id}/orden al finalizar")
        void modificarItem_publicaMesasYOrdenCliente() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem it = item(20L, borrador, prod, 2, null, null);
            ModificarItemRequest req = modificarReq(null, "sin sal");

            when(comandaItemRepository.findById(20L)).thenReturn(Optional.of(it));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(borrador));
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.modificarItem(20L, req, auth);

            verify(mesaWsPublisher).publicarActualizacionMesa(eq(1L), eq(MesaWsPublisher.TipoEventoMesa.ACTUALIZAR));
            verify(notificacionWsPublisher).publicarVisitaActualizada(eq(1L), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EliminarItem
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("eliminarItem")
    class EliminarItem {

        @Test
        @DisplayName("item no encontrado lanza ResourceNotFoundException")
        void itemNoEncontrado_lanzaResourceNotFound() {
            Authentication auth = crearAuth();
            when(comandaItemRepository.findById(77L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.eliminarItem(77L, auth))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("comanda no BORRADOR lanza BusinessException")
        void comandaNoEsBorrador_lanzaBusinessException() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Comanda pendiente = comanda(10L, EstadoComanda.PENDIENTE, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem it = item(20L, pendiente, producto(3L, CategoriaProducto.PLATO, false), 1, null, null);

            when(comandaItemRepository.findById(20L)).thenReturn(Optional.of(it));

            assertThatThrownBy(() -> service.eliminarItem(20L, auth))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("BORRADOR");
        }

        @Test
        @DisplayName("ítem con menuGrupo elimina par cocina+barra y puede dejar comandas vacías")
        void itemConMenuGrupo_eliminaParYComandas() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            String grupo = "uuid-grupo-1";

            Comanda borrCocina = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            Comanda borrBarra  = comanda(11L, EstadoComanda.BORRADOR, EstacionComanda.BARRA,  mesa.getVisita());

            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            ComandaItem itemCocina = item(20L, borrCocina, prod, 1, null, grupo);
            ComandaItem itemBarra  = item(21L, borrBarra,  prod, 1, null, grupo);

            when(comandaItemRepository.findById(20L)).thenReturn(Optional.of(itemCocina));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            // eliminarParMenu calls findByVisita first, then per-comanda items, then eliminarComandaSiVacia
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(borrCocina, borrBarra))   // primera llamada: eliminarParMenu
                    .thenReturn(List.of());                        // segunda llamada: obtenerBorradorInterno
            // Cocina: primero devuelve items (para ver si grupo coincide), luego vacío (eliminarComandaSiVacia)
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(10L))
                    .thenReturn(List.of(itemCocina))   // lectura en eliminarParMenu
                    .thenReturn(List.of());             // lectura en eliminarComandaSiVacia
            // Barra: igual
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(11L))
                    .thenReturn(List.of(itemBarra))    // lectura en eliminarParMenu
                    .thenReturn(List.of());             // lectura en eliminarComandaSiVacia
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.eliminarItem(20L, auth);

            verify(comandaItemRepository).delete(itemCocina);
            verify(comandaItemRepository).delete(itemBarra);
            verify(comandaRepository, atLeastOnce()).delete(any(Comanda.class));
        }

        @Test
        @DisplayName("ítem base sin descripción arrastra modificados del mismo producto")
        void itemBase_arrastraModificados() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem base = item(20L, borrador, prod, 2, null, null);
            ComandaItem mod1 = item(21L, borrador, prod, 1, "sin cebolla", null);
            ComandaItem mod2 = item(22L, borrador, prod, 1, "extra picante", null);

            when(comandaItemRepository.findById(20L)).thenReturn(Optional.of(base));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaItemRepository.findByComanda_ComandaIdAndProducto_ProductoIdAndComandaItemDescripcionIsNotNull(
                    10L, 3L))
                    .thenReturn(List.of(mod1, mod2));
            // eliminarComandaSiVacia - comanda no vacía
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(10L))
                    .thenReturn(List.of());
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of());
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.eliminarItem(20L, auth);

            verify(comandaItemRepository).deleteAll(List.of(mod1, mod2));
            verify(comandaItemRepository).delete(base);
        }

        @Test
        @DisplayName("ítem modificado solo elimina ese ítem, no el base")
        void itemModificado_soloEliminaEse() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem mod = item(21L, borrador, prod, 1, "sin cebolla", null);

            when(comandaItemRepository.findById(21L)).thenReturn(Optional.of(mod));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            // comanda tiene más ítems
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(10L))
                    .thenReturn(List.of(item(20L, borrador, prod, 2, null, null)));
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(borrador));
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.eliminarItem(21L, auth);

            verify(comandaItemRepository).delete(mod);
            verify(comandaRepository, never()).delete(any(Comanda.class));
        }

        @Test
        @DisplayName("comanda queda vacía al eliminar último ítem y se elimina la comanda")
        void comandaVacia_eliminaComanda() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem it = item(20L, borrador, prod, 1, "sin sal", null);

            when(comandaItemRepository.findById(20L)).thenReturn(Optional.of(it));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            // después de borrar el ítem la comanda queda sin ítems
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(10L))
                    .thenReturn(List.of());
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of());
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.eliminarItem(20L, auth);

            verify(comandaRepository).delete(borrador);
        }

        @Test
        @DisplayName("publica /topic/mesas y /topic/visita/{id}/orden al finalizar (ítem modificado)")
        void eliminarItemModificado_publicaMesasYOrdenCliente() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem mod = item(21L, borrador, prod, 1, "sin cebolla", null);

            when(comandaItemRepository.findById(21L)).thenReturn(Optional.of(mod));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(10L))
                    .thenReturn(List.of(item(20L, borrador, prod, 2, null, null)));
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(borrador));
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.eliminarItem(21L, auth);

            verify(mesaWsPublisher).publicarActualizacionMesa(eq(1L), eq(MesaWsPublisher.TipoEventoMesa.ACTUALIZAR));
            verify(notificacionWsPublisher).publicarVisitaActualizada(eq(1L), any());
        }

        @Test
        @DisplayName("publica /topic/mesas y /topic/visita/{id}/orden al finalizar (ítem base con cascade)")
        void eliminarItemBase_publicaMesasYOrdenCliente() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem base = item(20L, borrador, prod, 2, null, null);

            when(comandaItemRepository.findById(20L)).thenReturn(Optional.of(base));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaItemRepository.findByComanda_ComandaIdAndProducto_ProductoIdAndComandaItemDescripcionIsNotNull(
                    10L, 3L))
                    .thenReturn(List.of());
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(10L))
                    .thenReturn(List.of());
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of());
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.eliminarItem(20L, auth);

            verify(mesaWsPublisher).publicarActualizacionMesa(eq(1L), eq(MesaWsPublisher.TipoEventoMesa.ACTUALIZAR));
            verify(notificacionWsPublisher).publicarVisitaActualizada(eq(1L), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ActualizarNotas
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("actualizarNotas")
    class ActualizarNotas {

        @Test
        @DisplayName("comanda no encontrada lanza ResourceNotFoundException")
        void comandaNoEncontrada_lanzaResourceNotFound() {
            Authentication auth = crearAuth();
            NotasRequest req = notasReq("texto");

            when(comandaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.actualizarNotas(99L, req, auth))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("comanda no BORRADOR lanza BusinessException")
        void comandaNoEsBorrador_lanzaBusinessException() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Comanda pendiente = comanda(10L, EstadoComanda.PENDIENTE, EstacionComanda.COCINA, mesa.getVisita());
            NotasRequest req = notasReq("texto");

            when(comandaRepository.findById(10L)).thenReturn(Optional.of(pendiente));

            assertThatThrownBy(() -> service.actualizarNotas(10L, req, auth))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("BORRADOR");
        }

        @Test
        @DisplayName("actualiza notas con texto y persiste sin publicar WS mesas")
        void setNotas_persisteSinPublicarMesas() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            NotasRequest req = notasReq("sin sal");

            when(comandaRepository.findById(10L)).thenReturn(Optional.of(borrador));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(borrador));
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.actualizarNotas(10L, req, auth);

            assertThat(borrador.getComandaNotas()).isEqualTo("sin sal");
            verify(comandaRepository).save(borrador);
            // NO publica /topic/mesas
            verify(mesaWsPublisher, never()).publicarActualizacionMesa(anyLong(), any());
        }

        @Test
        @DisplayName("set null borra las notas existentes")
        void setNull_borraNotas() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            borrador.setComandaNotas("notas anteriores");
            NotasRequest req = notasReq(null);

            when(comandaRepository.findById(10L)).thenReturn(Optional.of(borrador));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(borrador));
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));

            service.actualizarNotas(10L, req, auth);

            assertThat(borrador.getComandaNotas()).isNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EnviarAProduccion
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("enviarAProduccion")
    class EnviarAProduccion {

        @Test
        @DisplayName("comanda no encontrada lanza ResourceNotFoundException")
        void comandaNoEncontrada_lanzaResourceNotFound() {
            Authentication auth = crearAuth();
            when(comandaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.enviarAProduccion(99L, auth))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("comanda no BORRADOR lanza BusinessException")
        void comandaNoEsBorrador_lanzaBusinessException() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Comanda pendiente = comanda(10L, EstadoComanda.PENDIENTE, EstacionComanda.COCINA, mesa.getVisita());

            when(comandaRepository.findById(10L)).thenReturn(Optional.of(pendiente));

            assertThatThrownBy(() -> service.enviarAProduccion(10L, auth))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("producción");
        }

        @Test
        @DisplayName("comanda sin ítems delega en validarTieneItems (que lanza)")
        void sinItems_validarTieneItemsLanza() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());

            when(comandaRepository.findById(10L)).thenReturn(Optional.of(borrador));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(10L))
                    .thenReturn(List.of());
            doThrow(new BusinessException(
                    co.edu.unicauca.backend.shared.exception.ErrorCode.BUSINESS_ERROR,
                    "sin ítems",
                    org.springframework.http.HttpStatus.BAD_REQUEST))
                    .when(validador).validarTieneItems(anyList());

            assertThatThrownBy(() -> service.enviarAProduccion(10L, auth))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("stock insuficiente en ítem normal lanza BusinessException")
        void stockInsuficiente_lanzaBusinessException() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem it = item(20L, borrador, prod, 5, null, null);

            when(comandaRepository.findById(10L)).thenReturn(Optional.of(borrador));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(10L))
                    .thenReturn(List.of(it));
            doThrow(new BusinessException(
                    co.edu.unicauca.backend.shared.exception.ErrorCode.BUSINESS_ERROR,
                    "stock insuficiente",
                    org.springframework.http.HttpStatus.CONFLICT))
                    .when(validador).validarStock(eq(prod), eq(5), eq(5));

            assertThatThrownBy(() -> service.enviarAProduccion(10L, auth))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("stock");
        }

        @Test
        @DisplayName("ítems de menú especial NO pasan por validarStock")
        void itemMenuEspecial_noValidaStock() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Producto prod = producto(3L, CategoriaProducto.PLATO, true); // menuEspecial
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem it = item(20L, borrador, prod, 1, null, "uuid-grupo-1"); // menuGrupo != null

            when(comandaRepository.findById(10L)).thenReturn(Optional.of(borrador));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(10L))
                    .thenReturn(List.of(it));
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of());
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));
            when(comandaRepository.findAllItemsActivosByVisita(1L)).thenReturn(List.of());
            when(visitaEstadoMapper.toItemsVisitaResponse(anyList())).thenReturn(List.of());

            service.enviarAProduccion(10L, auth);

            verify(validador, never()).validarStock(any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("transiciona a PENDIENTE y sella fechaHoraInicio")
        void transicionaAPendienteYSellaFecha() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem it = item(20L, borrador, prod, 2, null, null);

            stubEnviarHappyPath(auth, borrador, mesa, it);

            service.enviarAProduccion(10L, auth);

            assertThat(borrador.getComandaEstado()).isEqualTo(EstadoComanda.PENDIENTE);
            assertThat(borrador.getComandaFechaHoraInicio()).isNotNull();
        }

        @Test
        @DisplayName("mesa en ESPERA transiciona a EN_PREPARACION y publica cambio de estado")
        void mesaEnEspera_transicionaAEnPreparacion() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.ESPERA);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem it = item(20L, borrador, prod, 2, null, null);

            stubEnviarHappyPath(auth, borrador, mesa, it);

            service.enviarAProduccion(10L, auth);

            assertThat(mesa.getMesaEstado()).isEqualTo(EstadoMesa.EN_PREPARACION);
            verify(mesaRepository).save(mesa);
            verify(mesaWsPublisher).publicarCambioEstadoMesa(1L, EstadoMesa.EN_PREPARACION);
        }

        @Test
        @DisplayName("mesa ya EN_PREPARACION no cambia de estado")
        void mesaYaEnPreparacion_noModificaEstado() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem it = item(20L, borrador, prod, 2, null, null);

            stubEnviarHappyPath(auth, borrador, mesa, it);

            service.enviarAProduccion(10L, auth);

            verify(mesaRepository, never()).save(any());
            verify(mesaWsPublisher, never()).publicarCambioEstadoMesa(anyLong(), any());
        }

        @Test
        @DisplayName("publica RabbitMQ, estación WS, mapa y orden cliente")
        void publica_RabbitMQ_estacion_mapa_ordenCliente() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false);
            Comanda borrador = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem it = item(20L, borrador, prod, 2, null, null);

            stubEnviarHappyPath(auth, borrador, mesa, it);

            service.enviarAProduccion(10L, auth);

            verify(rabbitTemplate).convertAndSend(
                    eq("altoro.topic"),
                    eq("comanda.nueva"),
                    any(Object.class));
            verify(mesaWsPublisher).publicarActualizacionMesa(eq(1L), eq(MesaWsPublisher.TipoEventoMesa.ACTUALIZAR));
            verify(notificacionWsPublisher).publicarVisitaActualizada(eq(1L), any());

            // Publish CREADA al tablero de producción con el contrato unificado
            org.mockito.ArgumentCaptor<co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaProduccionEventoWsMessage> creadaCaptor =
                    org.mockito.ArgumentCaptor.forClass(co.edu.unicauca.backend.modules.notificaciones.dto.ws.ComandaProduccionEventoWsMessage.class);
            verify(notificacionWsPublisher).publicarEventoProduccion(
                    eq(co.edu.unicauca.backend.shared.enums.EstacionComanda.COCINA), creadaCaptor.capture());
            assertThat(creadaCaptor.getValue().tipo())
                    .isEqualTo(co.edu.unicauca.backend.modules.notificaciones.dto.ws.TipoEventoProduccion.CREADA);
            assertThat(creadaCaptor.getValue().comandaId()).isEqualTo(10L);
        }

        @Test
        @DisplayName("calcula total acumulado con precios de ítems activos (cubre lambdas de stream)")
        void calculaTotalConItemsConPrecio() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Producto prod = producto(3L, CategoriaProducto.PLATO, false); // precio 15.00
            Comanda pendiente = comanda(20L, EstadoComanda.PENDIENTE, EstacionComanda.COCINA, mesa.getVisita());
            Comanda borrador  = comanda(10L, EstadoComanda.BORRADOR,  EstacionComanda.COCINA, mesa.getVisita());
            ComandaItem itBorrador  = item(30L, borrador,  prod, 2, null, null);
            ComandaItem itPendiente = item(31L, pendiente, prod, 1, null, null);

            when(comandaRepository.findById(10L)).thenReturn(Optional.of(borrador));
            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(10L))
                    .thenReturn(List.of(itBorrador));
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of());
            when(comandaItemRepository.sumTotalActivosByVisita(1L)).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));
            // Proporcionar ítems activos con precio para ejercitar las lambdas de stream
            when(comandaRepository.findAllItemsActivosByVisita(1L))
                    .thenReturn(List.of(itBorrador, itPendiente));
            when(visitaEstadoMapper.toItemsVisitaResponse(anyList())).thenReturn(List.of());

            service.enviarAProduccion(10L, auth);

            // Verifica que publicarVisitaActualizada recibe total = 15*2 + 15*1 = 45
            ArgumentCaptor<co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage> captor =
                    ArgumentCaptor.forClass(co.edu.unicauca.backend.modules.notificaciones.dto.ws.VisitaActualizadaWsMessage.class);
            verify(notificacionWsPublisher).publicarVisitaActualizada(eq(1L), captor.capture());
            assertThat(captor.getValue().getTotal()).isEqualByComparingTo(new BigDecimal("45.00"));
        }

        /** Configura los mocks comunes del happy path de enviarAProduccion. */
        private void stubEnviarHappyPath(Authentication auth, Comanda borrador, Mesa mesa, ComandaItem it) {
            when(comandaRepository.findById(borrador.getComandaId())).thenReturn(Optional.of(borrador));
            when(mesaValidador.validarOwnership(mesa.getVisitaId(), auth)).thenReturn(mesa);
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(borrador.getComandaId()))
                    .thenReturn(List.of(it));
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(mesa.getVisitaId(), EstadoComanda.BORRADOR))
                    .thenReturn(List.of());
            when(comandaItemRepository.sumTotalActivosByVisita(mesa.getVisitaId())).thenReturn(BigDecimal.ZERO);
            when(borradorMapper.toBorradorResponse(any(), any(), any())).thenReturn(mock(BorradorComandaResponse.class));
            when(comandaRepository.findAllItemsActivosByVisita(mesa.getVisitaId())).thenReturn(List.of());
            when(visitaEstadoMapper.toItemsVisitaResponse(anyList())).thenReturn(List.of());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CancelarFormulario
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("cancelarFormulario")
    class CancelarFormulario {

        @Test
        @DisplayName("con borradores existentes los elimina y publica mesas")
        void conBorradores_eliminaYPublica() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Comanda b1 = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());
            Comanda b2 = comanda(11L, EstadoComanda.BORRADOR, EstacionComanda.BARRA,  mesa.getVisita());

            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(b1, b2));

            service.cancelarFormulario(1L, auth);

            verify(comandaRepository).deleteAll(List.of(b1, b2));
            verify(mesaWsPublisher).publicarActualizacionMesa(eq(1L), eq(MesaWsPublisher.TipoEventoMesa.ACTUALIZAR));
        }

        @Test
        @DisplayName("sin borradores es idempotente (no llama deleteAll) y publica mesas igual")
        void sinBorradores_idempotente_publicaMesas() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);

            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of());

            service.cancelarFormulario(1L, auth);

            verify(comandaRepository, never()).deleteAll(anyList());
            verify(mesaWsPublisher).publicarActualizacionMesa(eq(1L), eq(MesaWsPublisher.TipoEventoMesa.ACTUALIZAR));
        }

        @Test
        @DisplayName("siempre publica /topic/mesas independientemente del número de borradores")
        void siemprePublicaMesas() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Comanda b = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());

            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(b));

            service.cancelarFormulario(1L, auth);

            verify(mesaWsPublisher, times(1))
                    .publicarActualizacionMesa(eq(1L), eq(MesaWsPublisher.TipoEventoMesa.ACTUALIZAR));
        }

        @Test
        @DisplayName("publica /topic/visita/{id}/orden al finalizar para reflejar la eliminación al cliente")
        void cancelarFormulario_publicaOrdenCliente() {
            Authentication auth = crearAuth();
            Mesa mesa = crearMesa(1L, EstadoMesa.EN_PREPARACION);
            Comanda b = comanda(10L, EstadoComanda.BORRADOR, EstacionComanda.COCINA, mesa.getVisita());

            when(mesaValidador.validarOwnership(1L, auth)).thenReturn(mesa);
            when(comandaRepository.findByVisita_VisitaIdAndComandaEstado(1L, EstadoComanda.BORRADOR))
                    .thenReturn(List.of(b));

            service.cancelarFormulario(1L, auth);

            verify(notificacionWsPublisher).publicarVisitaActualizada(eq(1L), any());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helpers privados estáticos
    // ═══════════════════════════════════════════════════════════════════════════

    private static Producto producto(Long id, CategoriaProducto categoria, boolean menuEspecial) {
        return Producto.builder()
                .productoId(id)
                .productoNombre("Producto " + id)
                .productoCategoria(categoria)
                .menuEspecial(menuEspecial)
                .productoPrecio(new BigDecimal("15.00"))
                .build();
    }

    private static Visita crearVisita(Long visitaId) {
        Visita v = new Visita();
        v.setVisitaId(visitaId);
        return v;
    }

    private static Mesa crearMesa(Long visitaId, EstadoMesa estado) {
        Visita visita = crearVisita(visitaId);
        Mesa mesa = new Mesa();
        mesa.setVisitaId(visitaId);
        mesa.setVisita(visita);
        mesa.setMesaEstado(estado);
        mesa.setMesaIdentificador("T-0" + visitaId);
        return mesa;
    }

    private static Comanda comanda(Long id, EstadoComanda estado, EstacionComanda estacion, Visita visita) {
        return Comanda.builder()
                .comandaId(id)
                .comandaEstado(estado)
                .comandaEstacion(estacion)
                .visita(visita)
                .build();
    }

    private static ComandaItem item(Long id, Comanda comanda, Producto producto,
                                    int cantidad, String descripcion, String menuGrupo) {
        return ComandaItem.builder()
                .comandaItemId(id)
                .comanda(comanda)
                .producto(producto)
                .comandaItemCantidad(cantidad)
                .comandaItemPrecio(producto.getProductoPrecio())
                .comandaItemDescripcion(descripcion)
                .comandaItemMenuGrupo(menuGrupo)
                .build();
    }

    private static AgregarItemRequest agregarReq(Long visitaId, Long productoId, int cantidad, String descripcion) {
        AgregarItemRequest req = new AgregarItemRequest();
        req.setVisitaId(visitaId);
        req.setProductoId(productoId);
        req.setCantidad(cantidad);
        req.setDescripcion(descripcion);
        return req;
    }

    private static ModificarItemRequest modificarReq(Integer cantidad, String descripcion) {
        ModificarItemRequest req = new ModificarItemRequest();
        req.setCantidad(cantidad);
        req.setDescripcion(descripcion);
        return req;
    }

    private static NotasRequest notasReq(String notas) {
        NotasRequest req = new NotasRequest();
        req.setNotas(notas);
        return req;
    }

    private static Authentication crearAuth() {
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn("mesero@altoro.com");
        return auth;
    }
}
