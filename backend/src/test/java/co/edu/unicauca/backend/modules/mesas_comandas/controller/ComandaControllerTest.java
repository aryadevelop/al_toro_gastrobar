package co.edu.unicauca.backend.modules.mesas_comandas.controller;

import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.AgregarItemRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.ModificarItemRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.request.NotasRequest;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.BorradorComandaResponse;
import co.edu.unicauca.backend.modules.mesas_comandas.service.ComandaBorradorService;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ComandaController.class)
@Import(ComandaControllerTest.PermissiveSecurityConfig.class)
@DisplayName("ComandaController")
class ComandaControllerTest {

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

    @MockitoBean ComandaBorradorService borradorService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;

    // ─────────────────────────────────────────────────────────────────────────────
    // GET /api/comandas/borrador
    // ─────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/comandas/borrador")
    class ObtenerBorrador {

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("visitaId válido → 200 OK con mensaje")
        void visitaIdValido_devuelve200() throws Exception {
            when(borradorService.obtenerBorrador(eq(1L), any()))
                    .thenReturn(mock(BorradorComandaResponse.class));

            mockMvc.perform(get("/api/comandas/borrador").param("visitaId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Borrador obtenido exitosamente"));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("visita no encontrada → 404 NOT FOUND")
        void visitaNoEncontrada_devuelve404() throws Exception {
            when(borradorService.obtenerBorrador(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("Visita", 99L));

            mockMvc.perform(get("/api/comandas/borrador").param("visitaId", "99"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // POST /api/comandas/borrador/items
    // ─────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/comandas/borrador/items")
    class AgregarItem {

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("request válido → 200 OK")
        void requestValido_devuelve200() throws Exception {
            AgregarItemRequest req = new AgregarItemRequest();
            req.setVisitaId(1L);
            req.setProductoId(2L);
            req.setCantidad(2);

            when(borradorService.agregarItem(any(), any()))
                    .thenReturn(mock(BorradorComandaResponse.class));

            mockMvc.perform(post("/api/comandas/borrador/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Item agregado"));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("visitaId nulo → 422 UNPROCESSABLE_ENTITY")
        void visitaIdNulo_devuelve422() throws Exception {
            AgregarItemRequest req = new AgregarItemRequest();
            req.setProductoId(2L);
            req.setCantidad(2);
            // visitaId null → @NotNull violation

            mockMvc.perform(post("/api/comandas/borrador/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PATCH /api/comandas/borrador/items/{itemId}
    // ─────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PATCH /api/comandas/borrador/items/{itemId}")
    class ModificarItem {

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("request válido → 200 OK")
        void requestValido_devuelve200() throws Exception {
            ModificarItemRequest req = new ModificarItemRequest();
            req.setCantidad(3);

            when(borradorService.modificarItem(eq(10L), any(), any()))
                    .thenReturn(mock(BorradorComandaResponse.class));

            mockMvc.perform(patch("/api/comandas/borrador/items/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Item modificado"));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("cantidad 0 → 422 UNPROCESSABLE_ENTITY")
        void cantidadCero_devuelve422() throws Exception {
            ModificarItemRequest req = new ModificarItemRequest();
            req.setCantidad(0); // @Min(1) violation

            mockMvc.perform(patch("/api/comandas/borrador/items/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // DELETE /api/comandas/borrador/items/{itemId}
    // ─────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /api/comandas/borrador/items/{itemId}")
    class EliminarItem {

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("item existente → 200 OK")
        void itemExistente_devuelve200() throws Exception {
            when(borradorService.eliminarItem(eq(5L), any()))
                    .thenReturn(mock(BorradorComandaResponse.class));

            mockMvc.perform(delete("/api/comandas/borrador/items/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Item eliminado"));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("item no encontrado → 404 NOT FOUND")
        void itemNoEncontrado_devuelve404() throws Exception {
            when(borradorService.eliminarItem(eq(99L), any()))
                    .thenThrow(new ResourceNotFoundException("ComandaItem", 99L));

            mockMvc.perform(delete("/api/comandas/borrador/items/99"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // POST /api/comandas/borrador/{comandaId}/enviar
    // ─────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/comandas/borrador/{comandaId}/enviar")
    class EnviarAProduccion {

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("comanda BORRADOR con items → 200 OK")
        void comandaConItems_devuelve200() throws Exception {
            when(borradorService.enviarAProduccion(eq(3L), any()))
                    .thenReturn(mock(BorradorComandaResponse.class));

            mockMvc.perform(post("/api/comandas/borrador/3/enviar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Comanda enviada a producción"));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("regla de negocio violada → 400 BAD REQUEST")
        void reglaViolada_devuelve400() throws Exception {
            when(borradorService.enviarAProduccion(eq(3L), any()))
                    .thenThrow(new BusinessException(ErrorCode.BUSINESS_ERROR, "Sin ítems para enviar"));

            mockMvc.perform(post("/api/comandas/borrador/3/enviar"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // DELETE /api/comandas/borrador
    // ─────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("DELETE /api/comandas/borrador")
    class CancelarFormulario {

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("visitaId válido → 200 OK")
        void visitaIdValido_devuelve200() throws Exception {
            doNothing().when(borradorService).cancelarFormulario(eq(1L), any());

            mockMvc.perform(delete("/api/comandas/borrador").param("visitaId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Cambios descartados"));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("sin visitaId → 400 BAD REQUEST")
        void sinVisitaId_devuelve400() throws Exception {
            mockMvc.perform(delete("/api/comandas/borrador"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PATCH /api/comandas/borrador/{comandaId}/notas
    // ─────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PATCH /api/comandas/borrador/{comandaId}/notas")
    class ActualizarNotas {

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("notas válidas → 200 OK")
        void notasValidas_devuelve200() throws Exception {
            NotasRequest req = new NotasRequest();
            req.setNotas("Sin cebolla por favor");

            when(borradorService.actualizarNotas(eq(4L), any(), any()))
                    .thenReturn(mock(BorradorComandaResponse.class));

            mockMvc.perform(patch("/api/comandas/borrador/4/notas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Notas actualizadas"));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("notas > 500 chars → 422 UNPROCESSABLE_ENTITY")
        void notasMuyLargas_devuelve422() throws Exception {
            NotasRequest req = new NotasRequest();
            req.setNotas("x".repeat(501)); // @Size(max=500) violation

            mockMvc.perform(patch("/api/comandas/borrador/4/notas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnprocessableEntity());
        }
    }
}
