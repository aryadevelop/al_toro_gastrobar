package co.edu.unicauca.backend.modules.usuarios.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRepository;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRolRepository;
import co.edu.unicauca.backend.modules.usuarios.dto.request.CrearEmpleadoRequest;
import co.edu.unicauca.backend.modules.usuarios.dto.response.EmpleadoResponse;
import co.edu.unicauca.backend.modules.usuarios.dto.response.EmpleadoListadoResponse;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.entity.UsuarioRol;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.enums.RolEstado;
import co.edu.unicauca.backend.shared.enums.RolNombre;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmpleadoServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EmpleadoRepository empleadoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioRolRepository usuarioRolRepository;
    @Mock private SesionRepository sesionRepository;
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
        void creaEmpleadoYAsignaRoles() {
            when(usuarioRepository.findByUsuarioEmail(anyString())).thenReturn(Optional.empty());
            when(empleadoRepository.existsByEmpleadoTelefono(anyString())).thenReturn(false);
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

    @Test
    void listarEmpleados_filtraPorRolYNombreYEstado() {
        Usuario usuario = Usuario.builder()
                .usuarioId(1L)
                .usuarioEmail("ana@altoro.com")
                .build();

        when(empleadoRepository.findAll()).thenReturn(List.of(
                Empleado.builder()
                        .usuarioId(1L)
                        .usuario(usuario)
                        .empleadoNombre("Ana Rivera")
                        .empleadoTelefono("3001234567")
                        .empleadoDireccion("Calle 15")
                        .empleadoFechaIngreso(LocalDate.now())
                        .build()
        ));

        when(usuarioRolRepository.findByUsuarioIdIn(List.of(1L))).thenReturn(List.of(
                UsuarioRol.builder()
                        .usuarioId(1L)
                        .rolNombre(RolNombre.MESERO)
                        .rolEstado(RolEstado.ACTIVO)
                        .build()
        ));

        List<EmpleadoListadoResponse> result = empleadoService.listarEmpleados("mesero", "activo", "Ana");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombre()).isEqualTo("Ana Rivera");
        assertThat(result.get(0).getRoles()).containsExactly("MESERO");
        assertThat(result.get(0).getEstado()).isEqualTo("Activo");
    }

    @Test
    void listarEmpleados_filtraPorEstadoInactivo() {
        Usuario activoUsuario = Usuario.builder()
                .usuarioId(1L)
                .usuarioEmail("activo@altoro.com")
                .build();
        Usuario inactivoUsuario = Usuario.builder()
                .usuarioId(2L)
                .usuarioEmail("inactivo@altoro.com")
                .build();

        when(empleadoRepository.findAll()).thenReturn(List.of(
                Empleado.builder()
                        .usuarioId(1L)
                        .usuario(activoUsuario)
                        .empleadoNombre("Activo Empleado")
                        .empleadoTelefono("3001111111")
                        .empleadoDireccion("Avenida 1")
                        .empleadoFechaIngreso(LocalDate.now())
                        .build(),
                Empleado.builder()
                        .usuarioId(2L)
                        .usuario(inactivoUsuario)
                        .empleadoNombre("Inactivo Empleado")
                        .empleadoTelefono("3002222222")
                        .empleadoDireccion("Avenida 2")
                        .empleadoFechaIngreso(LocalDate.now())
                        .build()
        ));

        when(usuarioRolRepository.findByUsuarioIdIn(List.of(1L, 2L))).thenReturn(List.of(
                UsuarioRol.builder()
                        .usuarioId(1L)
                        .rolNombre(RolNombre.MESERO)
                        .rolEstado(RolEstado.ACTIVO)
                        .build(),
                UsuarioRol.builder()
                        .usuarioId(2L)
                        .rolNombre(RolNombre.CAJERO)
                        .rolEstado(RolEstado.INACTIVO)
                        .build()
        ));

        List<EmpleadoListadoResponse> result = empleadoService.listarEmpleados(null, "inactivo", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombre()).isEqualTo("Inactivo Empleado");
        assertThat(result.get(0).getEstado()).isEqualTo("Inactivo");
    }

    @Test
    void listarEmpleados_buscaPorNombreParcial() {
        Usuario usuario = Usuario.builder()
                .usuarioId(3L)
                .usuarioEmail("carlos@altoro.com")
                .build();

        when(empleadoRepository.findAll()).thenReturn(List.of(
                Empleado.builder()
                        .usuarioId(3L)
                        .usuario(usuario)
                        .empleadoNombre("Carlos Pérez")
                        .empleadoTelefono("3003333333")
                        .empleadoDireccion("Calle 33")
                        .empleadoFechaIngreso(LocalDate.now())
                        .build()
        ));

        when(usuarioRolRepository.findByUsuarioIdIn(List.of(3L))).thenReturn(List.of(
                UsuarioRol.builder()
                        .usuarioId(3L)
                        .rolNombre(RolNombre.CAJERO)
                        .rolEstado(RolEstado.ACTIVO)
                        .build()
        ));

        List<EmpleadoListadoResponse> result = empleadoService.listarEmpleados(null, null, "car");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getNombre()).isEqualTo("Carlos Pérez");
    }

    @Test
    void cambiarEstadoEmpleado_desactivaYcierraSesiones() {
        Usuario usuario = Usuario.builder()
                .usuarioId(5L)
                .usuarioEmail("juan@altoro.com")
                .build();
        Empleado empleado = Empleado.builder()
                .usuarioId(5L)
                .usuario(usuario)
                .empleadoNombre("Juan Pérez")
                .empleadoTelefono("3005555555")
                .empleadoDireccion("Calle 55")
                .empleadoFechaIngreso(LocalDate.now())
                .build();

        when(empleadoRepository.findById(5L)).thenReturn(Optional.of(empleado));
        when(usuarioRolRepository.findByUsuarioId(5L)).thenReturn(List.of(
                UsuarioRol.builder()
                        .usuarioId(5L)
                        .rolNombre(RolNombre.CAJERO)
                        .rolEstado(RolEstado.ACTIVO)
                        .build()
        ));
        when(usuarioRolRepository.updateRolEstadoByUsuarioId(5L, RolEstado.INACTIVO)).thenReturn(1);
        when(sesionRepository.deactivateActiveSessionsByUsuarioId(5L)).thenReturn(1);

        String mensaje = empleadoService.cambiarEstadoEmpleado(5L, "INACTIVO");

        assertThat(mensaje).isEqualTo("Empleado deshabilitado correctamente");
        verify(sesionRepository).deactivateActiveSessionsByUsuarioId(5L);
    }
}
