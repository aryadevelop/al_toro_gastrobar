# Postman — Conventions

## Formato

- YAML format (Postman for VS Code plugin).
- `.resources/definition.yaml` — collection-level hooks.
- Archivos: `XX-NN Nombre – STATUS.request.yaml`.

## Autonomous Login Pattern — CRÍTICO

**TODOS los tests** incluyen script `beforeRequest` con login autónomo vía `pm.sendRequest`. **Nunca** usar `{{tokenVariable}}` sin `beforeRequest` que la setea.

## Token Naming

| Rol | Variable (Manual) | Variable (Automated) |
|------|-------------------|----------------------|
| CLIENTE | `tmpClienteToken` | `clienteToken` |
| MESERO | `tmpMeseroToken` | `meseroToken` |
| CAJERO | `tmpCajeroToken` | `cajeroToken` |
| ADMIN | `tmpAdminToken` | `adminToken` |

## Prerrequisito de ejecución

Estado limpio:
```bash
psql -U postgres -d altoro_db -f postman/cleanup-notificaciones.sql
```

---

## Reglas críticas

### 1. Variables de ambiente en URLs dinámicas

**Problema:** `{{reservaId}}` vacía → URLs malformadas (`//`) → `RequestRejectedException`.

**Solución:** Variable temporal en `beforeRequest`:
```javascript
const reservaId = pm.environment.get('reservaIdConPreOrden') || '10';
pm.environment.set('tmpReservaId', reservaId);
// ... login ...
// afterResponse: pm.environment.unset('tmpReservaId');
```

**Regla:** Nunca usar `{{var}}` directo en URL si puede estar vacío. Usar `tmp` prefix.

### 2. Fechas dinámicas

```javascript
// Reservas futuras: SIEMPRE mañana
const d = new Date();
d.setDate(d.getDate() + 1);
d.setHours(19, 0, 0, 0);
const fechaHora = `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T19:00:00`;
```

**Regla:** Nunca fechas fijas (`2026-12-25`). Usar `d.setDate(d.getDate() + 1)` para futuras.

### 3. Códigos de error

**Regla:** Verificar código serializado (`ENT-001`), **NO** enum (`ENTITY_NOT_FOUND`):
```javascript
pm.expect(body.code).to.equal('ENT-001');           // ✅ Correcto
// pm.expect(body.code).to.equal('ENTITY_NOT_FOUND'); // ❌ NUNCA
```

**Mapeo:** `ENTITY_NOT_FOUND`→`ENT-001`, `VALIDATION_ERROR`→`VAL-001`, `UNAUTHORIZED`→`AUTH-001`, `ACCESS_DENIED`→`AUTH-002`, `INVALID_STATE`→`NEG-002`, `BUSINESS_ERROR`→`NEG-001`.

---

## DOS TIPOS de Postman Testing

### Manual Testing (`manual-testing/`)

**Propósito:** Pruebas manuales rápidas sin validaciones automáticas.

**Convenciones:**
- ✅ Solo `{{baseUrl}}`
- ✅ Credenciales hardcoded en `beforeRequest`
- ✅ Password: `Al.Toro2026!`
- ✅ Tokens temporales con `tmp` prefix
- ✅ Cleanup en `afterResponse` (solo unset)
- ❌ NO tests en `afterResponse`
- ✅ Formato: `XX-YY Descripción – ROL.request.yaml`

**Numeración:**

| Rango | Módulo |
|-------|--------|
| `00-XX` | Auth |
| `10-XX` | Productos |
| `20-XX` | Reservas (CLIENTE) |
| `30-XX` | Reservas (MESERO) |
| `40-XX` | Visitas |
| `50-XX` | Puntos |
| `60-XX` | Ventas |
| `70-XX` | Notificaciones |
| `80-XX` | Mesas |

### Automated Collections (`collections/`)

**Propósito:** Pruebas automatizadas con validaciones (`pm.test`).

**Convenciones:**
- ✅ Variables de entorno (`emailMesero`, `passwordValida`)
- ✅ Login autónomo en `beforeRequest`
- ✅ Limpieza estado previo en `beforeRequest`
- ✅ Tests obligatorios en `afterResponse`
- ✅ Variables temporales SOLO si necesarias
- ❌ NO cleanup en `afterResponse`
- ✅ Formato: `XX-YY Descripción – Código HTTP.request.yaml`
- ✅ **INDEPENDENCIA**: ejecutable solo, sin depender del orden

---

## Diferencias clave Manual vs Automated

| Aspecto | Manual | Automated |
|---------|--------|-----------|
| Credenciales | Hardcoded | Variables entorno |
| Password | `Al.Toro2026!` | `passwordValida` |
| Tokens | `tmpMeseroToken` | `meseroToken` |
| afterResponse | Solo cleanup | Tests + guardar IDs |
| Cleanup | En `afterResponse` | En `beforeRequest` siguiente |
| Independencia | No requerida | OBLIGATORIA |

---

## Tests obligatorios al añadir/modificar endpoint

1. **Manual** — 1 request por endpoint, seguir template `00-01 Login CLIENTE.request.yaml`.
2. **Automated** — 1 carpeta por endpoint con `definition.yaml` + cobertura completa:
   - 200 OK happy path
   - 401 sin token
   - 403 con roles no autorizados (cada rol)
   - 404 ID inexistente
   - 409 estado inválido
   - 400 validaciones específicas
