# PROMPT  — Generación de Casos de Prueba Postman
## Al Toro Gastrobar · Estándar

---

> **Instrucciones de uso:** Copie el bloque del prompt a continuación, reemplace
> los campos entre corchetes `[]` con los valores del módulo a probar, y envíelo
> al asistente de IA. El resultado será una colección Postman conforme al
> estándar v2.0 del proyecto.

---

## PROMPT COMPLETO

```
Actúa como líder de pruebas del proyecto Al Toro Gastrobar.
Debes generar una colección Postman conforme al ESTÁNDAR v2.0 del proyecto.
A continuación se detallan los parámetros del módulo a probar:

═══════════════════════════════════════════════════════════
PARÁMETROS DEL MÓDULO
═══════════════════════════════════════════════════════════

MÓDULO FUNCIONAL    : [Ej. "Gestión de Reservas"]
HISTORIA ÉPICA      : [Ej. "HE-02"]
HISTORIA DE USUARIO : [Ej. "HU-03 — Crear reserva como cliente"]
NOMBRE DEL ARCHIVO  : [Ej. Reservas.postman_collection.json"]

ENDPOINTS A CUBRIR:
  1. [MÉTODO] [/api/ruta] — [Descripción breve]
  2. [MÉTODO] [/api/ruta] — [Descripción breve]
  (agregar más si es necesario)

PREFIJOS DE CASOS POR ENDPOINT:
  [/api/ruta1] → prefijo [RA-xx]
  [/api/ruta2] → prefijo [RB-xx]
  (seguir el patrón: primera letra del módulo + letra de endpoint + número)

CRITERIOS DE ACEPTACIÓN A CUBRIR:
  [ID-CA] | [Descripción del escenario] | [HTTP esperado]
  [ID-CA] | [Descripción del escenario] | [HTTP esperado]
  (incluir TODOS los CAs documentados para este módulo)

VARIABLES DE ENTORNO DISPONIBLES:
  {{baseUrl}}          → http://localhost:8080
  {{emailAdmin}}       → admin@altoro.com
  {{emailCliente}}     → andres.morales@gmail.com
  {{passwordValida}}   → Al.Toro2026!
  {{accessToken}}      → (generado dinámicamente)
  {{refreshToken}}     → (generado dinámicamente)
  [agregar variables específicas del módulo si aplica]

AUTENTICACIÓN REQUERIDA: [Sí / No] — [Tipo de token: ADMIN / CLIENTE / MESERO / etc.]

═══════════════════════════════════════════════════════════
ESTÁNDAR OBLIGATORIO — APLICAR SIN EXCEPCIÓN
═══════════════════════════════════════════════════════════

**ESTRUCTURA DE LA COLECCIÓN:**
- Un grupo principal que agrupa todos los endpoints del módulo.
- Una carpeta de segundo nivel por cada endpoint, con nombre:
  "[MÉTODO] /api/[ruta] – [Acción en infinitivo]"
- Los requests individuales son los ítems hoja dentro de cada carpeta.

**NOMBRE DE CADA REQUEST:**
  [ID-CASO] [Descripción del escenario] – [HTTP_CODE] [Nombre del estado]
  Ejemplo: CA-01 Login de administrador válido (override=true) – 200 OK

**DESCRIPCIÓN DE LA COLECCIÓN (nivel raíz):**
Debe incluir en Markdown:
  - Propósito y trazabilidad a HE/HU
  - Tabla de endpoints cubiertos (Método | Endpoint | Descripción)
  - Explicación de la convención de IDs de casos
  - Instrucciones de configuración (pasos numerados)
  - Tabla de usuarios de prueba (Variable | Email | Rol | Estado)
  - Formato estándar de errores: { success, code, message }
  - Convención de nomenclatura de requests con ejemplo

**DESCRIPCIÓN DE CADA CARPETA DE ENDPOINT:**
Debe incluir en Markdown:
  - Descripción funcional del endpoint
  - Estructura del body (bloque de código JSON)
  - Encabezados requeridos si aplica (Authorization, Content-Type)
  - Comportamientos especiales o dependencias entre casos
  - Tabla de casos: ID | Escenario | HTTP

**DESCRIPCIÓN DE CADA REQUEST INDIVIDUAL:**
Formato obligatorio:
  **Criterio de Aceptación:** [ID del CA]
  **Objetivo:** [Comportamiento del sistema a verificar]
  **Pre-condición:** [Estado del sistema requerido]
  **Resultado esperado:** [HTTP status + campos/códigos esperados]

**CONSTRUCCIÓN DE REQUESTS:**
- Nunca usar valores hardcodeados para credenciales, URLs ni tokens.
- Usar {{baseUrl}} para el host.
- Usar variables de entorno para todos los valores dinámicos.
- Incluir Content-Type: application/json en todos los requests con body.
- Incluir Authorization: Bearer {{accessToken}} (o token del rol) cuando el
  endpoint requiera autenticación.

**SCRIPTS DE PRUEBA (evento "test"):**
- Mínimo un pm.test() por request.
- Cada pm.test() valida UN SOLO aspecto del comportamiento.
- Los nombres de test deben describir el comportamiento, no el mecanismo.
  ✅ 'La respuesta contiene accessToken, refreshToken y user'
  ❌ 'Body tiene propiedad accessToken'
- Verificar SIEMPRE: código HTTP + al menos un campo de la respuesta.
- Guardar tokens/IDs en el entorno solo dentro de if (pm.response.code === 2xx).
- Limpiar variables temporales con pm.environment.unset() en el test del
  caso que las consume.

**SCRIPTS DE PRE-REQUEST:**
- A nivel de carpeta: si el grupo requiere token fresco independiente de CA-01.
  Usar pm.sendRequest() para hacer login autónomo de ADMIN (y de otros roles
  si el grupo los necesita), guardando los tokens resultantes en el entorno.
- A nivel de request: solo si el caso específico necesita preparar una variable
  temporal que usa ese mismo request (ej. adulterar firma de token).

**FORMATO DE BODY JSON:**
Usar JSON válido con indentación de 2 espacios. Variables con doble llave:
  {
    "email": "{{emailAdmin}}",
    "password": "{{passwordValida}}",
    "campoOpcional": true
  }

═══════════════════════════════════════════════════════════
ENTREGABLE ESPERADO
═══════════════════════════════════════════════════════════

Genera el archivo JSON completo de la colección Postman en formato
Collection v2.1.0, listo para importar en Postman sin modificaciones.
El archivo debe:
  1. Ser JSON válido y bien formado.
  2. Cumplir todos los puntos del estándar descritos arriba.
  3. Cubrir TODOS los criterios de aceptación listados en los parámetros.
  4. Tener el nombre de archivo: [NOMBRE_DEL_ARCHIVO_INDICADO_ARRIBA]

Si algún criterio de aceptación tiene dependencia secuencial con otro,
documentarlo explícitamente en la description del request afectado.
```

