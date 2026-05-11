# Diseño — Refactor de Tests Postman para Independencia Total

**Fecha:** 2026-05-03
**Branch:** PA-132-gestionar-notificaciones
**Autora:** PMunoz

## Problema

Las colecciones automatizadas en `backend/postman/postman/collections/` contienen tests que mutan estado de base de datos (ej: atender notificación, asignar mesa, cerrar cuenta) y dependen de un orden de ejecución específico. Si un test corre antes que otro que esperaba estado original, este último falla. Además, cualquier test ejecutado deja modificaciones permanentes que rompen futuros runs.

## Objetivos

1. **Independencia total** — cada test ejecutable solo, en cualquier orden, repetidamente, siempre pasa.
2. **No se asume estado compartido** — ningún test depende de seed mutable ni de otro test ejecutado previamente.
3. **No se requieren endpoints nuevos** — refactor 100% en archivos `.request.yaml`, sin tocar backend.

## Alcance

### Incluye (refactor obligatorio)

Solo tests que **mutan estado** (HTTP `POST`/`PUT`/`PATCH`/`DELETE`) en colecciones afectadas:

| Colección | Carpeta(s) |
|---|---|
| Notificaciones | `atender/`, `atender-cambio/`, `servir-platos/`, `servir-bebidas/` |
| Mesas/Comandas | `POST -api-mesas/`, `POST -api-visitas-{visitaId}-asistencia/` |
| Reservas | `POST -api-reservas/`, `PUT -api-reservas-{reservaId}/`, `PATCH cancelar`, `PATCH marcar-inasistencia` |
| Usuarios | `POST canje-puntos` |

### Excluye

- Todas las colecciones `auth/`, `produccion/` (no presentaron el problema).
- Carpetas `manual-testing/` (fuera de alcance).
- Todos los GET (no mutan).
- Tests `404` que no mutan estado antes de la request principal.

## Estrategia de Refactor

### Regla principal: "Create Fresh Data"

Cada test que muta estado debe crear, dentro de su propio `beforeRequest`, todos los datos prerequisito vía endpoints existentes de la API. El test opera exclusivamente sobre IDs recién generados, almacenados en variables de entorno con prefijo `tmp*`. **No se referencia ningún ID hardcoded de seed (`V3__dev_data.sql`).**

### Estructura tipo del `beforeRequest`

```javascript
// 1. Login del actor principal del test (ej: MESERO)
pm.sendRequest({ /* login MESERO */ }, (err, res) => {
  pm.environment.set('meseroToken', res.json().accessToken);

  // 2. Login(s) de actores secundarios necesarios (cadena)
  pm.sendRequest({ /* login CLIENTE */ }, (err2, res2) => {
    const clienteToken = res2.json().accessToken;

    // 3. Crear datos prerequisito vía API (tantos pasos como se requiera)
    pm.sendRequest({
      url: pm.environment.get('baseUrl') + '/api/visitas/X/asistencia',
      method: 'POST',
      header: { Authorization: 'Bearer ' + clienteToken, 'Content-Type': 'application/json' },
      body: { mode: 'raw', raw: JSON.stringify({ tipo: 'ATENCION' }) }
    }, (err3, res3) => {
      // 4. Capturar ID generado
      pm.environment.set('tmpNotificacionId', res3.json().data.id);
    });
  });
});
```

### Reglas del patrón

1. **Variables `tmp*` para todo ID generado** — siguiendo convención `docs/postman-conventions.md`.
2. **`afterResponse`:** mantiene los `pm.test(...)` existentes y hace `unset` de variables `tmp*`. **No revierte estado en BD.**
3. **Cero dependencias entre tests** — eliminar de los descriptions cláusulas tipo `"OB-01 debe ejecutarse primero"`.
4. **Datos huérfanos aceptados** — cada run agrega filas nuevas (visitas, reservas, notificaciones). Inocuo: ningún test asume "no existe X". La limpieza acumulativa, si llegara a requerirse, se resuelve fuera de Postman (psql manual o Newman hook). No es responsabilidad del refactor.
5. **Variantes de error que requieren estado mutado** (ej: `409 Conflict` por notificación ya atendida): la cadena `beforeRequest` ejecuta los pasos previos para llegar al estado de conflicto, luego el request principal lo verifica.

### Casos especiales — Plan A / Plan TODO condicional

Algunas notificaciones (`PLATOS_LISTOS`, `BEBIDAS_LISTAS`, `CAMBIO`) **se generan automáticamente** por flujos del sistema, no por endpoints de creación directa. Para esos:

