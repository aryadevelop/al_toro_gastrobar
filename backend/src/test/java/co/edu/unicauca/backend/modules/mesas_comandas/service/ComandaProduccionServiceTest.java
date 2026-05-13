package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ItemDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.TableroProduccionResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.ComandaProduccionMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaItemRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComandaProduccionService")
class ComandaProduccionServiceTest {

    @Mock ComandaRepository comandaRepository;
    @Mock ComandaItemRepository comandaItemRepository;
    @Mock MesaRepository mesaRepository;
    @Mock EstacionResolver estacionResolver;
    @Mock Authentication auth;

    ComandaProduccionService service;

    @BeforeEach
    void setUp() {
        ComandaProduccionMapper realMapper = new ComandaProduccionMapper();
        service = new ComandaProduccionService(
                comandaRepository, comandaItemRepository, mesaRepository,
                estacionResolver, realMapper);
    }

    private Visita visita(Long id) {
        return Visita.builder().visitaId(id).build();
    }

    private Mesa mesa(Long visitaId, String identificador, String meseroNombre) {
        return Mesa.builder()
                .visitaId(visitaId)
                .mesaIdentificador(identificador)
                .mesero(Empleado.builder().empleadoNombre(meseroNombre).build())
                .build();
    }

    private Comanda comanda(Long id, Long visitaId, EstacionComanda estacion,
                            EstadoComanda estado, LocalDateTime inicio, LocalDateTime listo,
                            LocalDateTime createdAt) {
        Comanda c = Comanda.builder()
                .comandaId(id)
                .visita(visita(visitaId))
                .comandaEstacion(estacion)
                .comandaEstado(estado)
                .comandaFechaHoraInicio(inicio)
                .comandaFechaHoraListo(listo)
                .build();
        c.setCreatedAt(createdAt);
        return c;
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("obtenerTableroProduccion")
    class TableroTests {

        @Test
        @DisplayName("Cocinero: tres columnas pobladas con orden cronológico ascendente")
        void cocinero_treColumnas() {
            when(estacionResolver.resolverEstaciones(auth))
                    .thenReturn(Set.of(EstacionComanda.COCINA));

            LocalDateTime t = LocalDateTime.of(2026, 5, 12, 18, 0);
            Comanda p1 = comanda(1L, 11L, EstacionComanda.COCINA, EstadoComanda.PENDIENTE, t.plusMinutes(1), null, t);
            Comanda p2 = comanda(2L, 12L, EstacionComanda.COCINA, EstadoComanda.PENDIENTE, t.plusMinutes(5), null, t);
            Comanda e1 = comanda(3L, 13L, EstacionComanda.COCINA, EstadoComanda.EN_PREPARACION, t.plusMinutes(2), null, t);
            Comanda l1 = comanda(4L, 14L, EstacionComanda.COCINA, EstadoComanda.LISTO, t.plusMinutes(3), t.plusMinutes(20), t);

            when(comandaRepository.findByEstacionAndEstadoIn(eq(EstacionComanda.COCINA), anySet()))
                    .thenReturn(List.of(p2, p1, e1, l1));

            when(mesaRepository.findByVisita_VisitaIdIn(any())).thenReturn(List.of(
                    mesa(11L, "T-01", "Ana"),
                    mesa(12L, "T-02", "Beto"),
                    mesa(13L, "T-03", "Cris"),
                    mesa(14L, "T-04", "Dany")));
            when(comandaItemRepository.sumCantidadByComandaIdIn(any()))
                    .thenReturn(List.of(
                            new Object[]{1L, 2L},
                            new Object[]{2L, 3L},
                            new Object[]{3L, 4L},
                            new Object[]{4L, 5L}));

            TableroProduccionResponse t1 = service.obtenerTableroProduccion(auth);

            assertThat(t1.getEstaciones()).containsExactly("COCINA");
            // Pendientes ordenadas por fechaHoraInicio ascendente: p1 (min+1) antes que p2 (min+5)
            assertThat(t1.getPendientes()).extracting(ComandaProduccionResumenResponse::getComandaId)
                    .containsExactly(1L, 2L);
            assertThat(t1.getEnPreparacion()).extracting(ComandaProduccionResumenResponse::getComandaId)
                    .containsExactly(3L);
            assertThat(t1.getListos()).extracting(ComandaProduccionResumenResponse::getComandaId)
                    .containsExactly(4L);
            assertThat(t1.getPendientes().get(0).getMesaIdentificador()).isEqualTo("T-01");
            assertThat(t1.getPendientes().get(0).getMeseroNombre()).isEqualTo("Ana");
            assertThat(t1.getPendientes().get(0).getTotalItems()).isEqualTo(2);
        }

        @Test
        @DisplayName("Sin comandas: tres columnas vacías y estaciones del usuario presentes")
        void sinComandas() {
            when(estacionResolver.resolverEstaciones(auth))
                    .thenReturn(Set.of(EstacionComanda.BARRA));
            when(comandaRepository.findByEstacionAndEstadoIn(eq(EstacionComanda.BARRA), anySet()))
                    .thenReturn(List.of());

            TableroProduccionResponse r = service.obtenerTableroProduccion(auth);

            assertThat(r.getEstaciones()).containsExactly("BARRA");
            assertThat(r.getPendientes()).isEmpty();
            assertThat(r.getEnPreparacion()).isEmpty();
            assertThat(r.getListos()).isEmpty();
        }

        @Test
        @DisplayName("Usuario con ambas estaciones: tablero combinado")
        void ambasEstaciones_combinado() {
            when(estacionResolver.resolverEstaciones(auth))
                    .thenReturn(Set.of(EstacionComanda.COCINA, EstacionComanda.BARRA));

            LocalDateTime t = LocalDateTime.of(2026, 5, 12, 18, 0);
            Comanda cocina = comanda(1L, 11L, EstacionComanda.COCINA, EstadoComanda.PENDIENTE, t, null, t);
            Comanda barra = comanda(2L, 12L, EstacionComanda.BARRA, EstadoComanda.PENDIENTE, t.plusMinutes(1), null, t);

            when(comandaRepository.findByEstacionAndEstadoIn(eq(EstacionComanda.COCINA), anySet()))
                    .thenReturn(List.of(cocina));
            when(comandaRepository.findByEstacionAndEstadoIn(eq(EstacionComanda.BARRA), anySet()))
                    .thenReturn(List.of(barra));
            when(mesaRepository.findByVisita_VisitaIdIn(any())).thenReturn(List.of(
                    mesa(11L, "T-01", "Ana"),
                    mesa(12L, "T-02", "Beto")));
            when(comandaItemRepository.sumCantidadByComandaIdIn(any()))
                    .thenReturn(List.of(new Object[]{1L, 1L}, new Object[]{2L, 1L}));

            TableroProduccionResponse r = service.obtenerTableroProduccion(auth);

            assertThat(r.getEstaciones()).containsExactly("BARRA", "COCINA");
            assertThat(r.getPendientes()).hasSize(2)
                    .extracting(ComandaProduccionResumenResponse::getEstacion)
                    .containsExactlyInAnyOrder("COCINA", "BARRA");
        }

        @Test
        @DisplayName("Sin rol de producción: BusinessException 403 propagada")
        void sinRol_excepcionPropagada() {
            when(estacionResolver.resolverEstaciones(auth))
                    .thenThrow(new BusinessException(
                            co.edu.unicauca.backend.shared.exception.ErrorCode.ACCESS_DENIED,
                            "sin rol", HttpStatus.FORBIDDEN));

            assertThatThrownBy(() -> service.obtenerTableroProduccion(auth))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("obtenerDetalleComanda")
    class DetalleTests {

        private void mockProduccion() {
            lenient().when(estacionResolver.resolverEstaciones(auth))
                    .thenReturn(Set.of(EstacionComanda.COCINA));
        }

        @Test
        @DisplayName("Comanda visible: devuelve detalle con ítems agrupados")
        void visible_devuelveDetalle() {
            mockProduccion();
            LocalDateTime t = LocalDateTime.of(2026, 5, 12, 18, 0);
            Comanda c = comanda(50L, 7L, EstacionComanda.COCINA, EstadoComanda.PENDIENTE, t, null, t);

            when(comandaRepository.findById(50L)).thenReturn(Optional.of(c));
            when(mesaRepository.findByVisita_VisitaId(7L))
                    .thenReturn(Optional.of(mesa(7L, "T-05", "Eva")));

            Producto plato = Producto.builder()
                    .productoId(1L).productoNombre("Arroz")
                    .productoCategoria(CategoriaProducto.PLATO).build();
            ComandaItem item = ComandaItem.builder()
                    .comandaItemId(99L).producto(plato)
                    .comandaItemCantidad(2).modificaciones(new ArrayList<>())
                    .build();
            when(comandaItemRepository.findByComanda_ComandaIdOrderByProductoNombreAsc(50L))
                    .thenReturn(List.of(item));

            ComandaProduccionDetalleResponse d = service.obtenerDetalleComanda(50L, auth);

            assertThat(d.getComandaId()).isEqualTo(50L);
            assertThat(d.getMesaIdentificador()).isEqualTo("T-05");
            assertThat(d.getMeseroNombre()).isEqualTo("Eva");
            assertThat(d.getPlatos()).hasSize(1)
                    .extracting(ItemDetalleResponse::getProductoNombre)
                    .containsExactly("Arroz");
        }

        @Test
        @DisplayName("Comanda inexistente: 404 ENT-001")
        void inexistente_404() {
            mockProduccion();
            when(comandaRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.obtenerDetalleComanda(99L, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(be.getCode()).isEqualTo("ENT-001");
                    });
        }

        @Test
        @DisplayName("Comanda de otra estación: 403 AUTH-002")
        void otraEstacion_403() {
            mockProduccion();
            LocalDateTime t = LocalDateTime.of(2026, 5, 12, 18, 0);
            Comanda c = comanda(50L, 7L, EstacionComanda.BARRA, EstadoComanda.PENDIENTE, t, null, t);
            when(comandaRepository.findById(50L)).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> service.obtenerDetalleComanda(50L, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(be.getCode()).isEqualTo("AUTH-002");
                    });
        }

        @Test
        @DisplayName("Comanda en estado no visible (COMPLETADO): 404 ENT-001")
        void estadoNoVisible_404() {
            mockProduccion();
            LocalDateTime t = LocalDateTime.of(2026, 5, 12, 18, 0);
            Comanda c = comanda(50L, 7L, EstacionComanda.COCINA, EstadoComanda.COMPLETADO, t, t, t);
            when(comandaRepository.findById(50L)).thenReturn(Optional.of(c));

            assertThatThrownBy(() -> service.obtenerDetalleComanda(50L, auth))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getStatus())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }
}
