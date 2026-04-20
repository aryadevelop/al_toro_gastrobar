package co.edu.unicauca.backend.modules.pagos_caja.controller;

import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.pagos_caja.service.VentaService;
import co.edu.unicauca.backend.shared.enums.MetodoPago;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = VentaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class VentaControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean VentaService ventaService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;

    private Map<String, Object> bodyValido() {
        return Map.of(
                "emailCajero", "cajero@altoro.com",
                "visitaId", 1L,
                "subtotal", new BigDecimal("100.00"),
                "total", new BigDecimal("100.00"),
                "metodo", MetodoPago.EFECTIVO.name());
    }

    @Nested
    @DisplayName("POST /api/ventas")
    class CerrarCuenta {

        @Test
        @DisplayName("Request válido → 201 Created")
        void bodyValido_retorna201() throws Exception {
            doNothing().when(ventaService).cerrarCuenta(any(), anyString());

            mockMvc.perform(post("/api/ventas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bodyValido()))
                            .with(user("cajero@altoro.com").roles("CAJERO")))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Subtotal null → 422 Unprocessable Entity")
        void subtotalNull_retorna422() throws Exception {
            Map<String, Object> body = Map.of(
                    "emailCajero", "cajero@altoro.com",
                    "visitaId", 1L,
                    "total", new BigDecimal("100.00"),
                    "metodo", MetodoPago.EFECTIVO.name());

            mockMvc.perform(post("/api/ventas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .with(user("cajero@altoro.com").roles("CAJERO")))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("VisitaId null → 422 Unprocessable Entity")
        void visitaIdNull_retorna422() throws Exception {
            Map<String, Object> body = Map.of(
                    "emailCajero", "cajero@altoro.com",
                    "subtotal", new BigDecimal("100.00"),
                    "total", new BigDecimal("100.00"),
                    "metodo", MetodoPago.EFECTIVO.name());

            mockMvc.perform(post("/api/ventas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body))
                            .with(user("cajero@altoro.com").roles("CAJERO")))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Cuenta ya cerrada → 409 Conflict")
        void cuentaYaCerrada_retorna409() throws Exception {
            doThrow(new BusinessException(ErrorCode.INVALID_STATE, "Cuenta ya cerrada", HttpStatus.CONFLICT))
                    .when(ventaService).cerrarCuenta(any(), anyString());

            mockMvc.perform(post("/api/ventas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bodyValido()))
                            .with(user("cajero@altoro.com").roles("CAJERO")))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Visita no existe → 404 Not Found")
        void visitaNoExiste_retorna404() throws Exception {
            doThrow(new ResourceNotFoundException("Visita", 1L))
                    .when(ventaService).cerrarCuenta(any(), anyString());

            mockMvc.perform(post("/api/ventas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bodyValido()))
                            .with(user("cajero@altoro.com").roles("CAJERO")))
                    .andExpect(status().isNotFound());
        }
    }
}
