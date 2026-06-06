package co.edu.unicauca.backend.modules.reservas.controller;

import co.edu.unicauca.backend.modules.reservas.dto.request.ActualizarDecoracionRequest;
import co.edu.unicauca.backend.modules.reservas.dto.request.CambioEstadoDecoracionRequest;
import co.edu.unicauca.backend.modules.reservas.dto.request.CrearDecoracionRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.DecoracionAdminResponse;
import co.edu.unicauca.backend.modules.reservas.service.DecoracionAdminService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Autowired
    private ObjectMapper objectMapper;

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

    @Test
    void listarDecoraciones_admin_ok() throws Exception {
        DecoracionAdminResponse response = DecoracionAdminResponse.builder()
                .decoracionId(1L)
                .decoracionNombre("Romántica")
                .decoracionEstado("ACTIVO")
                .decoracionCostoAdicional(BigDecimal.valueOf(12000))
                .decoracionImagenUrl("/uploads/decoraciones/romantica.jpg")
                .zonaIds(List.of(1L, 2L))
                .build();

        when(decoracionAdminService.listarDecoraciones()).thenReturn(List.of(response));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/decoraciones")
                        .with(user("admin@altoro.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].decoracionId").value(1))
                .andExpect(jsonPath("$.data[0].decoracionNombre").value("Romántica"));
    }

    @Test
    void crearDecoracion_admin_ok() throws Exception {
        CrearDecoracionRequest request = CrearDecoracionRequest.builder()
                .decoracionNombre("Familiar")
                .decoracionCostoAdicional(BigDecimal.valueOf(5000))
                .zonaIds(List.of(1L))
                .build();

        DecoracionAdminResponse response = DecoracionAdminResponse.builder()
                .decoracionId(2L)
                .decoracionNombre("Familiar")
                .decoracionEstado("ACTIVO")
                .decoracionCostoAdicional(BigDecimal.valueOf(5000))
                .zonaIds(List.of(1L))
                .build();

        when(decoracionAdminService.crearDecoracion(any())).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/decoraciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("admin@altoro.com").roles("ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.decoracionId").value(2))
                .andExpect(jsonPath("$.message").value("Decoración creada correctamente"));
    }

    @Test
    void actualizarDecoracion_admin_ok() throws Exception {
        ActualizarDecoracionRequest request = ActualizarDecoracionRequest.builder()
                .decoracionNombre("Terraza")
                .decoracionCostoAdicional(BigDecimal.valueOf(3000))
                .zonaIds(List.of(3L))
                .build();

        DecoracionAdminResponse response = DecoracionAdminResponse.builder()
                .decoracionId(3L)
                .decoracionNombre("Terraza")
                .decoracionCostoAdicional(BigDecimal.valueOf(3000))
                .zonaIds(List.of(3L))
                .build();

        when(decoracionAdminService.actualizarDecoracion(eq(3L), any())).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.put("/api/decoraciones/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("admin@altoro.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decoracionNombre").value("Terraza"))
                .andExpect(jsonPath("$.message").value("Decoración actualizada correctamente"));
    }

    @Test
    void cambiarEstadoDecoracion_admin_ok() throws Exception {
        CambioEstadoDecoracionRequest request = CambioEstadoDecoracionRequest.builder()
                .estado(co.edu.unicauca.backend.shared.enums.EstadoGenerico.INACTIVO)
                .build();

        DecoracionAdminResponse response = DecoracionAdminResponse.builder()
                .decoracionId(3L)
                .decoracionEstado("INACTIVO")
                .build();

        when(decoracionAdminService.cambiarEstadoDecoracion(eq(3L), any())).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.patch("/api/decoraciones/3/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("admin@altoro.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.decoracionEstado").value("INACTIVO"))
                .andExpect(jsonPath("$.message").value("Estado de decoración actualizado correctamente"));
    }

    @Test
    void eliminarDecoracion_admin_ok() throws Exception {
        mockMvc.perform(delete("/api/decoraciones/4")
                        .with(user("admin@altoro.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
