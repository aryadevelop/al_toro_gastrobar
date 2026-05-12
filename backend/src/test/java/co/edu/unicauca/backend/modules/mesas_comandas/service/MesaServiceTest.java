package co.edu.unicauca.backend.modules.mesas_comandas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.*;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.*;
import co.edu.unicauca.backend.modules.mesas_comandas.mapper.MesaMapper;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ComandaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.MesaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.notificaciones.repository.NotificacionRepository;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.shared.enums.EstadoMesa;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MesaService")
class MesaServiceTest {

    @Mock MesaRepository mesaRepository;
    @Mock ZonaRepository zonaRepository;
    @Mock ComandaRepository comandaRepository;
    @Mock NotificacionRepository notificacionRepository;
    @Mock MesaMapper mesaMapper;
    @Mock MesaValidador mesaValidador;

    @InjectMocks MesaService mesaService;

    // Builders de datos de prueba

    private Zona crearZona(Long id, String nombre) {
        Zona zona = new Zona();
        zona.setZonaId(id);
        zona.setZonaNombre(nombre);
        return zona;
    }

    private Mesa crearMesa(Long visitaId, String identificador, Zona zona, Empleado mesero) {
        Mesa mesa = new Mesa();
        mesa.setVisitaId(visitaId);
        mesa.setMesaIdentificador(identificador);
        mesa.setZona(zona);
        mesa.setMesero(mesero);
        mesa.setMesaEstado(EstadoMesa.EN_PREPARACION);

        Visita visita = new Visita();
        visita.setVisitaId(visitaId);
        visita.setVisitaFechaHoraInicio(LocalDateTime.now().minusHours(1));
        mesa.setVisita(visita);

        return mesa;
    }

    private Empleado crearEmpleado(String email, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setUsuarioEmail(email);

        Empleado empleado = new Empleado();
        empleado.setUsuario(usuario);
        empleado.setEmpleadoNombre(nombre);

        return empleado;
    }

    @SuppressWarnings("unchecked")
    private Authentication crearAuthentication(String email, String... roles) {
        List<GrantedAuthority> authorities = List.of(roles).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getName()).thenReturn(email);
        lenient().when(auth.getAuthorities()).thenReturn((Collection) authorities);
        
