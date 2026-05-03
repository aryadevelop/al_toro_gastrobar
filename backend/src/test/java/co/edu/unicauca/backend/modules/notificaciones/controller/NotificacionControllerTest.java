package co.edu.unicauca.backend.modules.notificaciones.controller;

import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.notificaciones.dto.response.AtenderCambioResponse;
import co.edu.unicauca.backend.modules.notificaciones.service.NotificacionService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificacionController.class)
@Import(NotificacionControllerTest.PermissiveSecurityConfig.class)
@DisplayName("NotificacionController")
class NotificacionControllerTest {

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

    @MockitoBean NotificacionService notificacionService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;

    @Nested
    @DisplayName("PATCH /api/notificaciones/{notificacionId}/atender")
    class AtenderAsistencia {

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("MESERO atiende solicitud activa → 200 OK")
        void atenderOk() throws Exception {
            doNothing().when(notificacionService).atenderAsistencia(eq(50L), any());

            mockMvc.perform(patch("/api/notificaciones/50/atender"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Solicitud ya atendida → 409 Conflict")
        void yaAtendida_retorna409() throws Exception {
            doThrow(new BusinessException(ErrorCode.INVALID_STATE, "Ya atendida.", HttpStatus.CONFLICT))
                    .when(notificacionService).atenderAsistencia(eq(50L), any());

            mockMvc.perform(patch("/api/notificaciones/50/atender"))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Notificación inexistente → 404 Not Found")
        void notificacionNoExiste_retorna404() throws Exception {
            doThrow(new ResourceNotFoundException("Notificacion", 99L))
                    .when(notificacionService).atenderAsistencia(eq(99L), any());

            mockMvc.perform(patch("/api/notificaciones/99/atender"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/notificaciones/{notificacionId}/servir-platos")
    class ServirPlatos {

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("MESERO sirve platos de comanda lista → 200 OK")
        void servirPlatosOk() throws Exception {
            doNothing().when(notificacionService).servirPlatos(eq(50L), any());

            mockMvc.perform(patch("/api/notificaciones/50/servir-platos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Notificación inexistente → 404 Not Found")
        void notificacionNoExiste_retorna404() throws Exception {
            doThrow(new ResourceNotFoundException("Notificacion", 99L))
                    .when(notificacionService).servirPlatos(eq(99L), any());

            mockMvc.perform(patch("/api/notificaciones/99/servir-platos"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Tipo distinto a PLATOS_LISTOS → 409 Conflict")
        void tipoIncorrecto_retorna409() throws Exception {
            doThrow(new BusinessException(ErrorCode.INVALID_STATE, "Tipo incorrecto.", HttpStatus.CONFLICT))
                    .when(notificacionService).servirPlatos(eq(50L), any());

            mockMvc.perform(patch("/api/notificaciones/50/servir-platos"))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Notificación ya atendida → 409 Conflict")
        void yaAtendida_retorna409() throws Exception {
            doThrow(new BusinessException(ErrorCode.INVALID_STATE, "Ya atendida.", HttpStatus.CONFLICT))
                    .when(notificacionService).servirPlatos(eq(50L), any());

            mockMvc.perform(patch("/api/notificaciones/50/servir-platos"))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Comanda sin notificación asociada → 400 Bad Request")
        void sinComanda_retorna400() throws Exception {
            doThrow(new BusinessException(ErrorCode.BUSINESS_ERROR, "Sin comanda.", HttpStatus.BAD_REQUEST))
                    .when(notificacionService).servirPlatos(eq(50L), any());

            mockMvc.perform(patch("/api/notificaciones/50/servir-platos"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /api/notificaciones/{notificacionId}/servir-bebidas")
    class ServirBebidas {

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("MESERO sirve bebidas de comanda lista → 200 OK")
        void servirBebidasOk() throws Exception {
            doNothing().when(notificacionService).servirBebidas(eq(50L), any());

            mockMvc.perform(patch("/api/notificaciones/50/servir-bebidas"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Notificación inexistente → 404 Not Found")
        void notificacionNoExiste_retorna404() throws Exception {
            doThrow(new ResourceNotFoundException("Notificacion", 99L))
                    .when(notificacionService).servirBebidas(eq(99L), any());

            mockMvc.perform(patch("/api/notificaciones/99/servir-bebidas"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Notificación ya atendida → 409 Conflict")
        void yaAtendida_retorna409() throws Exception {
            doThrow(new BusinessException(ErrorCode.INVALID_STATE, "Ya atendida.", HttpStatus.CONFLICT))
                    .when(notificacionService).servirBebidas(eq(50L), any());

            mockMvc.perform(patch("/api/notificaciones/50/servir-bebidas"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PATCH /api/notificaciones/{notificacionId}/atender-cambio")
    class AtenderCambio {

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("MESERO atiende cambio activo → 200 OK con data.comandaId")
        void atenderCambioOk() throws Exception {
            AtenderCambioResponse response = AtenderCambioResponse.builder()
                    .comandaId(80L)
                    .build();
            when(notificacionService.atenderCambio(eq(50L), any())).thenReturn(response);

            mockMvc.perform(patch("/api/notificaciones/50/atender-cambio"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.comandaId").value(80));
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Notificación inexistente → 404 Not Found")
        void notificacionNoExiste_retorna404() throws Exception {
            doThrow(new ResourceNotFoundException("Notificacion", 99L))
                    .when(notificacionService).atenderCambio(eq(99L), any());

            mockMvc.perform(patch("/api/notificaciones/99/atender-cambio"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "mesero@altoro.com", roles = "MESERO")
        @DisplayName("Tipo distinto a CAMBIO → 409 Conflict")
        void tipoIncorrecto_retorna409() throws Exception {
            doThrow(new BusinessException(ErrorCode.INVALID_STATE, "Tipo incorrecto.", HttpStatus.CONFLICT))
                    .when(notificacionService).atenderCambio(eq(50L), any());

            mockMvc.perform(patch("/api/notificaciones/50/atender-cambio"))
                    .andExpect(status().isConflict());
        }
    }
}
