# TODO — HU Admin: Subir imágenes de decoraciones y zonas

> **Estado:** Infraestructura lista. Falta el endpoint, servicio, tests y Postman.
> **Prerequisito aprobado:** `StorageService` + `LocalDiskStorageService` + volumen Docker ya implementados.

---

## Contexto

El ADMIN debe poder subir fotos propias para decoraciones y zonas.
Las imágenes del seed (V2) viven en el JAR (`/images/`). Las subidas del ADMIN
van a `/uploads/` (volumen Docker). Ambos prefijos coexisten sin conflicto.

Hoy las entidades ya tienen el campo imagen:
- `Decoracion.decoracionImagenUrl` — URL pública (`/images/…` o `/uploads/…`)
- `Zona.zonaImagenUrl` — ídem

---

## Endpoints a implementar

### 1. Subir / reemplazar imagen de una decoración

```
PATCH /api/admin/decoraciones/{id}/imagen
Content-Type: multipart/form-data
Body: file (imagen JPEG / PNG / WEBP, máx 5 MB)
Roles: ADMIN
```

Respuesta `200 OK`:
```json
{
  "status": "success",
  "data": {
    "decoracionId": 1,
    "decoracionImagenUrl": "/uploads/decoraciones/a1b2c3.jpg"
  }
}
```

### 2. Subir / reemplazar imagen de una zona

```
PATCH /api/admin/zonas/{id}/imagen
Content-Type: multipart/form-data
Body: file (imagen JPEG / PNG / WEBP, máx 5 MB)
Roles: ADMIN
```

---

## Lógica de implementación

### Controller
```java
@PatchMapping("/{id}/imagen")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<DecoracionImagenResponse>> subirImagen(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file) { ... }
```

### Service
1. Verificar que la entidad existe (lanzar `ResourceNotFoundException` si no).
2. Si ya tiene imagen en `/uploads/`, llamar `storageService.delete(urlAnterior)`.
   - No borrar si la URL es `/images/…` (seed inmutable).
3. Llamar `storageService.store(file, "decoraciones")` — devuelve la nueva URL.
4. Actualizar el campo `decoracionImagenUrl` y guardar.
5. Devolver DTO con la nueva URL.

### DTOs nuevos
```java
// Response
record DecoracionImagenResponse(Long decoracionId, String decoracionImagenUrl) {}
record ZonaImagenResponse(Long zonaId, String zonaImagenUrl) {}
```

---

## Validaciones

| Regla | Dónde |
|-------|-------|
| Archivo no nulo / no vacío | `LocalDiskStorageService.store()` — ya implementado |
| Tipos permitidos: JPEG, PNG, WEBP | `LocalDiskStorageService.store()` — ya implementado |
| Tamaño máximo 5 MB | `application.yml spring.servlet.multipart.max-file-size` — ya configurado |
| Entidad existente | Service (antes de llamar storage) |
| No borrar imágenes `/images/…` del seed | Service (condición antes de `delete`) |

---

## Tests a escribir (TDD — escribir ANTES de implementar)

### `LocalDiskStorageServiceTest`
- `store_archivoJpeg_devuelveUrlCorrecta()`
- `store_archivoPng_devuelveUrlCorrecta()`
- `store_tipoNoPermitido_lanzaStorageException()`
- `store_archivoVacio_lanzaStorageException()`
- `delete_urlValida_eliminaArchivo()`
- `delete_urlSeed_noHaceNada()` (`/images/…` no se toca)
- `delete_urlNula_noLanzaExcepcion()`

### `DecoracionServiceTest` (métodos nuevos)
- `subirImagen_decoracionExiste_actualizaUrl()`
- `subirImagen_decoracionNoExiste_lanzaResourceNotFound()`
- `subirImagen_imagenPreviaEnUploads_eliminaAnterior()`
- `subirImagen_imagenPreviaEnImages_noEliminaAnterior()`

### `DecoracionControllerTest` (método nuevo)
- `patchImagen_adminAutorizado_devuelve200()`
- `patchImagen_sinRolAdmin_devuelve403()`
- `patchImagen_archivoInvalido_devuelve400()`

---

## Postman

### Manual
- `PATCH /api/admin/decoraciones/1/imagen` — body `form-data`, key `file`, value = foto real
- Verificar que la URL devuelta es `/uploads/decoraciones/{uuid}.jpg`
- Verificar que `GET /uploads/decoraciones/{uuid}.jpg` devuelve la imagen (200)
- Reemplazar imagen: llamar de nuevo y verificar que la URL anterior ya no responde (404)

### Automatizado
- Happy path: subir JPEG → `pm.response.to.have.status(200)`, URL empieza con `/uploads/`
- Tipo inválido (PDF): `pm.response.to.have.status(400)`
- Sin autenticación: `pm.response.to.have.status(401)`
- Con rol MESERO: `pm.response.to.have.status(403)`
- Decoración inexistente (id=9999): `pm.response.to.have.status(404)`

---

## Notas de arquitectura

- `StorageService` es un bean `@Service`. Para migrar a Cloudflare R2: crear
  `R2StorageService implements StorageService`, marcarlo `@Primary` y retirar
  `@Primary` (o el perfil) de `LocalDiskStorageService`. Sin cambios en controllers ni services.
- El volumen `altoro_uploads` en `docker-compose.prod.yml` ya está declarado.
- La ruta `/uploads/**` ya está registrada en Caddy y en `StorageConfig`.
