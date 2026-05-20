package co.edu.unicauca.backend.modules.usuarios.service;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRepository;
import co.edu.unicauca.backend.modules.auth.repository.SesionRepository;
import co.edu.unicauca.backend.modules.auth.repository.UsuarioRolRepository;
import co.edu.unicauca.backend.modules.usuarios.dto.request.CrearEmpleadoRequest;
import co.edu.unicauca.backend.modules.usuarios.dto.response.EmpleadoResponse;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.modules.usuarios.entity.UsuarioRol;
import co.edu.unicauca.backend.modules.usuarios.dto.response.EmpleadoListadoResponse;
import co.edu.unicauca.backend.modules.usuarios.repository.ClienteRepository;
import co.edu.unicauca.backend.modules.usuarios.repository.EmpleadoRepository;
import co.edu.unicauca.backend.shared.enums.RolEstado;
import co.edu.unicauca.backend.shared.enums.RolNombre;
import co.edu.unicauca.backend.shared.exception.BusinessException;
import co.edu.unicauca.backend.shared.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class EmpleadoService {

    private final UsuarioRepository usuarioRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final SesionRepository sesionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    public EmpleadoResponse crearEmpleado(CrearEmpleadoRequest request) {
        validarPassword(request.getPassword(), request.getPasswordConfirmacion());
        validarFechaIngreso(request.getFechaIngreso());
        Set<RolNombre> roles = parsearRoles(request.getRoles());
        validarRolesPermitidos(roles);

        String correo = request.getCorreoElectronico().toLowerCase(Locale.ENGLISH);
        validarTelefono(request.getTelefono());
        verificarDuplicados(request, correo);

        Optional<Usuario> usuarioOptional = usuarioRepository.findByUsuarioEmail(correo);
        boolean clienteExisteConMismoEmail = false;
        Usuario usuario;

        if (usuarioOptional.isPresent()) {
            usuario = usuarioOptional.get();
            clienteExisteConMismoEmail = clienteRepository.findByUsuario_UsuarioEmail(correo).isPresent();
            if (empleadoRepository.findByUsuario_UsuarioEmail(correo).isPresent()) {
                throw new BusinessException(ErrorCode.ENTITY_ALREADY_EXISTS,
                        "Ya existe un empleado registrado con este correo electrónico", HttpStatus.CONFLICT);
            }
            if (!clienteExisteConMismoEmail) {
                throw new BusinessException(ErrorCode.ENTITY_ALREADY_EXISTS,
                        "Ya existe una cuenta con este correo electrónico", HttpStatus.CONFLICT);
            }
        } else {
            usuario = Usuario.builder()
                    .usuarioEmail(correo)
                    .usuarioPassword(passwordEncoder.encode(request.getPassword()))
                    .build();
            usuario = Objects.requireNonNull(usuarioRepository.save(usuario));
        }

        if (usuarioOptional.isPresent()) {
            usuario.setUsuarioPassword(passwordEncoder.encode(request.getPassword()));
            usuarioRepository.save(usuario);
        }

        Empleado empleado = Empleado.builder()
                .usuario(usuario)
                .empleadoNombre(request.getNombre())
                .empleadoDireccion(request.getDireccion())
                .empleadoTelefono(request.getTelefono())
                .empleadoFechaIngreso(request.getFechaIngreso())
                .build();
        empleado = Objects.requireNonNull(empleadoRepository.save(empleado));

        final Long usuarioId = Objects.requireNonNull(usuario.getUsuarioId());
        final Usuario usuarioFinal = usuario;
        List<UsuarioRol> usuarioRoles = Objects.requireNonNull(roles.stream()
                .map(rol -> UsuarioRol.builder()
                        .usuarioId(usuarioId)
                        .rolNombre(rol)
                        .rolEstado(RolEstado.ACTIVO)
                        .usuario(usuarioFinal)
                        .build())
                .collect(Collectors.toList()));
        usuarioRolRepository.saveAll(usuarioRoles);

        boolean emailEnviado = enviarCorreoBienvenida(request, correo);
        String warning = null;
        if (!emailEnviado) {
            warning = "No se pudo enviar el correo de bienvenida";
        }
        if (clienteExisteConMismoEmail) {
            warning = "⚠️ Este correo electrónico pertenece a un cliente registrado. ¿Deseas crear un empleado con el mismo correo?";
        }

        return EmpleadoResponse.builder()
                .empleadoId(empleado.getUsuarioId())
                .nombre(empleado.getEmpleadoNombre())
                .correoElectronico(usuario.getUsuarioEmail())
                .telefono(empleado.getEmpleadoTelefono())
                .direccion(empleado.getEmpleadoDireccion())
                .fechaIngreso(empleado.getEmpleadoFechaIngreso())
                .roles(roles.stream().map(Enum::name).collect(Collectors.toList()))
                .warning(warning)
                .build();
    }

    public List<EmpleadoListadoResponse> listarEmpleados(String rol, String estado, String nombre) {
        List<Empleado> empleados = empleadoRepository.findAll();
        if (empleados.isEmpty()) {
            return List.of();
        }

        List<Long> usuarioIds = empleados.stream()
                .map(Empleado::getUsuarioId)
                .collect(Collectors.toList());

        Map<Long, List<UsuarioRol>> rolesPorUsuario = usuarioRolRepository.findByUsuarioIdIn(usuarioIds).stream()
                .collect(Collectors.groupingBy(UsuarioRol::getUsuarioId));

        RolNombre rolNombre = parsearRolFiltro(rol);
        RolEstado rolEstado = parsearEstadoFiltro(estado);
        String nombreBusqueda = getNombreBusqueda(nombre);

        return empleados.stream()
                .filter(empleado -> aplicarFiltros(empleado, rolesPorUsuario.getOrDefault(empleado.getUsuarioId(), List.of()), rolNombre, rolEstado, nombreBusqueda))
                .map(empleado -> mapToEmpleadoListado(empleado, rolesPorUsuario.getOrDefault(empleado.getUsuarioId(), List.of())))
                .collect(Collectors.toList());
    }

    public String cambiarEstadoEmpleado(Long empleadoId, String estado) {
        Empleado empleado = empleadoRepository.findById(empleadoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND,
                        "Empleado no encontrado", HttpStatus.NOT_FOUND));

        List<UsuarioRol> roles = usuarioRolRepository.findByUsuarioId(empleadoId);
        if (roles.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "Este empleado no tiene roles asociados", HttpStatus.BAD_REQUEST);
        }

        RolEstado nuevoEstado = parsearEstadoFiltro(estado);
        boolean yaEstaEnEstado = roles.stream().allMatch(rol -> rol.getRolEstado() == nuevoEstado);
        if (yaEstaEnEstado) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "El empleado ya se encuentra " + nuevoEstado.getDescripcion().toLowerCase(Locale.ENGLISH),
                    HttpStatus.BAD_REQUEST);
        }

        int updatedRoles = usuarioRolRepository.updateRolEstadoByUsuarioId(empleadoId, nuevoEstado);
        if (updatedRoles == 0) {
            throw new BusinessException(ErrorCode.INVALID_STATE,
                    "No se pudo actualizar el estado de los roles del empleado", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (nuevoEstado == RolEstado.INACTIVO) {
            sesionRepository.deactivateActiveSessionsByUsuarioId(empleadoId);
            return "Empleado deshabilitado correctamente";
        }

        return "Empleado habilitado correctamente";
    }

    private boolean aplicarFiltros(Empleado empleado, List<UsuarioRol> roles, RolNombre rolNombre,
                                   RolEstado rolEstado, String nombreBusqueda) {
        if (rolNombre != null && roles.stream().noneMatch(rol -> rol.getRolNombre() == rolNombre)) {
            return false;
        }

        if (rolEstado != null) {
            boolean tieneRolesActivos = roles.stream().anyMatch(rol -> rol.getRolEstado() == RolEstado.ACTIVO);
            if (rolEstado == RolEstado.ACTIVO && !tieneRolesActivos) {
                return false;
            }
            if (rolEstado == RolEstado.INACTIVO && tieneRolesActivos) {
                return false;
            }
        }

        if (nombreBusqueda != null && !empleado.getEmpleadoNombre().toLowerCase(Locale.ENGLISH).contains(nombreBusqueda)) {
            return false;
        }

        return true;
    }

    private String getNombreBusqueda(String nombre) {
        return nombre == null || nombre.isBlank() ? null : nombre.trim().toLowerCase(Locale.ENGLISH);
    }

    private RolNombre parsearRolFiltro(String rol) {
        if (rol == null || rol.isBlank()) {
            return null;
        }
        try {
            return RolNombre.valueOf(rol.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Rol inválido: " + rol, HttpStatus.BAD_REQUEST);
        }
    }

    private RolEstado parsearEstadoFiltro(String estado) {
        if (estado == null || estado.isBlank()) {
            return null;
        }
        try {
            return RolEstado.valueOf(estado.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Estado inválido: " + estado, HttpStatus.BAD_REQUEST);
        }
    }

    private EmpleadoListadoResponse mapToEmpleadoListado(Empleado empleado, List<UsuarioRol> roles) {
        boolean activo = roles.stream().anyMatch(rol -> rol.getRolEstado() == RolEstado.ACTIVO);
        List<String> nombresRoles = roles.stream()
                .map(rol -> rol.getRolNombre().name())
                .collect(Collectors.toList());

        return EmpleadoListadoResponse.builder()
                .empleadoId(empleado.getUsuarioId())
                .nombre(empleado.getEmpleadoNombre())
                .correoElectronico(empleado.getUsuario().getUsuarioEmail())
                .telefono(empleado.getEmpleadoTelefono())
                .direccion(empleado.getEmpleadoDireccion())
                .fechaIngreso(empleado.getEmpleadoFechaIngreso())
                .roles(nombresRoles)
                .estado(activo ? RolEstado.ACTIVO.getDescripcion() : RolEstado.INACTIVO.getDescripcion())
                .build();
    }

    private void validarPassword(String password, String confirmacion) {
        if (!password.equals(confirmacion)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Las contraseñas no coinciden", HttpStatus.BAD_REQUEST);
        }
    }

    private void validarFechaIngreso(LocalDate fechaIngreso) {
        if (fechaIngreso.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "La fecha de ingreso no puede ser futura", HttpStatus.BAD_REQUEST);
        }
    }

    private void validarTelefono(String telefono) {
        if (!telefono.matches("^[0-9]{10}$")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Ingresa un número de teléfono válido (10 dígitos)", HttpStatus.BAD_REQUEST);
        }
    }

    private Set<RolNombre> parsearRoles(List<String> roles) {
        return roles.stream()
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(String::toUpperCase)
                .map(this::toRolNombre)
                .collect(Collectors.toSet());
    }

    private RolNombre toRolNombre(String rol) {
        try {
            return RolNombre.valueOf(rol);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Rol inválido: " + rol, HttpStatus.BAD_REQUEST);
        }
    }

    private void validarRolesPermitidos(Set<RolNombre> roles) {
        if (roles.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "El rol es obligatorio", HttpStatus.BAD_REQUEST);
        }
        if (roles.contains(RolNombre.CLIENTE)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "No es válido asignar el rol CLIENTE a un empleado", HttpStatus.BAD_REQUEST);
        }
    }

    private void verificarDuplicados(CrearEmpleadoRequest request, String correo) {
        if (empleadoRepository.existsByEmpleadoTelefono(request.getTelefono())) {
            throw new BusinessException(ErrorCode.ENTITY_ALREADY_EXISTS,
                    "Ya existe un empleado registrado con este número de teléfono", HttpStatus.CONFLICT);
        }
        if (usuarioRepository.findByUsuarioEmail(correo).isPresent() &&
                empleadoRepository.findByUsuario_UsuarioEmail(correo).isEmpty() &&
                !clienteRepository.findByUsuario_UsuarioEmail(correo).isPresent()) {
            throw new BusinessException(ErrorCode.ENTITY_ALREADY_EXISTS,
                    "Ya existe una cuenta con este correo electrónico", HttpStatus.CONFLICT);
        }
    }

    private boolean enviarCorreoBienvenida(CrearEmpleadoRequest request, String correo) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(correo);
            mail.setSubject("Bienvenido a Al Toro Gastrobar");
            mail.setText("Hola " + request.getNombre() + ",\n\n" +
                    "Tu cuenta en Al Toro Gastrobar ha sido creada exitosamente.\n" +
                    "Tu correo de usuario es: " + correo + "\n" +
                    "Por seguridad, tu contraseña temporal fue registrada en el sistema.\n\n" +
                    "Por favor cambia tu contraseña en el primer inicio de sesión.\n\n" +
                    "Saludos,\nAl Toro Gastrobar");
            mailSender.send(mail);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
