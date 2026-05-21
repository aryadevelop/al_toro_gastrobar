package co.edu.unicauca.backend.modules.auth.controller;

import co.edu.unicauca.backend.modules.auth.dto.response.AuthResponse;
import co.edu.unicauca.backend.modules.auth.dto.response.AuthUserResponse;
import co.edu.unicauca.backend.modules.auth.dto.response.RegisterResponse;
import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import co.edu.unicauca.backend.modules.auth.service.AuthService;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import(AuthControllerTest.PermissiveSecurityConfig.class)
class AuthControllerTest {

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

    @MockitoBean AuthService authService;
    @MockitoBean JwtTokenProvider jwtTokenProvider;
    @MockitoBean UserDetailsService userDetailsService;
    @MockitoBean SesionRepository sesionRepository;

    private AuthResponse authResponseStub() {
        return AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .build();
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("Registro válido → 201 Created con datos del usuario")
        void registroValido_retorna201() throws Exception {
            when(authService.register(any())).thenReturn(RegisterResponse.builder()
                    .success(true)
                    .message("Cuenta creada exitosamente")
                    .user(RegisterResponse.UserRegistrationData.builder()
                            .id("99")
                            .email("nuevo@altoro.com")
                            .nombre("Juan")
                            .telefono("3101234567")
                            .role("CLIENTE")
                            .build())
                    .build());

            String body = objectMapper.writeValueAsString(Map.of(
                    "email", "nuevo@altoro.com",
                    "nombre", "Juan",
                    "telefono", "3101234567",
                    "password", "Password123!",
                    "passwordConfirmation", "Password123!",
                    "aceptaTerminos", true));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.user.email").value("nuevo@altoro.com"));
        }

        @Test
        @DisplayName("Sin email → 422 Unprocessable Entity")
        void registroSinEmail_retorna422() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of(
                    "nombre", "Juan",
                    "telefono", "3101234567",
                    "password", "Password123!",
                    "passwordConfirmation", "Password123!",
                    "aceptaTerminos", true));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("Credenciales válidas → 200 con tokens")
        void loginValido_retorna200() throws Exception {
            when(authService.login(any())).thenReturn(authResponseStub());

            String body = objectMapper.writeValueAsString(Map.of(
                    "email", "cliente@altoro.com",
                    "password", "Al.Toro2026!"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"));
        }

        @Test
        @DisplayName("Sin email → 422 Unprocessable Entity")
        void sinEmail_retorna422() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of("password", "Al.Toro2026!"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("Sin password → 422 Unprocessable Entity")
        void sinPassword_retorna422() throws Exception {
            String body = objectMapper.writeValueAsString(Map.of("email", "cliente@altoro.com"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("Token de refresco válido → 200 con nuevos tokens")
        void refreshValido_retorna200() throws Exception {
            when(authService.refresh(any())).thenReturn(authResponseStub());

            String body = objectMapper.writeValueAsString(Map.of("refreshToken", "valid-refresh-token"));

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-token"));
        }
    }

    @Nested
    @DisplayName("GET /api/auth/me")
    class Me {

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Usuario autenticado → 200 con perfil")
        void meAutenticado_retorna200() throws Exception {
            when(authService.me(any())).thenReturn(AuthUserResponse.builder().build());

            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class Logout {

        @Test
        @WithMockUser(username = "cliente@altoro.com", roles = "CLIENTE")
        @DisplayName("Autenticado → 204 y servicio invocado con email")
        void logout_retorna204YLlamaServicio() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isNoContent());

            verify(authService).logout("cliente@altoro.com");
        }
    }
}
