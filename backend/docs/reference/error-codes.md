# Códigos de error de la API — Al Toro Gastrobar

Todos los errores de la API retornan el siguiente envelope JSON:

```json
{
  "success": false,
  "code": "ENT-001",
  "message": "El recurso solicitado no existe."
}
```

El campo `code` identifica unívocamente el tipo de error. El frontend reacciona según este código, no según el texto de `message`.

---
## Tabla de contenidos

- [ENT — Entidad](#ent--entidad)
- [AUTH — Autenticación y autorización](#auth--autenticación-y-autorización)
- [NEG — Reglas de negocio](#neg--reglas-de-negocio)
- [VAL — Validación de entrada](#val--validación-de-entrada)
- [SRV — Servidor](#srv--servidor)

---

## ENT — Entidad

| Código | Enum | HTTP | Condición |
|--------|------|------|-----------|
| `ENT-001` | `ENTITY_NOT_FOUND` | 404 | El recurso solicitado no existe en base de datos |
| `ENT-002` | `ENTITY_ALREADY_EXISTS` | 409 | Intento de crear un recurso que ya existe |

---

## AUTH — Autenticación y autorización

| Código | Enum | HTTP | Condición |
|--------|------|------|-----------|
| `AUTH-001` | `INVALID_CREDENTIALS` | 401 | Correo o contraseña incorrectos en el login |
| `AUTH-002` | `ACCESS_DENIED` | 403 | El usuario autenticado no tiene el rol requerido o intenta acceder a un recurso de otro cliente |

---

## NEG — Reglas de negocio

| Código | Enum | HTTP | Condición |
|--------|------|------|-----------|
| `NEG-001` | `BUSINESS_ERROR` | 400 / 409 | Violación de una regla de negocio |
| `NEG-002` | `INVALID_STATE` | 409 | El recurso no está en el estado correcto para la operación solicitada |
| `NEG-003` | `CAPACITY_EXCEEDED` | 409 | Se supera la capacidad máxima de una zona |
| `NEG-004` | `INSUFFICIENT_STOCK` | 409 | Stock insuficiente para completar la operación |

---

## VAL — Validación de entrada

| Código | Enum | HTTP | Condición |
|--------|------|------|-----------|
| `VAL-001` | `VALIDATION_ERROR` | 400 | El cuerpo de la petición contiene campos inválidos o requeridos ausentes |

---

## SRV — Servidor

| Código | Enum | HTTP | Condición |
|--------|------|------|-----------|
| `SRV-001` | `INTERNAL_ERROR` | 500 | Error inesperado del servidor; el mensaje detallado se oculta en producción |
