# Documentacion del frontend — Al Toro Gastrobar

> **Para el equipo de frontend:** Este documento describe la documentacion tecnica que debe crearse para el modulo Angular del proyecto. No es un plan de implementacion de features; es una guia de que documentar y como estructurarlo.

**Stack:** Angular 17+ · TypeScript · Angular Material · RxJS · STOMP/SockJS.

**Fuentes de referencia del backend** (leer antes de documentar la integracion):
- `backend/docs/reference/endpoints.md` — contratos de API REST
- `backend/docs/reference/websocket.md` — topicos STOMP y contratos de mensajes
- `backend/docs/reference/security.md` — flujo JWT, roles y endpoints publicos
- `backend/docs/reference/error-codes.md` — codigos de error de la API

---

## Convenciones de estilo para todos los documentos

1. Sin emojis en ninguna parte del documento.
2. Redaccion en tercera persona o modo imperativo formal, sin coloquialismos.
3. Titulos con mayuscula solo en la primera palabra (excepto nombres propios y siglas).
4. Terminos tecnicos en ingles sin traduccion: `component`, `service`, `guard`, `interceptor`, `pipe`, `module`, `route`, `Observable`, `Subject`, `signal`.
5. Sin indicadores de estado de implementacion — documentar solo lo que existe.

---

## Estructura propuesta

```
frontend/docs/
├── reference/                  # Que ES el sistema frontend
│   ├── architecture.md         Modulos Angular, lazy loading, guards, estructura de carpetas
│   ├── api-client.md           Como se consumen los endpoints REST (servicios HTTP, interceptors)
│   ├── websocket-client.md     Como se conecta y suscribe a los topicos STOMP
│   ├── state.md                Gestion de estado (servicios + BehaviorSubject / signals)
│   └── routing.md              Configuracion de rutas, guards por rol, lazy loading
└── conventions/                # COMO se trabaja en el frontend
    ├── component-conventions.md  Estructura de componentes, naming, OnPush, lifecycle hooks
    └── style-guide.md            SCSS, Angular Material theming, design tokens, clases utilitarias
```

---

## Documentos a crear

### `frontend/docs/reference/architecture.md`

**Proposito:** Orientar a cualquier dev nuevo sobre la estructura del proyecto Angular.

Contenido minimo:
- Diagrama o descripcion de modulos (`CoreModule`, `SharedModule`, modulos de feature por epica)
- Convension de carpetas dentro de cada modulo (components/, services/, models/, guards/)
- Estrategia de lazy loading: que modulos se cargan bajo demanda y desde que ruta
- Lista de guards existentes y que condicion evalua cada uno
- Lista de interceptors HTTP y su proposito (auth token, refresh, manejo de errores)

---

### `frontend/docs/reference/api-client.md`

**Proposito:** Documentar como el frontend consume la API REST del backend.

Contenido minimo:
- Servicio base o configuracion de `HttpClient` (base URL, cabeceras por defecto)
- Como se adjunta el `accessToken` a cada request (interceptor)
- Flujo de renovacion automatica del token con `refreshToken` (interceptor de 401)
- Como se mapean los errores de la API (`errorCode` del backend) a mensajes de usuario
- Referencia cruzada: ver `backend/docs/reference/endpoints.md` para los contratos de cada endpoint

---

### `frontend/docs/reference/websocket-client.md`

**Proposito:** Documentar como el frontend se suscribe a los eventos en tiempo real.

Contenido minimo:
- Libreria usada (SockJS + STOMP o `@stomp/rx-stomp`)
- Endpoint de conexion: `/ws`
- Como se gestiona la reconexion automatica
- Tabla de suscripciones activas: topico → componente o servicio que lo consume
- Patron de desuscripcion (en `ngOnDestroy` o con `takeUntilDestroyed`)
- Referencia cruzada: ver `backend/docs/reference/websocket.md` para los contratos de mensajes

---

### `frontend/docs/reference/state.md`

**Proposito:** Describir como se gestiona el estado compartido entre componentes.

Contenido minimo:
- Patron usado: servicios con `BehaviorSubject` / `signal` / NgRx (especificar cual aplica)
- Lista de servicios de estado globales y que datos gestionan (ej. `AuthService`, `MesasStateService`)
- Como se propaga el estado de WebSocket a los componentes suscritos
- Patron de limpieza de estado al hacer logout

---

### `frontend/docs/reference/routing.md`

**Proposito:** Documentar la configuracion de rutas y los guards de acceso por rol.

Contenido minimo:
- Arbol de rutas con sus modulos asociados
- Tabla de guards: nombre → roles que permite → ruta que protege
- Comportamiento al intentar acceder a una ruta sin permiso (redireccion, pagina 403)
- Rutas publicas (login, registro)

---

### `frontend/docs/conventions/component-conventions.md`

**Proposito:** Estandarizar la estructura interna de los componentes Angular.

Contenido minimo:
- Convencion de naming: `feature-name.component.ts`, selector `app-feature-name`
- Estructura de archivos por componente (`.ts`, `.html`, `.scss`, `.spec.ts`)
- Estrategia de change detection recomendada (`OnPush` por defecto o `Default`)
- Orden de declaracion dentro de la clase: inputs, outputs, injectables, lifecycle hooks, metodos
- Regla sobre logica en templates vs en la clase (donde va la logica de presentacion)
- Como pasar datos entre componentes: `@Input`/`@Output` vs servicio compartido

---

### `frontend/docs/conventions/style-guide.md`

**Proposito:** Mantener consistencia visual y de codigo SCSS en todo el proyecto.

Contenido minimo:
- Variables de Angular Material theming en uso (paleta de colores, tipografia, espaciado)
- Convencion de nombres de clases CSS (BEM u otra metodologia)
- Clases utilitarias globales disponibles (si existen en `styles.scss`)
- Como se aplican los estilos de Material: cuando usar `mat-` vs clases propias
- Regla sobre estilos en linea vs en el archivo `.scss` del componente

---

## Criterios de calidad aplicables a todos los documentos

- El documento describe lo que existe, no lo que esta planificado.
- Cada seccion tiene al menos un ejemplo concreto (ruta real, nombre de clase real, fragmento de codigo).
- Los fragmentos de codigo estan en bloques de codigo con el lenguaje indicado (` ```typescript `, ` ```html `).
- Las referencias cruzadas a documentos del backend usan rutas relativas desde la raiz del repositorio.
- Sin tablas con celdas vacias sin justificacion.
