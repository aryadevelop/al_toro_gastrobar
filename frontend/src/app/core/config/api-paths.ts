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
        me: `${API}/users/me`,
    },
    reservas: {
        disponibilidad: `${API}/reservas/disponibilidad`,
        crear: `${API}/reservas`,
        modificar: (reservaId: string | number) => `${API}/reservas/${reservaId}`,
        cancelar: (reservaId: string | number) => `${API}/reservas/${reservaId}/cancelar`,
        futuras: `${API}/reservas/cliente/futuras`,
        canceladasDevueltas: `${API}/reservas/cliente/canceladas-devueltas`,
        detalle: (reservaId: string | number) => `${API}/reservas/${reservaId}/detalle`,
        meseroConsulta: `${API}/reservas/mesero/consulta`,
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
    },
    clientes: {
        misPuntos: `${API}/clientes/me/puntos`,
    },
} as const;
