# Referencia de base de datos — Al Toro Gastrobar

El backend utiliza PostgreSQL 15 con el schema `restaurante`. Las migraciones son gestionadas por Flyway y el ORM es Spring Boot 3.5 con JPA/Hibernate.

---

## Tabla de contenidos

- [Migraciones Flyway](#migraciones-flyway)
- [Convenciones del schema](#convenciones-del-schema)
- [Módulo Auth](#módulo-auth)
- [Módulo Usuarios](#módulo-usuarios)
- [Módulo Reservas](#módulo-reservas)
- [Módulo Mesas y comandas](#módulo-mesas-y-comandas)
- [Módulo Producción e inventario](#módulo-producción-e-inventario)
- [Módulo Pagos y caja](#módulo-pagos-y-caja)
- [Módulo Notificaciones](#módulo-notificaciones)
- [Módulo Fidelización](#módulo-fidelización)
- [Índices de rendimiento](#índices-de-rendimiento)
- [Extensiones PostgreSQL](#extensiones-postgresql)
- [Diagrama de relaciones](#diagrama-de-relaciones)

---

## Migraciones Flyway

| Archivo | Entorno | Contenido |
|---|---|---|
| `db/migration/V1__init_schema.sql` | prod + dev | DDL completo: tablas, restricciones, índices, triggers, vistas |
| `db/migration/V2__seed_data.sql` | prod + dev | Datos de catálogo base (categorías, productos, insumos, opciones) |
| `db/migration-dev/V3__dev_data.sql` | dev únicamente | Datos de prueba: usuarios, reservas, visitas, comandas |

**Regla crítica de versiones:** Nunca crear migraciones más allá de V5. `V4` y `V5` están reservadas para seeds de tests de integración.

Resetear base de datos en desarrollo:

```bash
docker compose down -v && docker compose up --build
```

---

## Convenciones del schema

- Schema name: `restaurante`; todas las tablas viven dentro de este schema
- PK surrogate: `BIGSERIAL` para la mayoría de tablas; `SERIAL` para `CategoriaCarta`
- Patrón PK=FK: `Cliente`, `Empleado`, `Mesa` y `Venta` usan `usuario_id` / `visita_id` como PK que también es FK a la tabla padre (patrón `@MapsId` en JPA)
- PK compuesta: `Usuario_Rol` (`usuario_id`, `rol_nombre`), `Receta` (`insumo_id`, `producto_id`), `Decoracion_Zona` (`decoracion_id`, `zona_id`), `producto_opcion_modificacion` (`producto_id`, `opcion_id`), `menu_bebida_disponible` (`producto_menu_id`, `producto_bebida_id`)
- Enumeraciones: implementadas como `VARCHAR` con `CHECK` constraints
- Auditoría: la mayoría de tablas tienen `created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` y `updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP` con trigger `actualizar_updated_at()`. Las tablas `Abono`, `Venta`, `Notificacion`, `Decoracion_Zona`, `comanda_menu_modificacion`, `menu_bebida_disponible`, `producto_opcion_modificacion` y `canje_puntos` solo tienen `created_at`
- FKs con ON DELETE CASCADE en entidades hijo (ej. `Sesion → Usuario`); ON DELETE RESTRICT donde se requiere integridad (ej. `Reserva → Cliente`)

---

## Módulo Auth

### `Usuario`

Tabla central de autenticación; todos los perfiles de usuario (clientes y empleados) referencian esta tabla.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `usuario_id` | `BIGSERIAL` | PK | Identificador único autoincremental |
| `usuario_email` | `VARCHAR(150)` | NOT NULL, UNIQUE | Usado como username en JWT |
| `usuario_password` | `VARCHAR(255)` | NOT NULL | Hash BCrypt (strength 10) |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Fecha de creación del registro |
| `updated_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Actualizado por trigger `trg_usuario_updated_at` |

---

### `Sesion`

Gestiona sesiones activas con tokens JWT de acceso y refresco.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `sesion_id` | `BIGSERIAL` | PK | — |
| `usuario_id` | `BIGINT` | NOT NULL, FK → Usuario.usuario_id (CASCADE) | Propietario de la sesión |
| `sesion_token` | `VARCHAR(1024)` | NOT NULL, UNIQUE | Access token JWT (expira en 30 min) |
| `sesion_refresh_token` | `VARCHAR(1024)` | NOT NULL, UNIQUE | Refresh token JWT (expira en 7 días) |
| `sesion_fecha_creacion` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Momento de emisión del token |
| `sesion_activa` | `BOOLEAN` | NOT NULL, DEFAULT TRUE | Falso tras logout o rotación de token |

---

### `Usuario_Rol`

Tabla de join con estado propio; un usuario puede tener múltiples roles simultáneos.

PK compuesta: (`usuario_id`, `rol_nombre`).

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `usuario_id` | `BIGINT` | PK, NOT NULL, FK → Usuario.usuario_id (CASCADE) | Parte de la PK compuesta |
| `rol_nombre` | `VARCHAR(20)` | PK, NOT NULL, CHECK (`CLIENTE`, `MESERO`, `CAJERO`, `COCINERO`, `BARTENDER`, `ADMIN`) | Parte de la PK compuesta |
| `rol_estado` | `VARCHAR(20)` | NOT NULL, CHECK (`ACTIVO`, `INACTIVO`) | Solo `ACTIVO` otorga permisos en JWT |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | — |
| `updated_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Actualizado por trigger |

---

## Módulo Usuarios

### `Cliente`

Perfil de cliente; PK es FK a `Usuario` (patrón tabla hija compartida).

PK = FK → `Usuario.usuario_id`.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `usuario_id` | `BIGINT` | PK, FK → Usuario.usuario_id (CASCADE) | Mismo ID que el usuario base |
| `cliente_nombre` | `VARCHAR(100)` | NOT NULL | Nombre completo del cliente |
| `cliente_telefono` | `VARCHAR(10)` | NOT NULL, CHECK (`^[0-9]{10}$`) | Solo dígitos, exactamente 10 |
| `cliente_direccion` | `VARCHAR(255)` | — | Opcional |
| `cliente_fecha_nacimiento` | `DATE` | — | Opcional |
| `cliente_puntos` | `INTEGER` | NOT NULL, DEFAULT 0, CHECK (>= 0) | Puntos canjeables actuales; se resetea en cada canje |
| `cliente_puntos_acumulados` | `INTEGER` | NOT NULL, DEFAULT 0, CHECK (>= 0) | Total histórico; nunca disminuye |
| `cliente_acepta_terminos` | `BOOLEAN` | NOT NULL | Debe ser `true` en el registro |
| `cliente_fecha_aceptacion` | `TIMESTAMP` | NOT NULL | Momento en que aceptó términos |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | — |
| `updated_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Actualizado por trigger |

---

### `Empleado`

Perfil de empleado; PK es FK a `Usuario`. El email se obtiene a través del `Usuario` asociado.

PK = FK → `Usuario.usuario_id`.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `usuario_id` | `BIGINT` | PK, FK → Usuario.usuario_id (CASCADE) | Mismo ID que el usuario base |
| `empleado_nombre` | `VARCHAR(100)` | NOT NULL | Nombre completo |
| `empleado_direccion` | `VARCHAR(255)` | — | Opcional |
| `empleado_telefono` | `VARCHAR(10)` | NOT NULL, CHECK (`^[0-9]{10}$`) | Solo dígitos, exactamente 10 |
| `empleado_fecha_ingreso` | `DATE` | NOT NULL | Fecha de vinculación laboral |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | — |
| `updated_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Actualizado por trigger |

---

## Módulo Reservas

### `Zona`

Áreas del restaurante disponibles para asignar a reservas y mesas.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `zona_id` | `BIGSERIAL` | PK | — |
| `zona_nombre` | `VARCHAR(100)` | NOT NULL | Nombre descriptivo del área |
| `zona_capacidad_personas` | `INTEGER` | NOT NULL, CHECK (> 0) | Aforo máximo de la zona |
| `zona_imagen_url` | `VARCHAR(500)` | — | URL de imagen ilustrativa |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | — |
| `updated_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Actualizado por trigger |

---

### `Decoracion`

Opciones de decoración contratables para reservas especiales.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `decoracion_id` | `BIGSERIAL` | PK | — |
| `decoracion_nombre` | `VARCHAR(100)` | NOT NULL | Nombre de la decoración |
| `decoracion_estado` | `VARCHAR(20)` | NOT NULL, CHECK (`ACTIVO`, `INACTIVO`) | Controla disponibilidad para reservas |
| `decoracion_costo_adicional` | `DECIMAL(12,2)` | CHECK (>= 0) | NULL = sin costo; valor >= 0 |
| `decoracion_imagen_url` | `VARCHAR(500)` | — | URL de imagen |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | — |
| `updated_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Actualizado por trigger |

---

### `Decoracion_Zona`

Tabla de join M:N entre decoraciones y las zonas en que están disponibles.

PK compuesta: (`decoracion_id`, `zona_id`).

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `decoracion_id` | `BIGINT` | PK, FK → Decoracion.decoracion_id (CASCADE) | — |
| `zona_id` | `BIGINT` | PK, FK → Zona.zona_id (CASCADE) | — |
| `created_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Sin `updated_at` (tabla de join inmutable) |

---

### `Reserva`

Reservas de clientes; soporta dos tipos con ciclos de vida distintos.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `reserva_id` | `BIGSERIAL` | PK | — |
| `cliente_id` | `BIGINT` | NOT NULL, FK → Cliente.usuario_id (RESTRICT) | Propietario de la reserva |
| `zona_id` | `BIGINT` | FK → Zona.zona_id (SET NULL) | NULL si no se asigna zona |
| `decoracion_id` | `BIGINT` | FK → Decoracion.decoracion_id (SET NULL) | NULL si no hay decoración |
| `reserva_fecha_hora_llegada` | `TIMESTAMP` | NOT NULL | Fecha/hora planificada de llegada |
| `reserva_numero_personas` | `INTEGER` | NOT NULL, CHECK (> 0) | Personas esperadas |
| `reserva_notas` | `TEXT` | — | Notas libres del cliente |
| `reserva_estado` | `VARCHAR(20)` | NOT NULL, CHECK (`PENDIENTE`, `CONFIRMADA`, `ATENDIDA`, `CANCELADA`, `DEVUELTA`, `INASISTENCIA`) | Estado del ciclo de vida |
| `reserva_tipo` | `VARCHAR(20)` | NOT NULL, CHECK (`BASICA`, `ESPECIAL`) | Determina ventana de modificación y flujo |
| `reserva_fecha_creacion` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Momento de creación de la reserva |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | — |
| `updated_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Actualizado por trigger |

---

### `Abono`

Anticipos y devoluciones monetarias asociados a reservas.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `abono_id` | `BIGSERIAL` | PK | — |
| `cajero_id` | `BIGINT` | NOT NULL, FK → Empleado.usuario_id (RESTRICT) | Cajero que registró el abono |
| `reserva_id` | `BIGINT` | NOT NULL, FK → Reserva.reserva_id (RESTRICT) | Reserva a la que aplica |
| `abono_monto` | `DECIMAL(12,2)` | NOT NULL, CHECK (> 0) | Monto siempre positivo; dirección por `abono_tipo` |
| `abono_fecha_hora` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Momento del registro |
| `abono_metodo` | `VARCHAR(20)` | NOT NULL, CHECK (`EFECTIVO`, `TARJETA`, `TRANSFERENCIA`, `OTRO`) | Medio de pago |
| `abono_tipo` | `VARCHAR(20)` | NOT NULL, CHECK (`ANTICIPO`, `DEVOLUCION`) | Dirección del flujo de dinero |
| `created_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Sin `updated_at` (registro inmutable) |

---

### `Bloque_Disponibilidad`

Franjas de tiempo bloqueadas por el administrador para impedir reservas.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `bloque_id` | `BIGSERIAL` | PK | — |
| `bloque_fecha_inicio` | `DATE` | NOT NULL | Primer día del bloqueo (inclusive) |
| `bloque_fecha_fin` | `DATE` | NOT NULL, CHECK (>= `bloque_fecha_inicio`) | Último día del bloqueo (inclusive) |
| `bloque_hora_inicio` | `TIME` | — | NULL = día completo bloqueado |
| `bloque_hora_fin` | `TIME` | — | NULL = día completo bloqueado |
| `bloque_motivo` | `VARCHAR(255)` | — | Descripción del motivo |
| `admin_id` | `BIGINT` | NOT NULL, FK → Empleado.usuario_id (RESTRICT) | Debe tener rol ADMIN activo (validado por trigger `trg_bloque_admin_check`) |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | — |
| `updated_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Actualizado por trigger |

---

## Módulo Mesas y comandas

### `Visita`

Registro de cada visita al restaurante; puede estar vinculada a una reserva o ser walk-in.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `visita_id` | `BIGSERIAL` | PK | — |
| `cliente_id` | `BIGINT` | FK → Cliente.usuario_id (SET NULL) | Auto-poblado desde `reserva.cliente_id` si hay reserva (trigger `trg_visita_cliente_consistency`) |
| `reserva_id` | `BIGINT` | FK → Reserva.reserva_id (SET NULL) | NULL para walk-ins |
| `visita_fecha_hora_inicio` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Momento de apertura de la visita |
| `visita_fecha_hora_fin` | `TIMESTAMP` | CHECK (>= `visita_fecha_hora_inicio`) | NULL mientras la visita está activa |

---

### `Mesa`

Asignación de mesa y mesero para una visita; PK es FK a `Visita`.

PK = FK → `Visita.visita_id`.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `visita_id` | `BIGINT` | PK, FK → Visita.visita_id (CASCADE) | Una mesa por visita |
| `zona_id` | `BIGINT` | NOT NULL, FK → Zona.zona_id (RESTRICT) | Zona asignada |
| `mesero_id` | `BIGINT` | NOT NULL, FK → Empleado.usuario_id (RESTRICT) | Mesero responsable |
| `mesa_identificador` | `VARCHAR(20)` | NOT NULL | Etiqueta visible (ej. "M-01", "Terraza-3") |
| `mesa_numero_personas` | `INTEGER` | NOT NULL, CHECK (> 0) | Personas presentes en la visita |
| `mesa_estado` | `VARCHAR(20)` | NOT NULL, CHECK (`ESPERA`, `EN_PREPARACION`, `ATENDIDA`, `CERRADA`) | Estado operativo de la mesa |
| `mesa_notas` | `TEXT` | NULL | Notas del mesero sobre la mesa |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | — |
| `updated_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Actualizado por trigger |

---

### `Comanda`

Orden de producción dirigida a cocina o barra; también sirve como pre-orden de reserva en estado `PRE_RESERVA`.

Al menos uno de `visita_id` o `reserva_id` debe estar presente (CHECK constraint `chk_comanda_contexto`).

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `comanda_id` | `BIGSERIAL` | PK | — |
| `visita_id` | `BIGINT` | FK → Visita.visita_id (CASCADE) | NULL mientras estado = `PRE_RESERVA` |
| `reserva_id` | `BIGINT` | FK → Reserva.reserva_id (CASCADE) | NULL en comandas de walk-in |
| `comanda_estacion` | `VARCHAR(20)` | NOT NULL, CHECK (`COCINA`, `BARRA`) | Destino de producción |
| `comanda_fecha_hora_inicio` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Momento de creación |
| `comanda_fecha_hora_listo` | `TIMESTAMP` | CHECK (>= `comanda_fecha_hora_inicio`) | NULL hasta alcanzar estado `LISTO` |
| `comanda_notas` | `TEXT` | — | Notas libres para la estación |
| `comanda_estado` | `VARCHAR(20)` | NOT NULL, CHECK (`PRE_RESERVA`, `BORRADOR`, `PENDIENTE`, `EN_PREPARACION`, `LISTO`, `COMPLETADO`) | Estado en el ciclo de producción |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | — |
| `updated_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Actualizado por trigger |

---

### `Comanda_Item`

Línea de producto dentro de una comanda, activa o pre-orden.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `comanda_item_id` | `BIGSERIAL` | PK | — |
| `comanda_id` | `BIGINT` | NOT NULL, FK → Comanda.comanda_id (CASCADE) | Comanda a la que pertenece |
| `producto_id` | `BIGINT` | NOT NULL, FK → Producto.producto_id (RESTRICT) | Producto pedido |
| `comanda_item_cantidad` | `INTEGER` | NOT NULL, CHECK (> 0) | Cantidad de unidades |
| `comanda_item_precio` | `DECIMAL(12,2)` | NOT NULL, CHECK (>= 0) | Precio unitario en el momento del pedido |
| `comanda_item_descripcion` | `VARCHAR(500)` | — | Instrucción libre del cliente |
| `comanda_item_menu_grupo` | `VARCHAR(36)` | — | UUID que agrupa items COCINA+BARRA del mismo menú especial; NULL si no es menú |
| `created_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Sin `updated_at` |

---

### `Comanda_Menu_Modificacion`

Opciones de modificación de menú especial seleccionadas para un ítem de comanda.

No extiende `AuditableEntity`; solo tiene `created_at` (tabla de asociación inmutable, sin trigger de update).

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | — |
| `comanda_item_id` | `BIGINT` | NOT NULL, FK → Comanda_Item.comanda_item_id (CASCADE) | Ítem al que aplica la modificación |
| `opcion_id` | `BIGINT` | NOT NULL, FK → opcion_modificacion.opcion_id (RESTRICT) | Opción seleccionada |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Sin `updated_at` ni trigger |

---

## Módulo Producción e inventario

### `CategoriaCarta`

Categorías de agrupación para la carta del restaurante; controlan el orden de presentación.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `categoriacarta_id` | `SERIAL` | PK | `SERIAL` (INT), no `BIGSERIAL` |
| `categoria_nombre` | `VARCHAR(100)` | NOT NULL, UNIQUE | Nombre de la categoría |
| `orden` | `INTEGER` | NOT NULL, DEFAULT 0 | Posición en la carta (ascendente) |
| `activo` | `BOOLEAN` | NOT NULL, DEFAULT TRUE | Controla visibilidad en la carta |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | — |
| `updated_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Actualizado por trigger |

---

### `Producto`

Catálogo de productos: platos y bebidas, sean de venta directa o de preparación (menú especial).

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `producto_id` | `BIGSERIAL` | PK | — |
| `categoriacarta_id` | `INTEGER` | NOT NULL, FK → CategoriaCarta.categoriacarta_id (RESTRICT) | Categoría en la carta |
| `producto_nombre` | `VARCHAR(100)` | NOT NULL | Nombre visible al cliente |
| `producto_descripcion` | `VARCHAR(500)` | — | Descripción opcional |
| `producto_estado` | `VARCHAR(20)` | NOT NULL, CHECK (`ACTIVO`, `INACTIVO`) | Controla si aparece en la carta |
| `producto_precio` | `DECIMAL(12,2)` | NOT NULL, CHECK (>= 0) | Precio de venta |
| `producto_tipo` | `VARCHAR(20)` | NOT NULL, CHECK (`VENTA_DIRECTA`, `PREPARACION`) | Determina si descuenta stock o insumos |
| `producto_categoria` | `VARCHAR(20)` | NOT NULL, CHECK (`PLATO`, `BEBIDA`) | Categoría de producto |
| `menu_especial` | `BOOLEAN` | CHECK (NULL o `producto_tipo = 'PREPARACION'`) | Solo válido para tipo `PREPARACION` |
| `stock_actual` | `DECIMAL(12,3)` | CHECK (NULL o >= 0); NOT NULL si `producto_tipo = 'VENTA_DIRECTA'` | Stock para productos de venta directa |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | — |
| `updated_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Actualizado por trigger |

---

### `Insumo`

Materias primas y semielaborados usados en las recetas de producción.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `insumo_id` | `BIGSERIAL` | PK | — |
| `insumo_nombre` | `VARCHAR(100)` | NOT NULL | Nombre del insumo |
| `insumo_unidad` | `VARCHAR(20)` | NOT NULL, CHECK (`KG`, `G`, `L`, `ML`, `UNIDAD`, `DOCENA`, `OTRO`) | Unidad de medida del stock |
| `insumo_stock_actual` | `DECIMAL(12,3)` | NOT NULL, DEFAULT 0, CHECK (>= 0) | Stock disponible en bodega |
| `insumo_estado` | `VARCHAR(20)` | NOT NULL, CHECK (`ACTIVO`, `INACTIVO`) | Estado del insumo |
| `tipo_insumo` | `VARCHAR(20)` | NOT NULL, DEFAULT `'MATERIA_PRIMA'`, CHECK (`MATERIA_PRIMA`, `SEMIELABORADO`) | Clasificación para la receta |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | — |
| `updated_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Actualizado por trigger |

---

### `Receta`

Relación M:N entre insumos y productos de tipo `PREPARACION`; define las cantidades por receta.

PK compuesta: (`insumo_id`, `producto_id`).

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `insumo_id` | `BIGINT` | PK, FK → Insumo.insumo_id (CASCADE) | — |
| `producto_id` | `BIGINT` | PK, FK → Producto.producto_id (CASCADE) | — |
| `receta_cantidad` | `DECIMAL(12,3)` | NOT NULL, CHECK (> 0) | Cantidad del insumo requerida por unidad producida |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | — |
| `updated_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Actualizado por trigger |

---

### `Movimiento_Inventario`

Historial de ingresos y egresos de stock; exactamente uno de `producto_id` o `insumo_id` debe tener valor.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `movimiento_id` | `BIGSERIAL` | PK | — |
| `empleado_id` | `BIGINT` | NOT NULL, FK → Empleado.usuario_id (RESTRICT) | Empleado que registró el movimiento |
| `producto_id` | `BIGINT` | FK → Producto.producto_id (SET NULL) | Mutuamente excluyente con `insumo_id` |
| `insumo_id` | `BIGINT` | FK → Insumo.insumo_id (SET NULL) | Mutuamente excluyente con `producto_id` |
| `movimiento_cantidad` | `DECIMAL(12,3)` | NOT NULL, CHECK (> 0) | Siempre positivo; dirección por `movimiento_tipo` |
| `movimiento_tipo` | `VARCHAR(20)` | NOT NULL, CHECK (`INGRESO`, `EGRESO`) | Dirección del movimiento |
| `movimiento_proveedor` | `VARCHAR(150)` | — | Proveedor (ingresos de compra) |
| `movimiento_numero_factura` | `VARCHAR(150)` | — | Número de factura del proveedor |
| `movimiento_observaciones` | `TEXT` | — | Notas adicionales |
| `movimiento_fecha_hora` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Momento del movimiento |
| `created_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Sin `updated_at` (registro inmutable) |

---

### `Opcion_Modificacion`

Opciones predefinidas de modificación por componente para menús especiales.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `opcion_id` | `BIGSERIAL` | PK | — |
| `tipo_componente` | `VARCHAR(30)` | NOT NULL | Categoría del componente modificable (ej. `PROTEINA`, `SALSA`, `ACOMPAÑAMIENTO`) |
| `opcion_nombre` | `VARCHAR(150)` | NOT NULL | Nombre de la opción específica |
| `opcion_estado` | `VARCHAR(20)` | NOT NULL, DEFAULT `'ACTIVO'`, CHECK (`ACTIVO`, `INACTIVO`) | Controla disponibilidad |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | — |
| `updated_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Actualizado por trigger |

---

### `Producto_Opcion_Modificacion`

Tabla de join M:N que define qué opciones de modificación ofrece cada menú especial.

PK compuesta: (`producto_id`, `opcion_id`).

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `producto_id` | `BIGINT` | PK, FK → Producto.producto_id (CASCADE) | Debe ser un producto con `menu_especial = true` |
| `opcion_id` | `BIGINT` | PK, FK → opcion_modificacion.opcion_id (CASCADE) | — |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Sin `updated_at` |

---

### `Menu_Bebida_Disponible`

Tabla M:N que define qué bebidas puede llevar cada menú especial.

PK compuesta: (`producto_menu_id`, `producto_bebida_id`).

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `producto_menu_id` | `BIGINT` | PK, FK → Producto.producto_id (CASCADE) | Producto con `menu_especial = true` |
| `producto_bebida_id` | `BIGINT` | PK, FK → Producto.producto_id (RESTRICT) | Producto con `producto_categoria = 'BEBIDA'` |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Sin `updated_at` |

---

## Módulo Pagos y caja

### `Venta`

Registro de cierre de venta por visita; PK es FK a `Visita` (una venta por visita).

PK = FK → `Visita.visita_id`.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `visita_id` | `BIGINT` | PK, FK → Visita.visita_id (RESTRICT) | Vincula la venta a su visita |
| `cajero_id` | `BIGINT` | NOT NULL, FK → Empleado.usuario_id (RESTRICT) | Cajero que procesó el cobro |
| `venta_fecha_hora` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Momento del cobro |
| `venta_subtotal` | `DECIMAL(12,2)` | NOT NULL, CHECK (>= 0) | Suma de items antes de descuento |
| `venta_descuento` | `DECIMAL(12,2)` | NOT NULL, DEFAULT 0, CHECK (>= 0) | Descuento aplicado (ej. canje de puntos) |
| `venta_total` | `DECIMAL(12,2)` | NOT NULL, CHECK (>= 0) | Monto cobrado al cliente |
| `venta_metodo` | `VARCHAR(20)` | NOT NULL, CHECK (`EFECTIVO`, `TRANSFERENCIA`, `TARJETA`, `OTRO`) | Medio de pago |
| `created_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Sin `updated_at` (registro inmutable) |

---

## Módulo Notificaciones

### `Notificacion`

Alertas en tiempo real generadas durante el servicio de mesa.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `notificacion_id` | `BIGSERIAL` | PK | — |
| `mesa_id` | `BIGINT` | NOT NULL, FK → Mesa.visita_id (CASCADE) | Mesa que originó la notificación |
| `empleado_id` | `BIGINT` | NULL, FK → Empleado.usuario_id (RESTRICT) | NULL mientras la notificación está `ACTIVA`; se puebla al atenderla |
| `comanda_id` | `BIGINT` | NULL, FK → Comanda.comanda_id (CASCADE) | Presente solo en tipos `PLATOS_LISTOS` y `BEBIDAS_LISTAS` |
| `notificacion_estado` | `VARCHAR(20)` | NOT NULL, CHECK (`ACTIVA`, `ATENDIDA`) | Estado del ciclo de vida |
| `notificacion_tipo` | `VARCHAR(20)` | NOT NULL, CHECK (`ATENCION`, `PLATOS_LISTOS`, `BEBIDAS_LISTAS`, `CAMBIO`) | Categoría de la alerta |
| `notificacion_fecha_hora` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Momento de generación |
| `created_at` | `TIMESTAMP` | DEFAULT CURRENT_TIMESTAMP | Sin `updated_at` |

---

## Módulo Fidelización

### `Canje_Puntos`

Auditoría de canjes del programa de lealtad; registra los puntos canjeados en el momento del evento.

| Columna | Tipo | Restricciones | Notas |
|---|---|---|---|
| `canje_id` | `BIGSERIAL` | PK | — |
| `cliente_id` | `BIGINT` | NOT NULL, FK → Cliente.usuario_id (RESTRICT) | Cliente que realizó el canje |
| `empleado_id` | `BIGINT` | NOT NULL, FK → Empleado.usuario_id (RESTRICT) | Empleado que procesó el canje |
| `canje_puntos_canjeados` | `INTEGER` | NOT NULL, CHECK (> 0) | Puntos canjeados |
| `canje_fecha_hora` | `TIMESTAMP` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Momento del canje |

---

## Índices de rendimiento

Solo índices explícitos definidos en `V1__init_schema.sql`.

| Índice | Tabla | Propósito |
|---|---|---|
| `idx_usuario_email` | `Usuario` | Búsqueda de usuario por email en login |
| `idx_sesion_usuario_id` | `Sesion` | Sesiones por usuario |
| `idx_sesion_token` | `Sesion` | Validación de access token en cada request |
| `idx_sesion_refresh_token` | `Sesion` | Renovación de token (parcial: WHERE NOT NULL) |
| `idx_sesion_activa` | `Sesion` | Filtro de sesiones activas (parcial: WHERE activa = TRUE) |
| `idx_sesion_fecha_creacion` | `Sesion` | Limpieza de sesiones antiguas |
| `idx_cliente_telefono` | `Cliente` | Búsqueda por teléfono |
| `idx_cliente_puntos` | `Cliente` | Ranking de puntos (DESC) |
| `idx_empleado_telefono` | `Empleado` | Búsqueda por teléfono |
| `idx_empleado_fecha_ingreso` | `Empleado` | Reportes de antigüedad |
| `idx_usuario_rol_rol_nombre` | `Usuario_Rol` | Búsqueda de usuarios por rol |
| `idx_usuario_rol_estado` | `Usuario_Rol` | Filtro por estado de rol |
| `idx_usuario_rol_activo` | `Usuario_Rol` | Roles activos por usuario (parcial: WHERE estado = 'ACTIVO') |
| `idx_reserva_cliente_id` | `Reserva` | Reservas por cliente |
| `idx_reserva_fecha_hora_llegada` | `Reserva` | Reservas por fecha |
| `idx_reserva_estado` | `Reserva` | Filtro por estado |
| `idx_reserva_fecha_creacion` | `Reserva` | Orden cronológico de creación (DESC) |
| `idx_reserva_zona_id` | `Reserva` | Ocupación de zona |
| `idx_reserva_fecha_estado` | `Reserva` | Consultas de disponibilidad (fecha + estado) |
| `idx_reserva_activas_hoy` | `Reserva` | Reservas pendientes/confirmadas (parcial) |
| `idx_abono_reserva_id` | `Abono` | Abonos por reserva |
| `idx_abono_cajero_id` | `Abono` | Abonos registrados por cajero |
| `idx_abono_fecha_hora` | `Abono` | Orden cronológico (DESC) |
| `idx_visita_cliente_id` | `Visita` | Historial de visitas por cliente |
| `idx_visita_reserva_id` | `Visita` | Visita originada por reserva |
| `idx_visita_fecha_inicio` | `Visita` | Orden cronológico (DESC) |
| `idx_visita_fecha_fin` | `Visita` | Visitas por fecha de cierre |
| `idx_visita_activas` | `Visita` | Visitas sin cierre (parcial: WHERE fin IS NULL) |
| `idx_mesa_mesero_id` | `Mesa` | Mesas atendidas por mesero |
| `idx_mesa_zona_id` | `Mesa` | Mesas por zona |
| `idx_mesa_estado` | `Mesa` | Filtro por estado operativo |
| `idx_mesa_identificador` | `Mesa` | Búsqueda por etiqueta |
| `idx_mesa_activas` | `Mesa` | Mesas en servicio (parcial: WHERE estado IN ESPERA/EN_PREPARACION/ATENDIDA) |
| `idx_notificacion_mesa_id` | `Notificacion` | Notificaciones por mesa |
| `idx_notificacion_empleado_id` | `Notificacion` | Notificaciones atendidas por empleado |
| `idx_notificacion_estado` | `Notificacion` | Filtro por estado |
| `idx_notificacion_fecha_hora` | `Notificacion` | Orden cronológico (DESC) |
| `idx_notificacion_comanda_id` | `Notificacion` | Notificaciones asociadas a comanda |
| `idx_venta_cajero_id` | `Venta` | Ventas por cajero |
| `idx_venta_fecha_hora` | `Venta` | Orden cronológico (DESC) |
| `idx_venta_metodo` | `Venta` | Análisis por método de pago |
| `idx_venta_fecha_total` | `Venta` | Reportes de ingresos (fecha DESC + total) |
| `idx_producto_categoria_id` | `Producto` | Productos por categoría de carta |
| `idx_producto_estado` | `Producto` | Filtro por estado |
| `idx_producto_tipo` | `Producto` | Filtro por tipo |
| `idx_producto_categoria` | `Producto` | Filtro por categoría (PLATO/BEBIDA) |
| `idx_producto_nombre` | `Producto` | Búsqueda por nombre |
| `idx_producto_activos` | `Producto` | Productos activos por categoría (parcial: WHERE estado = 'ACTIVO') |
| `idx_insumo_nombre` | `Insumo` | Búsqueda por nombre |
| `idx_insumo_estado` | `Insumo` | Filtro por estado |
| `idx_insumo_stock` | `Insumo` | Stock actual (consultas de bajo stock) |
| `idx_insumo_tipo` | `Insumo` | Filtro por tipo de insumo |
| `idx_receta_producto_id` | `Receta` | Receta por producto |
| `idx_movimiento_empleado_id` | `Movimiento_Inventario` | Movimientos por empleado |
| `idx_movimiento_producto_id` | `Movimiento_Inventario` | Movimientos por producto |
| `idx_movimiento_insumo_id` | `Movimiento_Inventario` | Movimientos por insumo |
| `idx_movimiento_fecha_hora` | `Movimiento_Inventario` | Orden cronológico (DESC) |
| `idx_movimiento_tipo` | `Movimiento_Inventario` | Filtro por tipo (INGRESO/EGRESO) |
| `idx_movimiento_fecha_tipo` | `Movimiento_Inventario` | Reportes por tipo y fecha |
| `idx_comanda_visita_id` | `Comanda` | Comandas de una visita (parcial: WHERE visita_id IS NOT NULL) |
| `idx_comanda_reserva_id` | `Comanda` | Comandas de una reserva (parcial: WHERE reserva_id IS NOT NULL) |
| `idx_comanda_estacion` | `Comanda` | Comandas por estación de producción |
| `idx_comanda_estado` | `Comanda` | Filtro por estado |
| `idx_comanda_fecha_inicio` | `Comanda` | Orden cronológico (DESC) |
| `idx_comanda_pendientes` | `Comanda` | Tablero de producción (parcial: WHERE estado IN PENDIENTE/EN_PREPARACION) |
| `idx_comanda_prereserva` | `Comanda` | Pre-órdenes por reserva (parcial: WHERE estado = 'PRE_RESERVA') |
| `idx_comanda_item_comanda_id` | `Comanda_Item` | Items de una comanda |
| `idx_comanda_item_producto_id` | `Comanda_Item` | Items por producto |
| `idx_comanda_item_menu_grupo` | `Comanda_Item` | Agrupación de menú especial (parcial: WHERE menu_grupo IS NOT NULL) |
| `idx_opcion_tipo_componente` | `opcion_modificacion` | Opciones por tipo de componente |
| `idx_opcion_estado` | `opcion_modificacion` | Filtro por estado |
| `idx_prod_opcion_producto_id` | `producto_opcion_modificacion` | Opciones de un producto |
| `idx_cmd_menu_mod_detalle` | `comanda_menu_modificacion` | Modificaciones de un ítem |
| `idx_cmd_menu_mod_opcion` | `comanda_menu_modificacion` | Items que usan una opción |
| `idx_menu_bebida_menu` | `menu_bebida_disponible` | Bebidas disponibles de un menú |
| `idx_menu_bebida_bebida` | `menu_bebida_disponible` | Menús que incluyen una bebida |
| `idx_canje_cliente_id` | `canje_puntos` | Canjes por cliente |
| `idx_canje_empleado_id` | `canje_puntos` | Canjes procesados por empleado |
| `idx_canje_fecha_hora` | `canje_puntos` | Orden cronológico (DESC) |
| `idx_bloque_fecha_inicio` | `Bloque_Disponibilidad` | Búsqueda de bloqueos por fecha de inicio |
| `idx_bloque_fecha_fin` | `Bloque_Disponibilidad` | Búsqueda de bloqueos por fecha de fin |

---

## Extensiones PostgreSQL

- `uuid-ossp` — generación de UUIDs (usados como valor de `comanda_item_menu_grupo`)
- `pgcrypto` — funciones criptográficas de bajo nivel
- `unaccent` — búsqueda de texto sin distinción de acentos (consultas de productos por nombre)

---

## Diagrama de relaciones

```text
-- Auth y usuarios
Usuario (usuario_id) 1:N Sesion (usuario_id)
Usuario (usuario_id) 1:N Usuario_Rol (usuario_id)
Usuario (usuario_id) 1:1 Cliente (usuario_id)
Usuario (usuario_id) 1:1 Empleado (usuario_id)

-- Reservas
Cliente (usuario_id) 1:N Reserva (cliente_id)
Zona (zona_id) 1:N Reserva (zona_id)
Decoracion (decoracion_id) 1:N Reserva (decoracion_id)
Decoracion (decoracion_id) M:N Zona (zona_id) via Decoracion_Zona
Reserva (reserva_id) 1:N Abono (reserva_id)
Empleado (usuario_id) 1:N Abono (cajero_id)
Empleado (usuario_id) 1:N Bloque_Disponibilidad (admin_id)

-- Visitas y mesas
Cliente (usuario_id) 1:N Visita (cliente_id)
Reserva (reserva_id) 1:1 Visita (reserva_id)
Visita (visita_id) 1:1 Mesa (visita_id)
Zona (zona_id) 1:N Mesa (zona_id)
Empleado (usuario_id) 1:N Mesa (mesero_id)

-- Comandas
Visita (visita_id) 1:N Comanda (visita_id)
Reserva (reserva_id) 1:N Comanda (reserva_id)
Comanda (comanda_id) 1:N Comanda_Item (comanda_id)
Producto (producto_id) 1:N Comanda_Item (producto_id)
Comanda_Item (comanda_item_id) 1:N Comanda_Menu_Modificacion (comanda_item_id)
opcion_modificacion (opcion_id) 1:N Comanda_Menu_Modificacion (opcion_id)
Mesa (visita_id) 1:N Notificacion (mesa_id)
Comanda (comanda_id) 1:N Notificacion (comanda_id)
Empleado (usuario_id) 1:N Notificacion (empleado_id)

-- Pagos
Visita (visita_id) 1:1 Venta (visita_id)
Empleado (usuario_id) 1:N Venta (cajero_id)

-- Producción e inventario
CategoriaCarta (categoriacarta_id) 1:N Producto (categoriacarta_id)
Insumo (insumo_id) M:N Producto (producto_id) via Receta
Empleado (usuario_id) 1:N Movimiento_Inventario (empleado_id)
Producto (producto_id) 1:N Movimiento_Inventario (producto_id)
Insumo (insumo_id) 1:N Movimiento_Inventario (insumo_id)
Producto (producto_id) M:N opcion_modificacion (opcion_id) via producto_opcion_modificacion
Producto (producto_menu_id) M:N Producto (producto_bebida_id) via menu_bebida_disponible

-- Fidelización
Cliente (usuario_id) 1:N canje_puntos (cliente_id)
Empleado (usuario_id) 1:N canje_puntos (empleado_id)
```
