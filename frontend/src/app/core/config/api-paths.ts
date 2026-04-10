import { environment } from '../../../environments/environment';

const API = environment.apiBaseUrl;

export const API_PATHS = {
    auth: {
        login: `${API}/auth/login`,       // <-- CAMBIA AQUÍ si backend cambia esta ruta
        register: `${API}/auth/register`, // <-- CAMBIA AQUÍ
        refresh: `${API}/auth/refresh`,   // <-- CAMBIA AQUÍ
        me: `${API}/auth/me`,             // <-- CAMBIA AQUÍ
        logout: `${API}/auth/logout`,     // <-- CAMBIA AQUÍ
    },
    users: {
        me: `${API}/users/me`,            // <-- CAMBIA AQUÍ
    },
} as const;
