package co.edu.unicauca.backend.modules.mesas_comandas.controller;

import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionDetalleResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.ComandaProduccionResumenResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.TableroProduccionResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.service.ComandaProduccionService;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ComandaProduccionController.class)
@Import(ComandaProduccionControllerTest.PermissiveSecurityConfig.class)
@DisplayName("ComandaProduccionController")
class ComandaProduccionControllerTest {

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

    @MockitoBean ComandaProduccionService comandaProduccionService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/comandas/produccion")
    class TableroEndpoint {

        @Test
        @WithMockUser(username = "cocinero@altoro.com", roles = "PRODUCCION")
        @DisplayName("Solicitud válida: 200 OK con estructura del tablero")
        void solicitud_devuelve200() throws Exception {
            TableroProduccionResponse tablero = TableroProduccionResponse.builder()
                    .estaciones(List.of("COCINA"))
                    .pendientes(List.of())
                    .enPreparacion(List.of())
                    .listos(List.of())
                    .build();
            when(comandaProduccionService.obtenerTableroProduccion(any())).thenReturn(tablero);

            mockMvc.perform(get("/api/comandas/produccion"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.estaciones[0]").value("COCINA"));
        }

        @Test
        @WithMockUser(username = "admin@altoro.com", roles = "PRODUCCION")
        @DisplayName("Servicio lanza 403: el controller propaga el código HTTP")
        void servicio_403_propaga() throws Exception {
            when(comandaProduccionService.obtenerTableroProduccion(any()))
                    .thenThrow(new BusinessException(
                            ErrorCode.ACCESS_DENIED,
                            "Sin rol producción", HttpStatus.FORBIDDEN));

            mockMvc.perform(get("/api/comandas/produccion"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH-002"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/comandas/produccion/{comandaId}")
    class DetalleEndpoint {

        @Test
        @WithMockUser(username = "cocinero@altoro.com", roles = "PRODUCCION")
        @DisplayName("Comanda visible: 200 OK con detalle")
        void comandaVisible_devuelve200() throws Exception {
            ComandaProduccionDetalleResponse detalle = ComandaProduccionDetalleResponse.builder()
                    .comandaId(15L)
                    .estacion("COCINA")
                    .comandaEstado("PENDIENTE")
                    .platos(List.of()).bebidas(List.of()).otros(List.of())
                    .build();
            when(comandaProduccionService.obtenerDetalleComanda(eq(15L), any()))
                    .thenReturn(detalle);

            mockMvc.perform(get("/api/comandas/produccion/15"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.comandaId").value(15));
        }

        @Test
        @WithMockUser(username = "cocinero@altoro.com", roles = "PRODUCCION")
        @DisplayName("Comanda inexistente o estado no visible: 404")
        void noVisible_devuelve404() throws Exception {
            when(comandaProduccionService.obtenerDetalleComanda(eq(99L), any()))
                    .thenThrow(new BusinessException(
                            ErrorCode.ENTITY_NOT_FOUND, "no existe", HttpStatus.NOT_FOUND));

            mockMvc.perform(get("/api/comandas/produccion/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("ENT-001"));
        }

        @Test
        @WithMockUser(username = "cocinero@altoro.com", roles = "PRODUCCION")
        @DisplayName("Comanda de otra estación: 403")
        void otraEstacion_devuelve403() throws Exception {
            when(comandaProduccionService.obtenerDetalleComanda(eq(50L), any()))
                    .thenThrow(new BusinessException(
                            ErrorCode.ACCESS_DENIED, "otra estación", HttpStatus.FORBIDDEN));

            mockMvc.perform(get("/api/comandas/produccion/50"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("AUTH-002"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/comandas/produccion/{comandaId}/iniciar")
    class IniciarPreparacion {

        @Test
        @WithMockUser(username = "cocinero@altoro.com", roles = "PRODUCCION")
        @DisplayName("comanda PENDIENTE accesible → 200 OK con resumen")
        void happy() throws Exception {
            when(comandaProduccionService.iniciarPreparacion(eq(1L), any())).thenReturn(resumenStub());

            mockMvc.perform(post("/api/comandas/produccion/1/iniciar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.comandaId").value(1L));

            verify(comandaProduccionService).iniciarPreparacion(eq(1L), any());
        }

        @Test
        @WithMockUser(roles = "PRODUCCION")
        @DisplayName("comanda inexistente → 404")
        void inexistente_404() throws Exception {
            when(comandaProduccionService.iniciarPreparacion(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("Comanda", 99L));

            mockMvc.perform(post("/api/comandas/produccion/99/iniciar"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "PRODUCCION")
        @DisplayName("estado inválido → 409")
        void estado_409() throws Exception {
            when(comandaProduccionService.iniciarPreparacion(eq(1L), any()))
                    .thenThrow(new BusinessException(ErrorCode.INVALID_STATE, "msg", HttpStatus.CONFLICT));

            mockMvc.perform(post("/api/comandas/produccion/1/iniciar"))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(roles = "PRODUCCION")
        @DisplayName("estación ajena → 403")
        void estacion_403() throws Exception {
            when(comandaProduccionService.iniciarPreparacion(eq(1L), any()))
                    .thenThrow(new BusinessException(ErrorCode.ACCESS_DENIED, "msg", HttpStatus.FORBIDDEN));

            mockMvc.perform(post("/api/comandas/produccion/1/iniciar"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "PRODUCCION")
        @DisplayName("stock insuficiente → 409")
        void stock_409() throws Exception {
            when(comandaProduccionService.iniciarPreparacion(eq(1L), any()))
                    .thenThrow(new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "msg", HttpStatus.CONFLICT));

            mockMvc.perform(post("/api/comandas/produccion/1/iniciar"))
                    .andExpect(status().isConflict());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/comandas/produccion/{comandaId}/listo")
    class MarcarListo {

        @Test
        @WithMockUser(username = "cocinero@altoro.com", roles = "PRODUCCION")
        @DisplayName("comanda EN_PREPARACION accesible → 200 OK con resumen")
        void happy() throws Exception {
            when(comandaProduccionService.marcarListo(eq(1L), any())).thenReturn(resumenStub());

            mockMvc.perform(post("/api/comandas/produccion/1/listo"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.comandaId").value(1L));

            verify(comandaProduccionService).marcarListo(eq(1L), any());
        }

        @Test
        @WithMockUser(roles = "PRODUCCION")
        @DisplayName("comanda inexistente → 404")
        void inexistente_404() throws Exception {
            when(comandaProduccionService.marcarListo(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("Comanda", 99L));

            mockMvc.perform(post("/api/comandas/produccion/99/listo"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "PRODUCCION")
        @DisplayName("estado inválido → 409")
        void estado_409() throws Exception {
            when(comandaProduccionService.marcarListo(eq(1L), any()))
                    .thenThrow(new BusinessException(ErrorCode.INVALID_STATE, "msg", HttpStatus.CONFLICT));

            mockMvc.perform(post("/api/comandas/produccion/1/listo"))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(roles = "PRODUCCION")
        @DisplayName("estación ajena → 403")
        void estacion_403() throws Exception {
            when(comandaProduccionService.marcarListo(eq(1L), any()))
                    .thenThrow(new BusinessException(ErrorCode.ACCESS_DENIED, "msg", HttpStatus.FORBIDDEN));

            mockMvc.perform(post("/api/comandas/produccion/1/listo"))
                    .andExpect(status().isForbidden());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    private ComandaProduccionResumenResponse resumenStub() {
        return ComandaProduccionResumenResponse.builder()
                .comandaId(1L)
                .estacion("COCINA")
                .comandaEstado("EN_PREPARACION")
                .mesaIdentificador("Mesa 3")
                .meseroNombre("Juan")
                .totalItems(2)
                .build();
    }
}
