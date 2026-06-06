package co.edu.unicauca.backend.modules.reservas.service;

import co.edu.unicauca.backend.modules.reservas.entity.Decoracion;
import co.edu.unicauca.backend.modules.reservas.repository.DecoracionRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecoracionAdminServiceTest {

    @Mock
    private DecoracionRepository decoracionRepository;

    private Path tempDirectory;
    private DecoracionAdminService service;

    @BeforeEach
    void setUp() throws Exception {
        tempDirectory = Files.createTempDirectory("decoracion-imagenes-test");
        service = new DecoracionAdminService(decoracionRepository,
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
                .hasMessageContaining("Solo se aceptan imágenes JPG o PNG");
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
}
