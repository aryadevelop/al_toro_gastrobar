# Colección de Pruebas Manuales - Al Toro Gastrobar

Esta colección está diseñada para **exploración manual** del backend, sin assertions automáticas.

## Cómo usar

### 1. Configurar ambiente

Asegurarse de que el ambiente `Al Toro – Local.environment.yaml` tenga:

```yaml
baseUrl: http://localhost:8080
emailCliente: carlos.perez@gmail.com
emailMesero: juan.gomez@gmail.com
emailCajero: sofia.lopez@gmail.com
emailAdmin: admin@altoro.com
passwordValida: <tu-password-seed>
```

### 2. Ejecutar servidor backend

```bash
cd backend
./mvnw spring-boot:run
```

### 3. Abrir Postman for VS Code

1. Instalar extensión "Postman for VS Code"
2. Abrir carpeta `backend/postman`
3. Seleccionar ambiente "Al Toro – Local"
4. Navegar a colección "Al Toro - Manual Testing"

### 4. Ejecutar requests

Cada request tiene login automático en `beforeRequest`, por lo que:
- ✅ Se pueden ejecutar de forma aislada
- ✅ No requieren ejecutar login primero
- ✅ Tokens se refrescan automáticamente

**Ejemplo de flujo manual:**

1. Ejecutar `00-01 Login CLIENTE` → Verificar que retorna 200 y guarda token
2. Ejecutar `10-01 Estado visita activa – CLIENTE` → Ver items, total, asistencia
3. Ejecutar `10-02 Detalle visita – CLIENTE` → Verificar agrupación de items
4. Ejecutar `20-01 Consulta reservas – MESERO` → Ver reservas del día

## Requests disponibles

### 00 - Autenticación
- `00-01 Login CLIENTE` — Guardar token manualmente
- `00-02 Login MESERO`
- `00-03 Login CAJERO`
- `00-04 Login ADMIN`

### 10 - Visitas
- `10-01 Estado visita activa – CLIENTE` — Estado en tiempo real
- `10-02 Detalle visita – CLIENTE` — Items agrupados

### 20 - Reservas (Mesero)
- `20-01 Consulta reservas – MESERO` — Reservas del día
