package co.edu.unicauca.backend.modules.reservas.controller;

import co.edu.unicauca.backend.modules.reservas.service.DecoracionAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.security.JwtTokenProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = DecoracionAdminController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class DecoracionAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DecoracionAdminService decoracionAdminService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @MockitoBean
    private SesionRepository sesionRepository;

    @Test
    void subirImagenDecoracion_admin_ok() throws Exception {
        when(decoracionAdminService.guardarImagenDecoracion(
                        org.mockito.ArgumentMatchers.eq(5L),
                        org.mockito.ArgumentMatchers.any(org.springframework.web.multipart.MultipartFile.class)))
                .thenReturn("https://cdn.example.com/decoraciones/imagen-guardada.png");

        MockMultipartFile archivo = new MockMultipartFile(
                "imagen", "foto.png", "image/png", "contenido".getBytes());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/api/decoraciones/5/imagen")
                        .file(archivo)
                        .with(user("admin@altoro.com").roles("ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").value("https://cdn.example.com/decoraciones/imagen-guardada.png"));
    }

    @Test
    void eliminarImagenDecoracion_admin_ok() throws Exception {
        mockMvc.perform(delete("/api/decoraciones/7/imagen")
                        .with(user("admin@altoro.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
