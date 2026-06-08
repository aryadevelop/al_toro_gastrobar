# Servicios y WebSockets

Todo el intercambio de información entre el Frontend (Angular) y el Backend (Spring Boot) se realiza mediante servicios inyectables (`@Injectable({ providedIn: 'root' })`).

## Peticiones HTTP

### `api-paths.ts`
Todas las rutas de la API de backend están centralizadas en un solo archivo inmutable: `src/app/core/config/api-paths.ts`.

Ningún servicio realiza hardcoding (escribir cadenas explícitas) de URLs. Todos los servicios llaman a `API_PATHS.<modulo>.<endpoint>`, inyectando parámetros si se trata de una función (ej. `API_PATHS.mesas.detalle(id)`).

### Interceptores (`AuthInterceptor`)
A nivel global, la aplicación inyecta automáticamente el token JWT en cada petición saliente gracias a la configuración de proveedores en `app.config.ts`.
Si la petición falla con un código `401 Unauthorized` o `403 Forbidden`, los interceptores manejarán la limpieza de la sesión en el cliente de manera silenciosa.

---

## WebSockets y Tiempo Real

La aplicación requiere características de tiempo real para las **Comandas en Cocina/Barra** y las **Notificaciones a Meseros** (llamado a mesa, cuentas, platos listos).

Esto se logra utilizando `@stomp/stompjs`, una biblioteca que implementa el protocolo STOMP sobre WebSockets.

### `WebSocketService`

El archivo `websocket.service.ts` se encarga del ciclo de vida de la conexión:
1. **Conexión Automática:** Al loguearse el usuario, se activa el cliente y establece la conexión con el servidor.
2. **URLs Dinámicas de Conexión:** 
   - En **desarrollo local**, el WebSocket conecta a `ws://localhost:8080/ws`.
   - En **producción**, el servicio detecta dinámicamente la procedencia (`https://dominio.com/api`) y reescribe la URI a absoluta asegurando conexiones seguras: `wss://dominio.com/ws`.
3. **Manejo de Re-conexiones:** STOMP reintentará conectarse si el servidor se cae, permitiendo recuperación sin requerir recargar la página.
4. **Suscripción Reactiva:** El servicio provee métodos que retornan `Observables` de los tópicos para que cualquier componente se suscriba y reaccione a los mensajes entrantes (ej. `subscribeToTablero()`).
