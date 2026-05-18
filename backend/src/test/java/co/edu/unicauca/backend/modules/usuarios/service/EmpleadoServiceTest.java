package co.edu.unicauca.backend.modules.usuarios.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRepository;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRolRepository;
import co.edu.unicauca.backend.modules.usuarios.dto.request.CrearEmpleadoRequest;
import co.edu.unicauca.backend.modules.usuarios.dto.response.EmpleadoResponse;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.mail.SimpleMailMessage;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpleadoServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioRolRepository usuarioRolRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JavaMailSender mailSender;

    @InjectMocks private EmpleadoService empleadoService;

    private CrearEmpleadoRequest request;

    @BeforeEach
    void setUp() {
        request = CrearEmpleadoRequest.builder()
                .nombre("Juan Perez")
                .correoElectronico("juan@altoro.com")
                .telefono("3001234567")
                .direccion("Calle 123")
                .roles(List.of("CAJERO"))
                .fechaIngreso(LocalDate.now())
                .password("Password1!")
                .passwordConfirmacion("Password1!")
                .build();
    }

    @Nested
    @DisplayName("crearEmpleado")
    class CrearEmpleado {

        @Test
        @DisplayName("Crea el empleado y asigna roles")
        @SuppressWarnings("null")
        void creaEmpleadoYAsignaRoles() {
            when(usuarioRepository.findByUsuarioEmail(anyString())).thenReturn(Optional.empty());
            when(empleadoRepository.existsByEmpleadoTelefono(anyString())).thenReturn(false);
            when(clienteRepository.findByUsuario_UsuarioEmail(anyString())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
            when(usuarioRepository.save(Mockito.<Usuario>any())).thenAnswer(invocation -> {
                Usuario usuario = Objects.requireNonNull(invocation.getArgument(0, Usuario.class));
                usuario.setUsuarioId(1L);
                return usuario;
            });
            when(empleadoRepository.save(Mockito.<Empleado>any())).thenAnswer(invocation -> {
                Empleado empleado = Objects.requireNonNull(invocation.getArgument(0, Empleado.class));
                empleado.setUsuarioId(1L);
                return empleado;
            });
            doNothing().when(mailSender).send(Mockito.<SimpleMailMessage>any());

            EmpleadoResponse response = empleadoService.crearEmpleado(request);

            assertThat(response).isNotNull();
            assertThat(response.getEmpleadoId()).isEqualTo(1L);
            assertThat(response.getNombre()).isEqualTo("Juan Perez");
            assertThat(response.getCorreoElectronico()).isEqualTo("juan@altoro.com");
            assertThat(response.getRoles()).containsExactly("CAJERO");
            assertThat(response.getWarning()).isNull();
        }

        @Test
        @DisplayName("Teléfono duplicado arroja conflicto")
        void telefonoDuplicadoLanzaExcepcion() {
            when(empleadoRepository.existsByEmpleadoTelefono(anyString())).thenReturn(true);

            assertThatThrownBy(() -> empleadoService.crearEmpleado(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Ya existe un empleado registrado con este número de teléfono");
        }

        @Test
        @DisplayName("Contraseñas distintas arrojan validación")
        void passwordsDistintasLanzanValidacion() {
            request = CrearEmpleadoRequest.builder()
                    .nombre("Juan Perez")
                    .correoElectronico("juan@altoro.com")
                    .telefono("3001234567")
                    .direccion("Calle 123")
                    .roles(List.of("CAJERO"))
                    .fechaIngreso(LocalDate.now())
                    .password("Password1!")
                    .passwordConfirmacion("Password2!")
                    .build();

            assertThatThrownBy(() -> empleadoService.crearEmpleado(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Las contraseñas no coinciden");
        }
    }
}
