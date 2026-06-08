package co.edu.unicauca.backend.modules.pagos_caja.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Comanda;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.ComandaItem;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Mesa;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.notificaciones.dto.ws.CuentaCerradaWsMessage;
import co.edu.unicauca.backend.modules.notificaciones.service.MesaWsPublisher;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionWsPublisher;
import co.edu.unicauca.backend.modules.pagos_caja.dto.request.CerrarCuentaRequest;
import co.edu.unicauca.backend.modules.pagos_caja.entity.Venta;
import co.edu.unicauca.backend.modules.pagos_caja.repository.VentaRepository;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.enums.EstadoComanda;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VentaService")
class VentaServiceTest {

    @Mock VisitaRepository visitaRepository;
    @Mock VentaRepository ventaRepository;
    @Mock EmpleadoRepository empleadoRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock MesaRepository mesaRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock NotificacionWsPublisher wsPublisher;
    @Mock MesaWsPublisher mesaWsPublisher;
    @InjectMocks VentaService ventaService;

    private CerrarCuentaRequest req(BigDecimal descuento) {
        return CerrarCuentaRequest.builder()
                .emailCajero("cajero@altoro.com").visitaId(5L)
                .descuento(descuento).metodo(MetodoPago.EFECTIVO).build();
    }

