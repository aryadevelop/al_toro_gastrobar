package co.edu.unicauca.backend.modules.mesas_comandas.controller;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.*;
import co.edu.unicauca.backend.modules.mesas_comandas.service.MesaService;
import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MesaController.class)
@Import(MesaControllerTest.PermissiveSecurityConfig.class)
@DisplayName("MesaController")
class MesaControllerTest {

    static class PermissiveSecurityConfig {
        @Bean
        @Order(1)
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            return http
                    .securityMatcher("/**")
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired MockMvc mockMvc;

    @MockBean MesaService mesaService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean UserDetailsService userDetailsService;
    @MockBean SesionRepository sesionRepository;

    @Nested
    @DisplayName("GET /api/mesas")
    class ObtenerMapaMesas {

        @Test
        @WithMockUser(username = "mesero1@altoro.com", roles = "MESERO")
        @DisplayName("sin zonaId retorna todas las zonas → 200 OK")
        void sinZonaId_retornaTodasLasZonas() throws Exception {
            // Arrange
            ZonaMesasResponse zona1 = ZonaMesasResponse.builder()
                    .zonaId(1L)
                    .zonaNombre("Terraza")
                    .cantidadMesasActivas(2)
                    .mesas(List.of())
                    .build();

            ZonaMesasResponse zona2 = ZonaMesasResponse.builder()
                    .zonaId(2L)
                    .zonaNombre("Interior")
                    .cantidadMesasActivas(0)
                    .mesas(List.of())
                    .build();

            MapaMesasResponse response = MapaMesasResponse.builder()
                    .zonas(List.of(zona1, zona2))
                    .build();

            when(mesaService.obtenerMapaMesas(isNull(), eq("mesero1@altoro.com"))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/mesas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.zonas").isArray())
                    .andExpect(jsonPath("$.data.zonas.length()").value(2))
                    .andExpect(jsonPath("$.data.zonas[0].zonaId").value(1))
                    .andExpect(jsonPath("$.data.zonas[0].zonaNombre").value("Terraza"))
                    .andExpect(jsonPath("$.data.zonas[0].cantidadMesasActivas").value(2))
                    .andExpect(jsonPath("$.data.zonas[1].zonaId").value(2))
                    .andExpect(jsonPath("$.data.zonas[1].zonaNombre").value("Interior"))
                    .andExpect(jsonPath("$.data.zonas[1].cantidadMesasActivas").value(0))
                    .andExpect(jsonPath("$.message").value("Mapa de mesas obtenido exitosamente"));

            verify(mesaService).obtenerMapaMesas(null, "mesero1@altoro.com");
        }

        @Test
        @WithMockUser(username = "mesero1@altoro.com", roles = "MESERO")
        @DisplayName("con zonaId específico retorna solo esa zona → 200 OK")
        void conZonaIdEspecifico_retornaSoloEsaZona() throws Exception {
            // Arrange
            ZonaMesasResponse zona = ZonaMesasResponse.builder()
                    .zonaId(1L)
                    .zonaNombre("Terraza")
                    .cantidadMesasActivas(3)
                    .mesas(List.of())
                    .build();

            MapaMesasResponse response = MapaMesasResponse.builder()
                    .zonas(List.of(zona))
                    .build();

            when(mesaService.obtenerMapaMesas(eq(1L), eq("mesero1@altoro.com"))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/mesas")
                            .param("zonaId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.zonas").isArray())
                    .andExpect(jsonPath("$.data.zonas.length()").value(1))
                    .andExpect(jsonPath("$.data.zonas[0].zonaId").value(1))
                    .andExpect(jsonPath("$.data.zonas[0].cantidadMesasActivas").value(3));

            verify(mesaService).obtenerMapaMesas(1L, "mesero1@altoro.com");
        }

        @Test
        @WithMockUser(username = "admin@altoro.com", roles = "ADMIN")
        @DisplayName("admin puede acceder al mapa → 200 OK")
        void adminPuedeAcceder() throws Exception {
            // Arrange
            MapaMesasResponse response = MapaMesasResponse.builder()
                    .zonas(List.of())
                    .build();

            when(mesaService.obtenerMapaMesas(isNull(), eq("admin@altoro.com"))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/mesas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(mesaService).obtenerMapaMesas(null, "admin@altoro.com");
        }

        @Test
        @WithMockUser(username = "mesero1@altoro.com", roles = "MESERO")
        @DisplayName("zonaId inexistente → 404 Not Found")
        void zonaIdInexistente_retorna404() throws Exception {
            // Arrange
            when(mesaService.obtenerMapaMesas(eq(999L), anyString()))
                    .thenThrow(new BusinessException(
                            ErrorCode.ENTITY_NOT_FOUND,
                            "Zona no encontrada",
                            HttpStatus.NOT_FOUND));

            // Act & Assert
            mockMvc.perform(get("/api/mesas")
                            .param("zonaId", "999"))
                    .andExpect(status().isNotFound());

            verify(mesaService).obtenerMapaMesas(999L, "mesero1@altoro.com");
        }
    }

    @Nested
    @DisplayName("GET /api/mesas/{mesaId}/detalle")
    class ObtenerDetalleMesa {

        @Test
        @WithMockUser(username = "mesero1@altoro.com", roles = "MESERO")
        @DisplayName("mesaId válido retorna detalle completo → 200 OK")
        void mesaIdValido_retornaDetalle() throws Exception {
            // Arrange
            MesaDetalleResponse response = MesaDetalleResponse.builder()
                    .visitaId(1L)
                    .identificador("T1")
                    .nombreCliente("Juan Pérez")
                    .numeroPersonas(4)
                    .estado("EN_PREPARACION")
                    .itemsComanda(List.of())
                    .build();

            when(mesaService.obtenerDetalleMesa(1L)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/mesas/1/detalle"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.visitaId").value(1))
                    .andExpect(jsonPath("$.data.identificador").value("T1"))
                    .andExpect(jsonPath("$.data.nombreCliente").value("Juan Pérez"))
                    .andExpect(jsonPath("$.data.numeroPersonas").value(4))
                    .andExpect(jsonPath("$.data.estado").value("EN_PREPARACION"))
                    .andExpect(jsonPath("$.message").value("Detalle de mesa obtenido exitosamente"));

            verify(mesaService).obtenerDetalleMesa(1L);
        }

        @Test
        @WithMockUser(username = "admin@altoro.com", roles = "ADMIN")
        @DisplayName("admin puede acceder al detalle → 200 OK")
        void adminPuedeAcceder() throws Exception {
            // Arrange
            MesaDetalleResponse response = MesaDetalleResponse.builder()
                    .visitaId(1L)
                    .identificador("T1")
                    .build();

            when(mesaService.obtenerDetalleMesa(1L)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/mesas/1/detalle"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(mesaService).obtenerDetalleMesa(1L);
        }

        @Test
        @WithMockUser(username = "mesero1@altoro.com", roles = "MESERO")
        @DisplayName("mesaId inexistente → 404 Not Found")
        void mesaIdInexistente_retorna404() throws Exception {
            // Arrange
            when(mesaService.obtenerDetalleMesa(999L))
                    .thenThrow(new BusinessException(
                            ErrorCode.ENTITY_NOT_FOUND,
                            "Mesa no encontrada",
                            HttpStatus.NOT_FOUND));

            // Act & Assert
            mockMvc.perform(get("/api/mesas/999/detalle"))
                    .andExpect(status().isNotFound());

            verify(mesaService).obtenerDetalleMesa(999L);
        }
    }

    @Nested
    @DisplayName("GET /api/mesas/{mesaId}/items-produccion")
    class ObtenerItemsProduccion {

        @Test
        @WithMockUser(username = "mesero1@altoro.com", roles = "MESERO")
        @DisplayName("mesaId válido retorna items en producción → 200 OK")
        void mesaIdValido_retornaItemsProduccion() throws Exception {
            // Arrange
            ItemComandaEnProduccionResponse item = ItemComandaEnProduccionResponse.builder()
                    .nombreProducto("Bandeja Paisa")
                    .categoriaProducto("PLATO")
                    .cantidad(2)
                    .estadoComanda("EN_PREPARACION")
                    .build();

            MesaItemsProduccionResponse response = MesaItemsProduccionResponse.builder()
                    .identificadorMesa("T1")
                    .itemsEnProduccion(List.of(item))
                    .build();

            when(mesaService.obtenerItemsProduccion(1L)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/mesas/1/items-produccion"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.identificadorMesa").value("T1"))
                    .andExpect(jsonPath("$.data.itemsEnProduccion").isArray())
                    .andExpect(jsonPath("$.data.itemsEnProduccion.length()").value(1))
                    .andExpect(jsonPath("$.data.itemsEnProduccion[0].nombreProducto").value("Bandeja Paisa"))
                    .andExpect(jsonPath("$.data.itemsEnProduccion[0].cantidad").value(2))
                    .andExpect(jsonPath("$.message").value("Items en producción obtenidos exitosamente"));

            verify(mesaService).obtenerItemsProduccion(1L);
        }

        @Test
        @WithMockUser(username = "admin@altoro.com", roles = "ADMIN")
        @DisplayName("admin puede acceder a items producción → 200 OK")
        void adminPuedeAcceder() throws Exception {
            // Arrange
            MesaItemsProduccionResponse response = MesaItemsProduccionResponse.builder()
                    .identificadorMesa("T1")
                    .itemsEnProduccion(List.of())
                    .build();

            when(mesaService.obtenerItemsProduccion(1L)).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/mesas/1/items-produccion"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(mesaService).obtenerItemsProduccion(1L);
        }

        @Test
        @WithMockUser(username = "mesero1@altoro.com", roles = "MESERO")
        @DisplayName("mesaId inexistente → 404 Not Found")
        void mesaIdInexistente_retorna404() throws Exception {
            // Arrange
            when(mesaService.obtenerItemsProduccion(999L))
                    .thenThrow(new BusinessException(
                            ErrorCode.ENTITY_NOT_FOUND,
                            "Mesa no encontrada",
                            HttpStatus.NOT_FOUND));

            // Act & Assert
            mockMvc.perform(get("/api/mesas/999/items-produccion"))
                    .andExpect(status().isNotFound());

            verify(mesaService).obtenerItemsProduccion(999L);
        }
    }
}
