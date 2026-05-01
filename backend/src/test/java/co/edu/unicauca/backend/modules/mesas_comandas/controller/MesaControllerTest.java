package co.edu.unicauca.backend.modules.mesas_comandas.controller;

import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.AsignarMesaRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.*;
import co.edu.unicauca.backend.modules.mesas_comandas.service.MesaAsignarService;
import co.edu.unicauca.backend.modules.mesas_comandas.service.MesaService;
import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @Autowired ObjectMapper objectMapper;

    @MockitoBean MesaService mesaService;
    @MockitoBean MesaAsignarService mesaAsignarService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;

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

            when(mesaService.obtenerItemsProduccion(eq(1L), any(Authentication.class))).thenReturn(response);

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

            verify(mesaService).obtenerItemsProduccion(eq(1L), any(Authentication.class));
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

            when(mesaService.obtenerItemsProduccion(eq(1L), any(Authentication.class))).thenReturn(response);

            // Act & Assert
            mockMvc.perform(get("/api/mesas/1/items-produccion"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(mesaService).obtenerItemsProduccion(eq(1L), any(Authentication.class));
        }

        @Test
        @WithMockUser(username = "mesero1@altoro.com", roles = "MESERO")
        @DisplayName("mesaId inexistente → 404 Not Found")
        void mesaIdInexistente_retorna404() throws Exception {
            // Arrange
            when(mesaService.obtenerItemsProduccion(eq(999L), any(Authentication.class)))
                    .thenThrow(new BusinessException(
                            ErrorCode.ENTITY_NOT_FOUND,
                            "Mesa no encontrada",
                            HttpStatus.NOT_FOUND));

            // Act & Assert
            mockMvc.perform(get("/api/mesas/999/items-produccion"))
                    .andExpect(status().isNotFound());

            verify(mesaService).obtenerItemsProduccion(eq(999L), any(Authentication.class));
        }
    }

    @Nested
    @DisplayName("POST /api/mesas")
    class AsignarMesa {

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("MESERO asigna mesa walk-in → 201 CREATED")
        void meseroAsignaMesaWalkIn_retorna201() throws Exception {
            // Arrange
            AsignarMesaRequest request = new AsignarMesaRequest(
                    "Mesa A1",
                    1L,
                    4,
                    null,
                    "Cliente VIP"
            );

            MesaAsignadaResponse response = MesaAsignadaResponse.builder()
                    .visitaId(100L)
                    .mesaIdentificador("Mesa A1")
                    .zonaId(1L)
                    .zonaNombre("Terraza")
                    .numeroPersonas(4)
                    .estadoMesa("ESPERA")
                    .emailMesero("mesero@altoro.com")
                    .reservaId(null)
                    .build();

            when(mesaAsignarService.asignarMesa(any(AsignarMesaRequest.class), anyString()))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/mesas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Mesa creada correctamente"))
                    .andExpect(jsonPath("$.data.visitaId").value(100))
                    .andExpect(jsonPath("$.data.mesaIdentificador").value("Mesa A1"))
                    .andExpect(jsonPath("$.data.zonaId").value(1))
                    .andExpect(jsonPath("$.data.zonaNombre").value("Terraza"))
                    .andExpect(jsonPath("$.data.numeroPersonas").value(4))
                    .andExpect(jsonPath("$.data.estadoMesa").value("ESPERA"))
                    .andExpect(jsonPath("$.data.emailMesero").value("mesero@altoro.com"))
                    .andExpect(jsonPath("$.data.reservaId").isEmpty());

            verify(mesaAsignarService).asignarMesa(any(AsignarMesaRequest.class), anyString());
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("MESERO asigna mesa con reserva → 201 CREATED")
        void meseroAsignaMesaConReserva_retorna201() throws Exception {
            // Arrange
            AsignarMesaRequest request = new AsignarMesaRequest(
                    "Mesa B2",
                    2L,
                    2,
                    50L,
                    null
            );

            MesaAsignadaResponse response = MesaAsignadaResponse.builder()
                    .visitaId(101L)
                    .mesaIdentificador("Mesa B2")
                    .zonaId(2L)
                    .zonaNombre("Salón Principal")
                    .numeroPersonas(2)
                    .estadoMesa("ESPERA")
                    .emailMesero("mesero@altoro.com")
                    .reservaId(50L)
                    .build();

            when(mesaAsignarService.asignarMesa(any(AsignarMesaRequest.class), anyString()))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/mesas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.visitaId").value(101))
                    .andExpect(jsonPath("$.data.mesaIdentificador").value("Mesa B2"))
                    .andExpect(jsonPath("$.data.reservaId").value(50));

            verify(mesaAsignarService).asignarMesa(any(AsignarMesaRequest.class), anyString());
        }

        @Test
        @WithMockUser(username = "admin@altoro.com", roles = "ADMIN")
        @DisplayName("ADMIN asigna mesa → 201 CREATED")
        void adminAsignaMesa_retorna201() throws Exception {
            // Arrange
            AsignarMesaRequest request = new AsignarMesaRequest(
                    "Mesa C3",
                    1L,
                    6,
                    null,
                    null
            );

            MesaAsignadaResponse response = MesaAsignadaResponse.builder()
                    .visitaId(102L)
                    .mesaIdentificador("Mesa C3")
                    .zonaId(1L)
                    .zonaNombre("Terraza")
                    .numeroPersonas(6)
                    .estadoMesa("ESPERA")
                    .emailMesero("admin@altoro.com")
                    .reservaId(null)
                    .build();

            when(mesaAsignarService.asignarMesa(any(AsignarMesaRequest.class), anyString()))
                    .thenReturn(response);

            // Act & Assert
            mockMvc.perform(post("/api/mesas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.visitaId").value(102));

            verify(mesaAsignarService).asignarMesa(any(AsignarMesaRequest.class), anyString());
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("mesaIdentificador blank → 422 UNPROCESSABLE_ENTITY")
        void mesaIdentificadorBlank_retorna422() throws Exception {
            // Arrange
            AsignarMesaRequest request = new AsignarMesaRequest(
                    "",
                    1L,
                    4,
                    null,
                    null
            );

            // Act & Assert
            mockMvc.perform(post("/api/mesas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("mesaIdentificador > 20 chars → 422 UNPROCESSABLE_ENTITY")
        void mesaIdentificadorExcedeLongitud_retorna422() throws Exception {
            // Arrange
            AsignarMesaRequest request = new AsignarMesaRequest(
                    "Mesa con identificador muy largo que supera los 20 caracteres",
                    1L,
                    4,
                    null,
                    null
            );

            // Act & Assert
            mockMvc.perform(post("/api/mesas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("numeroPersonas null → 422 UNPROCESSABLE_ENTITY")
        void numeroPersonasNull_retorna422() throws Exception {
            // Arrange
            AsignarMesaRequest request = new AsignarMesaRequest(
                    "Mesa D4",
                    1L,
                    null,
                    null,
                    null
            );

            // Act & Assert
            mockMvc.perform(post("/api/mesas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("numeroPersonas < 1 → 422 UNPROCESSABLE_ENTITY")
        void numeroPersonasMenorQue1_retorna422() throws Exception {
            // Arrange
            AsignarMesaRequest request = new AsignarMesaRequest(
                    "Mesa E5",
                    1L,
                    0,
                    null,
                    null
            );

            // Act & Assert
            mockMvc.perform(post("/api/mesas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("zonaId null → 422 UNPROCESSABLE_ENTITY")
        void zonaIdNull_retorna422() throws Exception {
            // Arrange
            AsignarMesaRequest request = new AsignarMesaRequest(
                    "Mesa F6",
                    null,
                    4,
                    null,
                    null
            );

            // Act & Assert
            mockMvc.perform(post("/api/mesas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("zona no encontrada → 404 NOT_FOUND")
        void zonaNoEncontrada_retorna404() throws Exception {
            // Arrange
            AsignarMesaRequest request = new AsignarMesaRequest(
                    "Mesa G7",
                    999L,
                    4,
                    null,
                    null
            );

            when(mesaAsignarService.asignarMesa(any(AsignarMesaRequest.class), anyString()))
                    .thenThrow(new BusinessException(
                            ErrorCode.ENTITY_NOT_FOUND,
                            "Zona no encontrada",
                            HttpStatus.NOT_FOUND));

            // Act & Assert
            mockMvc.perform(post("/api/mesas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());

            verify(mesaAsignarService).asignarMesa(any(AsignarMesaRequest.class), anyString());
        }
    }

    @Nested
    @DisplayName("GET /api/mesas/zonas-disponibles")
    class ListarZonasDisponibles {

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("MESERO obtiene zonas disponibles → 200 OK")
        void meseroObtieneZonasDisponibles_retorna200() throws Exception {
            // Arrange
            List<ZonaDisponibleMesaResponse> zonas = List.of(
                    ZonaDisponibleMesaResponse.builder()
                            .zonaId(1L)
                            .zonaNombre("Terraza")
                            .capacidadTotal(20)
                            .personasOcupadas(8)
                            .disponibilidad(12)
                            .build(),
                    ZonaDisponibleMesaResponse.builder()
                            .zonaId(2L)
                            .zonaNombre("Salón Principal")
                            .capacidadTotal(30)
                            .personasOcupadas(15)
                            .disponibilidad(15)
                            .build()
            );

            when(mesaAsignarService.listarZonasDisponibles()).thenReturn(zonas);

            // Act & Assert
            mockMvc.perform(get("/api/mesas/zonas-disponibles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Zonas disponibles obtenidas exitosamente"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].zonaId").value(1))
                    .andExpect(jsonPath("$.data[0].zonaNombre").value("Terraza"))
                    .andExpect(jsonPath("$.data[0].capacidadTotal").value(20))
                    .andExpect(jsonPath("$.data[0].personasOcupadas").value(8))
                    .andExpect(jsonPath("$.data[0].disponibilidad").value(12))
                    .andExpect(jsonPath("$.data[1].zonaId").value(2))
                    .andExpect(jsonPath("$.data[1].zonaNombre").value("Salón Principal"))
                    .andExpect(jsonPath("$.data[1].capacidadTotal").value(30))
                    .andExpect(jsonPath("$.data[1].personasOcupadas").value(15))
                    .andExpect(jsonPath("$.data[1].disponibilidad").value(15));

            verify(mesaAsignarService).listarZonasDisponibles();
        }

        @Test
        @WithMockUser(username = "admin@altoro.com", roles = "ADMIN")
        @DisplayName("ADMIN obtiene zonas disponibles → 200 OK")
        void adminObtieneZonasDisponibles_retorna200() throws Exception {
            // Arrange
            List<ZonaDisponibleMesaResponse> zonas = List.of(
                    ZonaDisponibleMesaResponse.builder()
                            .zonaId(1L)
                            .zonaNombre("Terraza")
                            .capacidadTotal(20)
                            .personasOcupadas(0)
                            .disponibilidad(20)
                            .build()
            );

            when(mesaAsignarService.listarZonasDisponibles()).thenReturn(zonas);

            // Act & Assert
            mockMvc.perform(get("/api/mesas/zonas-disponibles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1));

            verify(mesaAsignarService).listarZonasDisponibles();
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("sin zonas disponibles → 200 OK con lista vacía")
        void sinZonasDisponibles_retorna200ConListaVacia() throws Exception {
            // Arrange
            when(mesaAsignarService.listarZonasDisponibles()).thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/api/mesas/zonas-disponibles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            verify(mesaAsignarService).listarZonasDisponibles();
        }
    }
}
