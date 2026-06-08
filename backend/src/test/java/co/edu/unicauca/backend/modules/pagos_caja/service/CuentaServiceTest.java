package co.edu.unicauca.backend.modules.pagos_caja.service;

import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.VisitaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.Visita;
import co.edu.unicauca.backend.modules.pagos_caja.dto.response.CuentaPreliminarResponse;
import co.edu.unicauca.backend.modules.pagos_caja.mapper.CuentaMapper;
import co.edu.unicauca.backend.modules.pagos_caja.repository.AbonoRepository;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CuentaService")
class CuentaServiceTest {

    @Mock VisitaRepository visitaRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock AbonoRepository abonoRepository;
    @Mock MesaRepository mesaRepository;
    @Mock CuentaMapper cuentaMapper;
    @InjectMocks CuentaService cuentaService;

    @Test
    @DisplayName("visita inexistente → ResourceNotFoundException")
    void visitaInexistente_lanza() {
        when(visitaRepository.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> cuentaService.obtenerCuenta(5L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("visita con reserva → carga abonos y delega al mapper")
    void conReserva_cargaAbonos() {
        Reserva reserva = Reserva.builder().reservaId(2L).build();
        Visita visita = Visita.builder().visitaId(5L).reserva(reserva).build();
        when(visitaRepository.findById(5L)).thenReturn(Optional.of(visita));
        when(comandaRepository.findAllItemsActivosByVisita(5L)).thenReturn(List.of());
        when(abonoRepository.findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(2L)).thenReturn(List.of());
        when(mesaRepository.findById(5L)).thenReturn(Optional.empty());
        when(cuentaMapper.toCuenta(any(), any(), any(), any()))
                .thenReturn(CuentaPreliminarResponse.builder().visitaId(5L).build());

        CuentaPreliminarResponse r = cuentaService.obtenerCuenta(5L);

        assertThat(r.getVisitaId()).isEqualTo(5L);
        verify(abonoRepository).findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(2L);
        verify(cuentaMapper).toCuenta(eq(visita), anyList(), anyList(), eq(Optional.empty()));
    }

    @Test
    @DisplayName("visita sin reserva → no consulta abonos")
    void sinReserva_noConsultaAbonos() {
        Visita visita = Visita.builder().visitaId(5L).build();
        when(visitaRepository.findById(5L)).thenReturn(Optional.of(visita));
        when(comandaRepository.findAllItemsActivosByVisita(5L)).thenReturn(List.of());
        when(mesaRepository.findById(5L)).thenReturn(Optional.empty());
        when(cuentaMapper.toCuenta(any(), any(), any(), any()))
                .thenReturn(CuentaPreliminarResponse.builder().visitaId(5L).build());

        cuentaService.obtenerCuenta(5L);

        verify(abonoRepository, never()).findByReserva_ReservaIdOrderByAbonoFechaHoraAsc(anyLong());
    }
}
