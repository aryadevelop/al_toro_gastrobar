# Seguridad — Al Toro Gastrobar

El backend usa autenticación stateless basada en JWT. Cada petición debe incluir un token de acceso válido en el header `Authorization`, excepto los endpoints públicos listados más abajo.

---

## Tabla de contenidos

- [Flujo de autenticación](#flujo-de-autenticación)
- [Tokens JWT](#tokens-jwt)
- [Roles del sistema](#roles-del-sistema)
- [Endpoints públicos](#endpoints-públicos)
- [Control de acceso por endpoint](#control-de-acceso-por-endpoint)
- [CORS](#cors)

---

## Flujo de autenticación

1. El cliente llama a `POST /api/auth/login` con correo y contraseña.
2. `JwtAuthenticationFilter` no intercepta este endpoint (es público).
3. `AuthService` valida credenciales con `DaoAuthenticationProvider` + BCrypt.
4. Si son válidas, `JwtTokenProvider` emite un access token y un refresh token.
5. Ambos tokens se persisten en la tabla `Sesion` con `sesion_activa = true`.
6. En peticiones posteriores, `JwtAuthenticationFilter` extrae el token del header, verifica firma, expiración y que `sesion_activa = true` en base de datos.
7. Para renovar el access token, el cliente llama a `POST /api/auth/refresh` con el refresh token.
8. Al hacer logout, `sesion_activa` se pone en `false` — el token queda inválido aunque no haya expirado.

---

## Tokens JWT

| Parámetro | Valor |
|-----------|-------|
| Algoritmo | HMAC-SHA256 |
| Subject | `usuario_email` |
| Claim `type` | `access` o `refresh` |
| Expiración access token | 30 min (configurable via `JWT_EXPIRATION`) |
| Expiración refresh token | 7 días (configurable via `jwt.refresh-expiration`) |
| Clave | Mínimo 32 caracteres; generada con `openssl rand -base64 48` en producción |

El header de cada petición autenticada debe incluir:

```
Authorization: Bearer <access-token>
```

---

## Roles del sistema

Un usuario puede tener múltiples roles activos simultáneamente (tabla `Usuario_Rol`).

| Rol | Descripción |
|-----|-------------|
| `CLIENTE` | Portal del cliente: reservas, pre-orden, historial de visitas y puntos |
| `MESERO` | Mapa de mesas, comandas en tiempo real y notificaciones |
| `CAJERO` | Reservas, pagos, cierre de caja y mapa de mesas |
| `COCINERO` | Comandas de estación COCINA |
| `BARTENDER` | Comandas de estación BARRA |
| `ADMIN` | Acceso total: inventario, reportes, personal, clientes y configuración |

Los roles se verifican con `@PreAuthorize("hasRole('ROL')")` o `@PreAuthorize("hasAnyRole('ROL1', 'ROL2')")` a nivel de método en el controller. El prefijo `ROLE_` lo agrega Spring Security automáticamente.

---

## Endpoints públicos

Los siguientes paths no requieren token JWT:

| Path | Descripción |
|------|-------------|
| `POST /api/auth/login` | Login de usuario |
| `POST /api/auth/register` | Registro de cliente |
| `POST /api/auth/refresh` | Renovación de access token |
| `/swagger-ui/**` | Documentación Swagger (solo perfil `dev`) |
| `/swagger-ui.html` | Swagger UI (solo perfil `dev`) |
| `/v3/api-docs/**` | OpenAPI spec (solo perfil `dev`) |
| `/actuator/health` | Health check para Docker |
| `/ws/**` | Handshake WebSocket STOMP |

Todos los demás endpoints requieren autenticación. Si no hay token o es inválido, la API retorna `401` con `{"success":false,"code":"UNAUTHORIZED","message":"Autenticación requerida"}`.

---

## Control de acceso por endpoint

El control de acceso combina dos niveles:

**Nivel 1 — Filtro global (`SecurityConfig`):** cualquier request sin token JWT válido recibe `401` antes de llegar al controller.

**Nivel 2 — Anotación en el método (`@PreAuthorize`):** verifica que el usuario autenticado tenga el rol requerido. Si no lo tiene, retorna `403` con código `AUTH-002`.

El rol `CLIENTE` solo puede acceder a sus propios recursos. El service verifica que el `clienteId` del recurso coincida con el `clienteId` extraído del token JWT. Si no coincide, lanza `ACCESS_DENIED` (`403`).

Los roles operativos (`MESERO`, `CAJERO`, `COCINERO`, `BARTENDER`, `ADMIN`) no tienen restricción de ownership — pueden acceder a cualquier recurso de su módulo.

---

## CORS

| Parámetro | Valor |
|-----------|-------|
| Orígenes permitidos | Configurados via `CORS_ALLOWED_ORIGINS` |
| Métodos permitidos | `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` |
| Headers permitidos | Todos (`*`) |
| Credenciales | Habilitadas |
| Paths cubiertos | `/**` |
