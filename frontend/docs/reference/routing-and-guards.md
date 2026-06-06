# Enrutamiento y Protección de Rutas

## Lazy Loading (Carga Diferida)

Con Angular 17+ y componentes Standalone, el *Lazy Loading* se realiza importando componentes directamente a través de promesas, eliminando la necesidad de importar NgModules engorrosos.

El archivo principal `app.routes.ts` define las raíces y delega las sub-rutas a los archivos `.routes.ts` de cada Feature de esta manera:

```typescript
export const routes: Routes = [
  {
    path: 'app',
    component: MainLayoutComponent,
    children: [
      {
        path: 'admin',
        loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
      }
    ]
  }
];
```

## Protección y Autorización (Guards)

El acceso a las pantallas no es público. Todo acceso está regido por el sistema de roles y el JWT emitido por el Backend.

### `authGuard`
Se asegura de que exista un token JWT válido en `localStorage`. De no existir, redirige al `/auth/login`.

### `roleGuard`
Verifica si el Payload del JWT contiene uno de los roles autorizados para el módulo en cuestión. 
Por ejemplo, para proteger las rutas de administración:

```typescript
{
  path: 'admin',
  canActivate: [authGuard, roleGuard],
  data: { roles: ['ADMIN'] },
  loadChildren: () => import('./features/admin/admin.routes').then(m => m.ADMIN_ROUTES)
}
```

> [!CAUTION]
> El `roleGuard` es una validación del cliente para brindar una mejor experiencia de usuario (evitar ver botones que no aplican). **El backend sigue siendo responsable de autorizar la transacción final** en cada petición HTTP, rechazando aquellas peticiones sin permisos mediante `403 Forbidden`.
