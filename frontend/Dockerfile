# ============================================================
# STAGE 1: BUILD
# ============================================================
FROM node:20-alpine AS builder
WORKDIR /app

# Cachear dependencias primero
COPY package.json package-lock.json ./
RUN npm ci

# Compilar el SPA en modo producción
COPY . .
RUN npm run build

# ============================================================
# STAGE 2: RUNTIME (nginx sirviendo estáticos)
# ============================================================
FROM nginx:1.27-alpine

# Config con fallback de SPA
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiar SOLO los estáticos del build (builder 'application' => subcarpeta browser)
COPY --from=builder /app/dist/al-toro-gastrobar-frontend/browser /usr/share/nginx/html

EXPOSE 80
