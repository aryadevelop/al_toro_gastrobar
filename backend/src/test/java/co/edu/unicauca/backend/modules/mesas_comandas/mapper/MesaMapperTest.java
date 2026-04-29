package co.edu.unicauca.backend.modules.mesas_comandas.mapper;

import co.edu.unicauca.backend.modules.auth.entity.Usuario;
import co.edu.unicauca.backend.modules.mesas_comandas.dto.response.*;
import co.edu.unicauca.backend.modules.mesas_comandas.entity.*;
import co.edu.unicauca.backend.modules.notificaciones.entity.Notificacion;
import co.edu.unicauca.backend.modules.produccion.entity.CategoriaCarta;
import co.edu.unicauca.backend.modules.produccion.entity.Producto;
import co.edu.unicauca.backend.modules.reservas.entity.Reserva;
import co.edu.unicauca.backend.modules.usuarios.entity.Cliente;
import co.edu.unicauca.backend.modules.usuarios.entity.Empleado;
import co.edu.unicauca.backend.shared.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MesaMapper")
class MesaMapperTest {

    private final MesaMapper mapper = new MesaMapper();

    @Test
    @DisplayName("toNotificacionActivaResponse mapea correctamente")
    void toNotificacionActivaResponse_DeberiaMapearCorrectamente() {
        // Arrange
        LocalDateTime fechaHora = LocalDateTime.of(2026, 4, 29, 19, 30);
        Notificacion notificacion = Notificacion.builder()
                .notificacionId(1L)
                .notificacionTipo(TipoNotificacion.ATENCION)
                .notificacionFechaHora(fechaHora)
                .build();

        // Act
        NotificacionActivaResponse response = mapper.toNotificacionActivaResponse(notificacion);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getNotificacionId()).isEqualTo(1L);
        assertThat(response.getTipo()).isEqualTo("ATENCION");  // Enum → String
        assertThat(response.getFechaHora()).isEqualTo(fechaHora);
    }

    @Test
    @DisplayName("toMesaMapaResponse con mesa propia retorna nombreMesero null")
    void toMesaMapaResponse_MesaPropia_NombreMeseroNull() {
        // Arrange
        Mesa mesa = crearMesa("T-01", "mesero1@altoro.com", "Juan Pérez");
        List<Notificacion> notificaciones = List.of();

        // Act
        MesaMapaResponse response = mapper.toMesaMapaResponse(mesa, notificaciones, false, "mesero1@altoro.com");

        // Assert
        assertThat(response.getNombreMesero()).isNull();  // RN-04: mesa propia
        assertThat(response.getEmailMesero()).isEqualTo("mesero1@altoro.com");
        assertThat(response.getEsMesaPropia()).isTrue();
        assertThat(response.getTieneBorrador()).isFalse();
    }

    @Test
    @DisplayName("toMesaMapaResponse con mesa ajena retorna nombreMesero")
    void toMesaMapaResponse_MesaAjena_NombreMeseroPresente() {
        // Arrange
        Mesa mesa = crearMesa("T-02", "mesero1@altoro.com", "Juan Pérez");
        Notificacion notif = Notificacion.builder()
                .notificacionId(1L)
                .notificacionTipo(TipoNotificacion.PLATOS_LISTOS)
                .notificacionFechaHora(LocalDateTime.now())
                .build();

        // Act
        MesaMapaResponse response = mapper.toMesaMapaResponse(mesa, List.of(notif), true, "mesero2@altoro.com");

        // Assert
        assertThat(response.getNombreMesero()).isEqualTo("Juan Pérez");  // RN-04: mesa ajena
        assertThat(response.getEsMesaPropia()).isFalse();
        assertThat(response.getTieneBorrador()).isTrue();
        assertThat(response.getNotificacionesActivas()).hasSize(1);
    }

    @Test
    @DisplayName("agruparItemsEnProduccion agrupa items EN_PREPARACION con mismo nombre y estado")
    void agruparItemsEnProduccion_AgrupaCorrectamente() {
        // Arrange
        ComandaItem item1 = crearItem("Bandeja Paisa", "Sin frijoles", 2, EstadoComanda.EN_PREPARACION, CategoriaProducto.PLATO);
        ComandaItem item2 = crearItem("Bandeja Paisa", "Sin frijoles", 1, EstadoComanda.EN_PREPARACION, CategoriaProducto.PLATO);

        // Act
        List<ItemComandaEnProduccionResponse> resultado = mapper.agruparItemsEnProduccion(List.of(item1, item2));

        // Assert
        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCantidad()).isEqualTo(3);  // 2 + 1
        assertThat(resultado.get(0).getNombreProducto()).isEqualTo("Bandeja Paisa");
    }

    @Test
    @DisplayName("agruparItemsEnProduccion NO agrupa items PENDIENTE")
    void agruparItemsEnProduccion_NoAgrupaPendientes() {
        // Arrange
        ComandaItem item1 = crearItem("Limonada", null, 2, EstadoComanda.PENDIENTE, CategoriaProducto.BEBIDA);
        ComandaItem item2 = crearItem("Limonada", null, 1, EstadoComanda.PENDIENTE, CategoriaProducto.BEBIDA);

        // Act
        List<ItemComandaEnProduccionResponse> resultado = mapper.agruparItemsEnProduccion(List.of(item1, item2));

        // Assert - RN-06: PENDIENTE no se agrupa
        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getCantidad()).isEqualTo(2);
        assertThat(resultado.get(1).getCantidad()).isEqualTo(1);
    }

    @Test
    @DisplayName("toMesaDetalleResponse mapea correctamente con cliente y reserva")
    void toMesaDetalleResponse_ConClienteYReserva() {
        // Arrange
        Mesa mesa = crearMesaConClienteYReserva();
        List<ItemComandaEnProduccionResponse> items = List.of(
                ItemComandaEnProduccionResponse.builder()
                        .nombreProducto("Bandeja Paisa")
                        .cantidad(2)
                        .build()
        );

        // Act
        MesaDetalleResponse response = mapper.toMesaDetalleResponse(mesa, items);

        // Assert
        assertThat(response.getNombreCliente()).isEqualTo("María López");
        assertThat(response.getNotasReserva()).isEqualTo("Sin cebolla");
        assertThat(response.getItemsComanda()).hasSize(1);
    }

    @Test
    @DisplayName("toMesaItemsProduccionResponse mapea correctamente")
    void toMesaItemsProduccionResponse_MapeaCorrectamente() {
        // Arrange
        List<ItemComandaEnProduccionResponse> items = List.of(
                ItemComandaEnProduccionResponse.builder()
                        .nombreProducto("Limonada")
                        .cantidad(3)
                        .build()
        );

        // Act
        MesaItemsProduccionResponse response = mapper.toMesaItemsProduccionResponse("T-05", items);

        // Assert
        assertThat(response.getIdentificadorMesa()).isEqualTo("T-05");
        assertThat(response.getItemsEnProduccion()).hasSize(1);
    }

    // Helpers

    private Mesa crearMesa(String identificador, String emailMesero, String nombreMesero) {
        Usuario usuario = new Usuario();
        usuario.setUsuarioEmail(emailMesero);

        Empleado empleado = new Empleado();
        empleado.setUsuario(usuario);
        empleado.setEmpleadoNombre(nombreMesero);

        Visita visita = new Visita();
        visita.setVisitaId(1L);
        visita.setVisitaFechaHoraInicio(LocalDateTime.now());

        Mesa mesa = new Mesa();
        mesa.setVisitaId(1L);
        mesa.setMesaIdentificador(identificador);
        mesa.setMesaNumeroPersonas(4);
        mesa.setMesaEstado(EstadoMesa.EN_PREPARACION);
        mesa.setMesero(empleado);
        mesa.setVisita(visita);

        return mesa;
    }

    private Mesa crearMesaConClienteYReserva() {
        Usuario usuario = new Usuario();
        usuario.setUsuarioEmail("mesero1@altoro.com");

        Empleado empleado = new Empleado();
        empleado.setUsuario(usuario);
        empleado.setEmpleadoNombre("Juan Pérez");

        Cliente cliente = new Cliente();
        cliente.setClienteNombre("María López");

        Reserva reserva = new Reserva();
        reserva.setReservaNotas("Sin cebolla");

        Visita visita = new Visita();
        visita.setVisitaId(1L);
        visita.setVisitaFechaHoraInicio(LocalDateTime.now());
        visita.setCliente(cliente);
        visita.setReserva(reserva);

        Mesa mesa = new Mesa();
        mesa.setVisitaId(1L);
        mesa.setMesaIdentificador("T-10");
        mesa.setMesaNumeroPersonas(2);
        mesa.setMesaEstado(EstadoMesa.ATENDIDA);
        mesa.setMesero(empleado);
        mesa.setVisita(visita);

        return mesa;
    }

    private ComandaItem crearItem(String nombreProducto, String descripcion, int cantidad, EstadoComanda estadoComanda, CategoriaProducto categoria) {
        CategoriaCarta categoriaCarta = new CategoriaCarta();
        categoriaCarta.setCategoriacartaId(1);

        Producto producto = new Producto();
        producto.setProductoId(1L);
        producto.setProductoNombre(nombreProducto);
        producto.setProductoCategoria(categoria);
        producto.setCategoriaCarta(categoriaCarta);

        Comanda comanda = new Comanda();
        comanda.setComandaId(1L);
        comanda.setComandaEstado(estadoComanda);

        ComandaItem item = new ComandaItem();
        item.setComandaItemId(1L);
        item.setProducto(producto);
        item.setComandaItemDescripcion(descripcion);
        item.setComandaItemCantidad(cantidad);
        item.setComanda(comanda);

        return item;
    }
}
