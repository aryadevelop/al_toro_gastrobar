import { environment } from '../../../environments/environment';

const API = environment.apiBaseUrl;

export const API_PATHS = {
    auth: {
        login: `${API}/auth/login`,
        register: `${API}/auth/register`,
        refresh: `${API}/auth/refresh`,
        me: `${API}/auth/me`,
        logout: `${API}/auth/logout`,
    },
    users: {
        me: `${API}/clientes/me`,
    },
    reservas: {
        disponibilidad: `${API}/reservas/disponibilidad`,
        crear: `${API}/reservas`,
        modificar: (reservaId: string | number) => `${API}/reservas/${reservaId}`,
        cancelar: (reservaId: string | number) => `${API}/reservas/${reservaId}/cancelar`,
        abonos: (reservaId: string | number) => `${API}/reservas/${reservaId}/abonos`,
        resumenPago: (reservaId: string | number) => `${API}/reservas/${reservaId}/resumen-pago`,
        futuras: `${API}/reservas/cliente/futuras`,
        canceladasDevueltas: `${API}/reservas/cliente/canceladas-devueltas`,
        detalle: (reservaId: string | number) => `${API}/reservas/${reservaId}/detalle`,
        meseroConsulta: `${API}/reservas/mesero/consulta`,
        marcarInasistencia: (reservaId: string | number) => `${API}/reservas/${reservaId}/marcar-inasistencia`,
    },
    visitas: {
        historial: `${API}/visitas/cliente/historial`,
        detalle: (visitaId: string | number) => `${API}/visitas/cliente/${visitaId}/detalle`,
        activa: `${API}/visitas/activa`,
        asistencia: (visitaId: string | number) => `${API}/visitas/${visitaId}/asistencia`,
    },
    productos: {
        carta: `${API}/productos/carta`,
        menuEspecial: `${API}/productos/menu-especial`,
        buscar: `${API}/productos/buscar`,
    },
    clientes: {
        misPuntos: `${API}/clientes/me/puntos`,
        me: `${API}/clientes/me`,
        updateMe: `${API}/clientes/me`,
        changePassword: `${API}/clientes/me/cambiar-contraseña`,
    },
    clientesAdmin: {
        listar: `${API}/clientes`,
        buscarNombre: `${API}/clientes/buscar/nombre`,
        buscarCorreo: `${API}/clientes/buscar/correo`,
        buscarTelefono: `${API}/clientes/buscar/telefono`,
        ventas: (clienteId: string | number) => `${API}/clientes/${clienteId}/ventas`,
        ventasResumen: (clienteId: string | number) => `${API}/clientes/${clienteId}/ventas/resumen`,
        ventasAgrupadasAnio: (clienteId: string | number) => `${API}/clientes/${clienteId}/ventas/agrupadas/anio`,
        ventasAgrupadasMes: (clienteId: string | number) => `${API}/clientes/${clienteId}/ventas/agrupadas/mes`,
        enviarRecordatorio: (clienteId: string | number) => `${API}/clientes/${clienteId}/ventas/recordatorio`,
    },
    empleados: {
        crear: `${API}/empleados`,
        listar: `${API}/empleados`,
        cambiarEstado: (empleadoId: string | number) => `${API}/empleados/${empleadoId}/estado`,
    },
    mesas: {
        mapa: `${API}/mesas`,
        asignar: `${API}/mesas`,
        detalle: (mesaId: string | number) => `${API}/mesas/${mesaId}/detalle`,
        itemsProduccion: (mesaId: string | number) => `${API}/mesas/${mesaId}/items-produccion`,
        zonasDisponibles: `${API}/mesas/zonas-disponibles`,
    },
    comandas: {
        borrador: `${API}/comandas/borrador`,
        borradorItems: `${API}/comandas/borrador/items`,
        borradorItem: (itemId: string | number) => `${API}/comandas/borrador/items/${itemId}`,
        enviar: (comandaId: string | number) => `${API}/comandas/borrador/${comandaId}/enviar`,
        notas: (comandaId: string | number) => `${API}/comandas/borrador/${comandaId}/notas`,
    },
    comandasProduccion: {
        tablero: `${API}/comandas/produccion`,
        detalle: (comandaId: string | number) => `${API}/comandas/produccion/${comandaId}`,
        iniciar: (comandaId: string | number) => `${API}/comandas/produccion/${comandaId}/iniciar`,
        listo: (comandaId: string | number) => `${API}/comandas/produccion/${comandaId}/listo`,
    },
    notificaciones: {
        atender: (notificacionId: string | number) => `${API}/notificaciones/${notificacionId}/atender`,
        servirPlatos: (notificacionId: string | number) => `${API}/notificaciones/${notificacionId}/servir-platos`,
        servirBebidas: (notificacionId: string | number) => `${API}/notificaciones/${notificacionId}/servir-bebidas`,
        atenderCambio: (notificacionId: string | number) => `${API}/notificaciones/${notificacionId}/atender-cambio`,
        cambio: `${API}/notificaciones/cambio`,
    },
    ventas: {
        detalle: (visitaId: string | number) => `${API}/ventas/${visitaId}/detalle`,
        cuenta: (visitaId: string | number) => `${API}/ventas/${visitaId}/cuenta`,
        cerrar: `${API}/ventas`,
    },
    visitasAcciones: {
        asignarCliente: (visitaId: string | number) => `${API}/visitas/${visitaId}/cliente`,
        ajustarItems: (visitaId: string | number) => `${API}/visitas/${visitaId}/items`,
    },
    clientesPuntos: {
        buscar: `${API}/clientes/buscar`,
        puntos: (clienteId: string | number) => `${API}/clientes/${clienteId}/puntos`,
        canjearPuntos: (clienteId: string | number) => `${API}/clientes/${clienteId}/canje-puntos`,
    },
    adminProductos: {
        listar: `${API}/admin/productos`,
        validarEstado: (id: string | number) => `${API}/admin/productos/${id}/validar-estado`,
        cambiarEstado: (id: string | number) => `${API}/admin/productos/${id}/estado`,
    },
    adminPreparaciones: {
        validarEstado: (id: string | number) => `${API}/admin/preparaciones/${id}/validar-estado`,
        cambiarEstado: (id: string | number) => `${API}/admin/preparaciones/${id}/estado`,
        historialEstados: (id: string | number) => `${API}/admin/preparaciones/${id}/historial-estados`,
    },
    adminInsumos: {
        validarEstado: (id: string | number) => `${API}/admin/insumos/${id}/validar-estado`,
        cambiarEstado: (id: string | number) => `${API}/admin/insumos/${id}/estado`,
    },
} as const;
