package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.mesas_comandas.entity.Zona;
import co.edu.unicauca.backend.modules.reservas.dto.request.ActualizarDecoracionRequest;
import co.edu.unicauca.backend.modules.reservas.dto.request.CrearDecoracionRequest;
import co.edu.unicauca.backend.modules.reservas.dto.response.DecoracionAdminResponse;
import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.entity.DecoracionZona;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionZonaRepository;
import co.edu.unicauca.backend.modules.mesas_comandas.repository.ZonaRepository;
import co.edu.unicauca.backend.shared.enums.EstadoGenerico;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecoracionAdminServiceTest {

    @Mock
    private DecoracionRepository decoracionRepository;

    @Mock
    private DecoracionZonaRepository decoracionZonaRepository;

    @Mock
    private ZonaRepository zonaRepository;

    private Path tempDirectory;
    private DecoracionAdminService service;

    @BeforeEach
    void setUp() throws Exception {
        tempDirectory = Files.createTempDirectory("decoracion-imagenes-test");
        service = new DecoracionAdminService(decoracionRepository,
                decoracionZonaRepository,
                zonaRepository,
                tempDirectory.toString(),
                "https://cdn.example.com/decoraciones");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (Files.exists(tempDirectory)) {
            Files.walk(tempDirectory)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }

    @Test
    void guardarImagenDecoracion_valida_guardaYRetornaUrl() throws Exception {
        Decoracion decoracion = new Decoracion();
        decoracion.setDecoracionId(1L);
        decoracion.setDecoracionImagenUrl(null);
        when(decoracionRepository.findById(1L)).thenReturn(Optional.of(decoracion));
        when(decoracionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile archivo = new MockMultipartFile(
                "imagen", "foto.jpeg", "image/jpeg", "contenido".getBytes());

        String url = service.guardarImagenDecoracion(1L, archivo);

        assertThat(url).startsWith("https://cdn.example.com/decoraciones/");
        assertThat(decoracion.getDecoracionImagenUrl()).isEqualTo(url);
        Path storedFile = tempDirectory.resolve(url.substring(url.lastIndexOf('/') + 1));
        assertThat(Files.exists(storedFile)).isTrue();
    }

    @Test
    void guardarImagenDecoracion_valida_guardaRutaRelativaCuandoNoHayBaseUrl() throws Exception {
        service = new DecoracionAdminService(decoracionRepository,
                decoracionZonaRepository,
                zonaRepository,
                tempDirectory.toString(),
                "");

        Decoracion decoracion = new Decoracion();
        decoracion.setDecoracionId(1L);
        decoracion.setDecoracionImagenUrl(null);
        when(decoracionRepository.findById(1L)).thenReturn(Optional.of(decoracion));
        when(decoracionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile archivo = new MockMultipartFile(
                "imagen", "foto.jpeg", "image/jpeg", "contenido".getBytes());

        String url = service.guardarImagenDecoracion(1L, archivo);

        assertThat(url).doesNotStartWith("file:");
        assertThat(url).contains("decoracion-");
        assertThat(decoracion.getDecoracionImagenUrl()).isEqualTo(url);
    }

    @Test
    void guardarImagenDecoracion_extensionNoPermitida_lanzaBusinessException() {
        MockMultipartFile archivo = new MockMultipartFile(
                "imagen", "foto.gif", "image/gif", "contenido".getBytes());

        assertThatThrownBy(() -> service.guardarImagenDecoracion(2L, archivo))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Solo se aceptan imágenes JPG, PNG o WEBP");
    }

    @Test
    void borrarImagenDecoracion_eliminaUrlYArchivoExistente() throws Exception {
        Decoracion decoracion = new Decoracion();
        decoracion.setDecoracionId(3L);
        decoracion.setDecoracionImagenUrl("https://cdn.example.com/decoraciones/imagen-antigua.png");
        when(decoracionRepository.findById(3L)).thenReturn(Optional.of(decoracion));
        when(decoracionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Path oldFile = tempDirectory.resolve("imagen-antigua.png");
        Files.write(oldFile, "contenido".getBytes());

        service.borrarImagenDecoracion(3L);

        assertThat(decoracion.getDecoracionImagenUrl()).isNull();
        assertThat(Files.exists(oldFile)).isFalse();
    }

    @Test
    void borrarImagenDecoracion_urlExternaNoFalla() {
        Decoracion decoracion = new Decoracion();
        decoracion.setDecoracionId(4L);
        decoracion.setDecoracionImagenUrl("https://picsum.photos/seed/decor/360/220");
        when(decoracionRepository.findById(4L)).thenReturn(Optional.of(decoracion));
        when(decoracionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.borrarImagenDecoracion(4L);

        assertThat(decoracion.getDecoracionImagenUrl()).isNull();
    }

    @Test
    void guardarImagenDecoracion_decoracionNoExiste_lanzaResourceNotFoundException() {
        when(decoracionRepository.findById(99L)).thenReturn(Optional.empty());
        MockMultipartFile archivo = new MockMultipartFile(
                "imagen", "foto.png", "image/png", "contenido".getBytes());

        assertThatThrownBy(() -> service.guardarImagenDecoracion(99L, archivo))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void guardarImagenDecoracion_webpEsValido() throws Exception {
        Decoracion decoracion = new Decoracion();
        decoracion.setDecoracionId(10L);
        decoracion.setDecoracionImagenUrl(null);
        when(decoracionRepository.findById(10L)).thenReturn(Optional.of(decoracion));
        when(decoracionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile archivo = new MockMultipartFile(
                "imagen", "foto.webp", "image/webp", "contenido".getBytes());

        String url = service.guardarImagenDecoracion(10L, archivo);

        assertThat(url).startsWith("https://cdn.example.com/decoraciones/");
        assertThat(decoracion.getDecoracionImagenUrl()).isEqualTo(url);
    }

    @Test
    void crearDecoracion_guardaDecoracionYZonas() {
        CrearDecoracionRequest request = CrearDecoracionRequest.builder()
                .decoracionNombre("Romántica")
                .decoracionCostoAdicional(null)
                .zonaIds(List.of(1L, 2L))
                .build();

        Decoracion saved = new Decoracion();
        saved.setDecoracionId(5L);
        when(decoracionRepository.save(any())).thenAnswer(invocation -> {
            Decoracion arg = invocation.getArgument(0);
            arg.setDecoracionId(5L);
            return arg;
        });

        Zona zona1 = Zona.builder().zonaId(1L).build();
        Zona zona2 = Zona.builder().zonaId(2L).build();
        when(zonaRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(zona1, zona2));
        when(decoracionZonaRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(decoracionZonaRepository.findByDecoracionId(5L)).thenReturn(List.of(
                DecoracionZona.builder().decoracionId(5L).zonaId(1L).build(),
                DecoracionZona.builder().decoracionId(5L).zonaId(2L).build()
        ));

        DecoracionAdminResponse response = service.crearDecoracion(request);

        assertThat(response.getDecoracionId()).isEqualTo(5L);
        assertThat(response.getDecoracionNombre()).isEqualTo("Romántica");
        assertThat(response.getDecoracionCostoAdicional()).isNull();
        assertThat(response.getZonaIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void actualizarDecoracion_cambiaNombreCostoYZonas() {
        ActualizarDecoracionRequest request = ActualizarDecoracionRequest.builder()
                .decoracionNombre("Familiar")
                .decoracionCostoAdicional(null)
                .zonaIds(List.of(3L))
                .build();

        Decoracion decoracion = new Decoracion();
        decoracion.setDecoracionId(6L);
        decoracion.setDecoracionNombre("Vieja");
        decoracion.setDecoracionCostoAdicional(null);
        decoracion.setDecoracionEstado(EstadoGenerico.ACTIVO);
        when(decoracionRepository.findById(6L)).thenReturn(Optional.of(decoracion));
        when(decoracionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Zona zona = Zona.builder().zonaId(3L).build();
        when(zonaRepository.findAllById(List.of(3L))).thenReturn(List.of(zona));
        when(decoracionZonaRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(decoracionZonaRepository.findByDecoracionId(6L)).thenReturn(List.of(
                DecoracionZona.builder().decoracionId(6L).zonaId(3L).build()
        ));

        DecoracionAdminResponse response = service.actualizarDecoracion(6L, request);

        assertThat(response.getDecoracionNombre()).isEqualTo("Familiar");
        assertThat(response.getZonaIds()).containsExactly(3L);
    }

    @Test
    void eliminarDecoracion_borraDecoracionYZonas() {
        Decoracion decoracion = new Decoracion();
        decoracion.setDecoracionId(7L);
        decoracion.setDecoracionImagenUrl(null);
        when(decoracionRepository.findById(7L)).thenReturn(Optional.of(decoracion));

        service.eliminarDecoracion(7L);

        assertThat(decoracion.getDecoracionImagenUrl()).isNull();
    }
}
