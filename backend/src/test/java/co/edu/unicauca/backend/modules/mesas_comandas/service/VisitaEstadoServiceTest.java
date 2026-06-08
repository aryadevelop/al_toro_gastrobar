package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.inventario.entity.Producto;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.EstadoVisitaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.VisitaEstadoMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.shared.enums.CategoriaProducto;
import co.edu.unicauca.backend.shared.enums.EstacionComanda;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoNotificacion;
import co.edu.unicauca.backend.shared.enums.TipoNotificacion;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VisitaEstadoService")
class VisitaEstadoServiceTest {

    @Mock VisitaRepository visitaRepository;
    @Mock MesaRepository mesaRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock NotificacionRepository notificacionRepository;
    @Mock co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository abonoRepository;

    VisitaEstadoMapper visitaEstadoMapper;
    VisitaEstadoService visitaEstadoService;

    private static final String EMAIL = "cliente@test.com";
    private static final Long VISITA_ID = 10L;

    @BeforeEach
    void setUp() {
        visitaEstadoMapper = new VisitaEstadoMapper();
        visitaEstadoService = new VisitaEstadoService(
                visitaRepository,
                mesaRepository,
                comandaRepository,
                notificacionRepository,
                abonoRepository,
                visitaEstadoMapper
        );
    }

    private Visita visitaActiva() {
        return Visita.builder().visitaId(VISITA_ID).build();
    }

    private Comanda comanda(Long id, EstadoComanda estado) {
        return Comanda.builder()
                .comandaId(id)
                .comandaEstado(estado)
                .comandaEstacion(EstacionComanda.COCINA)
                .build();
    }

    private ComandaItem item(Long id, Comanda comanda, String nombre, int qty, BigDecimal precio) {
        Producto producto = Producto.builder()
                .productoNombre(nombre)
                .productoCategoria(nombre.toLowerCase().contains("limonada") || nombre.toLowerCase().contains("bebida")
                        ? CategoriaProducto.BEBIDA
                        : CategoriaProducto.PLATO)
                .build();
        return ComandaItem.builder()
                .comandaItemId(id)
                .comanda(comanda)
                .producto(producto)
                .comandaItemCantidad(qty)
                .comandaItemPrecio(precio)
                .build();
    }

    @Nested
    @DisplayName("obtenerEstadoVisitaActiva")
    class ObtenerEstadoVisitaActiva {

        @Test
        @DisplayName("retorna estado con ítems y total cuando la visita está activa")
        void retornaEstadoConItems() {
            Visita visita = visitaActiva();
            Mesa mesa = Mesa.builder().visitaId(VISITA_ID).mesaIdentificador("T-01").build();
            Comanda c1 = comanda(1L, EstadoComanda.EN_PREPARACION);
            ComandaItem i1 = item(100L, c1, "Bandeja", 2, new BigDecimal("18000"));

            when(visitaRepository.findActiveByClienteEmail(EMAIL)).thenReturn(List.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.of(mesa));
            when(comandaRepository.findAllItemsActivosByVisita(VISITA_ID)).thenReturn(List.of(i1));
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.empty());

            EstadoVisitaResponse res = visitaEstadoService.obtenerEstadoVisitaActiva(EMAIL);

            assertThat(res.getVisitaId()).isEqualTo(VISITA_ID);
            assertThat(res.getMesaIdentificador()).isEqualTo("T-01");
            assertThat(res.isVisitaCerrada()).isFalse();
            assertThat(res.getItems()).hasSize(1);
            assertThat(res.getItems().get(0).getEstadoItem()).isEqualTo("En preparación");
            assertThat(res.getItems().get(0).getSubtotal()).isEqualByComparingTo("36000");
            assertThat(res.getTotalPreorden()).isEqualByComparingTo("36000");
            assertThat(res.isAsistenciaSolicitada()).isFalse();
        }

        @Test
        @DisplayName("estadoItem es 'Servido' cuando la comanda está en LISTO")
        void estadoItemServidoCuandoListo() {
            Visita visita = visitaActiva();
            Comanda c1 = comanda(2L, EstadoComanda.LISTO);
            ComandaItem i1 = item(101L, c1, "Limonada", 1, new BigDecimal("8000"));

            when(visitaRepository.findActiveByClienteEmail(EMAIL)).thenReturn(List.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(comandaRepository.findAllItemsActivosByVisita(VISITA_ID)).thenReturn(List.of(i1));
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.empty());

            EstadoVisitaResponse res = visitaEstadoService.obtenerEstadoVisitaActiva(EMAIL);

            assertThat(res.getItems().get(0).getEstadoItem()).isEqualTo("Servido");
        }

        @Test
        @DisplayName("asistenciaSolicitada es true cuando hay notificación ATENCION ACTIVA")
        void asistenciaSolicitadaCuandoNotificacionActiva() {
            Visita visita = visitaActiva();
            Notificacion notif = Notificacion.builder()
                    .notificacionId(99L)
                    .notificacionTipo(TipoNotificacion.ATENCION)
                    .notificacionEstado(EstadoNotificacion.ACTIVA)
                    .build();

            when(visitaRepository.findActiveByClienteEmail(EMAIL)).thenReturn(List.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(comandaRepository.findAllItemsActivosByVisita(VISITA_ID)).thenReturn(List.of());
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.of(notif));

            EstadoVisitaResponse res = visitaEstadoService.obtenerEstadoVisitaActiva(EMAIL);

            assertThat(res.isAsistenciaSolicitada()).isTrue();
            assertThat(res.getNotificacionAsistenciaId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("lanza BusinessException cuando el cliente no tiene visita activa")
        void lanzaExcepcionSinVisitaActiva() {
            when(visitaRepository.findActiveByClienteEmail(EMAIL)).thenReturn(List.of());

            assertThatThrownBy(() -> visitaEstadoService.obtenerEstadoVisitaActiva(EMAIL))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("total es ZERO cuando no hay ítems en la visita")
        void totalCeroCuandoSinItems() {
            Visita visita = visitaActiva();

            when(visitaRepository.findActiveByClienteEmail(EMAIL)).thenReturn(List.of(visita));
            when(mesaRepository.findByVisita_VisitaId(VISITA_ID)).thenReturn(Optional.empty());
            when(comandaRepository.findAllItemsActivosByVisita(VISITA_ID)).thenReturn(List.of());
            when(notificacionRepository.findFirstByMesa_VisitaIdAndNotificacionTipoAndNotificacionEstado(
                    VISITA_ID, TipoNotificacion.ATENCION, EstadoNotificacion.ACTIVA))
                    .thenReturn(Optional.empty());

            EstadoVisitaResponse res = visitaEstadoService.obtenerEstadoVisitaActiva(EMAIL);

            assertThat(res.getTotalPreorden()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