- **Plan A (preferido):** si **ya existe** endpoint para llevar el flujo previo al estado disparador (ej: COCINERO/BARTENDER marca comanda como `LISTO`), el `beforeRequest` encadena el flujo completo. Costo: 5-6 calls. Beneficio: cero SQL, cero endpoints nuevos, funciona en UI y CLI.
- **Plan TODO (fallback temporal):** si el endpoint **aún no existe** pero se planea construir, el test **se conserva tal cual está hoy** (continúa modificando estado de seed data sin reversión completa) y se marca con un comentario `TODO` explícito en el archivo `.request.yaml` y en su `definition.yaml`. El comentario indica:
  - Qué endpoint falta para poder automatizar con "create fresh".
  - Que mientras tanto el test es **dependiente de seed** y puede romperse en runs repetidos.
  - Que debe re-refactorizarse cuando el endpoint exista.

  **No se mueve a `manual-testing/`.** **No se elimina la carpeta automatizada.** El test queda en su estado actual con la deuda técnica documentada.

Verificación de qué endpoints existen ya y cuáles están planeados se hace durante implementación, por colección.

### `canje-puntos`

Los puntos de cliente se generan por ventas cerradas; no hay endpoint público que los inyecte. Si la cadena vía API no es factible y no se planea endpoint, aplica **Plan TODO**: el test se mantiene como hoy con `TODO` documentando qué se requiere para automatizar con create-fresh.

## Plan de Verificación

Cada test refactorizado debe pasar **tres ejecuciones**:

1. **Aislado** — `newman run --folder "<test>"` (un único test, partiendo de DB sin pre-setup específico).
2. **Repetido** — el mismo test corrido 3 veces seguidas debe pasar las 3.
3. **Orden inverso** — colección completa ejecutada en orden inverso al original.

Si los tres pasan, el test queda marcado como completo en el plan de implementación.

## Cambios Documentales

Por cada colección refactorizada, su `.resources/definition.yaml`:

- Elimina cláusulas de orden ("X debe ejecutarse antes que Y").
- Añade nota: "Cada test es ejecutable de forma aislada y en cualquier orden. Setup completo en `beforeRequest` de cada request."

## Fuera de Alcance

- Refactor de colecciones no afectadas (`auth/`, `produccion/`).
- Movimiento o eliminación de carpetas automatizadas existentes (los tests sin endpoint disponible se conservan tal cual con `TODO`).
- Limpieza acumulativa de filas huérfanas en BD.
- Creación de endpoints nuevos en backend.
- Modificación de `manual-testing/` salvo para añadir réplicas de tests no automatizables (Plan C).

## Bug Conocido a Corregir Durante el Refactor

Actualmente los tests de notificaciones fallan con:

```json
{
  "success": false,
  "code": "VAL-001",
  "message": "Formato de parámetro inválido: notificacionId"
}
```

**Causa raíz:** las variables de entorno (ej: `tmpNotificacionId`) no están siendo seteadas correctamente en el `beforeRequest` o se referencian antes de quedar disponibles. Postman envía la URL con `{{tmpNotificacionId}}` literal o vacío y el backend lo rechaza.

**Cómo lo resuelve este refactor:**

- Cada `beforeRequest` setea explícitamente la `tmp*` después de capturar el `id` desde `res.json().data.id` (o el path correcto del payload).
- Las `pm.sendRequest` se anidan en callbacks para garantizar orden secuencial: el `set` de la `tmp*` ocurre **antes** de que termine `beforeRequest`.
- En la URL del request principal se usa `{{tmpNotificacionId}}` y se valida durante implementación que el valor capturado sea numérico antes del set.
- El comportamiento se prueba con la verificación "run aislado" — si el test pasa partiendo de cero, la cadena de seteo funciona.

## Riesgos y Mitigaciones

| Riesgo | Mitigación |
|---|---|
| `beforeRequest` largo (5-6 calls) puede ser lento | Aceptable; suite local. CI puede paralelizar. |
| Algún flujo prerequisito no tiene endpoint hoy | Plan TODO: test queda como hoy + comentario `TODO` para refactor cuando exista endpoint. |
| Cliente sin puntos para canje | Plan TODO aplicable. |
| Acumulación de datos huérfanos en dev DB | Aceptable. `docker compose down -v` resetea. |
| Test `marcar-inasistencia` con MESERO requiere fecha hoy | Cálculo dinámico de fecha en `beforeRequest`. |
