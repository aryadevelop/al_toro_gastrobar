# Arquitectura del Frontend

El frontend de Al Toro Gastrobar está construido con **Angular 17+** utilizando un enfoque **100% Standalone Components**. Esto significa que no existen módulos tradicionales (`NgModules`), simplificando el árbol de dependencias e impulsando el *Lazy Loading* granular.

## Estructura de Directorios

La aplicación sigue una arquitectura orientada a características (*Feature-based architecture*), garantizando un acoplamiento suelto entre distintos dominios del negocio.

```text
src/app/
├── core/         # Elementos singleton de la app (Servicios, Modelos, Configuración API, Guards)
├── features/     # Módulos de dominio del negocio (Mesero, Cajero, Admin, Cliente, Producción)
├── shared/       # Componentes de UI, Pipes y Directivas reusables a través de las Features
└── layouts/      # Plantillas maestras que envuelven las páginas (MainLayout, AuthLayout)
```

### 1. `core/`
Debe contener únicamente código que se instancia **una sola vez** en toda la vida de la aplicación.
- **`services/`**: Lógica de negocio y llamadas HTTP (ej. `AuthService`, `WebSocketService`).
- **`models/`**: Interfaces de TypeScript (`api.models.ts` para respuestas HTTP, `domain.models.ts` para entidades internas).
- **`config/`**: Configuración centralizada, como `api-paths.ts`.
- **`guards/`**: Protección de rutas y resolución de permisos.

> [!WARNING]
> Nunca importes un componente visual (`.component.ts`) dentro del directorio `core/`.

### 2. `features/`
Cada subdirectorio en `features/` representa una porción independiente de la aplicación (ej. `cajero/`, `mesero/`).
Dentro de una feature, típicamente encontramos:
- **`pages/`**: Componentes *Smart* enrutables. Controlan el estado y se comunican con los servicios.
- **`components/`**: Componentes *Dumb* o de presentación específicos para esta característica.

### 3. `shared/`
Contiene la librería de componentes visuales genéricos que no están atados a un contexto de negocio específico.
- Botones, Modales, Tarjetas, Alertas.
- Directivas o Pipes utilitarios.

### 4. `layouts/`
Componentes que estructuran la página alrededor del `<router-outlet>`. Por ejemplo, `MainLayoutComponent` renderiza la barra de navegación lateral dinámica dependiente del rol y deja el contenido a la Feature enrutada.