---

## EJEMPLO DE USO RELLENADO

```
Actúa como líder de pruebas del proyecto Al Toro Gastrobar.
Debes generar una colección Postman conforme al ESTÁNDAR v2.0 del proyecto.

MÓDULO FUNCIONAL    : Gestión de Reservas
HISTORIA ÉPICA      : HE-02
HISTORIA DE USUARIO : HU-04 — Crear reserva como cliente autenticado
NOMBRE DEL ARCHIVO  : Al-Toro-Reservas-HE02-HU04.postman_collection.json

ENDPOINTS A CUBRIR:
  1. POST /api/reservas — Crear nueva reserva
  2. GET  /api/reservas/{id} — Consultar reserva por ID

PREFIJOS DE CASOS POR ENDPOINT:
  POST /api/reservas      → prefijo RA-xx
  GET  /api/reservas/{id} → prefijo RB-xx

CRITERIOS DE ACEPTACIÓN A CUBRIR:
  RA-01 | Reserva exitosa con fecha disponible        | 201
  RA-02 | Fecha ya ocupada (sin capacidad)            | 409
  RA-03 | Fecha en el pasado                          | 422
  RA-04 | Sin token de autenticación                  | 401
  RB-01 | Consulta de reserva propia exitosa          | 200
  RB-02 | Consulta de reserva de otro cliente         | 403
  RB-03 | ID de reserva inexistente                   | 404

VARIABLES DE ENTORNO DISPONIBLES:
  {{baseUrl}}        → http://localhost:8080
  {{emailCliente}}   → andres.morales@gmail.com
  {{passwordValida}} → Al.Toro2026!
  {{accessToken}}    → (generado dinámicamente)
  {{fechaLibre}}     → 2026-12-15T19:00:00
  {{fechaCapacidad}} → 2026-12-16T20:00:00

AUTENTICACIÓN REQUERIDA: Sí — token de CLIENTE

[... continúa con el bloque ESTÁNDAR OBLIGATORIO sin cambios ...]
```

---

## CHECKLIST DE REVISIÓN POST-GENERACIÓN

Antes de integrar la colección al repositorio, verificar:

- [ ] El JSON es válido y puede importarse en Postman sin errores.
- [ ] La colección tiene `description` completa en formato Markdown.
- [ ] Cada carpeta de endpoint tiene `description` con matriz de casos.
- [ ] Cada request tiene `description` con los 4 campos obligatorios.
- [ ] El nombre de cada request sigue el patrón `[ID] [Descripción] – [HTTP] [Estado]`.
- [ ] No hay valores hardcodeados de credenciales, URLs ni tokens.
- [ ] Todos los requests con body tienen `Content-Type: application/json`.
- [ ] Los requests protegidos tienen `Authorization: Bearer {{token}}`.
- [ ] Cada request tiene al menos un `pm.test()`.
- [ ] Los tests verifican HTTP status Y contenido de la respuesta.
- [ ] Las variables temporales se limpian con `pm.environment.unset()`.
- [ ] Los grupos con dependencia de token tienen pre-request de carpeta.
- [ ] Los tokens se guardan solo dentro de `if (pm.response.code === 2xx)`.

