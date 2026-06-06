# Despliegue y Puesta en Producción

El Frontend no utiliza el servidor de desarrollo nativo de Angular (`ng serve`) en producción. Se compila estáticamente y se sirve a través de un servidor web ligero, contenido en una imagen Docker.

## Arquitectura de Contenedores

La aplicación utiliza un patrón de construcción Docker **Multi-Stage**:

1. **Stage 1 (Builder):** Un contenedor `node:20-alpine` ejecuta la instalación de dependencias limpios (`npm ci`) y compila la aplicación (`npm run build`). Este paso puede tardar más, pero se garantiza que las pesadas carpetas locales como `node_modules/` nunca lleguen a la imagen final.
2. **Stage 2 (Runtime):** Un contenedor `nginx:1.27-alpine` se crea copiando únicamente la carpeta compilada `/browser` que resultó del *Stage 1*. Este será el contenedor que se levantará en los ambientes productivos.

## Nginx y Fallback SPA

Dado que Angular es una Single Page Application (SPA), el navegador solo carga `index.html`. Si un usuario navega a la URL interna `/mesas/2` y luego **recarga el navegador (F5)**, el servidor `nginx` arrojará un error `404 Not Found` porque no existe un archivo real llamado `2` en la ruta `/mesas/`.

Para arreglar esto, nuestro `nginx.conf` implementa la directiva mágica de fallback:

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```
Esto le dice a nginx: *"Si no encuentras el archivo físico que el usuario pidió, sírvele el index.html y deja que el enrutador de Angular decida qué vista mostrar internamente"*.

## Compresión y Caché
Nginx también está configurado para habilitar la compresión `GZIP` (`gzip on;`), ahorrando ancho de banda. 
Además, Angular emite archivos hasheados (`main-GZW42HHM.js`). Nginx instruye a los navegadores web a cachear estos archivos estáticos inmutables por 1 año de forma agresiva.

## Integración con Caddy

Este contenedor **no** se expone directamente a Internet, ni se encarga de los certificados TLS (HTTPS) ni de los CORS.
Para eso, existe un contenedor Reverse Proxy maestro llamado **Caddy** a nivel repositorio, el cual redirige las peticiones entrantes:
- `/api/*` y `/ws/*` van al contenedor Backend Java.
- `/*` (cualquier otra cosa) van hacia el contenedor Frontend Nginx.
