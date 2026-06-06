# Patrones de Código en el Frontend

## 1. Reactividad con Signals

Angular 17+ introduce las Signals como el nuevo estándar para la gestión del estado local y reactivo, desplazando a `RxJS/BehaviorSubject` en escenarios síncronos de UI.

### Reglas de Uso
- Utiliza `signal()` para estado mutables primitivos o arrays dentro de los componentes.
- Utiliza `computed()` para derivar valores de otras signals de forma eficiente (sin re-computar innecesariamente).
- Reserva `RxJS` (`Observable`, `Subject`) **exclusivamente** para peticiones asíncronas HTTP, WebSockets o eventos del DOM (`debounceTime`).

**Ejemplo de Patrón:**
```typescript
import { Component, signal, computed } from '@angular/core';

@Component({...})
export class PedidosComponent {
  // Estado local manejado por Signals
  readonly pedidos = signal<Pedido[]>([]);
  readonly filtroEstado = signal<'TODOS' | 'PENDIENTES'>('TODOS');

  // Valor computado reactivo
  readonly pedidosFiltrados = computed(() => {
    const actual = this.pedidos();
    const filtro = this.filtroEstado();
    return filtro === 'TODOS' ? actual : actual.filter(p => p.estado === filtro);
  });
}
```

## 2. Formularios Reactivos Estrictos

Se prohíbe el uso de `ngModel` (Template-driven forms). Toda entrada de datos compleja debe utilizar **Reactive Forms**.

### Reglas de Uso
- Usa `FormBuilder.nonNullable` para evitar valores `null` accidentales en el tipado.
- Agrupa todas las validaciones BDD (mínimos, requeridos) utilizando validadores asíncronos o síncronos (`Validators.required`).

```typescript
readonly form = this.formBuilder.nonNullable.group({
  cantidad: [0, [Validators.required, Validators.min(1)]],
  tipoMovimiento: ['INGRESO', [Validators.required]]
});
```

## 3. Manejo de Modelos y Tipado API

Todo lo que entra y sale de la red debe estar tipado bajo la nomenclatura `Backend<Nombre>Request|Response`.

- **`api.models.ts`**: Almacena las firmas puras y contratos exactos que provee el Backend en Java.
- **`domain.models.ts`**: Almacena modelos transformados para uso interno del Front (si existiera alguna limpieza de datos).

> [!IMPORTANT]
> El Frontend confía ciegamente en `api.models.ts`. Si el backend cambia un DTO, debe actualizarse en esta interfaz para prevenir fallos en tiempo de ejecución.
