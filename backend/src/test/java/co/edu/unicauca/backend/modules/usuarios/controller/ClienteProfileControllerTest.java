package co.edu.unicauca.backend.modules.usuarios.controller;

import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.usuarios.dto.request.ChangePasswordRequest;
import co.edu.unicauca.backend.modules.usuarios.dto.request.UpdateClienteRequest;
import co.edu.unicauca.backend.modules.usuarios.dto.response.ChangePasswordResponse;
import co.edu.unicauca.backend.modules.usuarios.dto.response.UpdateClienteResponse;
import co.edu.unicauca.backend.modules.usuarios.service.ClienteProfileService;
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
import org.springframework.http.HttpStatus;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de {@link ClienteProfileController} con {@code @WebMvcTest}.
 *
 * <p>Cubre los tres endpoints (GET/PUT/POST sobre {@code /me}) con un caso feliz
 * y uno de error/validación por cada uno.
 */
@WebMvcTest(controllers = ClienteProfileController.class)
@Import(ClienteProfileControllerTest.PermissiveSecurityConfig.class)
@DisplayName("ClienteProfileController")
class ClienteProfileControllerTest {

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

    @MockitoBean ClienteProfileService clienteProfileService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GET /api/clientes/me")
    class ObtenerMiPerfil {

        @Test
        @WithMockUser(username = "cli@a.co", roles = "CLIENTE")
        @DisplayName("cliente existente → 200 OK con perfil")
        void clienteExistente_devuelve200() throws Exception {
            when(clienteProfileService.getClienteIdByEmail("cli@a.co")).thenReturn(1L);
            when(clienteProfileService.obtenerMiPerfil(1L))
                    .thenReturn(UpdateClienteResponse.ClienteData.builder()
                            .id(1L)
                            .nombre("Juan")
                            .email("cli@a.co")
                            .build());

            mockMvc.perform(get("/api/clientes/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.nombre").value("Juan"))
                    .andExpect(jsonPath("$.data.email").value("cli@a.co"));
        }

        @Test
        @WithMockUser(username = "cli@a.co", roles = "CLIENTE")
        @DisplayName("cliente no encontrado → 404 NOT FOUND")
        void clienteNoExiste_devuelve404() throws Exception {
            when(clienteProfileService.getClienteIdByEmail("cli@a.co"))
                    .thenThrow(new ResourceNotFoundException("Cliente", "email", "cli@a.co"));

            mockMvc.perform(get("/api/clientes/me"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PUT /api/clientes/me")
    class ActualizarMiPerfil {

        private UpdateClienteRequest validRequest() {
            return UpdateClienteRequest.builder()
                    .nombre("Maria Lopez")
                    .email("maria@a.co")
                    .telefono("3001234567")
                    .direccion("Cll 5")
                    .aceptaTerminos(true)
                    .build();
        }

        @Test
        @WithMockUser(username = "cli@a.co", roles = "CLIENTE")
        @DisplayName("request válido → 200 OK")
        void requestValido_devuelve200() throws Exception {
            when(clienteProfileService.getClienteIdByEmail("cli@a.co")).thenReturn(1L);
            when(clienteProfileService.actualizarPerfil(eq(1L), any()))
                    .thenReturn(UpdateClienteResponse.builder()
                            .success(true)
                            .message("Datos actualizados correctamente")
                            .build());

            mockMvc.perform(put("/api/clientes/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.success").value(true));
        }

        @Test
        @WithMockUser(username = "cli@a.co", roles = "CLIENTE")
        @DisplayName("email duplicado → 409 CONFLICT")
        void emailDuplicado_devuelve409() throws Exception {
            when(clienteProfileService.getClienteIdByEmail("cli@a.co")).thenReturn(1L);
            when(clienteProfileService.actualizarPerfil(eq(1L), any()))
                    .thenThrow(new BusinessException(
                            ErrorCode.ENTITY_ALREADY_EXISTS,
                            "Email en uso",
                            HttpStatus.CONFLICT));

            mockMvc.perform(put("/api/clientes/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isConflict());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("POST /api/clientes/me/cambiar-contraseña")
    class CambiarContrasena {

        private ChangePasswordRequest validRequest() {
            return ChangePasswordRequest.builder()
                    .contraseñaActual("Vieja1!aa")
                    .nuevaContraseña("Nueva1!aa")
                    .confirmacion("Nueva1!aa")
                    .build();
        }

        @Test
        @WithMockUser(username = "cli@a.co", roles = "CLIENTE")
        @DisplayName("request válido → 200 OK")
        void requestValido_devuelve200() throws Exception {
            when(clienteProfileService.getClienteIdByEmail("cli@a.co")).thenReturn(1L);
            when(clienteProfileService.cambiarContraseña(eq(1L), any()))
                    .thenReturn(ChangePasswordResponse.builder()
                            .success(true)
                            .message("Contraseña actualizada correctamente")
                            .build());

            mockMvc.perform(post("/api/clientes/me/cambiar-contraseña")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.success").value(true));
        }

        @Test
        @WithMockUser(username = "cli@a.co", roles = "CLIENTE")
        @DisplayName("contraseña actual incorrecta → 400 BAD REQUEST")
        void actualIncorrecta_devuelve400() throws Exception {
            when(clienteProfileService.getClienteIdByEmail("cli@a.co")).thenReturn(1L);
            when(clienteProfileService.cambiarContraseña(eq(1L), any()))
                    .thenThrow(new BusinessException(
                            ErrorCode.VALIDATION_ERROR,
                            "La contraseña actual no es correcta",
                            HttpStatus.BAD_REQUEST));

            mockMvc.perform(post("/api/clientes/me/cambiar-contraseña")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest())))
                    .andExpect(status().isBadRequest());
        }
    }
}