        return auth;
    }

    @Nested
    @DisplayName("obtenerMapaMesas")
    class ObtenerMapaMesas {

        @Test
        @DisplayName("con zonaId null retorna todas las zonas incluyendo vacías")
        void conZonaIdNull_retornaTodasLasZonas() {
            // Arrange
            Zona zona1 = crearZona(1L, "Terraza");
            Zona zona2 = crearZona(2L, "Interior");
            Empleado mesero = crearEmpleado("mesero1@altoro.com", "Juan Pérez");
            Mesa mesa1 = crearMesa(1L, "T1", zona1, mesero);

            when(zonaRepository.findAll()).thenReturn(List.of(zona1, zona2));
            when(mesaRepository.findAllMesasActivas()).thenReturn(List.of(mesa1));
            when(notificacionRepository.findNotificacionesActivasByMesa(anyLong())).thenReturn(List.of());
            when(mesaRepository.existeComandaBorradorEnMesa(anyLong())).thenReturn(false);

            MesaMapaResponse mesaDto = MesaMapaResponse.builder().build();
            when(mesaMapper.toMesaMapaResponse(any(), anyList(), anyBoolean(), anyString())).thenReturn(mesaDto);

            // Act
            MapaMesasResponse resultado = mesaService.obtenerMapaMesas(null, "mesero1@altoro.com");

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getZonas()).hasSize(2);

            ZonaMesasResponse zonaResponse1 = resultado.getZonas().get(0);
            assertThat(zonaResponse1.getZonaId()).isEqualTo(1L);
            assertThat(zonaResponse1.getZonaNombre()).isEqualTo("Terraza");
            assertThat(zonaResponse1.getCantidadMesasActivas()).isEqualTo(1);
            assertThat(zonaResponse1.getMesas()).hasSize(1);

            ZonaMesasResponse zonaResponse2 = resultado.getZonas().get(1);
            assertThat(zonaResponse2.getZonaId()).isEqualTo(2L);
            assertThat(zonaResponse2.getZonaNombre()).isEqualTo("Interior");
            assertThat(zonaResponse2.getCantidadMesasActivas()).isZero();
            assertThat(zonaResponse2.getMesas()).isEmpty();

            verify(zonaRepository).findAll();
            verify(mesaRepository).findAllMesasActivas();
            verify(notificacionRepository).findNotificacionesActivasByMesa(1L);
            verify(mesaRepository).existeComandaBorradorEnMesa(1L);
        }

        @Test
        @DisplayName("con zonaId específico retorna solo esa zona")
        void conZonaIdEspecifico_retornaSoloEsaZona() {
            // Arrange
            Zona zona = crearZona(1L, "Terraza");
            Empleado mesero = crearEmpleado("mesero1@altoro.com", "Juan Pérez");
            Mesa mesa1 = crearMesa(1L, "T1", zona, mesero);
            Mesa mesa2 = crearMesa(2L, "T2", zona, mesero);

            when(zonaRepository.findById(1L)).thenReturn(Optional.of(zona));
            when(mesaRepository.findMesasActivasByZona(1L)).thenReturn(List.of(mesa1, mesa2));
            when(notificacionRepository.findNotificacionesActivasByMesa(anyLong())).thenReturn(List.of());
            when(mesaRepository.existeComandaBorradorEnMesa(anyLong())).thenReturn(false);

            MesaMapaResponse mesaDto = MesaMapaResponse.builder().build();
            when(mesaMapper.toMesaMapaResponse(any(), anyList(), anyBoolean(), anyString())).thenReturn(mesaDto);

            // Act
            MapaMesasResponse resultado = mesaService.obtenerMapaMesas(1L, "mesero1@altoro.com");

            // Assert
            assertThat(resultado).isNotNull();
            assertThat(resultado.getZonas()).hasSize(1);

            ZonaMesasResponse zonaResponse = resultado.getZonas().get(0);
            assertThat(zonaResponse.getZonaId()).isEqualTo(1L);
            assertThat(zonaResponse.getCantidadMesasActivas()).isEqualTo(2);
            assertThat(zonaResponse.getMesas()).hasSize(2);

            verify(zonaRepository).findById(1L);
            verify(mesaRepository).findMesasActivasByZona(1L);
            verify(notificacionRepository, times(2)).findNotificacionesActivasByMesa(anyLong());
        }

        @Test
        @DisplayName("con zonaId inexistente lanza BusinessException")
        void conZonaIdInexistente_lanzaBusinessException() {
            // Arrange
            when(zonaRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> mesaService.obtenerMapaMesas(999L, "mesero1@altoro.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", ErrorCode.ENTITY_NOT_FOUND.getCode())
                    .hasMessageContaining("Zona no encontrada");

            verify(zonaRepository).findById(999L);
            verifyNoInteractions(mesaRepository);
        }

        @Test
        @DisplayName("obtiene notificaciones y flag borrador para cada mesa")
        void obtieneNotificacionesYFlagBorradorParaCadaMesa() {
            // Arrange
            Zona zona = crearZona(1L, "Terraza");
            Empleado mesero = crearEmpleado("mesero1@altoro.com", "Juan Pérez");
            Mesa mesa = crearMesa(1L, "T1", zona, mesero);

            Notificacion notif = new Notificacion();

            when(zonaRepository.findAll()).thenReturn(List.of(zona));
            when(mesaRepository.findAllMesasActivas()).thenReturn(List.of(mesa));
            when(notificacionRepository.findNotificacionesActivasByMesa(1L)).thenReturn(List.of(notif));
            when(mesaRepository.existeComandaBorradorEnMesa(1L)).thenReturn(true);

            MesaMapaResponse mesaDto = MesaMapaResponse.builder().build();
            when(mesaMapper.toMesaMapaResponse(any(), anyList(), anyBoolean(), anyString())).thenReturn(mesaDto);

            // Act
            mesaService.obtenerMapaMesas(null, "mesero1@altoro.com");

            // Assert
            ArgumentCaptor<List<Notificacion>> notifCaptor = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<Boolean> borradorCaptor = ArgumentCaptor.forClass(Boolean.class);
            ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);

            verify(mesaMapper).toMesaMapaResponse(
                    eq(mesa),
                    notifCaptor.capture(),
                    borradorCaptor.capture(),
                    emailCaptor.capture()
            );

            assertThat(notifCaptor.getValue()).hasSize(1);
            assertThat(borradorCaptor.getValue()).isTrue();
            assertThat(emailCaptor.getValue()).isEqualTo("mesero1@altoro.com");
        }
    }

    @Nested
    @DisplayName("obtenerDetalleMesa")
    class ObtenerDetalleMesa {

        @Test
        @DisplayName("con visitaId válido retorna detalle completo")
        void conVisitaIdValido_retornaDetalleCompleto() {
            // Arrange
            Zona zona = crearZona(1L, "Terraza");
            Empleado mesero = crearEmpleado("mesero1@altoro.com", "Juan Pérez");
            Mesa mesa = crearMesa(1L, "T1", zona, mesero);

            ComandaItem item = new ComandaItem();
            List<ComandaItem> items = List.of(item);

            ItemComandaEnProduccionResponse itemDto = ItemComandaEnProduccionResponse.builder().build();
            List<ItemComandaEnProduccionResponse> itemsDto = List.of(itemDto);

            MesaDetalleResponse expectedResponse = MesaDetalleResponse.builder().build();

            when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
            when(comandaRepository.findItemsEnProduccionByVisita(1L)).thenReturn(items);
            when(mesaMapper.agruparItemsEnProduccion(items)).thenReturn(itemsDto);
            when(mesaMapper.toMesaDetalleResponse(mesa, items, itemsDto)).thenReturn(expectedResponse);

            // Act
            MesaDetalleResponse resultado = mesaService.obtenerDetalleMesa(1L);

            // Assert
            assertThat(resultado).isEqualTo(expectedResponse);

            verify(mesaRepository).findById(1L);
            verify(comandaRepository).findItemsEnProduccionByVisita(1L);
            verify(mesaMapper).agruparItemsEnProduccion(items);
            verify(mesaMapper).toMesaDetalleResponse(mesa, items, itemsDto);
        }

        @Test
        @DisplayName("con visitaId válido sin items retorna detalle vacío")
        void conVisitaIdValidoSinItems_retornaDetalleVacio() {
            // Arrange
            Zona zona = crearZona(1L, "Terraza");
            Empleado mesero = crearEmpleado("mesero1@altoro.com", "Juan Pérez");
            Mesa mesa = crearMesa(1L, "T1", zona, mesero);

            List<ComandaItem> itemsVacios = List.of();
            List<ItemComandaEnProduccionResponse> itemsDtoVacios = List.of();

            MesaDetalleResponse expectedResponse = MesaDetalleResponse.builder().build();

            when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
            when(comandaRepository.findItemsEnProduccionByVisita(1L)).thenReturn(itemsVacios);
            when(mesaMapper.agruparItemsEnProduccion(itemsVacios)).thenReturn(itemsDtoVacios);
            when(mesaMapper.toMesaDetalleResponse(mesa, itemsVacios, itemsDtoVacios)).thenReturn(expectedResponse);

            // Act
            MesaDetalleResponse resultado = mesaService.obtenerDetalleMesa(1L);

            // Assert
            assertThat(resultado).isEqualTo(expectedResponse);
            verify(mesaMapper).agruparItemsEnProduccion(itemsVacios);
        }

        @Test
        @DisplayName("con visitaId inexistente lanza BusinessException")
        void conVisitaIdInexistente_lanzaBusinessException() {
            // Arrange
            when(mesaRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> mesaService.obtenerDetalleMesa(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", ErrorCode.ENTITY_NOT_FOUND.getCode())
                    .hasMessageContaining("Mesa no encontrada");

            verify(mesaRepository).findById(999L);
            verifyNoInteractions(comandaRepository);
            verifyNoInteractions(mesaMapper);
        }
    }

    @Nested
    @DisplayName("obtenerItemsProduccion")
    class ObtenerItemsProduccion {

        @Test
        @DisplayName("con visitaId válido retorna items en producción")
        void conVisitaIdValido_retornaItemsEnProduccion() {
            // Arrange
            Zona zona = crearZona(1L, "Terraza");
            Empleado mesero = crearEmpleado("mesero1@altoro.com", "Juan Pérez");
            Mesa mesa = crearMesa(1L, "T1", zona, mesero);

            ComandaItem item = new ComandaItem();
            List<ComandaItem> items = List.of(item);

            ItemComandaEnProduccionResponse itemDto = ItemComandaEnProduccionResponse.builder().build();
            List<ItemComandaEnProduccionResponse> itemsDto = List.of(itemDto);

            MesaItemsProduccionResponse expectedResponse = MesaItemsProduccionResponse.builder().build();

            Authentication auth = crearAuthentication("mesero1@altoro.com", "MESERO");

            when(mesaValidador.validarOwnership(eq(1L), any())).thenReturn(mesa);
            when(comandaRepository.findItemsEnProduccionByVisita(1L)).thenReturn(items);
            when(mesaMapper.agruparItemsEnProduccion(items)).thenReturn(itemsDto);
            when(mesaMapper.toMesaItemsProduccionResponse("T1", itemsDto)).thenReturn(expectedResponse);

            // Act
            MesaItemsProduccionResponse resultado = mesaService.obtenerItemsProduccion(1L, auth);

            // Assert
            assertThat(resultado).isEqualTo(expectedResponse);

            verify(mesaValidador).validarOwnership(eq(1L), any());
            verify(comandaRepository).findItemsEnProduccionByVisita(1L);
            verify(mesaMapper).agruparItemsEnProduccion(items);
            verify(mesaMapper).toMesaItemsProduccionResponse("T1", itemsDto);
        }

        @Test
        @DisplayName("con visitaId válido sin items retorna lista vacía")
        void conVisitaIdValidoSinItems_retornaListaVacia() {
            // Arrange
            Zona zona = crearZona(1L, "Terraza");
            Empleado mesero = crearEmpleado("mesero1@altoro.com", "Juan Pérez");
            Mesa mesa = crearMesa(1L, "T1", zona, mesero);

            List<ComandaItem> itemsVacios = List.of();
            List<ItemComandaEnProduccionResponse> itemsDtoVacios = List.of();

            MesaItemsProduccionResponse expectedResponse = MesaItemsProduccionResponse.builder().build();

            Authentication auth = crearAuthentication("mesero1@altoro.com", "MESERO");

            when(mesaValidador.validarOwnership(eq(1L), any())).thenReturn(mesa);
            when(comandaRepository.findItemsEnProduccionByVisita(1L)).thenReturn(itemsVacios);
            when(mesaMapper.agruparItemsEnProduccion(itemsVacios)).thenReturn(itemsDtoVacios);
            when(mesaMapper.toMesaItemsProduccionResponse("T1", itemsDtoVacios)).thenReturn(expectedResponse);

            // Act
            MesaItemsProduccionResponse resultado = mesaService.obtenerItemsProduccion(1L, auth);

            // Assert
            assertThat(resultado).isEqualTo(expectedResponse);
            verify(mesaMapper).agruparItemsEnProduccion(itemsVacios);
        }

@Test
        @DisplayName("con visitaId inexistente lanza BusinessException")
        void conVisitaIdInexistente_lanzaBusinessException() {
            // Arrange
            doThrow(new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Mesa no encontrada", HttpStatus.NOT_FOUND))
                    .when(mesaValidador).validarOwnership(eq(999L), any());

            // Act & Assert
            assertThatThrownBy(() -> mesaService.obtenerItemsProduccion(999L, null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", ErrorCode.ENTITY_NOT_FOUND.getCode())
                    .hasMessageContaining("Mesa no encontrada");

            verify(mesaValidador).validarOwnership(eq(999L), any());
            verifyNoInteractions(mesaRepository);
            verifyNoInteractions(comandaRepository);
            verifyNoInteractions(mesaMapper);
        }
    }
}