    @BeforeEach
    void base() {
        when(empleadoRepository.findByUsuario_UsuarioEmail("cajero@altoro.com"))
                .thenReturn(Optional.of(Empleado.builder().build()));
        when(ventaRepository.findByVisita_VisitaId(5L)).thenReturn(Optional.empty());
        // Ítems activos suman 100.00 (precio 100 × cantidad 1); el de precio null (bebida de menú) aporta 0
        when(comandaRepository.findAllItemsActivosByVisita(5L)).thenReturn(List.of(
                ComandaItem.builder().comandaItemPrecio(new BigDecimal("100.00")).comandaItemCantidad(1).build(),
                ComandaItem.builder().comandaItemPrecio(null).comandaItemCantidad(2).build()));
        when(comandaRepository.findByVisita_VisitaId(5L)).thenReturn(List.of());
        when(mesaRepository.findById(5L)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("visita inexistente → ResourceNotFoundException")
    void visitaInexistente_lanza() {
        when(visitaRepository.findByIdForUpdate(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ventaService.cerrarCuenta(req(BigDecimal.ZERO), "cajero@altoro.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("cuenta ya cerrada → INVALID_STATE")
    void cuentaYaCerrada_lanza() {
        when(visitaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(Visita.builder().visitaId(5L).build()));
        when(ventaRepository.findByVisita_VisitaId(5L)).thenReturn(Optional.of(Venta.builder().build()));
        assertThatThrownBy(() -> ventaService.cerrarCuenta(req(BigDecimal.ZERO), "cajero@altoro.com"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("descuento mayor al total → BUSINESS_ERROR")
    void descuentoMayorTotal_lanza() {
        when(visitaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(Visita.builder().visitaId(5L).build()));
        assertThatThrownBy(() -> ventaService.cerrarCuenta(req(new BigDecimal("150.00")), "cajero@altoro.com"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("recompute con decoración: subtotal = ítems + decoración; total = subtotal − descuento")
    void recomputeConDecoracion() {
        Decoracion deco = Decoracion.builder().decoracionCostoAdicional(new BigDecimal("20.00")).build();
        Reserva reserva = Reserva.builder().reservaId(2L).decoracion(deco).build();
        Cliente cliente = Cliente.builder().usuarioId(7L).clientePuntos(0).clientePuntosAcumulados(0).build();
        Visita visita = Visita.builder().visitaId(5L).reserva(reserva).cliente(cliente).build();
        when(visitaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(visita));
        when(clienteRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(cliente));

        ventaService.cerrarCuenta(req(new BigDecimal("30.00")), "cajero@altoro.com");

        ArgumentCaptor<Venta> cap = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(cap.capture());
        assertThat(cap.getValue().getVentaSubtotal()).isEqualByComparingTo("120.00");
        assertThat(cap.getValue().getVentaDescuento()).isEqualByComparingTo("30.00");
        assertThat(cap.getValue().getVentaTotal()).isEqualByComparingTo("90.00");
    }

    @Test
    @DisplayName("cliente presente → +1 punto con lock y visita cerrada")
    void clientePresente_incrementaPuntos() {
        Cliente cliente = Cliente.builder().usuarioId(7L).clientePuntos(4).clientePuntosAcumulados(10).build();
        Visita visita = Visita.builder().visitaId(5L).cliente(cliente).build();
        when(visitaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(visita));
        when(clienteRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(cliente));

        ventaService.cerrarCuenta(req(BigDecimal.ZERO), "cajero@altoro.com");

        assertThat(cliente.getClientePuntos()).isEqualTo(5);
        assertThat(cliente.getClientePuntosAcumulados()).isEqualTo(11);
        assertThat(visita.getVisitaFechaHoraFin()).isNotNull();
        verify(clienteRepository).findByIdForUpdate(7L);
    }

    @Test
    @DisplayName("invitado (cliente null) → no incrementa puntos, puntosActuales null en WS")
    void invitado_noIncrementaPuntos() {
        Visita visita = Visita.builder().visitaId(5L).build();
        when(visitaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(visita));

        ventaService.cerrarCuenta(req(BigDecimal.ZERO), "cajero@altoro.com");

        verify(clienteRepository, never()).findByIdForUpdate(anyLong());
        ArgumentCaptor<CuentaCerradaWsMessage> cap = ArgumentCaptor.forClass(CuentaCerradaWsMessage.class);
        verify(wsPublisher).publicarCuentaCerrada(eq(5L), cap.capture());
        assertThat(cap.getValue().getPuntosActuales()).isNull();
    }

    @Test
    @DisplayName("mesa pasa a CERRADA y se publica al mapa")
    void cierraMesaYPublicaMapa() {
        Visita visita = Visita.builder().visitaId(5L).build();
        Mesa mesa = Mesa.builder().visitaId(5L).mesaEstado(EstadoMesa.ATENDIDA).build();
        when(visitaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(visita));
        when(mesaRepository.findById(5L)).thenReturn(Optional.of(mesa));

        ventaService.cerrarCuenta(req(BigDecimal.ZERO), "cajero@altoro.com");

        assertThat(mesa.getMesaEstado()).isEqualTo(EstadoMesa.CERRADA);
        verify(mesaRepository).save(mesa);
        verify(mesaWsPublisher).publicarActualizacionMesa(5L, MesaWsPublisher.TipoEventoMesa.CERRAR);
    }

    @Test
    @DisplayName("comandas no completadas pasan a COMPLETADO; las ya completadas no se re-guardan")
    void completaComandasPendientes() {
        Visita visita = Visita.builder().visitaId(5L).build();
        Comanda pendiente = Comanda.builder().comandaId(1L).comandaEstado(EstadoComanda.LISTO).build();
        Comanda completada = Comanda.builder().comandaId(2L).comandaEstado(EstadoComanda.COMPLETADO).build();
        when(visitaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(visita));
        when(comandaRepository.findByVisita_VisitaId(5L)).thenReturn(List.of(pendiente, completada));

        ventaService.cerrarCuenta(req(BigDecimal.ZERO), "cajero@altoro.com");

        assertThat(pendiente.getComandaEstado()).isEqualTo(EstadoComanda.COMPLETADO);
        verify(comandaRepository).save(pendiente);
        verify(comandaRepository, never()).save(completada);
    }

    @Test
    @DisplayName("descuento null → asume 0")
    void descuentoNull_asumeCero() {
        Visita visita = Visita.builder().visitaId(5L).build();
        when(visitaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(visita));

        ventaService.cerrarCuenta(req(null), "cajero@altoro.com");

        ArgumentCaptor<Venta> cap = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(cap.capture());
        assertThat(cap.getValue().getVentaDescuento()).isEqualByComparingTo("0.00");
        assertThat(cap.getValue().getVentaTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("reserva sin decoración → subtotal = solo ítems")
    void reservaSinDecoracion_subtotalSinDeco() {
        Reserva reserva = Reserva.builder().reservaId(2L).build(); // decoración null
        Visita visita = Visita.builder().visitaId(5L).reserva(reserva).build();
        when(visitaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(visita));

        ventaService.cerrarCuenta(req(BigDecimal.ZERO), "cajero@altoro.com");

        ArgumentCaptor<Venta> cap = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(cap.capture());
        assertThat(cap.getValue().getVentaSubtotal()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("reserva con decoración sin costo → subtotal = solo ítems")
    void reservaDecoracionSinCosto_subtotalSinDeco() {
        Decoracion deco = Decoracion.builder().decoracionCostoAdicional(null).build();
        Reserva reserva = Reserva.builder().reservaId(2L).decoracion(deco).build();
        Visita visita = Visita.builder().visitaId(5L).reserva(reserva).build();
        when(visitaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(visita));

        ventaService.cerrarCuenta(req(BigDecimal.ZERO), "cajero@altoro.com");

        ArgumentCaptor<Venta> cap = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(cap.capture());
        assertThat(cap.getValue().getVentaSubtotal()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("cajero inexistente → ResourceNotFoundException")
    void cajeroInexistente_lanza() {
        Visita visita = Visita.builder().visitaId(5L).build();
        when(visitaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(visita));
        when(empleadoRepository.findByUsuario_UsuarioEmail("cajero@altoro.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> ventaService.cerrarCuenta(req(BigDecimal.ZERO), "cajero@altoro.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("cliente de la visita no recuperable bajo lock → ResourceNotFoundException")
    void clienteNoLockeable_lanza() {
        Cliente cliente = Cliente.builder().usuarioId(7L).clientePuntos(0).clientePuntosAcumulados(0).build();
        Visita visita = Visita.builder().visitaId(5L).cliente(cliente).build();
        when(visitaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(visita));
        when(clienteRepository.findByIdForUpdate(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ventaService.cerrarCuenta(req(BigDecimal.ZERO), "cajero@altoro.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
